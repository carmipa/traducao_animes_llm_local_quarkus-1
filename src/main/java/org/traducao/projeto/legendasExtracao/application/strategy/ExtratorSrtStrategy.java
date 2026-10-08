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

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe a faixa SRT de diálogo completo para a tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: faixa que se declara reduzida (forced, sign/song) só é escolhida
     * se não houver outra (ver {@link FaixaDeLetreiro}); entre as restantes vence a default ou a
     * eng/por, senão a primeira.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem faixa SRT, {@link Optional#empty()}; nunca lança.
     */
    @Override
    public Optional<FaixaLegenda> selecionarMelhorFaixa(List<FaixaLegenda> faixasDisponiveis) {
        List<FaixaLegenda> candidatas = FaixaDeLetreiro.semLetreiros(faixasDisponiveis.stream()
                .filter(f -> {
                    String c = f.codec().toLowerCase();
                    String cid = f.codecId().toLowerCase();
                    return c.contains("srt") || c.contains("subrip") || c.contains("utf8") || cid.contains("utf8");
                })
                .toList());

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
