package org.traducao.projeto.analisadorMidia.domain;

import org.traducao.projeto.core.exception.BasePipelineException;

public class AnalisadorException extends BasePipelineException {
    public AnalisadorException(String message) {
        super(message);
    }

    public AnalisadorException(String message, Throwable cause) {
        super(message, cause);
    }
}
