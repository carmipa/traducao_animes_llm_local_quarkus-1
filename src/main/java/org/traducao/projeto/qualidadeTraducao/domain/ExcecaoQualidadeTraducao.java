package org.traducao.projeto.qualidadeTraducao.domain;

import org.traducao.projeto.core.exception.BasePipelineException;

/**
 * PROPÓSITO DE NEGÓCIO: raiz da hierarquia de exceções do módulo compartilhado
 * {@code qualidadeTraducao} — falhas ligadas à QUALIDADE do texto traduzido
 * (alucinação, corrupção de marcadores de formatação) detectadas por regras
 * consumíveis por qualquer fatia. NÃO representa falhas gerais de tradução/LLM
 * (essas vivem sob {@code TradutorException}, na fatia {@code traducao}), de legenda
 * (sob {@code ExcecaoLegenda}) nem de contexto (sob {@code ExcecaoContexto}).
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
 * {@code ExcecaoQualidadeTraducao} ou uma de suas subclasses.
 */
public class ExcecaoQualidadeTraducao extends BasePipelineException {

    /**
     * PROPÓSITO DE NEGÓCIO: cria a falha de qualidade com uma mensagem explicativa.
     * <p>INVARIANTES DO DOMÍNIO: delega ao {@code BasePipelineException}, preservando
     * a mensagem e carimbando {@code errorId}/{@code timestamp}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; a mensagem nula é aceita como no
     * contrato de {@code RuntimeException}.
     */
    public ExcecaoQualidadeTraducao(String message) {
        super(message);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria a falha de qualidade preservando a causa raiz.
     * <p>INVARIANTES DO DOMÍNIO: delega ao {@code BasePipelineException}, preservando
     * mensagem e causa encadeada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; mensagem/causa nulas são aceitas.
     */
    public ExcecaoQualidadeTraducao(String message, Throwable cause) {
        super(message, cause);
    }
}
