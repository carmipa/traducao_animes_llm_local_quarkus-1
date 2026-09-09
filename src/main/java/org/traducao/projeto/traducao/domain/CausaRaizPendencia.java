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
    /**
     * O servidor parou de gerar por ter batido no teto de tokens ({@code finish_reason=length})
     * e a fala voltou cortada no meio. Precedência logo abaixo do marcador: quando acontece,
     * TODO o resto — estrutura divergente, resíduo, eco — é sintoma de um texto que nunca
     * chegou inteiro, e atribuir a causa ao sintoma manda procurar o defeito no lugar errado.
     *
     * <p>Só passou a ser distinguível em 2026-09-09: até então o adaptador nem desserializava
     * o {@code finish_reason} e declarava sucesso, então esta causa era invisível por
     * construção, não por não existir.
     */
    RESPOSTA_TRUNCADA,
    /**
     * A tradução alterou ou apagou um identificador numérico da fonte (ex.: {@code 04th Team}
     * publicado como {@code Equipe 08}). Precedência logo abaixo do marcador e acima da
     * estrutura: é corrupção de CONTEÚDO, não de formatação, e o eco que ela possa gerar é
     * apenas sintoma.
     */
    IDENTIFICADOR_NUMERICO_ALTERADO,
    /** A resposta veio com número de linhas divergente ou quebras/tags estruturalmente incompatíveis. */
    ESTRUTURA_DIVERGENTE,
    /**
     * O modelo falou SOBRE a tarefa em vez de executá-la — recusa, preâmbulo, meta-resposta,
     * recitação do prompt de sistema. Separada de {@link #RESIDUO} em 2026-09-09 porque as duas
     * pedem conserto oposto: recusa se ataca no PROMPT e no portão de recusa, resíduo se ataca
     * na cobertura de idioma. Coladas na mesma métrica, o painel dizia "resíduo subiu" quando o
     * que subira era o modelo se recusando a traduzir.
     */
    RECUSA_DO_MODELO,
    /** Resíduo em inglês ou idioma incorreto detectados no texto. */
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
