package org.traducao.projeto.traducaoKaraoke.domain;

/**
 * Erro de negócio do módulo Tradução de Karaokê (validação de pastas,
 * LLM indisponível, falha de leitura/escrita das legendas).
 */
public class TraducaoKaraokeException extends RuntimeException {

    public TraducaoKaraokeException(String mensagem) {
        super(mensagem);
    }

    public TraducaoKaraokeException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
