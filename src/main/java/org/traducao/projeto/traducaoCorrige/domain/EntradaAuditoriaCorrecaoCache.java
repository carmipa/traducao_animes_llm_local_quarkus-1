package org.traducao.projeto.traducaoCorrige.domain;

/**
 * PROPÓSITO DE NEGÓCIO: registra cada decisão que alterou ou tentou reparar uma
 * tradução persistida, formando dataset auditável para descobrir falhas e
 * aperfeiçoar o pipeline.
 *
 * <p>INVARIANTES DO DOMÍNIO: o registro é append-only e contém antes/depois,
 * operação, resultado, motivo, lore e arquivo de origem.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; a infraestrutura de
 * persistência registra warning sem interromper a correção principal.
 */
public record EntradaAuditoriaCorrecaoCache(
    String instante,
    String operacao,
    String arquivo,
    int indice,
    String estilo,
    String contextoId,
    String contextoHash,
    String modeloLlm,
    String resultado,
    String motivo,
    String original,
    String traducaoAntes,
    String traducaoDepois,
    String detalhe
) {}
