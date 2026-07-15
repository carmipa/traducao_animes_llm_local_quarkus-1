package org.traducao.projeto.traducao.domain;

/**
 * Resultado da checagem de disponibilidade do servidor LLM local (ex: LM Studio)
 * feita no início da execução, antes de começar a traduzir qualquer episódio.
 */
public record StatusLlm(
    boolean servidorOnline,
    boolean modeloCarregado,
    String mensagem
) {
}
