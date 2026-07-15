package org.traducao.projeto.legendasExtracao.domain;

public record FaixaLegenda(
    int id,
    String type,
    String codec,
    String codecId,
    String idioma,
    String nome,
    boolean isDefault,
    boolean isForced
) {
}
