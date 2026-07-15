package org.traducao.projeto.traducao.domain;

import java.util.List;

public record Lote(
    int idLote,
    List<String> linhasOriginais
) {
}
