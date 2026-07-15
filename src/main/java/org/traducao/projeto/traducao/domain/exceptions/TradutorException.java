package org.traducao.projeto.traducao.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class TradutorException extends BasePipelineException {
    public TradutorException(String message) {
        super(message);
    }
    
    public TradutorException(String message, Throwable cause) {
        super(message, cause);
    }
}
