package org.traducao.projeto.trocaTipoLegenda.domain;

public record AuditoriaFonteInfo(
    String estilo,
    String fonteAtual,
    String fonteSugerida,
    boolean problematica
) {}
