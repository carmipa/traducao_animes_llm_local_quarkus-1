package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

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
