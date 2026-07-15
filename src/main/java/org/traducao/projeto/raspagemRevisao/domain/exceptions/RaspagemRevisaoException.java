package org.traducao.projeto.raspagemRevisao.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class RaspagemRevisaoException extends BasePipelineException {
    public RaspagemRevisaoException(String message) {
        super(message);
    }

    public RaspagemRevisaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
