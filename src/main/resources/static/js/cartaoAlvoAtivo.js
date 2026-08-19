/**
 * CARTÃO DO ALVO ATIVO — o lembrete permanente de PARA ONDE a tela vai escrever.
 *
 * PROPÓSITO DE NEGÓCIO: o operador escolhe a obra num combo no alto e preenche a pasta mais
 * abaixo; ao chegar no botão, a confirmação já saiu de vista. Este cartão mantém as duas coisas
 * visíveis o tempo todo — obra escolhida e pasta que SERÁ REESCRITA — porque é exatamente ali
 * que o erro custa caro: em 06/08/2026 uma tradução apontou para `legenda-simplificada`, que é
 * pasta de SAÍDA, e sobrescreveu 17 arquivos limpos. O código fez o que foi mandado; a interface
 * é que deixou mandar.
 *
 * POR QUE MORA AQUI: o mesmo cartão (`.op-alvo` + `.op-alvo-texto`) nasceu copiado em quatro
 * telas, e as cópias JÁ tinham divergido em 19/08/2026 — a Revisão de Lore interpolava a pasta
 * direto no `innerHTML`, a Revisão de Legendas usava `textContent` num `<code>`, e a Correção de
 * Cache um terceiro jeito. Ninguém decidiu isso; foi acontecendo. Um caminho com `<` ou `&`
 * renderizava diferente em cada tela.
 *
 * <p>Migradas em 19/08/2026: <b>3.1 Revisão de Legendas</b>, <b>3.2 Revisão de Lore</b> e
 * <b>3.3 Concordância</b> chamam este módulo. A <b>Correção de Cache</b> segue com código próprio
 * DE PROPÓSITO: o cartão dela alterna entre "somente esta pasta" e "o acervo INTEIRO", que é outra
 * decisão, não a mesma com outro texto.
 *
 * <p>O risco real aqui nunca foi o `<` — caminho do Windows não aceita esse caractere. É a
 * divergência silenciosa: quatro cópias do mesmo cartão já andaram sozinhas, e a quinta andaria
 * também sem ninguém perceber.
 *
 * INVARIANTES:
 *  - A pasta entra por `textContent`, NUNCA por `innerHTML`: caminho é dado do usuário.
 *  - Só LÊ o estado dos campos — não altera valor, não dispara requisição.
 *  - Repinta em `change` do seletor e em `input` da pasta, e uma vez ao ligar, para o cartão
 *    nunca começar em branco.
 *
 * COMPORTAMENTO EM CASO DE FALHA: elemento ausente devolve `null` e não lança — a tela perde o
 * cartão, nunca o carregamento. Devolve a função de repintura para quem precisar chamá-la depois
 * de um "Limpar Campos", que grava o valor direto e não dispara evento.
 */

/**
 * @param {object} opcoes
 * @param {string} opcoes.alvoTextoId id do `<span class="op-alvo-texto">`
 * @param {string} opcoes.selectId id do `<select>` de obra
 * @param {string} opcoes.pastaId id do campo de pasta que será reescrita
 * @param {string} [opcoes.rotuloObra] rótulo antes do nome da obra
 * @param {string} [opcoes.rotuloPasta] rótulo antes do caminho
 * @param {string} [opcoes.semEscolha] frase quando nenhuma obra foi escolhida
 * @param {string} [opcoes.caixaId] id do `<div class="op-alvo">`; só quem passa isto ganha o
 *        destaque visual enquanto falta obra ou pasta
 * @param {string} [opcoes.classeAlerta] classe do destaque (padrão `op-alvo-acervo`)
 * @returns {(() => void)|null} a função de repintura, ou null se a tela não tem o cartão
 */
export function ligarCartaoAlvoAtivo(opcoes) {
    const alvo = document.getElementById(opcoes.alvoTextoId);
    const select = document.getElementById(opcoes.selectId);
    const pasta = document.getElementById(opcoes.pastaId);
    const caixa = opcoes.caixaId ? document.getElementById(opcoes.caixaId) : null;
    if (!alvo || !select) return null;

    const rotuloObra = opcoes.rotuloObra || 'Obra ativa';
    const rotuloPasta = opcoes.rotuloPasta || 'Pasta';
    const semEscolha = opcoes.semEscolha || 'Escolha a obra acima para liberar os campos.';
    const classeAlerta = opcoes.classeAlerta || 'op-alvo-acervo';

    const pintar = () => {
        const escolheu = !!select.value || select.selectedOptions?.[0]?.dataset?.liberaTrava === 'true';
        const obra = (select.selectedOptions?.[0]?.textContent || '').trim();
        const caminho = (pasta?.value || '').trim();

        // Só quem passou `caixaId` muda de estilo: a 3.1 destaca o cartão enquanto falta obra ou
        // pasta, e as outras telas não faziam isso — unificar o código não é licença para
        // inventar comportamento novo em tela que já funciona.
        caixa?.classList.toggle(classeAlerta, !escolheu || !caminho);

        alvo.textContent = '';

        const parteObra = document.createElement('span');
        if (escolheu) {
            parteObra.innerHTML = `${rotuloObra}: <strong></strong>. `;
            parteObra.querySelector('strong').textContent = obra;
        } else {
            parteObra.innerHTML = `${rotuloObra}: <strong>nenhuma</strong>. ${semEscolha} `;
        }
        alvo.appendChild(parteObra);

        // A pasta aparece SEMPRE, inclusive sem obra escolhida. Na 3.1 o campo já vem preenchido
        // do sessionStorage antes de a lore ser escolhida — esconder a linha ali apagaria da tela
        // justamente o dado que o cartão existe para mostrar.
        const parteRotulo = document.createElement('span');
        parteRotulo.textContent = `${rotuloPasta}: `;
        alvo.appendChild(parteRotulo);

        // O caminho vai por textContent — é dado do usuário, e um `<` ou `&` no nome da pasta não
        // pode virar marcação. Foi a diferença entre as três cópias que este módulo unifica.
        const parteCaminho = document.createElement(caminho ? 'code' : 'strong');
        if (caminho) parteCaminho.className = 'alvo-pasta';
        parteCaminho.textContent = caminho || 'ainda não informada';
        alvo.appendChild(parteCaminho);
        alvo.appendChild(document.createTextNode('.'));
    };

    select.addEventListener('change', pintar);
    pasta?.addEventListener('input', pintar);
    pintar();
    return pintar;
}
