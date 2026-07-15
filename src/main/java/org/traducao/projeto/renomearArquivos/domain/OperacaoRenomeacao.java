package org.traducao.projeto.renomearArquivos.domain;

import java.util.List;

public record OperacaoRenomeacao(
    String idOperacao,
    String dataHora,
    String pastaBase,
    List<ItemRenomeado> itens
) {
    public record ItemRenomeado(
        String nomeOriginal,
        String nomeNovo
    ) {}
}
