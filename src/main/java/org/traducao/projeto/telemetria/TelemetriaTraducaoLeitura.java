package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: DTO de LEITURA próprio do módulo de telemetria para
 * interpretar o arquivo canônico da Tradução Local ({@code telemetria_traducao.json}).
 * Permite ao Painel Unificado consolidar a telemetria da tradução como agregador
 * CQRS read-only, SEM importar as classes de domínio do pacote {@code traducao} — o
 * contrato entre os módulos é exclusivamente o JSON no filesystem.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Espelha o schema do arquivo por estrutura, ignorando campos desconhecidos,
 *       para tolerar evolução do {@code schemaVersion} sem quebrar a leitura.</li>
 *   <li>É estritamente de leitura: o módulo de telemetria nunca escreve este arquivo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Estruturas ausentes desserializam como {@code null}/vazias; o agregador trata um
 * documento ausente, vazio ou ilegível como conjunto vazio, sem destruir o arquivo.
 */
public class TelemetriaTraducaoLeitura {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Registro(
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
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Documento(
        String schemaVersion,
        List<Registro> registros,
        int alucinacoesPrevenidas,
        int respostasTraducaoRejeitadas,
        int falhasTraducaoRecuperadas,
        int fallbacksTraducaoMantidos
    ) {
        /** Documento vazio determinístico para arquivo ausente/ilegível. */
        public static Documento vazio() {
            return new Documento(null, List.of(), 0, 0, 0, 0);
        }
    }
}
