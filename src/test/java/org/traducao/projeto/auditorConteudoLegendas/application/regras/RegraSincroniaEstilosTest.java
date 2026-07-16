package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.auditorConteudoLegendas.support.AssAuditoriaFixtures;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraSincroniaEstilosTest {

    private final RegraSincroniaEstilos regra = new RegraSincroniaEstilos();
    private final LeitorLegendaAss leitor = new LeitorLegendaAss();

    @Test
    void detectaEstiloRemovido(@TempDir Path tempDir) throws Exception {
        Path original = tempDir.resolve("orig.ass");
        Path traduzido = tempDir.resolve("trad.ass");
        AssAuditoriaFixtures.escreverParComEstiloRemovido(original, traduzido);

        DocumentoLegenda docOrig = leitor.ler(original);
        DocumentoLegenda docTrad = leitor.ler(traduzido);

        var anomalias = regra.auditar(docOrig, docTrad);
        assertEquals(1, anomalias.size());
        assertEquals(AnomaliaConteudo.TipoSeveridade.ERROR, anomalias.getFirst().severidade());
        assertTrue(anomalias.getFirst().descricao().contains("Opening"));
    }
}
