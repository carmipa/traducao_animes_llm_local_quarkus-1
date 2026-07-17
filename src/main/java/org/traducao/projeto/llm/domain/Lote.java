package org.traducao.projeto.llm.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: unidade de trabalho enviada ao LLM — um conjunto de linhas
 * originais a traduzir de uma vez, identificado para que a resposta possa ser
 * correlacionada de volta ao pedido.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code idLote} identifica o lote e é preservado no {@link TraducaoLote} de resposta.</li>
 *   <li>{@code linhasOriginais} é a sequência a traduzir, na ordem em que deve ser devolvida.</li>
 *   <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não valida os argumentos; é um portador de dados. A ausência ou o formato inválido de
 * linhas é tratado pela implementação da porta, não por este tipo.
 *
 * @param idLote identificador do lote, ecoado na resposta
 * @param linhasOriginais linhas originais a traduzir, na ordem de saída esperada
 */
public record Lote(
    int idLote,
    List<String> linhasOriginais
) {
}
