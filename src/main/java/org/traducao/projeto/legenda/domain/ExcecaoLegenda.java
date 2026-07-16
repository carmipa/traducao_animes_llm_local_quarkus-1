package org.traducao.projeto.legenda.domain;

import org.traducao.projeto.core.exception.BasePipelineException;

/**
 * PROPÓSITO DE NEGÓCIO: raiz da hierarquia de exceções pertencentes ao módulo
 * compartilhado {@code legenda} — falhas ligadas a arquivos e conteúdo de legenda.
 * NÃO representa falhas gerais de tradução nem do LLM (essas vivem sob
 * {@code TradutorException}, na fatia {@code traducao}); é a base específica das
 * falhas do domínio de legenda, consumível por qualquer fatia.
 *
 * <p>INVARIANTES DO DOMÍNIO: estende {@code BasePipelineException} (core), herdando
 * {@code errorId} e {@code timestamp}; é concreta e oferece apenas os dois construtores
 * canônicos (mensagem; mensagem+causa). Não declara estado próprio, código de
 * infraestrutura nem status HTTP — o mapeamento HTTP é responsabilidade única do
 * {@code BasePipelineExceptionMapper}, comum a toda a família.
 *
 * <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
 * verificada; por ser {@code BasePipelineException}, é convertida em resposta HTTP
 * estruturada pelo mapper e pode ser capturada por qualquer bloco que trate
 * {@code ExcecaoLegenda} ou uma de suas subclasses.
 */
public class ExcecaoLegenda extends BasePipelineException {
    public ExcecaoLegenda(String message) {
        super(message);
    }

    public ExcecaoLegenda(String message, Throwable cause) {
        super(message, cause);
    }
}
