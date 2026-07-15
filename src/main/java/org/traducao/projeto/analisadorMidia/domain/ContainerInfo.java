package org.traducao.projeto.analisadorMidia.domain;

public record ContainerInfo(
    String formato,
    Long tamanhoBytes,
    Double duracaoSegundos,
    Long bitrateGeral,
    String aplicacaoEscrita
) {}
