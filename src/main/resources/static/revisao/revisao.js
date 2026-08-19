import { logNoConsole } from '../js/app.js';
import { montarOpcoesContextos } from '../js/selectContextos.js';
import { ligarCartaoAlvoAtivo } from '../js/cartaoAlvoAtivo.js?v=1.1';

const STORAGE_PASTA_PT = 'revisao.ultimaPastaPt';

let contextosCarregados = false;

// Fonte de referência escolhida na aba (Ambos = EN+cache; Cache = só cache).
let modoReferencia = 'AMBOS';

const DESCRICAO_MODO_REVISAO = {
    AMBOS: 'Referência: legenda EN (.ass) + cache do projeto. Comportamento padrão.',
    CACHE: 'Referência: apenas o cache. O campo "original" de cada entrada substitui a legenda EN, com vínculo seguro (índice + estilo + proveniência + texto). Entradas que não casam ficam SEM_REFERÊNCIA_SEGURA e não são usadas em silêncio.'
};

function parecePastaCache(entrada) {
    const normalizada = entrada.replace(/\//g, '\\').toLowerCase();
    return normalizada.includes('\\cache\\')
        || normalizada.endsWith('\\cache')
        || normalizada.includes('.cache.json');
}

function pareceTextoDeConsole(valor) {
    return /Logs da Revis[aã]o|Corrigir via Scraping|Console limpo|\[\d{1,2}:\d{2}:\d{2}\]|Pasta PT:/i.test(valor);
}

function pareceCaminhoPasta(valor) {
    if (!valor || valor.length > 260) return false;
    if (pareceTextoDeConsole(valor)) return false;
    if (/[\r\n]/.test(valor)) return false;
    return /^([A-Za-z]:\\|\\\\|\/)/.test(valor.trim());
}

function extrairCaminhoWindows(valor) {
    const candidatos = valor.match(/(?:[A-Za-z]:\\(?:[^<>:"|?*\r\n\[\]]|\\.)+|\\\\[^<>:"|?*\r\n\[\]]+)/g);
    if (!candidatos || candidatos.length === 0) return null;
    return candidatos[candidatos.length - 1].replace(/[\s.]+$/, '');
}

function lerCaminhoDoInput(input, rotulo) {
    if (!(input instanceof HTMLInputElement)) {
        throw new Error(`Campo "${rotulo}" indisponível na interface. Recarregue a página.`);
    }

    let valor = input.value.trim();
    if (!valor) return '';

    if (!pareceCaminhoPasta(valor)) {
        const recuperado = extrairCaminhoWindows(valor);
        if (recuperado && pareceCaminhoPasta(recuperado)) {
            input.value = recuperado;
            logNoConsole(
                'console-revisao',
                `${rotulo}: caminho recuperado automaticamente (${recuperado})`,
                'aviso'
            );
            return recuperado;
        }

        throw new Error(
            `${rotulo} inválido. Informe apenas o caminho da pasta (ex.: E:\\animes\\DANMACHI\\legendas_ptbr). `
                + 'Evite colar logs ou textos da tela no campo.'
        );
    }

    return valor;
}

function lerPastasDoFormulario(inputPt, inputEn) {
    const entrada = lerCaminhoDoInput(inputPt, 'Pasta PT');
    if (!entrada) {
        throw new Error('Informe a pasta com as legendas traduzidas.');
    }

    let entradaEn = '';
    if (inputEn instanceof HTMLInputElement && inputEn.value.trim()) {
        entradaEn = lerCaminhoDoInput(inputEn, 'Pasta EN');
    }

    if (parecePastaCache(entrada) || (entradaEn && parecePastaCache(entradaEn))) {
        throw new Error(
            'Use pastas com arquivos .ass de legenda — não a pasta cache/ do projeto. '
                + 'Ex.: E:\\animes\\DANMACHI\\temporada_5\\legendas_extraidas_ass'
        );
    }

    sessionStorage.setItem(STORAGE_PASTA_PT, entrada);
    return { entrada, entradaEn };
}

/**
 * PROPÓSITO DE NEGÓCIO: alterna a fonte de referência (Ambos/Cache), mostrando o
 * campo pertinente e escondendo o outro.
 * INVARIANTES DO DOMÍNIO: Ambos mostra a pasta EN; Cache mostra a pasta de cache.
 * COMPORTAMENTO EM CASO DE FALHA: elementos ausentes são ignorados sem erro.
 */
function aplicarModoRevisao() {
    const campoEn = document.querySelector('#panel-revisao .revisao-campo-en');
    const campoCache = document.querySelector('#panel-revisao .revisao-campo-cache');
    const desc = document.getElementById('revisao-modo-desc');
    if (campoEn) campoEn.classList.toggle('hidden', modoReferencia === 'CACHE');
    if (campoCache) campoCache.classList.toggle('hidden', modoReferencia !== 'CACHE');
    if (desc) desc.textContent = DESCRICAO_MODO_REVISAO[modoReferencia] || DESCRICAO_MODO_REVISAO.AMBOS;
}

function vincularAbasRevisao() {
    const tabs = document.querySelectorAll('#panel-revisao .revisao-modo-tab');
    if (!tabs.length) return;
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            modoReferencia = tab.dataset.modo || 'AMBOS';
            tabs.forEach(t => {
                const ativo = t === tab;
                t.classList.toggle('ativo', ativo);
                t.setAttribute('aria-selected', String(ativo));
            });
            aplicarModoRevisao();
        });
    });
    aplicarModoRevisao();
}

/**
 * PROPÓSITO DE NEGÓCIO: acrescenta ao payload a fonte de referência e, no modo
 * Cache, a pasta de cache; no modo Ambos, a pasta EN opcional.
 * INVARIANTES DO DOMÍNIO: no modo Cache nunca se envia a pasta EN; a pasta de
 * cache pode apontar para cache/ (não passa pela rejeição de pasta de cache).
 * COMPORTAMENTO EM CASO DE FALHA: campos vazios simplesmente não entram no payload.
 */
function aplicarReferenciaAoPayload(payload, entradaEn, inputCache) {
    payload.modoReferencia = modoReferencia;
    if (modoReferencia === 'CACHE') {
        const cache = lerCaminhoCacheValidado(inputCache);
        if (cache) payload.caminhoCache = cache;
    } else if (entradaEn) {
        payload.saida = entradaEn;
    }
    return payload;
}

/**
 * PROPÓSITO DE NEGÓCIO: valida o campo de cache com a mesma proteção dos demais
 * campos (recupera caminho colado de logs, rejeita texto inválido) e exige uma
 * PASTA, não um .cache.json avulso.
 * INVARIANTES DO DOMÍNIO: a pasta de cache pode apontar para cache/ (não passa
 * pela rejeição de pasta de cache aplicada às legendas).
 * COMPORTAMENTO EM CASO DE FALHA: caminho inválido ou arquivo .cache.json isolado
 * lança erro exibido no console; campo vazio devolve string vazia.
 */
function lerCaminhoCacheValidado(inputCache) {
    if (!(inputCache instanceof HTMLInputElement) || !inputCache.value.trim()) {
        return '';
    }
    const caminho = lerCaminhoDoInput(inputCache, 'Pasta de cache');
    if (/\.cache\.json$/i.test(caminho)) {
        throw new Error(
            'Informe a PASTA do cache (que contém os .cache.json), não um arquivo .cache.json individual.'
        );
    }
    return caminho;
}

async function enviarRevisao(endpoint, payload, mensagemInicio) {
    logNoConsole('console-revisao', mensagemInicio, 'info');
    logNoConsole('console-revisao', `Pasta PT: ${payload.entrada}`, 'info');
    if (payload.modoReferencia) {
        logNoConsole('console-revisao', `Fonte de referência: ${payload.modoReferencia}`, 'info');
    }
    if (payload.saida) {
        logNoConsole('console-revisao', `Pasta EN: ${payload.saida}`, 'info');
    }
    if (payload.caminhoCache) {
        logNoConsole('console-revisao', `Pasta de cache: ${payload.caminhoCache}`, 'info');
    }
    if (payload.contextoId) {
        logNoConsole('console-revisao', `Contexto: ${payload.contextoId}`, 'info');
    }

    const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        const erroTexto = await res.text();
        let msg = 'Erro ao iniciar revisão';
        try {
            const parsed = JSON.parse(erroTexto);
            if (parsed.mensagem) msg = parsed.mensagem;
        } catch (ignored) {
            if (erroTexto) msg = erroTexto;
        }
        throw new Error(msg);
    }

    const data = await res.json();
    logNoConsole('console-revisao', 'Processamento iniciado no servidor!', 'sucesso');
    if (data.mensagem) {
        logNoConsole('console-revisao', data.mensagem, 'info');
    }
}

async function carregarContextos() {
    const select = document.getElementById('revisao-contexto');
    if (!select) return;

    try {
        const response = await fetch('/api/contextos', { cache: 'no-store' });
        if (!response.ok) {
            throw new Error('Resposta HTTP ' + response.status);
        }

        const contextos = await response.json();
        if (!Array.isArray(contextos) || contextos.length === 0) {
            throw new Error('Nenhum contexto cadastrado no servidor.');
        }

        select.innerHTML = '';
        montarOpcoesContextos(select, contextos, (opt, ctx) => {
            if (ctx.padrao) opt.selected = true;
        });

        contextosCarregados = true;
    } catch (err) {
        console.error('Erro ao carregar contextos de revisão:', err);
        select.innerHTML = '<option value="">Erro ao carregar — recarregue a página</option>';
        contextosCarregados = false;
    }
    // Avisa o espelho do alvo nos DOIS caminhos: no sucesso ele mostra a lore que ficou
    // selecionada por padrão; no erro ele precisa sair de "carregando" e acusar a ausência.
    select.dispatchEvent(new CustomEvent('kronos:contextos-carregados'));
}

export function initRevisao() {
    const form = document.getElementById('form-revisao');
    const inputPt = document.getElementById('revisao-entrada');
    const inputEn = document.getElementById('revisao-entrada-en');
    const inputCache = document.getElementById('revisao-cache');
    const btnConcordancia = document.getElementById('btn-revisao-concordancia');
    const contextoSelect = document.getElementById('revisao-contexto');

    if (!form || !(inputPt instanceof HTMLInputElement)) return;

    carregarContextos();
    vincularAbasRevisao();

    const ultimaPasta = sessionStorage.getItem(STORAGE_PASTA_PT);
    if (!inputPt.value.trim() && ultimaPasta) {
        inputPt.value = ultimaPasta;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escreve na tela a LORE ATIVA e a pasta antes de qualquer clique.
     * A lore decide quais nomes ficam protegidos do tradutor externo, e escolher a obra errada
     * — fácil quando duas séries dividem a mesma pasta — só aparece depois, no arquivo gravado.
     * O relatório também não registra a lore usada, então a tela é o único lugar onde isso é
     * visível a tempo.
     *
     * <p>A montagem do cartão saiu daqui em 19/08/2026 para `js/cartaoAlvoAtivo.js`: quatro telas
     * tinham o MESMO cartão com o JS copiado, e as cópias já haviam divergido no jeito de mostrar
     * a pasta. Esta tela mantém o que é só dela — o `sessionStorage`, o evento dos contextos que
     * chegam por fetch e o destaque da caixa enquanto falta lore ou pasta.
     *
     * INVARIANTES DO DOMÍNIO: só lê o estado dos campos; não altera nada nem dispara requisição.
     * COMPORTAMENTO EM CASO DE FALHA: elementos ausentes fazem a função não ter efeito.
     */
    const atualizarAlvo = ligarCartaoAlvoAtivo({
        alvoTextoId: 'revisao-alvo-texto',
        selectId: 'revisao-contexto',
        pastaId: 'revisao-entrada',
        caixaId: 'revisao-alvo',
        rotuloObra: 'Lore ativa',
        semEscolha: 'Sem ela os nomes da obra não ficam protegidos do tradutor externo, '
            + 'e as duas passadas se recusam a começar.'
    }) || (() => {});

    // `change` do seletor, `input` da pasta e a primeira pintura já vêm do módulo. Ficam aqui só
    // os dois avisos que são desta tela e nenhum evento de campo dispara:
    // o "Limpar Campos" compartilhado grava o valor direto, sem disparar evento...
    document.querySelector('#panel-revisao .btn-clear-form')
        ?.addEventListener('click', () => setTimeout(atualizarAlvo, 0));
    // ...e os contextos chegam por fetch; sem isto o cartão ficaria preso em "nenhuma".
    contextoSelect?.addEventListener('kronos:contextos-carregados', atualizarAlvo);

    if (btnConcordancia) {
        btnConcordancia.addEventListener('click', async () => {
            if (!contextosCarregados || !contextoSelect?.value) {
                logNoConsole(
                    'console-revisao',
                    'Aguarde o carregamento do contexto de lore ou recarregue a página.',
                    'erro'
                );
                return;
            }

            try {
                const { entrada, entradaEn } = lerPastasDoFormulario(inputPt, inputEn);
                const payload = aplicarReferenciaAoPayload({
                    entrada,
                    contextoId: contextoSelect.value
                }, entradaEn, inputCache);

                await enviarRevisao(
                    '/api/revisar-legendas-concordancia',
                    payload,
                    'Traduzindo o que faltou — 1ª etapa: LLM local; o Google entra só no que ela não resolver...'
                );
            } catch (err) {
                logNoConsole('console-revisao', `Erro: ${err.message}`, 'erro');
            }
        });
    }

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        // O Google também precisa do contexto de lore para proteger nomes/termos
        // canônicos; não iniciar antes de os contextos carregarem.
        if (!contextosCarregados || !contextoSelect?.value) {
            logNoConsole(
                'console-revisao',
                'Aguarde o carregamento do contexto de lore ou recarregue a página.',
                'erro'
            );
            return;
        }

        try {
            const { entrada, entradaEn } = lerPastasDoFormulario(inputPt, inputEn);
            const payload = aplicarReferenciaAoPayload({
                entrada,
                contextoId: contextoSelect.value
            }, entradaEn, inputCache);

            await enviarRevisao(
                '/api/revisar-legendas',
                payload,
                'Só a 2ª etapa (Google), sem o LLM local — fala com tag inline tende a ficar pendente...'
            );
        } catch (err) {
            logNoConsole('console-revisao', `Erro: ${err.message}`, 'erro');
        }
    });
}
