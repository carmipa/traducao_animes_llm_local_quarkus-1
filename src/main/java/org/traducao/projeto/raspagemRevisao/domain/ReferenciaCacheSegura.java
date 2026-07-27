package org.traducao.projeto.raspagemRevisao.domain;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: o que o cache pode servir de referência para uma legenda, separado do que
 * ele NÃO pode. As duas coleções juntas são o ponto: uma fala sem vínculo seguro não é uma fala
 * sem referência — é uma fala que ninguém deve comparar, e tratá-las igual foi o defeito que este
 * tipo existe para impedir.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os dois conjuntos são disjuntos: um índice está numa lista ou na outra, nunca em ambas.</li>
 *   <li>Falas SEM entrada no índice não aparecem em nenhum dos dois — não ter referência é
 *       diferente de ter uma referência duvidosa.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de dados; não lança.
 *
 * @param originaisPorIndice original inglês confiável de cada fala, por índice
 * @param semReferenciaSegura índices cujo vínculo com o cache não pôde ser confirmado
 */
public record ReferenciaCacheSegura(
    Map<Integer, String> originaisPorIndice,
    Set<Integer> semReferenciaSegura
) {
}
