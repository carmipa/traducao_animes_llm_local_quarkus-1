package org.traducao.projeto.traducao.domain.fallback;

/**
 * PROPÓSITO DE NEGÓCIO: taxonomia da CAUSA de uma tentativa de recuperação de fala pendente.
 * Substitui o {@code Optional.empty()} que antes colapsava nove desfechos diferentes num único
 * "vazio", apagando a razão da recusa. É a mesma lição que o motor de contexto de cena custou
 * caro: um {@code motivo} calculado e descartado deixou 813 falas em branco sem uma linha de
 * diagnóstico. Aqui a causa é um valor de domínio — não pode ser esquecida.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Exatamente um status por tentativa; {@link #RECUPERADA} é o único desfecho de sucesso e
 *       o único em que há tradução publicável.</li>
 *   <li>As causas separam o que é responsabilidade do PROVEDOR (indisponível, timeout, HTTP,
 *       resposta vazia) do que é responsabilidade da NOSSA validação (marcador, guarda de lore,
 *       validação canônica) — sem essa separação não se sabe se vale trocar de provedor.</li>
 *   <li>Enum puro de domínio: só JDK, sem framework e sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de causa; não lança. Todo status diferente de {@link #RECUPERADA} implica manter o
 * texto original — nunca gravar tradução vazia.
 */
public enum StatusFallback {

    /** Tradução aceita por todas as verificações; é a única publicável. */
    RECUPERADA,

    /** Modo desligado, entrada vazia ou provedor não configurado — nem chegou a tentar. */
    NAO_TENTADA,

    /** Provedor fora do ar (container parado, conexão recusada). */
    PROVEDOR_INDISPONIVEL,

    /** Estourou o tempo limite da chamada. */
    TIMEOUT,

    /** Provedor respondeu com status HTTP de erro. */
    HTTP_ERRO,

    /** Provedor respondeu, mas o corpo veio vazio ou em formato inesperado. */
    RESPOSTA_VAZIA,

    /** A "tradução" devolvida é igual ao original — o provedor não traduziu. */
    ECO,

    /** Tags ASS ou quebras {@code \N} perdidas, duplicadas ou mutiladas na volta. */
    MARCADOR_CORROMPIDO,

    /** Um termo protegido da lore, sigla ou identificador não sobreviveu à tradução. */
    GUARDA_LORE,

    /** Reprovada pela validação canônica final (a mesma aplicada à saída do LLM). */
    REPROVADA_VALIDACAO
}
