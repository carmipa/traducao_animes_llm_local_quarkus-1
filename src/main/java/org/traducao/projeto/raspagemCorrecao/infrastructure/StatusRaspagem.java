package org.traducao.projeto.raspagemCorrecao.infrastructure;

/**
 * Desfecho semântico de uma tentativa de tradução via Google Translate.
 * <p>
 * Substitui a convenção antiga de "texto de saída == original" — que era
 * ambígua e interpretada de formas <b>inconsistentes</b> pelos consumidores (um
 * tratava como falha, outro como 'sem alteração'). Também é a base para um retry
 * seletivo: só {@link #FALHA_TRANSITORIA} vale repetir; resposta estruturalmente
 * inválida ou tag corrompida não deve ser retentada.
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
