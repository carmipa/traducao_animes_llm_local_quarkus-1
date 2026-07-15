package org.traducao.projeto.analisadorMidia.domain;

/**
 * Capítulo (marcador de tempo) do contêiner, como reportado por
 * {@code ffprobe -show_chapters}.
 */
public record CapituloInfo(
    int numero,
    String titulo,
    double inicioSegundos,
    double fimSegundos
) {}
