package org.traducao.projeto.legenda.domain;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza falha ao ler ou escrever um arquivo de legenda
 * (I/O, formato inválido, seção ausente) dentro do módulo compartilhado
 * {@code legenda}. É a exceção de I/O de legenda que leitores e escritores lançam
 * e que os fluxos consumidores tratam como falha de arquivo.
 *
 * <p>INVARIANTES DO DOMÍNIO: estende {@link ExcecaoLegenda} (raiz do módulo legenda),
 * portanto é {@code BasePipelineException} e NÃO é {@code TradutorException}. Preserva
 * os dois construtores canônicos (mensagem; mensagem+causa); não adiciona estado nem
 * lógica.
 *
 * <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
 * verificada; é mapeada para resposta HTTP pelo {@code BasePipelineExceptionMapper} e
 * pode ser capturada por blocos que tratem {@link ExcecaoLegenda} (ou este tipo).
 */
public class ArquivoLegendaException extends ExcecaoLegenda {
    public ArquivoLegendaException(String message) {
        super(message);
    }

    public ArquivoLegendaException(String message, Throwable cause) {
        super(message, cause);
    }
}
