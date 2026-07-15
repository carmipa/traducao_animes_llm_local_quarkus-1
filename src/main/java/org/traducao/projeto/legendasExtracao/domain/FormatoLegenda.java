package org.traducao.projeto.legendasExtracao.domain;

import org.traducao.projeto.legendasExtracao.domain.exceptions.FormatoLegendaInvalidoException;

public enum FormatoLegenda {
    ASS("ass", "ASS/SSA (SubStation Alpha)"),
    PGS("sup", "PGS (Presentation Graphic Stream)"),
    SRT("srt", "SRT (SubRip Text)");

    private final String extensaoSaida;
    private final String descricao;

    FormatoLegenda(String extensaoSaida, String descricao) {
        this.extensaoSaida = extensaoSaida;
        this.descricao = descricao;
    }

    public String getExtensaoSaida() {
        return extensaoSaida;
    }

    public String getDescricao() {
        return descricao;
    }

    public static FormatoLegenda fromString(String formatoStr) {
        if (formatoStr == null || formatoStr.isBlank()) {
            throw new FormatoLegendaInvalidoException("Formato de legenda não informado. Escolha ASS, PGS ou SRT.");
        }
        return switch (formatoStr.toUpperCase()) {
            case "ASS" -> ASS;
            case "PGS" -> PGS;
            case "SRT" -> SRT;
            default -> throw new FormatoLegendaInvalidoException(
                    "Formato de legenda inválido: \"" + formatoStr + "\". Escolha ASS, PGS ou SRT.");
        };
    }
}
