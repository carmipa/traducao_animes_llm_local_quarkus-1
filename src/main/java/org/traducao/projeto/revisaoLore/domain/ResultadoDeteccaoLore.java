package org.traducao.projeto.revisaoLore.domain;

import java.util.List;

public record ResultadoDeteccaoLore(boolean suspeito, List<String> motivos) {

    public static ResultadoDeteccaoLore limpo() {
        return new ResultadoDeteccaoLore(false, List.of());
    }
}
