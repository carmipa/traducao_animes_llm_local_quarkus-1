package org.traducao.projeto.raspagemRevisao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: de onde vem o texto ORIGINAL contra o qual a legenda PT-BR é julgada. É a
 * escolha mais consequente da revisão: comparar contra o original errado produz "correções" que
 * degradam uma tradução boa, e o operador só descobre assistindo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Em {@link #CACHE} não se usa {@code .ass} inglês irmão NEM casamento por texto não
 *       validado. Só entradas cujo vínculo foi confirmado (índice + estilo + proveniência + texto)
 *       viram referência; o resto fica SEM_REFERÊNCIA_SEGURA e é pendência, nunca comparação
 *       silenciosa.</li>
 *   <li>{@link #AMBOS} é o comportamento histórico e mais permissivo: o {@code .ass} inglês manda e
 *       o cache preenche lacunas.</li>
 *   <li>Enum puro de domínio: só JDK, sem framework e sem I/O. Era aninhado no caso de uso, o que
 *       obrigava o controller e o preparador a importarem o caso de uso só para nomear o modo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de escolha; não lança.
 */
public enum ModoReferenciaRevisao {

    /** Legenda inglesa irmã como referência principal, cache preenchendo lacunas. */
    AMBOS,

    /** Somente o cache, entrada a entrada, com vínculo confirmado. */
    CACHE
}
