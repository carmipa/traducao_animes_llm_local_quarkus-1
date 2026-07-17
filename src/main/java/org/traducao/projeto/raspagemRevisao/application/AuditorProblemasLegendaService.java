package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.util.ArrayList;
import java.util.List;

/**
 * Agrega detecção de resíduo em inglês, falas não traduzidas e erros de
 * concordância PT-BR.
 */
@Service
public class AuditorProblemasLegendaService {

    private final ValidadorTraducaoService validador;
    private final DetectorConcordanciaService detectorConcordancia;
    private final DetectorTraducaoIdenticaService detectorIdentica;

    public AuditorProblemasLegendaService(
        ValidadorTraducaoService validador,
        DetectorConcordanciaService detectorConcordancia,
        DetectorTraducaoIdenticaService detectorIdentica
    ) {
        this.validador = validador;
        this.detectorConcordancia = detectorConcordancia;
        this.detectorIdentica = detectorIdentica;
    }

    public ResultadoDeteccaoConcordancia auditar(String originalIngles, String traducaoPt) {
        List<String> motivos = new ArrayList<>();

        try {
            validador.validarFala(traducaoPt);
        } catch (AlucinacaoDetectadaException e) {
            motivos.add(e.getMessage());
        }

        if (detectorIdentica.pareceNaoTraduzida(originalIngles, traducaoPt)) {
            motivos.add("Fala não traduzida (idêntica ao original em inglês): " + traducaoPt);
        }

        ResultadoDeteccaoConcordancia concordancia = detectorConcordancia.analisar(originalIngles, traducaoPt);
        if (concordancia.suspeito()) {
            motivos.addAll(concordancia.motivos());
        }

        if (motivos.isEmpty()) {
            return ResultadoDeteccaoConcordancia.limpo();
        }
        return new ResultadoDeteccaoConcordancia(true, List.copyOf(motivos));
    }
}
