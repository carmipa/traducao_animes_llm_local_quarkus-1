package org.traducao.projeto.traducao.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Resultado por arquivo da tradução — o que a tabela da UI
 * mostra (Arquivo | Lore | Falas | Cache | Traduzidas | Pendentes | Tempo | Status)
 * e o que consolida o status do lote. Substitui o retorno "só o Path", que escondia
 * se o arquivo concluiu, falhou ou foi bloqueado.
 *
 * <p>{@code tempoMs}, {@code falasPendentes} e {@code pendenciasPorCausa} existiam
 * desde sempre DENTRO do use case e só viajavam para
 * {@code logs/telemetria_traducao.json}: o console recebia cinco números de um
 * registro de doze campos, e responder "quanto tempo levou" ou "por que sobrou
 * pendência" exigia abrir o JSON à mão. Aqui eles chegam à apresentação pelo mesmo
 * caminho do resto — sem nova medição e sem ler o arquivo de telemetria de volta.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code arquivo} e {@code status} nunca nulos;
 * {@code arquivoSaida} é nulo quando o arquivo não gerou saída (falha/bloqueio);
 * as contagens são zero nesses casos. {@code falasPendentes} conta falas DISTINTAS
 * que sobraram depois do fallback — é o que decide PARCIAL —, enquanto
 * {@code avisos} inclui mensagens meramente informativas e NÃO é a mesma coisa.
 * {@code pendenciasPorCausa} é cópia imutável.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; as fábricas não lançam.
 * Lista nula vira lista vazia.
 */
public record ResultadoTraducaoArquivo(
    Path arquivoSaida,
    String arquivo,
    String lore,
    int falasTraduziveis,
    int falasDoCache,
    int falasTraduzidas,
    int avisos,
    StatusArquivoTraducao status,
    long tempoMs,
    int falasPendentes,
    List<ResumoPendencia> pendenciasPorCausa
) {
    public ResultadoTraducaoArquivo {
        pendenciasPorCausa = pendenciasPorCausa == null ? List.of() : List.copyOf(pendenciasPorCausa);
    }

    public static ResultadoTraducaoArquivo concluido(
            Path arquivoSaida, String arquivo, String lore,
            int falasTraduziveis, int falasDoCache, int falasTraduzidas, int avisos) {
        StatusArquivoTraducao status = avisos > 0 ? StatusArquivoTraducao.PARCIAL : StatusArquivoTraducao.CONCLUIDO;
        return new ResultadoTraducaoArquivo(arquivoSaida, arquivo, lore,
            falasTraduziveis, falasDoCache, falasTraduzidas, avisos, status, 0L, 0, List.of());
    }

    public static ResultadoTraducaoArquivo falha(String arquivo, String lore) {
        return new ResultadoTraducaoArquivo(null, arquivo, lore, 0, 0, 0, 1,
            StatusArquivoTraducao.FALHOU, 0L, 0, List.of());
    }

    public static ResultadoTraducaoArquivo bloqueado(String arquivo, String lore) {
        return new ResultadoTraducaoArquivo(null, arquivo, lore, 0, 0, 0, 1,
            StatusArquivoTraducao.BLOQUEADO, 0L, 0, List.of());
    }
}
