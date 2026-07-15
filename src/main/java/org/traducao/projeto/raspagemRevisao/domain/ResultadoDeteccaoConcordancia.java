package org.traducao.projeto.raspagemRevisao.domain;

import java.util.List;

public record ResultadoDeteccaoConcordancia(boolean suspeito, List<String> motivos) {

    public static ResultadoDeteccaoConcordancia limpo() {
        return new ResultadoDeteccaoConcordancia(false, List.of());
    }
}
