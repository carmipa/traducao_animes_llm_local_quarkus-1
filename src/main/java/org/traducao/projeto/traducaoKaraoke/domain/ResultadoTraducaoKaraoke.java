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
    /**
     * Falas em que a camada portuguesa teve acento REPOSTO nesta fatia — o efeito medido do
     * {@link AcentosLetraKaraoke} mais o do dicionário. Existe para que o defeito apareça na
     * telemetria DA FATIA no momento em que acontece, em vez de ser descoberto meses depois
     * lendo o {@code .ass}: na tradução do 86 de 2026-08-14, 5 falas distintas saíram com
     * {@code nao} sem acento e ninguém tinha um número para olhar.
     */
    int acentosRepostos,
    List<String> avisos
) {
}
