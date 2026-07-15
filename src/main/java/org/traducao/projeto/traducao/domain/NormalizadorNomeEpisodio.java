package org.traducao.projeto.traducao.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: proprietário ÚNICO, dentro da Tradução Local, da
 * normalização do nome de episódio usada como chave de deduplicação da telemetria
 * própria. Garante que o mesmo episódio — apesar de variações inócuas de caixa,
 * espaços, forma Unicode ou diretório — projete uma única entrada consolidada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Considera apenas o nome do arquivo (descarta diretórios), aparando e
 *       colapsando espaços, normalizando Unicode para NFC e reduzindo a caixa.</li>
 *   <li>É CONSERVADORA: mantém a extensão e os números intactos — {@code ep1} e
 *       {@code ep11}, ou {@code .ass} e {@code .srt}, permanecem distintos.</li>
 *   <li>Determinística e idempotente: {@code normalizar(normalizar(x)) == normalizar(x)}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entrada {@code null} ou em branco devolve string vazia, chave estável para
 * registros sem nome.
 */
public final class NormalizadorNomeEpisodio {

    private NormalizadorNomeEpisodio() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: produz a chave canônica de deduplicação de um episódio.
     * <p>INVARIANTES DO DOMÍNIO: nome de arquivo apenas, NFC, sem espaços
     * redundantes, minúsculo; extensão e dígitos preservados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null}/branco devolve {@code ""}.
     */
    public static String normalizar(String nomeEpisodio) {
        if (nomeEpisodio == null || nomeEpisodio.isBlank()) {
            return "";
        }
        String semDiretorio = nomeEpisodio.replace('\\', '/');
        int barra = semDiretorio.lastIndexOf('/');
        if (barra >= 0) {
            semDiretorio = semDiretorio.substring(barra + 1);
        }
        String nfc = Normalizer.normalize(semDiretorio, Normalizer.Form.NFC);
        String semEspacosRedundantes = nfc.strip().replaceAll("\\s+", " ");
        return semEspacosRedundantes.toLowerCase(Locale.ROOT);
    }
}
