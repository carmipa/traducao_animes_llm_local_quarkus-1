package org.traducao.projeto.novoKaraoke.domain;

/** Falha de negócio na conversão de karaokê para legenda simples. */
public class NovoKaraokeException extends RuntimeException {

    public NovoKaraokeException(String mensagem) {
        super(mensagem);
    }

    public NovoKaraokeException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
