package org.traducao.projeto.remuxer.domain;

import java.nio.file.Path;

public record RemuxTarefa(
    String nomeVideo,
    Path caminhoVideo,
    Path caminhoLegenda,
    Path caminhoSaida
) {
}
