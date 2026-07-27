package org.traducao.projeto.raspagemRevisao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho da comparação de datas entre o cache de tradução e a legenda já
 * publicada. É ele que decide se a ponte 5→6 sincroniza o ASS a partir do cache mais novo.
 *
 * <h2>Por que três valores e não um booleano</h2>
 * Havia um {@code boolean} que colapsava dois casos diferentes em {@code false}: "a legenda está
 * atualizada" e "não foi possível ler as datas". O segundo precisa de aviso ao operador — a
 * sincronização automática ficou desligada e ele não tem como saber —, e um booleano não carrega
 * essa distinção. É a mesma lição que o {@code StatusRaspagem} custou: um desfecho inferido por
 * igualdade é lido de formas inconsistentes por quem chama.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só {@link #CACHE_MAIS_NOVO} autoriza sobrescrever a legenda. Empate de data NÃO autoriza:
 *       na dúvida, preserva-se o ASS atual.</li>
 *   <li>{@link #INDETERMINADO} exige aviso visível de quem chama; silenciá-lo devolve o problema
 *       que este enum existe para resolver.</li>
 *   <li>Enum puro de domínio: só JDK, sem framework e sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de desfecho; não lança.
 */
public enum FrescorCache {

    /** O cache foi mantido DEPOIS da legenda — a sincronização 5→6 pode acontecer. */
    CACHE_MAIS_NOVO,

    /** A legenda está igual ou mais nova que o cache — nada a sincronizar. */
    LEGENDA_ATUAL,

    /** Arquivo ausente ou metadados ilegíveis — não se sabe, e por isso não se sobrescreve. */
    INDETERMINADO
}
