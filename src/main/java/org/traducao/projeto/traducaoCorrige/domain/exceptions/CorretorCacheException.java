package org.traducao.projeto.traducaoCorrige.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class CorretorCacheException extends BasePipelineException {
    public CorretorCacheException(String message) {
        super(message);
    }

    public CorretorCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
