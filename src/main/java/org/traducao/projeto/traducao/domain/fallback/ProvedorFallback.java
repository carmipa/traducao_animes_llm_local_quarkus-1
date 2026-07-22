package org.traducao.projeto.traducao.domain.fallback;

/**
 * PROPÓSITO DE NEGÓCIO: identifica QUAL provedor produziu (ou tentou produzir) a recuperação
 * de uma fala que o LLM local não conseguiu traduzir. Existe para que o relatório da execução
 * responda "quem recuperou o quê" em vez de apenas "quantas foram recuperadas" — sem isso, a
 * cadeia de provedores vira uma caixa-preta e não há como decidir se um provedor paga o próprio
 * custo operacional.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Enum puro de domínio: só JDK, sem framework, sem I/O e sem conhecer o transporte
 *       concreto de nenhum provedor.</li>
 *   <li>{@link #NENHUM} não é um provedor: marca a tentativa que sequer chegou a um deles
 *       (modo desligado, entrada inválida), distinguindo "não tentou" de "tentou e falhou".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de identidade; não lança e não valida nada.
 */
public enum ProvedorFallback {

    /** Instância local do LibreTranslate (CPU), segundo nível da cadeia. */
    LIBRETRANSLATE,

    /** Endpoint público do Google, último recurso externo da cadeia. */
    GOOGLE,

    /** Nenhum provedor foi acionado (modo desligado ou entrada inválida). */
    NENHUM
}
