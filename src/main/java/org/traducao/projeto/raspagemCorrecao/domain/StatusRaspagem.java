package org.traducao.projeto.raspagemCorrecao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho semântico de uma tentativa de recuperar uma fala num tradutor
 * de máquina externo. Substitui a convenção antiga de "texto de saída == original" — que era
 * ambígua e interpretada de formas <b>inconsistentes</b> pelos consumidores (um tratava como
 * falha, outro como 'sem alteração'). Também é a base do retry seletivo: só
 * {@link #FALHA_TRANSITORIA} vale repetir; resposta estruturalmente inválida ou tag corrompida
 * não deve ser retentada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Exatamente um status por tentativa; {@link #SUCESSO} é o único desfecho publicável.</li>
 *   <li>Os NOMES das constantes são contrato de dados, não detalhe interno: são gravados crus no
 *       dataset de auditoria da correção ({@code resposta.status().name()}) e impressos no
 *       painel. Renomear uma constante quebra a comparabilidade histórica do JSONL sem quebrar
 *       compilação nenhuma — por isso não se renomeia.</li>
 *   <li>Enum puro de domínio: só JDK, sem framework, sem I/O, sem conhecer transporte.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de causa; não lança. Todo status diferente de {@link #SUCESSO} implica manter o texto
 * original — nunca gravar tradução vazia.
 */
public enum StatusRaspagem {
    /** Tradução válida e diferente do original. */
    SUCESSO,
    /** Google devolveu texto idêntico ao original — nada a corrigir. */
    SEM_ALTERACAO,
    /** HTTP transitório (408/429/5xx), timeout ou falha de rede — pode valer retry. */
    FALHA_TRANSITORIA,
    /** HTTP não transitório, JSON inesperado ou resposta sem segmentos traduzíveis. */
    RESPOSTA_INVALIDA,
    /** Marcador de tag/quebra mutilado ou tag ASS perdida na volta da tradução. */
    TAG_CORROMPIDA
}
