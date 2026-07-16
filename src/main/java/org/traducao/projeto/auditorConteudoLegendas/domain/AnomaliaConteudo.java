package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.legenda.domain.EventoLegenda;

public record AnomaliaConteudo(
        TipoSeveridade severidade,
        String regra,
        String descricao,
        EventoLegenda eventoOriginal,
        EventoLegenda eventoTraduzido,
        String sugestaoCorrecao
) {
    public enum TipoSeveridade {
        WARNING, ERROR, CRITICAL
    }
}
