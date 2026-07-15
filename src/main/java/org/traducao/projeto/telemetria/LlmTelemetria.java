package org.traducao.projeto.telemetria;

import java.util.List;

public record LlmTelemetria(
    String nomeEpisodio,
    String modeloLlm,
    Integer totalLinhas,
    Integer falasTraduzidas,
    Integer falasDoCache,
    Long tempoTotalMs,
    List<String> errosOcorridos,
    String animeNome,
    String temporada,
    String registradoEm,
    String loreNome,
    String statusFinal
) {
    /**
     * Compat: construtor antigo (sem lore/status) para chamadas legadas — assume
     * lore desconhecido e status CONCLUIDO. Novos registros usam o construtor
     * completo para carregar a proveniência (lore) e o desfecho na telemetria.
     */
    public LlmTelemetria(
        String nomeEpisodio, String modeloLlm, Integer totalLinhas, Integer falasTraduzidas,
        Integer falasDoCache, Long tempoTotalMs, List<String> errosOcorridos, String animeNome,
        String temporada, String registradoEm) {
        this(nomeEpisodio, modeloLlm, totalLinhas, falasTraduzidas, falasDoCache, tempoTotalMs,
            errosOcorridos, animeNome, temporada, registradoEm, null, null);
    }
}
