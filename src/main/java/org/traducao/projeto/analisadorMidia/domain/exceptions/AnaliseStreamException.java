package org.traducao.projeto.analisadorMidia.domain.exceptions;

import org.traducao.projeto.analisadorMidia.domain.AnalisadorException;

public class AnaliseStreamException extends AnalisadorException {
    public AnaliseStreamException(String message) {
        super(message);
    }

    public AnaliseStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
