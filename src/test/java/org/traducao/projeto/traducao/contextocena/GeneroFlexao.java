package org.traducao.projeto.traducao.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: classifica a flexão de gênero exibida por um trecho de português
 * — masculina, feminina, neutra (sem marca) ou ambígua (marcas dos dois gêneros na mesma
 * fala). É a saída da "régua" de medição {@link VerificadorPropriedadeGenero}, usada no
 * conjunto-ouro (D0) para provar objetivamente que "Estou certa" (dito por homem) carrega
 * flexão feminina — sem depender do LLM nem de verdade-base externa.
 *
 * <p>INVARIANTES DO DOMÍNIO: enum fechado; {@link #NEUTRO} significa AUSÊNCIA de adjetivo/
 * particípio marcado (ex.: "Tenho certeza"), nunca "gênero desconhecido"; {@link #AMBIGUO}
 * só ocorre quando marcas masculinas E femininas coexistem (ex.: "Estou cansado e ela está
 * cansada"), caso legítimo que exige revisão e não pode ser tratado como violação simples.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: tipo puro sem estado; não lança.
 */
public enum GeneroFlexao {
    MASCULINO,
    FEMININO,
    NEUTRO,
    AMBIGUO
}
