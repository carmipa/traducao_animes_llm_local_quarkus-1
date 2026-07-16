package org.traducao.projeto.contexto.domain;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza que um id de contexto/lore selecionado na UI não
 * corresponde a nenhum provedor registrado. Impede que um anime seja traduzido com
 * a lore errada silenciosamente — cair no contexto padrão sem aviso esconderia o
 * erro de seleção do operador.
 *
 * <p>INVARIANTES DO DOMÍNIO: pertence ao módulo compartilhado {@code contexto} e
 * estende {@link ExcecaoContexto} (deixou de ser {@code TradutorException} na E7a),
 * portanto continua sendo {@code BasePipelineException}; mensagem preservada
 * (lista os contextos disponíveis). Só é lançada por quem resolve o contexto ativo
 * a partir de um id explícito não vazio.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: propaga como {@code RuntimeException} não
 * verificada; é convertida em resposta HTTP estruturada pelo
 * {@code BasePipelineExceptionMapper} (comum a toda a família) e pode ser capturada
 * por qualquer bloco que trate {@link ExcecaoContexto} ou {@code BasePipelineException}.
 */
public class ContextoNaoEncontradoException extends ExcecaoContexto {
    public ContextoNaoEncontradoException(String message) {
        super(message);
    }
}
