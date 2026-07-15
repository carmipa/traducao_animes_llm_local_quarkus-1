package org.traducao.projeto.legendasExtracao.application.strategy;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.util.List;
import java.util.Optional;

@Component
public class ExtratorAssStrategy implements ExtratorStrategy {

    @Override
    public boolean suporta(FormatoLegenda formato) {
        return formato == FormatoLegenda.ASS;
    }

    @Override
    public Optional<FaixaLegenda> selecionarMelhorFaixa(List<FaixaLegenda> faixasDisponiveis) {
        List<FaixaLegenda> candidatas = faixasDisponiveis.stream()
                .filter(f -> {
                    String c = f.codec().toLowerCase();
                    String cid = f.codecId().toLowerCase();
                    return c.contains("ass") || c.contains("substation") || cid.contains("ass");
                })
                .toList();

        // 1. Tentar por palavras-chave
        for (FaixaLegenda f : candidatas) {
            String n = f.nome().toLowerCase();
            if (n.contains("dialogue") || n.contains("full") || n.contains("complete") 
                || n.contains("legendado") || n.contains("gcs8") || n.contains("english")) {
                return Optional.of(f);
            }
        }

        // 2. Tentar a última candidata (geralmente a faixa completa em ASS, a primeira é signs)
        if (!candidatas.isEmpty()) {
            return Optional.of(candidatas.getLast());
        }

        return Optional.empty();
    }
}
