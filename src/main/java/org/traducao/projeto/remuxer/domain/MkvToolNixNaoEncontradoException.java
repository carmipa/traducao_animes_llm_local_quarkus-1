package org.traducao.projeto.remuxer.domain;

public class MkvToolNixNaoEncontradoException extends RemuxerException {
    public MkvToolNixNaoEncontradoException(String message) {
        super(message);
    }

    public MkvToolNixNaoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}
