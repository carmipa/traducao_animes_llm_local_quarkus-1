package org.traducao.projeto.analisadorMidia.domain;

public record VideoInfo(
    Integer index,
    String codecId,
    String format,
    Integer width,
    Integer height,
    Integer bitDepth,
    Double fps,
    String displayAspectRatio,
    Long bitrate
) {}
