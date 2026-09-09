package org.traducao.projeto.traducao.domain.exceptions;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza que o LLM parou de gerar por ter batido no teto de tokens, e
 * não por ter terminado a tradução. O texto que volta nesse caso é uma fala CORTADA no meio —
 * "I promise to protect everyone here." devolvido como "Eu prometo" — e publicá-lo na legenda é
 * pior do que não traduzir, porque parece tradução completa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Truncamento é FALHA, nunca sucesso: quem o recebe não pode devolver
 *       {@code sucesso=true}. Era exatamente o defeito medido em 2026-09-09, com resposta
 *       controlada devolvendo {@code content="Eu prometo"} e {@code finish_reason="length"}.</li>
 *   <li>É recuperável dentro do limite de tentativas do adaptador, como a resposta vazia: a
 *       geração seguinte pode caber no teto.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é a própria sinalização de falha; participa das tentativas
 * e, esgotadas elas, o lote volta sem sucesso e com a mensagem de diagnóstico.
 */
public class RespostaLlmTruncadaException extends TradutorException {
    public RespostaLlmTruncadaException(String message) {
        super(message);
    }
}
