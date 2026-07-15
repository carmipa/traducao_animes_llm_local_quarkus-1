package org.traducao.projeto.telemetria;

import java.util.List;

public record MidiaTelemetria(
    String nomeArquivo,
    String formatoContainer,
    Double tamanhoMB,
    Double duracaoSegundos,
    String codecVideo,
    String resolucao,
    Double fps,
    List<LegendaTelemetria> legendas,
    String registradoEm
) {
    public record LegendaTelemetria(
        Integer indexRelativo,
        String idioma,
        String formato,
        String tipo,
        String categoria,
        boolean traduzivel,
        Double diferencaFimSegundos
    ) {}
}
