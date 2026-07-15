package org.traducao.projeto.legendasExtracao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: Linha da tabela de resultado da extração — o que Paulo vê
 * por vídeo (Vídeo | Formato | Track | Arquivo gerado | Status). É o registro
 * granular que o relatório agregado ({@link RelatorioExtracao}) não expunha antes.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code video}, {@code formato} e {@code status}
 * nunca são nulos. {@code trackId} e {@code arquivoGerado} são nulos justamente
 * quando não houve faixa/arquivo (ex.: {@link StatusExtracao#FAIXA_NAO_ENCONTRADA}),
 * e a UI os renderiza como "—".
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; as fábricas não validam e
 * não lançam — o chamador é responsável por passar dados coerentes com o status.
 */
public record ItemExtracao(
    String video,
    String formato,
    Integer trackId,
    String arquivoGerado,
    StatusExtracao status,
    String detalhe
) {
    public static ItemExtracao sucesso(String video, String formato, int trackId, String arquivoGerado) {
        return new ItemExtracao(video, formato, trackId, arquivoGerado, StatusExtracao.SUCESSO, null);
    }

    public static ItemExtracao semFaixa(String video, String formato) {
        return new ItemExtracao(video, formato, null, null, StatusExtracao.FAIXA_NAO_ENCONTRADA, null);
    }

    public static ItemExtracao jaExiste(String video, String formato, int trackId, String arquivoGerado) {
        return new ItemExtracao(video, formato, trackId, arquivoGerado, StatusExtracao.JA_EXISTE,
            "Arquivo preservado; extração ignorada para não sobrescrever.");
    }

    public static ItemExtracao timeout(String video, String formato, int trackId) {
        return new ItemExtracao(video, formato, trackId, null, StatusExtracao.TIMEOUT, null);
    }

    public static ItemExtracao falha(String video, String formato, Integer trackId, String detalhe) {
        return new ItemExtracao(video, formato, trackId, null, StatusExtracao.FALHA, detalhe);
    }
}
