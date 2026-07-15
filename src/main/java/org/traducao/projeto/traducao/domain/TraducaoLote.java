package org.traducao.projeto.traducao.domain;

import java.util.List;

public record TraducaoLote(
    int idLote,
    List<String> linhasTraduzidas,
    boolean sucesso,
    String mensagemErro
) {
}
