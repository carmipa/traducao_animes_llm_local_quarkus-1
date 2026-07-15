package org.traducao.projeto.telemetria;

/**
 * Registro persistido de operações do pipeline que não passam pelo LLM de tradução
 * (revisão de legendas, correção Google, limpeza de cache, etc.).
 */
public record OperacaoTelemetria(
    String tipo,
    String detalhe,
    Long tempoTotalMs,
    Integer arquivosProcessados,
    Integer itensDetectados,
    Integer itensCorrigidos,
    String registradoEm
) {}
