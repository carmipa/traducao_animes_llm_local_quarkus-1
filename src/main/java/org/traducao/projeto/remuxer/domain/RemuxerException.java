package org.traducao.projeto.remuxer.domain;

import org.traducao.projeto.core.exception.BasePipelineException;

public class RemuxerException extends BasePipelineException {
    public RemuxerException(String message) {
        super(message);
    }

    public RemuxerException(String message, Throwable cause) {
        super(message, cause);
    }
}
