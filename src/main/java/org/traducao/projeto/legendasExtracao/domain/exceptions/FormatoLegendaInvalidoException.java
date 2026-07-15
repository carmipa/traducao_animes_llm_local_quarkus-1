package org.traducao.projeto.legendasExtracao.domain.exceptions;

import org.traducao.projeto.legendasExtracao.domain.ExtratorException;

public class FormatoLegendaInvalidoException extends ExtratorException {
    public FormatoLegendaInvalidoException(String message) {
        super(message);
    }

    public FormatoLegendaInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
