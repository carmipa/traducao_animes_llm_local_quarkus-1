package org.traducao.projeto.correcaoLegendas.domain;

public record LogEventoCorrecaoLegendas(
    String timestampUtc,
    String nivel,
    String arquivo,
    String mensagem
) {}
