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
 * POR QUE MORA AQUI: o mesmo cartão (`.op-alvo` + `.op-alvo-texto`) já existe em três telas —
 * Correção de Cache, Revisão de Legendas e Revisão de Lore — com o JS copiado em cada uma. As
 * cópias JÁ divergiram: a da Revisão de Lore interpola a pasta direto no `innerHTML`, e a da
 * Revisão de Legendas usa `textContent` num `<code>`. Um caminho com `<` ou `&` renderiza
 * diferente nas duas, e ninguém percebeu — é a divergência silenciosa que a invariante 10 do
 * projeto existe para impedir. Esta é a versão SEGURA, e é o endereço das próximas telas.
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
 * @returns {(() => void)|null} a função de repintura, ou null se a tela não tem o cartão
 */
export function ligarCartaoAlvoAtivo(opcoes) {
    const alvo = document.getElementById(opcoes.alvoTextoId);
    const select = document.getElementById(opcoes.selectId);
    const pasta = document.getElementById(opcoes.pastaId);
    if (!alvo || !select) return null;

    const rotuloObra = opcoes.rotuloObra || 'Obra ativa';
    const rotuloPasta = opcoes.rotuloPasta || 'Pasta';
    const semEscolha = opcoes.semEscolha || 'Escolha a obra acima para liberar os campos.';

    const pintar = () => {
        const escolheu = !!select.value || select.selectedOptions?.[0]?.dataset?.liberaTrava === 'true';
        const obra = (select.selectedOptions?.[0]?.textContent || '').trim();
        const caminho = (pasta?.value || '').trim();

        if (!escolheu) {
            alvo.textContent = '';
            const aviso = document.createElement('span');
            aviso.innerHTML = `${rotuloObra}: <strong>nenhuma</strong>. ${semEscolha}`;
            alvo.appendChild(aviso);
            return;
        }

        alvo.textContent = '';
        const parteObra = document.createElement('span');
        parteObra.innerHTML = `${rotuloObra}: <strong></strong>. ${rotuloPasta}: `;
        parteObra.querySelector('strong').textContent = obra;
        alvo.appendChild(parteObra);

        // O caminho vai por textContent — é dado do usuário, e um `<` no nome da pasta não pode
        // virar marcação. Foi a diferença entre as duas cópias que este módulo unifica.
        const parteCaminho = document.createElement(caminho ? 'code' : 'strong');
        parteCaminho.className = caminho ? 'alvo-pasta' : '';
        parteCaminho.textContent = caminho || 'ainda não informada';
        alvo.appendChild(parteCaminho);
        alvo.appendChild(document.createTextNode('.'));
    };

    select.addEventListener('change', pintar);
    pasta?.addEventListener('input', pintar);
    pintar();
    return pintar;
}
