package org.traducao.projeto.correcaoLegendas.domain;

import java.util.List;

public record CorrecaoLegendasRelatorioJson(
    ResumoOperacaoCorrecaoLegendas operacao,
    String pastaOriginal,
    String pastaTraduzida,
    boolean llmHabilitado,
    String contexto,
    ResultadoCorrecaoLegendas resultado,
    List<LogEventoCorrecaoLegendas> eventosSessao
) {}
