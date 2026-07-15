package org.traducao.projeto.legendasExtracao.application.strategy;

import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;

import java.util.List;
import java.util.Optional;

public interface ExtratorStrategy {
    boolean suporta(FormatoLegenda formato);
    Optional<FaixaLegenda> selecionarMelhorFaixa(List<FaixaLegenda> faixasDisponiveis);
}
