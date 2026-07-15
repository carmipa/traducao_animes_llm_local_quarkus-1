package org.traducao.projeto.analisadorMidia.domain;

/**
 * Faixa de legenda detectada, com classificação de traduzibilidade e flags do
 * contêiner. Os indicadores temporais ({@code duracaoSegundos},
 * {@code diferencaFimSegundos}) são apenas INFORMAÇÃO TÉCNICA — o módulo não
 * emite veredito automático de sincronismo.
 */
public record LegendaInfo(
    Integer index,
    Integer indexRelativo,
    String idioma,
    String formato,
    String codecId,
    String titulo,
    String tipoCompleto,
    String tipoCurto,
    String categoria,        // TEXTO | BITMAP | DESCONHECIDO
    boolean extraivel,
    boolean traduzivel,
    boolean exigeOcr,
    boolean isDefault,
    boolean isForced,
    boolean acessibilidade,  // hearing/visual impaired
    Double duracaoSegundos,
    Double diferencaFimSegundos
) {}
