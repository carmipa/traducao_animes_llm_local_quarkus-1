package org.traducao.projeto.apiDadosAnime.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class ApiDadosAnimeException extends BasePipelineException {
    public ApiDadosAnimeException(String message) {
        super(message);
    }

    public ApiDadosAnimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
