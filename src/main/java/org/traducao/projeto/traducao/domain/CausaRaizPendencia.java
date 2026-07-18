package org.traducao.projeto.traducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: nomeia a causa-raiz pela qual uma fala elegível terminou
 * pendente (sem tradução confiável) na Tradução Local, para o painel de telemetria
 * medir onde estão as perdas — em vez de contar o mesmo evento duas vezes como
 * "tags corrompidas" e "eco". A ORDEM de declaração é a PRECEDÊNCIA: quando mais de
 * uma causa se aplicaria à mesma fala (ex.: o LLM corrompe o marcador {@code [[TAGn]]},
 * o pipeline devolve o original e o avaliador ainda o marca como "devolveu o original"),
 * a primeira declarada vence — a corrupção de marcador é a causa real, o eco é o sintoma.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #MARCADORES_CORROMPIDOS} tem sempre a maior precedência (ordinal 0):
 *       vence {@link #ECO} quando ambos ocorrem na mesma fala.</li>
 *   <li>{@link #ECO} tem a MENOR precedência: qualquer causa concreta vence o eco,
 *       porque o eco costuma ser o desfecho secundário de outra falha.</li>
 *   <li>Uma fala pendente recebe EXATAMENTE uma causa-raiz (a mais grave aplicável).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * É um enum puro, sem estado nem I/O; não lança. A comparação de precedência usa o
 * {@code ordinal()} nativo — reordenar as constantes altera deliberadamente a política.
 */
public enum CausaRaizPendencia {
    /** O LLM perdeu/duplicou/inventou marcadores {@code [[TAGn]]}; o desmascarar recusou a resposta. */
    MARCADORES_CORROMPIDOS,
    /** A resposta veio com número de linhas divergente ou quebras/tags estruturalmente incompatíveis. */
    ESTRUTURA_DIVERGENTE,
    /** Resíduo em inglês, idioma incorreto, preâmbulo ou recusa/meta-resposta detectados no texto. */
    RESIDUO,
    /** O modelo devolveu uma resposta vazia. */
    VAZIA,
    /** O modelo devolveu o texto original sem traduzir (eco) — menor precedência. */
    ECO;

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe, entre duas causas aplicáveis à MESMA fala, a de
     * maior precedência (a causa-raiz real), impedindo dupla contagem no painel.
     *
     * <p>INVARIANTES DO DOMÍNIO: retorna a de menor {@code ordinal()}; {@code null} é
     * tratado como ausência (a outra vence). Comutativa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: com ambas {@code null}, retorna {@code null}.
     */
    public static CausaRaizPendencia maisGrave(CausaRaizPendencia a, CausaRaizPendencia b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.ordinal() <= b.ordinal() ? a : b;
    }
}
