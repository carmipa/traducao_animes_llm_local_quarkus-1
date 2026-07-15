package org.traducao.projeto.apiDadosAnime.domain.exceptions;

public class AnimeNaoEncontradoException extends ApiDadosAnimeException {
    public AnimeNaoEncontradoException(String message) {
        super(message);
    }

    public AnimeNaoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}
