package org.traducao.projeto.apiDadosAnime.domain.model;

import java.util.List;

public record AnimeMetadata(
    String titulo,
    String tituloIngles,
    String tituloJapones,
    String posterUrl,
    Integer ano,
    Integer episodios,
    Double score,
    String sinopse,
    List<String> generos
) {}
