package org.traducao.projeto.remuxer.domain;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza que um MKV final já existe e deve ser
 * preservado, impedindo sobrescrita ou remoção acidental.
 *
 * <p>INVARIANTES DO DOMÍNIO: é lançada antes de criar processo ou arquivo
 * temporário para o remux atual.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o caso de uso registra o item como pendente
 * seguro e mantém o destino existente intacto.
 */
public class SaidaRemuxJaExisteException extends RemuxerException {
    /**
     * PROPÓSITO DE NEGÓCIO: informa qual destino bloqueou a nova operação.
     *
     * <p>INVARIANTES DO DOMÍNIO: mensagem descreve preservação, não perda.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga ao relatório do lote.
     */
    public SaidaRemuxJaExisteException(String message) {
        super(message);
    }
}
