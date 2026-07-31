/**
 * TRAVA DE LORE — impede explorar pastas antes de escolher a obra.
 *
 * PROPÓSITO DE NEGÓCIO: em toda tela que escolhe lore, os campos de caminho e o botão de
 * ação ficam DESABILITADOS até que uma obra seja selecionada. Sem a obra, o pipeline não
 * sabe qual terminologia aplicar, qual capa exibir nem com qual proveniência carimbar o
 * cache — navegar pastas antes disso é convidar o operador a montar uma execução que o
 * servidor vai recusar (ou, pior, aceitar com a lore da execução anterior).
 *
 * INVARIANTES:
 *  - "Limpar Campos" (.btn-clear-form) NUNCA é travado: limpar não depende de lore, e
 *    travá-lo prenderia o operador num formulário que ele não consegue zerar.
 *  - As abas internas (.lore-tab) também não: são navegação, não ação.
 *  - O escopo é o .panel da tela. Telas com DOIS formulários e UM seletor (revisão de
 *    lore) são cobertas por inteiro justamente por isso — travar só o form mais próximo
 *    deixaria o segundo aberto.
 *  - A opção de escape (ex.: tradução sem lore) é declarada em `idsLiberadores`: quando
 *    selecionada, libera como se fosse uma obra.
 *
 * COMPORTAMENTO EM CASO DE FALHA: seletor inexistente devolve `null` e não lança — a tela
 * simplesmente não ganha a trava, em vez de quebrar o carregamento inteiro do painel.
 */

const AVISO_CLASSE = 'trava-lore-aviso';
const TEXTO_AVISO = 'Escolha a obra acima para liberar os campos de pasta.';

/**
 * Aplica a trava a um seletor de contexto.
 *
 * @param {HTMLSelectElement|string} seletor elemento ou id do <select> de contexto
 * @param {{idsLiberadores?: string[]}} [opcoes] ids que contam como escolha válida
 *        mesmo não sendo obra (ex.: 'sem_lore')
 * @returns {(() => void)|null} função para reaplicar após render dinâmico, ou null
 */
export function travarAteEscolherLore(seletor, opcoes = {}) {
    const select = typeof seletor === 'string' ? document.getElementById(seletor) : seletor;
    if (!select) {
        return null;
    }
    const liberadores = new Set(opcoes.idsLiberadores || []);
    const escopo = select.closest('.panel') || select.closest('form') || document;

    // IDEMPOTENTE: os painéis injetados republicam os contextos a cada
    // `*:painel-carregado`, então esta função roda mais de uma vez no mesmo <select>.
    // Sem esta guarda, cada passagem empilharia outro aviso e outro listener.
    const grupo = select.closest('.form-group') || select.parentElement;
    let aviso = grupo.querySelector('.' + AVISO_CLASSE);
    const primeiraVez = !aviso;
    if (primeiraVez) {
        aviso = document.createElement('small');
        aviso.className = AVISO_CLASSE;
        aviso.textContent = opcoes.textoAviso || TEXTO_AVISO;
        grupo.appendChild(aviso);
    }

    function alvos() {
        const encontrados = [
            ...escopo.querySelectorAll('.input-with-button input'),
            ...escopo.querySelectorAll('.btn-procurar'),
            ...escopo.querySelectorAll('button[type="submit"]'),
            ...escopo.querySelectorAll('.btn-primary'),
        ];
        return encontrados.filter(el =>
            !el.classList.contains('btn-clear-form') && !el.classList.contains('lore-tab'));
    }

    function escolheu() {
        const opcao = select.selectedOptions && select.selectedOptions[0];
        // Opção que LIBERA sem ser obra ("— Sem obra —" das telas em que a lore só enfeita
        // com a capa). Marcada com data-libera-trava porque o `value` dela continua vazio:
        // quem lê o select para montar a requisição precisa seguir vendo "" como antes.
        if (opcao && opcao.dataset && opcao.dataset.liberaTrava === 'true') {
            return true;
        }
        const valor = select.value;
        if (!valor) {
            return false;
        }
        if (liberadores.has(valor)) {
            return true;
        }
        return !(opcao && opcao.disabled);
    }

    function aplicar() {
        const liberado = escolheu();
        alvos().forEach(el => { el.disabled = !liberado; });
        aviso.classList.toggle('hidden', liberado);
        escopo.classList.toggle('lore-pendente', !liberado);
    }

    if (primeiraVez) {
        select.addEventListener('change', aplicar);
    }
    aplicar();
    return aplicar;
}
