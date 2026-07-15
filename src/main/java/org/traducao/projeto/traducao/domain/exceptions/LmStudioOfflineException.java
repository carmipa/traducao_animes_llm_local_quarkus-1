package org.traducao.projeto.traducao.domain.exceptions;

public class LmStudioOfflineException extends TradutorException {
    public LmStudioOfflineException(String message) {
        super(message);
    }

    public LmStudioOfflineException(String message, Throwable cause) {
        super(message, cause);
    }
}
