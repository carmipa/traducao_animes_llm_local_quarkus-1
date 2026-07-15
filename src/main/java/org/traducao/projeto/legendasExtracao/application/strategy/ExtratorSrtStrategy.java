package org.traducao.projeto.legendasExtracao.application.strategy;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.util.List;
import java.util.Optional;

@Component
public class ExtratorSrtStrategy implements ExtratorStrategy {

    @Override
    public boolean suporta(FormatoLegenda formato) {
        return formato == FormatoLegenda.SRT;
    }

    @Override
    public Optional<FaixaLegenda> selecionarMelhorFaixa(List<FaixaLegenda> faixasDisponiveis) {
        List<FaixaLegenda> candidatas = faixasDisponiveis.stream()
                .filter(f -> {
                    String c = f.codec().toLowerCase();
                    String cid = f.codecId().toLowerCase();
                    return c.contains("srt") || c.contains("subrip") || c.contains("utf8") || cid.contains("utf8");
                })
                .toList();

        for (FaixaLegenda f : candidatas) {
            if (f.isDefault() || f.idioma().equalsIgnoreCase("eng") || f.idioma().equalsIgnoreCase("por")) {
                return Optional.of(f);
            }
        }

        if (!candidatas.isEmpty()) {
            return Optional.of(candidatas.getFirst());
        }

        return Optional.empty();
    }
}
