package org.traducao.projeto.traducao.domain.contextocena;

/**
 * PROPÓSITO DE NEGÓCIO: porta PRÓPRIA da Tradução Local para traduzir UMA fala usando o
 * contexto de cena ao redor. Existe para manter a semântica de "janela de cena / falante /
 * gênero" DENTRO da fatia {@code traducao}, sem contaminar o peer técnico neutro {@code llm}
 * ({@code Lote}/{@code LlmPort} não conhecem cena nem gênero). O adaptador que a implementa
 * monta a chamada HTTP reusando apenas infraestrutura técnica neutra da fatia.
 *
 * <p>INVARIANTES DO DOMÍNIO: traduz SÓ a fala-alvo da requisição; as vizinhas entram apenas
 * como referência e NUNCA são traduzidas nem devolvidas; {@code lote=1} no alvo é preservado
 * (uma fala por chamada, uma linha de saída). Contrato de domínio, só JDK.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a implementação define a política de falha (ex.:
 * devolver a fala-alvo original quando o modelo não responde de forma utilizável); o contrato
 * não obriga exceção específica.
 */
public interface TradutorContextualPort {

    /**
     * PROPÓSITO DE NEGÓCIO: traduz a fala-alvo da requisição usando o contexto de cena.
     * <p>INVARIANTES DO DOMÍNIO: devolve a tradução de UMA linha (a fala-alvo); o contexto
     * não vaza na resposta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ver contrato da classe (política da implementação).
     *
     * @param requisicao prompt de sistema congelado + janela contextual da fala-alvo
     * @return a tradução PT-BR da fala-alvo (uma única linha)
     */
    String traduzirComContexto(RequisicaoTraducaoContextual requisicao);
}
