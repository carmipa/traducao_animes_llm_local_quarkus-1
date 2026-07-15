package org.traducao.projeto.traducao.domain.legenda;

import java.util.List;

public record DocumentoLegenda(
    String cabecalho,
    List<EventoLegenda> eventos,
    String quebraDeLinha,
    boolean comBom
) {
}
