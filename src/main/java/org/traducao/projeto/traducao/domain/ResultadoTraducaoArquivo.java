package org.traducao.projeto.traducao.domain;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: Resultado por arquivo da tradução — o que a tabela da UI
 * mostra (Arquivo | Lore | Falas | Cache | Traduzidas | Avisos | Status) e o que
 * consolida o status do lote. Substitui o retorno "só o Path", que escondia se o
 * arquivo concluiu, falhou ou foi bloqueado.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code arquivo} e {@code status} nunca nulos;
 * {@code arquivoSaida} é nulo quando o arquivo não gerou saída (falha/bloqueio);
 * as contagens são zero nesses casos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; as fábricas não lançam.
 */
public record ResultadoTraducaoArquivo(
    Path arquivoSaida,
    String arquivo,
    String lore,
    int falasTraduziveis,
    int falasDoCache,
    int falasTraduzidas,
    int avisos,
    StatusArquivoTraducao status
) {
    public static ResultadoTraducaoArquivo concluido(
            Path arquivoSaida, String arquivo, String lore,
            int falasTraduziveis, int falasDoCache, int falasTraduzidas, int avisos) {
        StatusArquivoTraducao status = avisos > 0 ? StatusArquivoTraducao.PARCIAL : StatusArquivoTraducao.CONCLUIDO;
        return new ResultadoTraducaoArquivo(arquivoSaida, arquivo, lore,
            falasTraduziveis, falasDoCache, falasTraduzidas, avisos, status);
    }

    public static ResultadoTraducaoArquivo falha(String arquivo, String lore) {
        return new ResultadoTraducaoArquivo(null, arquivo, lore, 0, 0, 0, 1, StatusArquivoTraducao.FALHOU);
    }

    public static ResultadoTraducaoArquivo bloqueado(String arquivo, String lore) {
        return new ResultadoTraducaoArquivo(null, arquivo, lore, 0, 0, 0, 1, StatusArquivoTraducao.BLOQUEADO);
    }
}
