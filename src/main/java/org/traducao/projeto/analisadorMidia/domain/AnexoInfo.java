package org.traducao.projeto.analisadorMidia.domain;

/**
 * Anexo do contêiner (ex.: fontes de karaokê em MKV), reportado pelo ffprobe
 * como stream {@code codec_type: attachment}.
 */
public record AnexoInfo(
    String nomeArquivo,
    String mimeType,
    long tamanhoBytes
) {}
