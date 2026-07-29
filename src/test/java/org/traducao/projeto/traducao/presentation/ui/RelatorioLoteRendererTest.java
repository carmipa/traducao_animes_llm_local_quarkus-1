package org.traducao.projeto.traducao.presentation.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.ResumoPendencia;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o fechamento do lote MOSTRA o que o pipeline já
 * media e descartava — tempo, vazão, destino das falas e pendências por causa —, e
 * que não inventa número onde não houve medição.
 *
 * <p>INVARIANTES COBERTAS: tempo e vazão só contam arquivos que rodaram; pendências
 * somam por categoria×causa entre arquivos e vêm ordenadas pelo maior ofensor; seção
 * sem dado é OMITIDA em vez de impressa zerada.
 *
 * <p>Os números do primeiro teste são os do run real de Gundam Unicorn ep.1 em
 * 2026-07-29 (374 traduzíveis, 365 pelo LLM, 6 pendentes por ECO, 407.887 ms), para
 * o teste falhar se o relatório deixar de responder as perguntas que motivaram sua
 * criação.
 */
class RelatorioLoteRendererTest {

    private static ResultadoTraducaoArquivo arquivo(
            String nome, int traduziveis, int cache, int traduzidas, int pendentes,
            long tempoMs, StatusArquivoTraducao status, List<ResumoPendencia> causas) {
        return new ResultadoTraducaoArquivo(
            Path.of("saida", nome), nome, "Mobile Suit Gundam Unicorn",
            traduziveis, cache, traduzidas, pendentes, status, tempoMs, pendentes, causas);
    }

    @Test
    @DisplayName("run real do Unicorn ep.1: tempo, vazão, falas e causa aparecem no fechamento")
    void mostraOsDadosQueAntesSoIamParaOJson() {
        String saida = RelatorioLoteRenderer.render(
            List.of(arquivo("Unicorn_S01E01_Track4.ass", 374, 0, 365, 6, 407_887L,
                StatusArquivoTraducao.PARCIAL, List.of(new ResumoPendencia("DIALOGO", "ECO", 6)))),
            "PARCIAL", 1);

        // tempo total: antes existia só em logs/telemetria_traducao.json
        assertTrue(saida.contains("6m47s"), "tempo total ausente: " + saida);
        // vazão: 365 falas em 407.887 ms
        assertTrue(saida.contains("54/min"), "vazão ausente: " + saida);
        // o parâmetro sob teste no experimento de tamanho de lote
        assertTrue(saida.contains("lote=1"), "tamanho de lote ausente: " + saida);
        assertTrue(saida.contains("374"), "falas traduzíveis ausentes: " + saida);
        assertTrue(saida.contains("365"), "falas pelo LLM ausentes: " + saida);
        assertTrue(saida.contains("97,6%"), "cobertura ausente: " + saida);
        // causa agrupada: antes eram seis linhas [ WARN ] soltas
        assertTrue(saida.contains("DIALOGO/ECO 6"), "causa agrupada ausente: " + saida);
    }

    @Test
    @DisplayName("pendências de arquivos diferentes somam por causa e o maior ofensor vem primeiro")
    void agrupaPendenciasEOrdenaPeloMaiorOfensor() {
        String saida = RelatorioLoteRenderer.render(
            List.of(
                arquivo("e01.ass", 100, 0, 95, 5, 60_000L, StatusArquivoTraducao.PARCIAL,
                    List.of(new ResumoPendencia("DIALOGO", "ECO", 2),
                            new ResumoPendencia("LETREIRO", "MARCADORES", 3))),
                arquivo("e02.ass", 100, 0, 96, 4, 60_000L, StatusArquivoTraducao.PARCIAL,
                    List.of(new ResumoPendencia("DIALOGO", "ECO", 4)))),
            "PARCIAL", 8);

        assertTrue(saida.contains("DIALOGO/ECO 6"), "2 + 4 não somaram entre arquivos: " + saida);
        assertTrue(saida.contains("LETREIRO/MARCADORES 3"), "causa do outro arquivo sumiu: " + saida);
        assertTrue(saida.indexOf("DIALOGO/ECO") < saida.indexOf("LETREIRO/MARCADORES"),
            "maior ofensor deveria vir primeiro: " + saida);
    }

    @Test
    @DisplayName("arquivo bloqueado não dilui o tempo médio nem vira '0s'")
    void bloqueadoNaoEntraNaMediaDeTempo() {
        String saida = RelatorioLoteRenderer.render(
            List.of(
                arquivo("rodou.ass", 100, 0, 100, 0, 120_000L, StatusArquivoTraducao.CONCLUIDO, List.of()),
                ResultadoTraducaoArquivo.bloqueado("nao-rodou.ass", "Mobile Suit Gundam Unicorn")),
            "PARCIAL", 8);

        // Um único arquivo medido: 2m0s. Se o bloqueado entrasse na média, sairia 1m0s.
        assertTrue(saida.contains("2m0s total"), "tempo do único arquivo medido: " + saida);
        assertFalse(saida.contains("1m0s"), "bloqueado diluiu a média: " + saida);
        assertTrue(saida.contains("2 arquivo(s)"), "contagem do lote: " + saida);
    }

    @Test
    @DisplayName("lote sem pendência OMITE a seção em vez de imprimir zero")
    void omiteSecaoDePendenciaQuandoNaoHa() {
        String saida = RelatorioLoteRenderer.render(
            List.of(arquivo("limpo.ass", 200, 0, 200, 0, 90_000L,
                StatusArquivoTraducao.CONCLUIDO, List.of())),
            "CONCLUÍDO", 8);

        assertFalse(saida.contains("PENDÊNCIAS"), "seção zerada treina o olho a ignorá-la: " + saida);
        assertTrue(saida.contains("0 pendente(s)"), "contagem de pendentes: " + saida);
        assertTrue(saida.contains("100,0%"), "cobertura total: " + saida);
    }

    @Test
    @DisplayName("lote vazio devolve string vazia e não lança")
    void loteVazioNaoQuebra() {
        assertEquals("", RelatorioLoteRenderer.render(List.of(), "CONCLUÍDO", 8));
        assertEquals("", RelatorioLoteRenderer.render(null, "CONCLUÍDO", 8));
    }

    @Test
    @DisplayName("duração e vazão sem medição saem como travessão, não como zero")
    void semMedicaoNaoFabricaNumero() {
        assertEquals("—", TabelaTraducaoRenderer.formatarDuracao(0));
        assertEquals("—", TabelaTraducaoRenderer.formatarDuracao(-1));
        assertEquals("—", TabelaTraducaoRenderer.formatarRitmo(10, 0));
        // Tudo veio do cache: zero fala traduzida não é "lentidão", é ausência de trabalho.
        assertEquals("—", TabelaTraducaoRenderer.formatarRitmo(0, 60_000L));
        assertEquals("6m47s", TabelaTraducaoRenderer.formatarDuracao(407_887L));
        assertEquals("42s", TabelaTraducaoRenderer.formatarDuracao(42_000L));
        assertEquals("54/min", TabelaTraducaoRenderer.formatarRitmo(365, 407_887L));
    }
}
