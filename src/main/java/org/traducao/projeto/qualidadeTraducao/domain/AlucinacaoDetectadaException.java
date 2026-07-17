package org.traducao.projeto.qualidadeTraducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: sinaliza que o LLM alucinou de forma que compromete a
 * INTEGRIDADE da legenda — corrupção/perda dos marcadores de formatação
 * ({@code [[TAGn]]}) ou fala rejeitada pelo validador de qualidade. Impede que uma
 * saída corrompida seja publicada como se fosse tradução válida. Pertence ao peer
 * compartilhado {@code qualidadeTraducao}, consumível por qualquer fatia.
 *
 * <p>INVARIANTES DO DOMÍNIO: é subclasse de {@link ExcecaoQualidadeTraducao} (logo de
 * {@code BasePipelineException}) — a partir da E8b NÃO é mais {@code TradutorException};
 * carrega apenas a mensagem descritiva da alucinação, sem estado próprio.
 *
 * <p>COMPORTAMENTO EM CASO DE PROPAGAÇÃO: propaga como {@code RuntimeException} não
 * verificada; nos fluxos de tradução é normalmente absorvida pela divisão/retry/fallback,
 * e os sítios que antes a capturavam via {@code TradutorException} preservam o
 * tratamento por captura explícita ({@code catch (... | AlucinacaoDetectadaException)}).
 */
public class AlucinacaoDetectadaException extends ExcecaoQualidadeTraducao {

    /**
     * PROPÓSITO DE NEGÓCIO: cria a falha de alucinação com a descrição do problema
     * detectado (ex.: marcadores perdidos, fala rejeitada).
     * <p>INVARIANTES DO DOMÍNIO: delega a {@link ExcecaoQualidadeTraducao}, preservando
     * a mensagem.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; mensagem nula é aceita.
     */
    public AlucinacaoDetectadaException(String message) {
        super(message);
    }
}
