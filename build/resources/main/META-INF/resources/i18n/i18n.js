const CHAVE_IDIOMA = 'kronos.idioma.interface';
const IDIOMAS = Object.freeze(['pt-BR', 'en-US', 'es-ES']);
const CODIGOS_GOOGLE = Object.freeze({ 'pt-BR': 'pt', 'en-US': 'en', 'es-ES': 'es' });
const SELETORES_PROTEGIDOS = Object.freeze([
    'script', 'style', 'code', 'pre', 'input', 'select', 'option', 'textarea', 'table',
    '[contenteditable]', '[translate="no"]', '[data-i18n-ignore]',
    '.notranslate', '.material-symbols-outlined', '.console-body', '.auditor-evento-texto',
    '.auditor-anomalias-lista', '.markdown-body', '.mapa-code', '.mapa-tree',
    '.anime-meta-banner', '.telemetria-log', '#panel-telemetria', '#toast-container',
    '[id*="resultado"]', '[id*="relatorio"]', '[id*="historico"]', '[id*="preview"]',
    '[class*="resultado"]', '[class*="relatorio"]'
]);

let idiomaAtual = 'pt-BR';
let observadorProtecao = null;

/**
 * PROPÓSITO DE NEGÓCIO: converte a preferência regional do navegador para um
 * dos três idiomas oferecidos pelo KRONOS.
 * INVARIANTES DO DOMÍNIO: português resolve para pt-BR, espanhol para es-ES e
 * qualquer outro idioma usa en-US como fallback internacional.
 * COMPORTAMENTO EM CASO DE FALHA: entrada nula ou ilegível retorna en-US.
 */
export function normalizarIdioma(valor) {
    const idioma = String(valor || '').trim().toLowerCase();
    if (idioma.startsWith('pt')) return 'pt-BR';
    if (idioma.startsWith('es')) return 'es-ES';
    if (idioma.startsWith('en')) return 'en-US';
    return 'en-US';
}

/**
 * PROPÓSITO DE NEGÓCIO: escolhe o idioma inicial priorizando a seleção manual
 * salva e usando a região do navegador somente na primeira visita.
 * INVARIANTES DO DOMÍNIO: somente os três idiomas das bandeiras são aceitos.
 * COMPORTAMENTO EM CASO DE FALHA: lista vazia ou preferência inválida usa en-US.
 */
export function resolverIdiomaInicial(preferencia, idiomasNavegador = []) {
    if (IDIOMAS.includes(preferencia)) return preferencia;
    const candidatos = Array.isArray(idiomasNavegador) ? idiomasNavegador : [idiomasNavegador];
    return normalizarIdioma(candidatos.find(Boolean));
}

/**
 * PROPÓSITO DE NEGÓCIO: informa se a página pode carregar o Google Translate
 * para internacionalizar a interface sem catálogos manuais.
 * INVARIANTES DO DOMÍNIO: português não depende da rede; tradução externa
 * exige DOM disponível e navegador não explicitamente offline.
 * COMPORTAMENTO EM CASO DE FALHA: ambiente sem DOM ou offline retorna falso.
 */
export function navegadorSuportaTraducao() {
    return typeof document !== 'undefined'
        && typeof navigator !== 'undefined'
        && navigator.onLine !== false;
}

/**
 * PROPÓSITO DE NEGÓCIO: mostra ao operador o carregamento ou a indisponibilidade
 * do tradutor sem usar o sistema global de notificações do pipeline.
 * INVARIANTES DO DOMÍNIO: a mensagem nunca bloqueia a navegação e permanece
 * fora do conteúdo enviado ao Google.
 * COMPORTAMENTO EM CASO DE FALHA: seletor ainda não renderizado é tolerado.
 */
function atualizarStatus(mensagem = '', erro = false) {
    const status = document.getElementById('idioma-status');
    if (!status) return;
    status.textContent = mensagem;
    status.classList.toggle('erro', erro);
    status.classList.toggle('visivel', Boolean(mensagem));
}

/**
 * PROPÓSITO DE NEGÓCIO: identifica domínios DNS onde também é necessário gravar
 * o cookie de tradução com atributo domain para compartilhamento consistente.
 * INVARIANTES DO DOMÍNIO: localhost, IPv4 e IPv6 usam somente cookie host-only.
 * COMPORTAMENTO EM CASO DE FALHA: hostname ausente retorna falso.
 */
function dominioAceitaCookieCompartilhado(hostname) {
    const host = String(hostname || '').trim();
    return Boolean(host && host !== 'localhost' && !/^\d{1,3}(\.\d{1,3}){3}$/.test(host) && !host.includes(':'));
}

/**
 * PROPÓSITO DE NEGÓCIO: grava ou remove uma variante do cookie `googtrans` que
 * orienta o widget sobre o par português→idioma escolhido.
 * INVARIANTES DO DOMÍNIO: cookie fica restrito ao caminho raiz e não contém
 * dados de legenda, credenciais ou identificadores pessoais.
 * COMPORTAMENTO EM CASO DE FALHA: navegadores que bloqueiam cookies continuam
 * em português e o carregador apresentará a indisponibilidade.
 */
function escreverCookieGoogle(valor, dominio = '') {
    const atributoDominio = dominio ? `; domain=${dominio}` : '';
    if (valor) {
        document.cookie = `googtrans=${valor}; path=/; max-age=31536000; SameSite=Lax${atributoDominio}`;
        return;
    }
    document.cookie = `googtrans=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax${atributoDominio}`;
}

/**
 * PROPÓSITO DE NEGÓCIO: sincroniza o idioma selecionado com o cookie consumido
 * pelo Google Translate no carregamento seguinte da SPA.
 * INVARIANTES DO DOMÍNIO: pt-BR remove todas as variantes; inglês e espanhol
 * sempre partem de português.
 * COMPORTAMENTO EM CASO DE FALHA: erro de cookie não interrompe a interface.
 */
function aplicarCookieIdioma(idioma) {
    try {
        const codigo = CODIGOS_GOOGLE[idioma] || 'pt';
        const valor = codigo === 'pt' ? '' : `/pt/${codigo}`;
        escreverCookieGoogle(valor);
        const host = globalThis.location?.hostname || '';
        if (dominioAceitaCookieCompartilhado(host)) escreverCookieGoogle(valor, host);
    } catch (erro) {
        console.warn('Não foi possível atualizar o cookie de idioma:', erro);
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: preserva toda área operacional que não pode ser enviada
 * ou alterada pelo tradutor externo.
 * INVARIANTES DO DOMÍNIO: o elemento e toda sua subárvore recebem os contratos
 * `translate=no` e `notranslate` reconhecidos pelo navegador e pelo Google.
 * COMPORTAMENTO EM CASO DE FALHA: valor que não é Element é ignorado.
 */
function protegerElemento(elemento) {
    if (!(elemento instanceof Element)) return;
    elemento.setAttribute('translate', 'no');
    elemento.classList.add('notranslate');
}

/**
 * PROPÓSITO DE NEGÓCIO: aplica a política de privacidade a conteúdos estáticos
 * e módulos carregados dinamicamente antes que o widget os processe.
 * INVARIANTES DO DOMÍNIO: scripts, código, legendas, consoles, telemetria,
 * caminhos e metadados de anime permanecem fora da tradução.
 * COMPORTAMENTO EM CASO DE FALHA: raiz vazia ou removida não afeta a SPA.
 */
function protegerSubarvore(raiz) {
    if (!raiz) return;
    if (raiz instanceof Element && raiz.matches(SELETORES_PROTEGIDOS.join(','))) {
        protegerElemento(raiz);
    }
    if (typeof raiz.querySelectorAll !== 'function') return;
    raiz.querySelectorAll(SELETORES_PROTEGIDOS.join(',')).forEach(protegerElemento);
}

/**
 * PROPÓSITO DE NEGÓCIO: protege imediatamente novos painéis, logs e resultados
 * inseridos por fetch após a inicialização da página.
 * INVARIANTES DO DOMÍNIO: somente nós adicionados são examinados; textos já
 * protegidos nunca perdem a marcação.
 * COMPORTAMENTO EM CASO DE FALHA: mutações sem nós adicionados são ignoradas.
 */
function processarMutacoes(mutacoes) {
    mutacoes.forEach(mutacao => mutacao.addedNodes.forEach(protegerSubarvore));
}

/**
 * PROPÓSITO DE NEGÓCIO: mantém a proteção de privacidade ativa durante toda a
 * navegação da SPA, inclusive em módulos HTML injetados sob demanda.
 * INVARIANTES DO DOMÍNIO: existe no máximo um observador global.
 * COMPORTAMENTO EM CASO DE FALHA: navegador sem MutationObserver conserva a
 * proteção inicial do documento.
 */
function observarConteudoDinamico() {
    if (observadorProtecao || typeof MutationObserver === 'undefined') return;
    observadorProtecao = new MutationObserver(processarMutacoes);
    observadorProtecao.observe(document.body, { childList: true, subtree: true });
}

/**
 * PROPÓSITO DE NEGÓCIO: reflete visualmente qual bandeira corresponde ao idioma
 * solicitado ou efetivamente restaurado.
 * INVARIANTES DO DOMÍNIO: exatamente uma bandeira fica pressionada.
 * COMPORTAMENTO EM CASO DE FALHA: seletor ausente é tolerado.
 */
function atualizarBandeiras() {
    document.querySelectorAll('[data-idioma]').forEach(botao => {
        const ativo = botao.dataset.idioma === idiomaAtual;
        botao.classList.toggle('ativo', ativo);
        botao.setAttribute('aria-pressed', String(ativo));
    });
}

/**
 * PROPÓSITO DE NEGÓCIO: inicializa o widget oficial depois que as áreas privadas
 * já foram marcadas como não traduzíveis.
 * INVARIANTES DO DOMÍNIO: somente português, inglês e espanhol são oferecidos e
 * o seletor nativo do Google permanece oculto.
 * COMPORTAMENTO EM CASO DE FALHA: ausência da biblioteca restaura pt-BR e
 * apresenta orientação sem quebrar o restante da aplicação.
 */
function inicializarWidgetGoogle() {
    try {
        const Tradutor = globalThis.google?.translate?.TranslateElement;
        if (!Tradutor) throw new Error('Biblioteca Google Translate não carregada.');
        new Tradutor({ pageLanguage: 'pt', includedLanguages: 'pt,en,es', autoDisplay: false }, 'google_translate_element');
        atualizarStatus('');
    } catch (erro) {
        console.warn('Falha ao inicializar Google Translate:', erro);
        idiomaAtual = 'pt-BR';
        aplicarCookieIdioma(idiomaAtual);
        atualizarBandeiras();
        atualizarStatus('Google Translate indisponível. A interface permaneceu em português.', true);
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: informa falha de rede ou bloqueio do script externo sem
 * confundir o operador com erros internos do navegador.
 * INVARIANTES DO DOMÍNIO: falha sempre restaura a seleção visual para pt-BR.
 * COMPORTAMENTO EM CASO DE FALHA: a interface operacional continua disponível.
 */
function tratarFalhaScriptGoogle() {
    idiomaAtual = 'pt-BR';
    aplicarCookieIdioma(idiomaAtual);
    atualizarBandeiras();
    atualizarStatus('Não foi possível carregar o Google Translate. Verifique a internet ou o bloqueador.', true);
}

/**
 * PROPÓSITO DE NEGÓCIO: carrega sob demanda o widget externo apenas quando o
 * operador precisa de inglês ou espanhol.
 * INVARIANTES DO DOMÍNIO: um único script e um único container são criados.
 * COMPORTAMENTO EM CASO DE FALHA: rede indisponível aciona fallback em português.
 */
function carregarGoogleTranslate() {
    if (idiomaAtual === 'pt-BR') return;
    if (!navegadorSuportaTraducao()) {
        tratarFalhaScriptGoogle();
        return;
    }
    atualizarStatus('Carregando Google Translate…');
    globalThis.googleTranslateElementInit = inicializarWidgetGoogle;
    if (globalThis.google?.translate?.TranslateElement) {
        inicializarWidgetGoogle();
        return;
    }
    if (document.getElementById('google-translate-script')) return;
    const script = document.createElement('script');
    script.id = 'google-translate-script';
    script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
    script.async = true;
    script.onerror = tratarFalhaScriptGoogle;
    document.head.appendChild(script);
}

/**
 * PROPÓSITO DE NEGÓCIO: persiste a bandeira escolhida, sincroniza o cookie do
 * Google e opcionalmente recarrega a SPA para aplicar a tradução integral.
 * INVARIANTES DO DOMÍNIO: idioma sempre pertence ao conjunto suportado; pt-BR
 * limpa o cookie de tradução.
 * COMPORTAMENTO EM CASO DE FALHA: localStorage bloqueado não impede o cookie e
 * idioma desconhecido usa o fallback internacional en-US.
 */
export function definirIdioma(idioma, persistir = true, recarregar = true) {
    idiomaAtual = IDIOMAS.includes(idioma) ? idioma : normalizarIdioma(idioma);
    aplicarCookieIdioma(idiomaAtual);
    document.documentElement.lang = idiomaAtual;
    if (persistir) {
        try { localStorage.setItem(CHAVE_IDIOMA, idiomaAtual); } catch (ignored) { /* cookie continua válido */ }
    }
    atualizarBandeiras();
    document.dispatchEvent(new CustomEvent('kronos:idioma-alterado', { detail: { idioma: idiomaAtual } }));
    if (recarregar) globalThis.location.reload();
    return idiomaAtual;
}

/**
 * PROPÓSITO DE NEGÓCIO: trata o clique acessível em uma bandeira e inicia a
 * troca completa de idioma da interface.
 * INVARIANTES DO DOMÍNIO: somente botões com data-idioma acionam recarga.
 * COMPORTAMENTO EM CASO DE FALHA: evento sem botão válido é ignorado.
 */
function tratarCliqueBandeira(evento) {
    const botao = evento.currentTarget;
    if (!botao?.dataset?.idioma) return;
    atualizarStatus('Aplicando idioma…');
    definirIdioma(botao.dataset.idioma, true, true);
}

/**
 * PROPÓSITO DE NEGÓCIO: liga as bandeiras à troca de idioma sem listeners
 * duplicados quando a SPA reinicializa módulos.
 * INVARIANTES DO DOMÍNIO: cada botão válido recebe exatamente um listener.
 * COMPORTAMENTO EM CASO DE FALHA: seletor incompleto mantém a página funcional.
 */
function vincularBandeiras() {
    document.querySelectorAll('[data-idioma]').forEach(botao => {
        if (botao.dataset.vinculado === 'true') return;
        botao.dataset.vinculado = 'true';
        botao.addEventListener('click', tratarCliqueBandeira);
    });
}

/**
 * PROPÓSITO DE NEGÓCIO: remove somente caches obsoletos criados pelo motor local
 * abandonado, evitando que preferências antigas reativem o fluxo quebrado.
 * INVARIANTES DO DOMÍNIO: a preferência de idioma e dados do KRONOS não são
 * apagados; somente chaves `kronos.i18n.automatico.v1.*` são removidas.
 * COMPORTAMENTO EM CASO DE FALHA: armazenamento indisponível é ignorado.
 */
function removerCachesMotorLocal() {
    try {
        Object.keys(localStorage)
            .filter(chave => chave.startsWith('kronos.i18n.automatico.v1.'))
            .forEach(chave => localStorage.removeItem(chave));
    } catch (ignored) {
        // O mecanismo por cookie não depende desse armazenamento técnico.
    }
}

/**
 * PROPÓSITO DE NEGÓCIO: inicializa detecção regional, privacidade, bandeiras e
 * tradução Google sem classes Java nem arquivos manuais por idioma.
 * INVARIANTES DO DOMÍNIO: áreas operacionais são protegidas antes do script
 * externo; pt-BR não carrega dependências de terceiros.
 * COMPORTAMENTO EM CASO DE FALHA: preferência indisponível usa o navegador e
 * falha externa mantém a interface original em português.
 */
export function inicializarI18n() {
    let preferencia = null;
    try { preferencia = localStorage.getItem(CHAVE_IDIOMA); } catch (ignored) { preferencia = null; }
    const navegadores = navigator.languages?.length ? navigator.languages : [navigator.language];
    removerCachesMotorLocal();
    protegerSubarvore(document);
    observarConteudoDinamico();
    vincularBandeiras();
    definirIdioma(resolverIdiomaInicial(preferencia, navegadores), false, false);
    carregarGoogleTranslate();
    return idiomaAtual;
}

/**
 * PROPÓSITO DE NEGÓCIO: fornece o locale ativo para módulos que formatam datas
 * e números sem depender dos detalhes internos do widget.
 * INVARIANTES DO DOMÍNIO: retorno sempre pertence ao conjunto das bandeiras.
 * COMPORTAMENTO EM CASO DE FALHA: antes da inicialização retorna pt-BR.
 */
export function obterIdiomaAtual() {
    return idiomaAtual;
}
