package org.traducao.projeto.trocaTipoLegenda.domain.exceptions;

import org.traducao.projeto.core.exception.BasePipelineException;

public class TrocaTipoLegendaException extends BasePipelineException {

    public TrocaTipoLegendaException(String message) {
        super(message);
    }

    public TrocaTipoLegendaException(String message, Throwable cause) {
        super(message, cause);
    }
}
