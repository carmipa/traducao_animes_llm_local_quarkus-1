package org.traducao.projeto.analisadorMidia.domain;

public record AudioInfo(
    Integer index,
    String idioma,
    String format,
    Integer channels,
    Double sampleRateKHz,
    Long bitrate,
    String titulo
) {}
