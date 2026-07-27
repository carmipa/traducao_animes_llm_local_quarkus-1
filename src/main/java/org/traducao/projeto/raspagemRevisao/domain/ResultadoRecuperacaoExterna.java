package org.traducao.projeto.raspagemRevisao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho tipado de uma tentativa de recuperação externa durante a revisão
 * de legendas — o {@link StatusRecuperacaoExterna} e o texto associado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Em {@link StatusRecuperacaoExterna#SUCESSO}, {@code texto} é a tradução; em qualquer outro
 *       caso é o <b>texto original</b>, nunca vazio. A revisão escreve por cima de legenda já
 *       publicada: um "vazio" em caso de recusa apagaria uma fala boa.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de desfecho; não lança.
 *
 * @param status causa canônica do desfecho
 * @param texto a tradução em caso de sucesso; o texto original em todos os demais
 */
public record ResultadoRecuperacaoExterna(StatusRecuperacaoExterna status, String texto) {

    /**
     * PROPÓSITO DE NEGÓCIO: informa se há tradução aproveitável, sem o chamador comparar status.
     * <p>INVARIANTES DO DOMÍNIO: verdadeiro só para {@link StatusRecuperacaoExterna#SUCESSO}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança.
     */
    public boolean sucesso() {
        return status == StatusRecuperacaoExterna.SUCESSO;
    }
}
