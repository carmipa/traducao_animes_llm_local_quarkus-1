package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraAlucinacaoQuebraLinhaTest {

    private final RegraAlucinacaoQuebraLinha regra = new RegraAlucinacaoQuebraLinha();

    @Test
    void naoDeveAcusarAnomaliaSeTraducaoMantiverMesmoNumeroDeQuebras() {
        EventoLegenda orig = new EventoLegenda(1, "Dialogue", "Default", "", "Hello\\NWorld");
        EventoLegenda trad = new EventoLegenda(1, "Dialogue", "Default", "", "Olá\\NMundo");

        DocumentoLegenda docOrig = new DocumentoLegenda("", List.of(orig), "\n", false);
        DocumentoLegenda docTrad = new DocumentoLegenda("", List.of(trad), "\n", false);

        List<AnomaliaConteudo> anomalias = regra.auditar(docOrig, docTrad);

        assertTrue(anomalias.isEmpty(), "Não deve acusar anomalia se as quebras estiverem corretas.");
    }

    @Test
    void deveAcusarAnomaliaCriticaSeIAInserirQuebrasExcessivas() {
        // Simulando o erro do anime 86 Ep 2
        EventoLegenda orig = new EventoLegenda(1, "Dialogue", "Default", "", "This is a single line.");
        EventoLegenda trad = new EventoLegenda(1, "Dialogue", "Default", "", "Esta\\Né\\Numa\\Núnica\\Nlinha.");

        DocumentoLegenda docOrig = new DocumentoLegenda("", List.of(orig), "\n", false);
        DocumentoLegenda docTrad = new DocumentoLegenda("", List.of(trad), "\n", false);

        List<AnomaliaConteudo> anomalias = regra.auditar(docOrig, docTrad);

        assertEquals(1, anomalias.size());
        AnomaliaConteudo anomalia = anomalias.get(0);
        assertEquals(AnomaliaConteudo.TipoSeveridade.CRITICAL, anomalia.severidade());
        assertTrue(anomalia.descricao().contains("A tradução inseriu 4 quebras de linha"));
    }
    
    @Test
    void deveIgnorarLinhasQueNaoSaoDialogos() {
        EventoLegenda orig = new EventoLegenda(1, "Comment", "Default", "", "Hello");
        EventoLegenda trad = new EventoLegenda(1, "Comment", "Default", "", "Olá\\N\\N\\N\\N");

        DocumentoLegenda docOrig = new DocumentoLegenda("", List.of(orig), "\n", false);
        DocumentoLegenda docTrad = new DocumentoLegenda("", List.of(trad), "\n", false);

        List<AnomaliaConteudo> anomalias = regra.auditar(docOrig, docTrad);

        assertTrue(anomalias.isEmpty(), "Deve ignorar Comments mesmo com anomalia");
    }
}
