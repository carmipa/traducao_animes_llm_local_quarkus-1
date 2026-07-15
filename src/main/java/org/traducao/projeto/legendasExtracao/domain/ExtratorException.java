package org.traducao.projeto.legendasExtracao.domain;

import org.traducao.projeto.core.exception.BasePipelineException;

public class ExtratorException extends BasePipelineException {
    public ExtratorException(String message) {
        super(message);
    }

    public ExtratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
