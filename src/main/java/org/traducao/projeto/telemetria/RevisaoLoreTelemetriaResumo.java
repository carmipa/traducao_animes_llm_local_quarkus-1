package org.traducao.projeto.telemetria;

/**
 * Métricas agregadas das sessões de Revisão de Lore para o painel de Telemetria.
 */
public record RevisaoLoreTelemetriaResumo(
    int totalSessoes,
    int totalArquivosProcessados,
    int totalFalasSinalizadas,
    int totalFalasCorrigidas,
    Integer taxaCorrecaoPercent
) {}
