package org.traducao.projeto.traducao.domain.ports;

import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: contrato da fatia Tradução Local para uma recuperação de
 * ÚLTIMO RECURSO online de uma única fala que o LLM local não conseguiu traduzir.
 * É uma porta PRÓPRIA da fatia (não reaproveita o módulo manual {@code raspagemCorrecao},
 * cuja fronteira arquitetural é proibida à Tradução Local): a implementação vive na
 * infraestrutura da própria fatia e traduz apenas a fala pendente informada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Recebe só o texto ORIGINAL de uma fala já dada como pendente nesta execução;
 *       nunca varre cache de execuções anteriores.</li>
 *   <li>Preserva a formatação (tags ASS e quebras {@code \N}) da fala: se o provedor
 *       externo corromper qualquer marcador, a implementação devolve {@link Optional#empty()}
 *       em vez de uma legenda com formatação quebrada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de rede, resposta inválida, tradução idêntica ao original ou marcador corrompido
 * resultam em {@link Optional#empty()} — a fala permanece pendente, exatamente como sem o
 * fallback. Nunca lança para o chamador.
 */
public interface FallbackTraducaoOnlinePort {

    /**
     * PROPÓSITO DE NEGÓCIO: tenta traduzir online uma única fala pendente, preservando
     * as tags e quebras da formatação ASS.
     *
     * <p>INVARIANTES DO DOMÍNIO: a saída, quando presente, mantém as mesmas tags/quebras
     * do original; caso contrário o resultado é vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Optional#empty()} em qualquer
     * falha (rede, resposta vazia/igual, marcador corrompido); não lança.
     *
     * @param original o texto original (com tags) da fala pendente
     * @return a tradução com formatação preservada, ou vazio se não foi possível traduzir com segurança
     */
    Optional<String> traduzir(String original);
}
