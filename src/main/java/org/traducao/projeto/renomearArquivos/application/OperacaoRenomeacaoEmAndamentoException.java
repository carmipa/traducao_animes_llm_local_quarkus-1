package org.traducao.projeto.renomearArquivos.application;

/**
 * PROPÓSITO DE NEGÓCIO: impede duas operações de renomeação concorrentes na
 * mesma pasta de mídia, evitando corridas e manifestos inconsistentes.
 *
 * <p>INVARIANTES DO DOMÍNIO: uma pasta normalizada admite no máximo uma
 * simulação, aplicação ou reversão por vez.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é lançada antes de qualquer alteração em
 * disco e convertida pelo controller em HTTP 409.
 */
public class OperacaoRenomeacaoEmAndamentoException extends RuntimeException {
    /**
     * PROPÓSITO DE NEGÓCIO: comunica de forma didática que a pasta já está em
     * processamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: a mensagem identifica uma recusa segura, não
     * uma falha parcial.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga a mensagem ao tratamento HTTP.
     */
    public OperacaoRenomeacaoEmAndamentoException(String mensagem) {
        super(mensagem);
    }
}
