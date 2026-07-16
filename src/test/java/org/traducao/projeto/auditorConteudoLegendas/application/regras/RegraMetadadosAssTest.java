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

class RegraMetadadosAssTest {

    private final RegraMetadadosAss regra = new RegraMetadadosAss();
    private final LeitorLegendaAss leitor = new LeitorLegendaAss();

    @Test
    void detectaPlayResAlterado(@TempDir Path tempDir) throws Exception {
        Path original = tempDir.resolve("orig.ass");
        Path traduzido = tempDir.resolve("trad.ass");
        AssAuditoriaFixtures.escreverParComPlayResAlterado(original, traduzido);

        DocumentoLegenda docOrig = leitor.ler(original);
        DocumentoLegenda docTrad = leitor.ler(traduzido);

        var anomalias = regra.auditar(docOrig, docTrad);
        assertTrue(anomalias.size() >= 2);
        assertEquals(AnomaliaConteudo.TipoSeveridade.CRITICAL, anomalias.getFirst().severidade());
    }
}
