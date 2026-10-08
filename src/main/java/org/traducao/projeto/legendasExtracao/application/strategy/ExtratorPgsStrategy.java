package org.traducao.projeto.legendasExtracao.application.strategy;

import org.springframework.stereotype.Component;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.util.List;
import java.util.Optional;

@Component
public class ExtratorPgsStrategy implements ExtratorStrategy {

    @Override
    public boolean suporta(FormatoLegenda formato) {
        return formato == FormatoLegenda.PGS;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe a faixa PGS de diálogo completo.
     *
     * <p>INVARIANTES DO DOMÍNIO: faixa que se declara reduzida (forced, sign/song) só é escolhida
     * se não houver outra (ver {@link FaixaDeLetreiro}) — o 86 tem 25 MKV com a PGS "Signs" antes
     * da completa; entre as restantes vence a default ou a por/eng, senão a primeira.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem faixa PGS, {@link Optional#empty()}; nunca lança.
     */
    @Override
    public Optional<FaixaLegenda> selecionarMelhorFaixa(List<FaixaLegenda> faixasDisponiveis) {
        List<FaixaLegenda> candidatas = FaixaDeLetreiro.semLetreiros(faixasDisponiveis.stream()
                .filter(f -> {
                    String c = f.codec().toUpperCase();
                    String cid = f.codecId().toUpperCase();
                    return c.contains("PGS") || cid.contains("PGS") || cid.contains("S_HDMV/PGS");
                })
                .toList());

        // Para PGS, geralmente pega a primeira encontrada ou a marcada como default
        for (FaixaLegenda f : candidatas) {
            if (f.isDefault() || f.idioma().equalsIgnoreCase("por") || f.idioma().equalsIgnoreCase("eng")) {
                return Optional.of(f);
            }
        }

        if (!candidatas.isEmpty()) {
            return Optional.of(candidatas.getFirst());
        }

        return Optional.empty();
    }
}
