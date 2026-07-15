package org.traducao.projeto.core.exception;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BasePipelineException extends RuntimeException {
    private final LocalDateTime timestamp;
    private final String errorId;

    public BasePipelineException(String message) {
        super(message);
        this.timestamp = LocalDateTime.now();
        this.errorId = UUID.randomUUID().toString();
    }

    public BasePipelineException(String message, Throwable cause) {
        super(message, cause);
        this.timestamp = LocalDateTime.now();
        this.errorId = UUID.randomUUID().toString();
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getErrorId() {
        return errorId;
    }
}
