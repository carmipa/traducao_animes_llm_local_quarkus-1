package org.traducao.projeto.llm.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: resultado da tradução de um {@link Lote} pelo LLM — as linhas
 * traduzidas mais o desfecho (sucesso ou falha com diagnóstico), para que o pipeline
 * decida entre publicar, retentar ou preservar o original.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code idLote} espelha o do {@link Lote} de origem, correlacionando pedido e resposta.</li>
 *   <li>{@code linhasTraduzidas} corresponde às linhas originais na mesma ordem.</li>
 *   <li>{@code sucesso} indica se a tradução é utilizável; {@code mensagemErro} traz o
 *       diagnóstico quando não é.</li>
 *   <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Em falha, {@code sucesso} é {@code false} e {@code mensagemErro} descreve a causa; o
 * chamador é quem decide preservar a tradução anterior. Este tipo não lança.
 *
 * @param idLote identificador do lote, herdado do {@link Lote} de origem
 * @param linhasTraduzidas linhas traduzidas, na ordem das originais
 * @param sucesso {@code true} se a tradução é utilizável
 * @param mensagemErro diagnóstico quando {@code sucesso} é {@code false}
 */
public record TraducaoLote(
    int idLote,
    List<String> linhasTraduzidas,
    boolean sucesso,
    String mensagemErro
) {
}
