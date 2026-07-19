/**
 * Renderiza as opções de contexto/obra num <select>, agrupando por franquia
 * (ctx.grupo) em <optgroup>. A lista JÁ vem ordenada do backend (seção alfabética
 * + ordem cronológica dentro do grupo), então basta abrir um novo <optgroup>
 * quando o grupo muda. Obras sem grupo (ex.: "86", "Guilty Crown") viram <option>
 * soltas, fora de qualquer <optgroup>.
 *
 * @param {HTMLSelectElement} select destino (será preenchido com <option>/<optgroup>)
 * @param {Array<{id:string,nome:string,grupo?:string}>} contextos itens já ordenados
 * @param {(opt:HTMLOptionElement, ctx:object)=>void} [aoConstruirOption] ajuste por opção
 *        (ex.: pré-selecionar padrão, gravar dataset)
 */
export function montarOpcoesContextos(select, contextos, aoConstruirOption) {
    let grupoAtual = null;
    let optgroupAtual = null;
    contextos.forEach(ctx => {
        const opt = document.createElement('option');
        opt.value = ctx.id;
        opt.textContent = ctx.nome;
        if (typeof aoConstruirOption === 'function') {
            aoConstruirOption(opt, ctx);
        }
        const grupo = ctx.grupo || '';
        if (!grupo) {
            select.appendChild(opt);
            grupoAtual = null;
            optgroupAtual = null;
        } else {
            if (grupo !== grupoAtual) {
                optgroupAtual = document.createElement('optgroup');
                optgroupAtual.label = grupo;
                select.appendChild(optgroupAtual);
                grupoAtual = grupo;
            }
            optgroupAtual.appendChild(opt);
        }
    });
}
