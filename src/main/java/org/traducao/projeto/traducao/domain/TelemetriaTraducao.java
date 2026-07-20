package org.traducao.projeto.traducao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: registro individual de telemetria da Tradução Local por
 * episódio — a unidade que a fatia grava no seu arquivo canônico próprio
 * ({@code logs/telemetria_traducao.json}), preservando proveniência (lore),
 * modelo, volume, origem das falas, desfecho, avisos e timestamp.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Modelo de dados próprio da Tradução Local — não reutiliza tipos do módulo
 *       de telemetria; o contrato entre os módulos é apenas o JSON no filesystem.</li>
 *   <li>{@code nomeEpisodio} identifica o episódio; a chave de deduplicação é a
 *       forma normalizada por {@link NormalizadorNomeEpisodio}.</li>
 *   <li>{@code registradoEm} é o timestamp UTC ISO-8601 da atualização, usado como
 *       critério de precedência dentro da mesma fonte.</li>
 *   <li>{@code errosOcorridos} e {@code pendenciasPorCausa} são cópias defensivas imutáveis:
 *       o estado gravado nunca diverge do momento do registro por mutação externa da lista.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Campos ausentes são serializados como {@code null}; a ausência de nome resolve
 * para a chave vazia na deduplicação. Listas nulas viram listas vazias imutáveis.
 */
public record TelemetriaTraducao(
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
    String statusFinal,
    List<ResumoPendencia> pendenciasPorCausa
) {
    /**
     * PROPÓSITO DE NEGÓCIO: blinda o registro contra aliasing — congela as listas no
     * instante do registro para o JSON persistido jamais refletir mutações posteriores
     * da lista original do chamador.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code errosOcorridos}/{@code pendenciasPorCausa} viram
     * cópias imutáveis; {@code null} vira lista vazia.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: listas com elemento {@code null} fazem
     * {@link List#copyOf} lançar {@code NullPointerException} (entrada inválida).
     */
    public TelemetriaTraducao {
        errosOcorridos = errosOcorridos == null ? List.of() : List.copyOf(errosOcorridos);
        pendenciasPorCausa = pendenciasPorCausa == null ? List.of() : List.copyOf(pendenciasPorCausa);
    }
}
