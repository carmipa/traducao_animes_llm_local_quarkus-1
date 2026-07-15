package org.traducao.projeto.trocaTipoLegenda.domain;

import java.util.List;

public record AuditoriaLegendaResultado(
    String arquivo,
    List<AuditoriaFonteInfo> fontes,
    boolean temProblemas
) {}
