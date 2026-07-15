package org.traducao.projeto.traducaoKaraoke.domain;

import java.util.List;

/**
 * Resumo, por arquivo .ass, do que a tradução de karaokê classificou e fez.
 * Alimenta o console da UI, o manifesto de auditoria e a telemetria.
 */
public record ResultadoTraducaoKaraoke(
    String arquivo,
    String arquivoDestino,
    int eventosTotais,
    int efeitosKfxPreservados,
    int preservadasOriginalJapones,
    int jaEmPortugues,
    int paraTraduzir,
    int reaproveitadasCache,
    int traduzidas,
    int mantidasSemTraducao,
    List<String> avisos
) {
}
