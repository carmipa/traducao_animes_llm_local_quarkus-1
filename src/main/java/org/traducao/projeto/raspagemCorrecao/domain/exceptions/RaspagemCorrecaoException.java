package org.traducao.projeto.raspagemCorrecao.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class RaspagemCorrecaoException extends BasePipelineException {
    public RaspagemCorrecaoException(String message) {
        super(message);
    }

    public RaspagemCorrecaoException(String message, Throwable cause) {
        super(message, cause);
    }
}
