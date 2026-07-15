package org.traducao.projeto.revisaoLore.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class RevisaoLoreException extends BasePipelineException {

    public RevisaoLoreException(String message) {
        super(message);
    }

    public RevisaoLoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
