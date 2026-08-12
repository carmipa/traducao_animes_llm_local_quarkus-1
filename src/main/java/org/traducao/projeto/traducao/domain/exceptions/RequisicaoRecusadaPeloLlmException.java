package org.traducao.projeto.traducao.domain.exceptions;

/**
 * PROPÓSITO DE NEGÓCIO: o servidor LLM respondeu e RECUSOU este pedido — HTTP 4xx permanente,
 * esgotadas as tentativas do adaptador. É problema DESTA fala, não do servidor, e por isso
 * pertence à mesma família de {@link DivergenciaLinhasException}: retenta com outra
 * temperatura, pede segunda opinião e, no limite, deixa a fala pendente com o original.
 *
 * <h2>O prejuízo que originou</h2>
 * 2026-08-11, DanMachi S01 E03. Um verso de karaokê mascarado com um {@code [[TAGn]]} por
 * LETRA fez o {@code towerinstruct-mistral-7b-v0.2} entrar em loop degenerado; o LM Studio
 * recusou o próprio output do modelo e devolveu HTTP 400. Como a recusa chegava ao pipeline
 * como {@link TradutorException} genérica — o mesmo tipo de "o servidor caiu" —, o episódio
 * inteiro foi abortado: <b>373 falas de diálogo perdidas por causa de um verso de música</b>.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só nasce de resposta HTTP recebida e classificada como permanente pelo adaptador.
 *       Timeout, 5xx e conexão recusada continuam sendo {@link TradutorException}, que aborta
 *       — insistir com o servidor fora do ar só gasta tempo.</li>
 *   <li>Estende {@link TradutorException} de propósito: quem já capturava a família toda
 *       (o catch defensivo de {@code traduzirEValidar}) continua capturando.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * É a própria sinalização de falha; não lança nada por conta própria.
 */
public class RequisicaoRecusadaPeloLlmException extends TradutorException {
    public RequisicaoRecusadaPeloLlmException(String message) {
        super(message);
    }
}
