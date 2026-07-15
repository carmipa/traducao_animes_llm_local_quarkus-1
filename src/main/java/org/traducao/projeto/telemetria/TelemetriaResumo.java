package org.traducao.projeto.telemetria;

import java.util.List;

/**
 * Resumo serializável da telemetria acumulada na sessão atual do servidor,
 * consumido pelo painel "Telemetria" da interface web.
 */
public record TelemetriaResumo(
    int cacheCount,
    int totalEpisodios,
    int totalLinhas,
    long tempoMedioPorLinhaMs,
    int totalCacheHits,
    List<OperacaoHistorico> historicoOperacoes,
    List<LlmTelemetria> traducoesLlm,
    List<OperacaoTelemetria> operacoes,
    RevisaoLoreTelemetriaResumo revisaoLore,
    int alucinacoesPrevenidas,
    int totalErros,
    double jvmCpuUso,
    int jvmThreadsAtivas,
    long jvmHeapUsadoBytes,
    long jvmHeapMaxBytes,
    int arquivosSanitizados,
    int respostasTraducaoRejeitadas,
    int falhasTraducaoRecuperadas,
    int fallbacksTraducaoMantidos
) {}
