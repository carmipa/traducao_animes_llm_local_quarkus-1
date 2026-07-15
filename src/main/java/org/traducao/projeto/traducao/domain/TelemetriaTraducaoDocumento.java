package org.traducao.projeto.traducao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: raiz do arquivo canônico próprio da telemetria da
 * Tradução Local. Projeta o ESTADO FINAL consolidado por episódio (não é
 * append-only) mais os quatro contadores persistentes da fatia, com um
 * {@code schemaVersion} explícito para evolução do contrato de arquivo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code registros} contém no máximo uma entrada por episódio (chave
 *       normalizada por {@link NormalizadorNomeEpisodio}); o mais recente vence.</li>
 *   <li>Os quatro contadores representam SOMENTE os incrementos da Tradução Local
 *       após a adoção deste arquivo (iniciam em zero) — nunca copiam o legado —,
 *       para que a agregação com {@code telemetria_compartilhada.json} não
 *       sobreponha eventos já contados.</li>
 *   <li>{@code schemaVersion} identifica a versão do contrato de arquivo (ex.: "1.0").</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Documento ausente ou ilegível é tratado pelos leitores como vazio (versão nula,
 * lista vazia, contadores zero), sem destruir o arquivo físico.
 */
public record TelemetriaTraducaoDocumento(
    String schemaVersion,
    List<TelemetriaTraducao> registros,
    int alucinacoesPrevenidas,
    int respostasTraducaoRejeitadas,
    int falhasTraducaoRecuperadas,
    int fallbacksTraducaoMantidos
) {
}
