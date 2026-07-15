package org.traducao.projeto.mapaProjeto.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class MapaProjetoException extends BasePipelineException {
    public MapaProjetoException(String message) {
        super(message);
    }

    public MapaProjetoException(String message, Throwable cause) {
        super(message, cause);
    }
}
