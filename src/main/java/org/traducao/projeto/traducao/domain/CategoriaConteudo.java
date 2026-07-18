package org.traducao.projeto.traducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: classifica o TIPO de conteúdo de uma fala para separar o KPI
 * de pendência por natureza — diálogo real versus as várias camadas musicais —, para
 * que a alta pendência intencional em karaokê/romaji/KFX nunca distorça o número que
 * importa (diálogo). O balde vem do classificador de karaokê real, nunca de heurística
 * de densidade de tags.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #ROMAJI_PRESERVADO}, {@link #KFX} e {@link #LETREIRO} são preservação
 *       INTENCIONAL: pendência alta neles não é defeito.</li>
 *   <li>{@link #DIALOGO} é o único balde cujo alvo de pendência é ~0.</li>
 *   <li>{@link #MUSICA_LATINA} (letra inglesa/latina de OP/ED) é traduzível por decisão
 *       de negócio e conta como perda quando fica pendente.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Enum puro, sem estado nem I/O; não lança.
 */
public enum CategoriaConteudo {
    /** Fala de diálogo comum — alvo de pendência ~0. */
    DIALOGO,
    /** Letra de música em idioma latino/inglês, traduzível por decisão de negócio. */
    MUSICA_LATINA,
    /** Letra original em japonês/romaji — preservada de propósito, nunca vai ao LLM. */
    ROMAJI_PRESERVADO,
    /** Camada de efeito de karaokê (sílaba/animação quadro a quadro) — preservada. */
    KFX,
    /** Letreiro/placa/título — typesetting preservado. */
    LETREIRO
}
