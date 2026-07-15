package org.traducao.projeto.trocaTipoLegenda.domain;

import java.util.List;

public record ResultadoGeralAuditoria(
    List<AuditoriaLegendaResultado> arquivos,
    int totalArquivosAnalisados,
    int totalComProblemas
) {}
