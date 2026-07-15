package org.traducao.projeto.correcaoLegendas.domain;

import org.traducao.projeto.telemetria.OperacaoTelemetria;

import java.util.List;

public record CorrecaoLegendasRelatorioJson(
    OperacaoTelemetria operacao,
    String pastaOriginal,
    String pastaTraduzida,
    boolean llmHabilitado,
    String contexto,
    ResultadoCorrecaoLegendas resultado,
    List<LogEventoCorrecaoLegendas> eventosSessao
) {}
