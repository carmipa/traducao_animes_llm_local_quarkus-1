package org.traducao.projeto.trocaTipoLegenda.domain;

import java.util.List;

public record AuditoriaLegendaResultado(
    String arquivo,
    String tipoLegenda,
    List<AuditoriaFonteInfo> fontes,
    boolean temProblemas
) {}
