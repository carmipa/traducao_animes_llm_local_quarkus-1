package org.traducao.projeto.analisadorMidia.domain;

/**
 * Falha individual na análise de um arquivo do lote — representada no resultado
 * (em vez de apenas logada), para que a UI exiba o que não pôde ser analisado.
 */
public record FalhaAnalise(
    String arquivo,
    String erro
) {}
