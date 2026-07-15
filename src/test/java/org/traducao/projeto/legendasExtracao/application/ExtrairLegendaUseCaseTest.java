package org.traducao.projeto.legendasExtracao.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorAssStrategy;
import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.legendasExtracao.domain.ItemExtracao;
import org.traducao.projeto.legendasExtracao.domain.RelatorioExtracao;
import org.traducao.projeto.legendasExtracao.domain.StatusExtracao;
import org.traducao.projeto.legendasExtracao.domain.exceptions.ExtracaoTimeoutException;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a orquestração do extrator sem ferramentas externas: seleção de faixa
 * pela strategy real, extração para arquivo temporário, validação de saída,
 * guarda anti-sobrescrita, cleanup de parcial, classificação de timeout e o
 * mapeamento da telemetria.
 */
class ExtrairLegendaUseCaseTest {

    private enum Modo { SUCESSO, VAZIO, CONTEUDO_SRT, TIMEOUT, FALHA }

    /** Adaptador de vídeo fake: suporta .mkv e simula a extração conforme o modo. */
    private static final class FakeAdapter implements ExtratorVideoPort {
        private final List<FaixaLegenda> faixas;
        private final Modo modo;

        FakeAdapter(List<FaixaLegenda> faixas, Modo modo) {
            this.faixas = faixas;
            this.modo = modo;
        }

        @Override public boolean suporta(Path v) { return v.toString().toLowerCase().endsWith(".mkv"); }
        @Override public void validarInfraestrutura() { }
        @Override public List<FaixaLegenda> identificarFaixas(Path v) { return faixas; }

        @Override
        public void extrairTrilha(Path v, int trackId, Path caminhoSaida) {
            switch (modo) {
                case TIMEOUT -> throw new ExtracaoTimeoutException("timeout simulado");
                case FALHA -> throw new ExtratorException("falha simulada");
                case VAZIO -> escrever(caminhoSaida, "");
                case CONTEUDO_SRT -> escrever(caminhoSaida, "1\n00:00:01,000 --> 00:00:02,000\nOi\n");
                case SUCESSO -> escrever(caminhoSaida,
                    "[Script Info]\n[Events]\nDialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Oi");
            }
        }

        private void escrever(Path out, String conteudo) {
            try {
                Files.writeString(out, conteudo, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new ExtratorException("io", e);
            }
        }
    }

    /** Captura a operação registrada sem tocar em disco (não chama super). */
    private static final class TelemetriaSpy extends TelemetriaService {
        OperacaoTelemetria ultima;
        @Override public void registrarOperacao(OperacaoTelemetria op) { this.ultima = op; }
    }

    private static FaixaLegenda faixaAss() {
        return new FaixaLegenda(2, "subtitles", "SubStationAlpha", "S_TEXT/ASS", "eng", "Full", true, false);
    }

    private static FaixaLegenda faixaPgs() {
        return new FaixaLegenda(3, "subtitles", "HDMV PGS", "S_HDMV/PGS", "por", "Sem Titulo", false, true);
    }

    private static ExtrairLegendaUseCase useCase(List<FaixaLegenda> faixas, Modo modo, TelemetriaService tel) {
        return new ExtrairLegendaUseCase(
            List.of(new FakeAdapter(faixas, modo)),
            List.of(new ExtratorAssStrategy()),
            tel);
    }

    private static Path prepararVideo(Path base) throws IOException {
        Path videos = Files.createDirectory(base.resolve("videos"));
        Files.writeString(videos.resolve("Ep01.mkv"), "fake");
        return videos;
    }

    @Test
    void extraiValidaEMoveParaFinal(@TempDir Path base) throws IOException {
        Path videos = prepararVideo(base);
        Path saida = base.resolve("out");
        TelemetriaSpy tel = new TelemetriaSpy();

        RelatorioExtracao rel = useCase(List.of(faixaAss()), Modo.SUCESSO, tel)
            .executar(videos, saida, FormatoLegenda.ASS);

        assertEquals(1, rel.getLegendasExtraidas());
        assertEquals(1, rel.getFaixasEncontradas());
        assertTrue(Files.exists(saida.resolve("Ep01_Track2.ass")));
        assertFalse(Files.exists(saida.resolve("Ep01_Track2.ass.part")), "parcial não deve sobrar");

        ItemExtracao item = rel.getItens().get(0);
        assertEquals(StatusExtracao.SUCESSO, item.status());
        assertEquals(2, item.trackId());
        assertEquals("Ep01_Track2.ass", item.arquivoGerado());

        assertNotNull(tel.ultima);
        assertEquals(1, tel.ultima.arquivosProcessados());
        assertEquals(1, tel.ultima.itensDetectados()); // faixas encontradas
        assertEquals(1, tel.ultima.itensCorrigidos()); // extraídas
    }

    @Test
    void semFaixaDoFormatoNaoCaiEmOutroFormato(@TempDir Path base) throws IOException {
        Path videos = prepararVideo(base);

        RelatorioExtracao rel = useCase(List.of(faixaPgs()), Modo.SUCESSO, new TelemetriaSpy())
            .executar(videos, base.resolve("out"), FormatoLegenda.ASS);

        assertEquals(0, rel.getLegendasExtraidas());
        assertEquals(1, rel.getArquivosSemLegenda());
        assertEquals(1, rel.getFaixasEncontradas()); // achou a faixa PGS, só não do formato pedido
        assertEquals(StatusExtracao.FAIXA_NAO_ENCONTRADA, rel.getItens().get(0).status());
    }

    @Test
    void naoSobrescreveArquivoExistente(@TempDir Path base) throws IOException {
        Path videos = prepararVideo(base);
        Path saida = Files.createDirectory(base.resolve("out"));
        Path existente = saida.resolve("Ep01_Track2.ass");
        Files.writeString(existente, "CONTEUDO ORIGINAL");

        RelatorioExtracao rel = useCase(List.of(faixaAss()), Modo.SUCESSO, new TelemetriaSpy())
            .executar(videos, saida, FormatoLegenda.ASS);

        assertEquals(0, rel.getLegendasExtraidas());
        assertEquals(1, rel.getArquivosJaExistentes());
        assertEquals("CONTEUDO ORIGINAL", Files.readString(existente), "arquivo deve ser preservado");
        assertEquals(StatusExtracao.JA_EXISTE, rel.getItens().get(0).status());
    }

    @Test
    void conteudoVazioViraFalhaERemoveParcial(@TempDir Path base) throws IOException {
        Path videos = prepararVideo(base);
        Path saida = base.resolve("out");

        RelatorioExtracao rel = useCase(List.of(faixaAss()), Modo.VAZIO, new TelemetriaSpy())
            .executar(videos, saida, FormatoLegenda.ASS);

        assertEquals(0, rel.getLegendasExtraidas());
        assertEquals(1, rel.getFalhasInesperadas());
        assertFalse(Files.exists(saida.resolve("Ep01_Track2.ass")));
        assertFalse(Files.exists(saida.resolve("Ep01_Track2.ass.part")));
        assertEquals(StatusExtracao.FALHA, rel.getItens().get(0).status());
    }

    @Test
    void conteudoDeOutroFormatoViraFalha(@TempDir Path base) throws IOException {
        Path videos = prepararVideo(base);
        Path saida = base.resolve("out");

        RelatorioExtracao rel = useCase(List.of(faixaAss()), Modo.CONTEUDO_SRT, new TelemetriaSpy())
            .executar(videos, saida, FormatoLegenda.ASS);

        assertEquals(0, rel.getLegendasExtraidas());
        assertEquals(1, rel.getFalhasInesperadas());
        assertFalse(Files.exists(saida.resolve("Ep01_Track2.ass.part")));
        assertEquals(StatusExtracao.FALHA, rel.getItens().get(0).status());
    }

    @Test
    void timeoutClassificadoSeparadoDeFalha(@TempDir Path base) throws IOException {
        Path videos = prepararVideo(base);
        Path saida = base.resolve("out");

        RelatorioExtracao rel = useCase(List.of(faixaAss()), Modo.TIMEOUT, new TelemetriaSpy())
            .executar(videos, saida, FormatoLegenda.ASS);

        assertEquals(0, rel.getLegendasExtraidas());
        assertEquals(1, rel.getTimeouts());
        assertEquals(0, rel.getFalhasInesperadas());
        assertFalse(Files.exists(saida.resolve("Ep01_Track2.ass.part")));
        assertEquals(StatusExtracao.TIMEOUT, rel.getItens().get(0).status());
    }
}
