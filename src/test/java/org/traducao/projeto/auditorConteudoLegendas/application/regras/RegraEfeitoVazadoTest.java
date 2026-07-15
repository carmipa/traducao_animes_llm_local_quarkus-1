package org.traducao.projeto.auditorConteudoLegendas.application.regras;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.auditorConteudoLegendas.domain.AnomaliaConteudo;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegraEfeitoVazadoTest {

    private final RegraEfeitoVazado regra = new RegraEfeitoVazado();

    @Test
    void naoDeveAcusarAnomaliaSeNaoTiverTagsPesadas() {
        EventoLegenda orig = new EventoLegenda(1, "Dialogue", "Default", "", "{\\i1}Hello!{\\i0}");
        EventoLegenda trad = new EventoLegenda(1, "Dialogue", "Default", "", "{\\i1}Olá!{\\i0}");

        DocumentoLegenda docOrig = new DocumentoLegenda("", List.of(orig), "\n", false);
        DocumentoLegenda docTrad = new DocumentoLegenda("", List.of(trad), "\n", false);

        List<AnomaliaConteudo> anomalias = regra.auditar(docOrig, docTrad);
        assertTrue(anomalias.isEmpty());
    }

    @Test
    void deveAcusarAnomaliaCriticaQuandoEfeitoVazaParaTraducao() {
        // Simulando o problema de 86 Ep 2, onde um typesetting (como "{\\pos(100,100)}na")
        // acaba sendo traduzido de forma louca pela IA.
        EventoLegenda orig = new EventoLegenda(1, "Dialogue", "Opening", "", "{\\pos(10,20)\\t(0,100,\\fad(100,200))}na");
        EventoLegenda trad = new EventoLegenda(1, "Dialogue", "Opening", "", "{\\pos(10,20)\\t(0,100,\\fad(100,200))}Não pode ser verdade!");

        DocumentoLegenda docOrig = new DocumentoLegenda("", List.of(orig), "\n", false);
        DocumentoLegenda docTrad = new DocumentoLegenda("", List.of(trad), "\n", false);

        List<AnomaliaConteudo> anomalias = regra.auditar(docOrig, docTrad);

        assertEquals(1, anomalias.size());
        AnomaliaConteudo anomalia = anomalias.get(0);
        assertEquals(AnomaliaConteudo.TipoSeveridade.CRITICAL, anomalia.severidade());
        assertTrue(anomalia.descricao().contains("Fragmento de efeito visual"));
    }

    @Test
    void deveAcusarWarningSeTagPesadaFoiAlterada() {
        EventoLegenda orig = new EventoLegenda(1, "Dialogue", "Default", "", "{\\pos(100,200)}This is a long sentence that should not be touched.");
        EventoLegenda trad = new EventoLegenda(1, "Dialogue", "Default", "", "{\\pos(100,200)}Esta é uma sentença longa.");

        DocumentoLegenda docOrig = new DocumentoLegenda("", List.of(orig), "\n", false);
        DocumentoLegenda docTrad = new DocumentoLegenda("", List.of(trad), "\n", false);

        List<AnomaliaConteudo> anomalias = regra.auditar(docOrig, docTrad);

        assertEquals(1, anomalias.size());
        assertEquals(AnomaliaConteudo.TipoSeveridade.WARNING, anomalias.get(0).severidade());
        assertTrue(anomalias.get(0).descricao().contains("Linha com efeitos pesados foi modificada"));
    }
}
