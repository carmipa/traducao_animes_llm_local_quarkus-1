package org.traducao.projeto.traducao.domain.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: a requisição de uma tradução COM contexto de cena — o prompt de
 * sistema congelado do job (mesma origem carimbada na proveniência) e a janela contextual
 * da fala-alvo. É o insumo da {@link TradutorContextualPort}: entra a janela, sai a tradução
 * de UMA fala (a alvo), com as vizinhas servindo só de referência para o modelo inferir o
 * falante/gênero que a fonte não declara.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code record} imutável, só JDK; {@code promptSistema} é o
 * prompt JÁ congelado no início do job (não se busca de novo aqui, para não misturar origens
 * dentro do mesmo episódio); {@code janela} traz a fala-alvo e suas vizinhas elegíveis.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; não valida — a porta/adaptador
 * que consome decide o que fazer com campos nulos.
 *
 * @param promptSistema prompt de sistema congelado do job
 * @param janela janela contextual (fala-alvo + vizinhas de referência)
 */
public record RequisicaoTraducaoContextual(
    String promptSistema,
    JanelaContextual janela
) {
}
