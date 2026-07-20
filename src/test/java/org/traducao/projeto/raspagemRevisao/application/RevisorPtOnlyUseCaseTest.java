package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o {@link RevisorPtOnlyUseCase} — corrigir um {@code .ass}
 * PT-BR sem inglês/cache, com dry-run seguro, backup obrigatório e sinalização de asterisco.
 *
 * <p>INVARIANTES DO DOMÍNIO: dry-run não escreve; aplicar faz backup antes de gravar; asterisco
 * é reportado sem sumir da fala.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: escrita no dry-run, backup ausente ou fala perdida reprova.
 */
class RevisorPtOnlyUseCaseTest {

    private final RevisorPtOnlyUseCase useCase = new RevisorPtOnlyUseCase(
        new LeitorLegendaAss(), new EscritorLegendaAss(),
        new RevisorPtOnlyService(new NormalizadorAcentosComuns(), new CorretorDeterministicoConcordanciaService()),
        new TelemetriaNoOp());

    /** Telemetria no-op para o teste unitário: não persiste em disco (não chama super). */
    static class TelemetriaNoOp extends TelemetriaService {
        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria op) {
            // no-op: o teste do use case não verifica telemetria (coberta em concordância)
        }
    }

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 1920
        PlayResY: 1080

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    private void escreverAss(Path arquivo, String... falas) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (String f : falas) {
            sb.append("Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,").append(f).append("\n");
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    @Test
    void dryRunContaMasNaoEscreve(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Nao vou tambem.");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        RevisorPtOnlyUseCase.ResultadoPtOnly r = useCase.revisarPasta(dir, false);

        assertEquals(1, r.arquivosAnalisados());
        assertEquals(1, r.arquivosAlterados());
        assertFalse(r.aplicado());
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8), "dry-run não pode escrever");
    }

    @Test
    void aplicaCorrigeAcentoComBackup(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Nao vou tambem.");

        RevisorPtOnlyUseCase.ResultadoPtOnly r = useCase.revisarPasta(dir, true);

        assertTrue(r.aplicado());
        assertEquals(1, r.falasAlteradas());
        assertEquals(1, r.backups().size());
        assertTrue(Files.exists(r.backups().get(0)), "backup deve existir");
        String depois = Files.readString(ass, StandardCharsets.UTF_8);
        assertTrue(depois.contains("Não vou também."), "deveria corrigir acentos");
        assertFalse(depois.contains("Nao vou tambem."));
    }

    @Test
    void sinalizaAsteriscoSemPerderFala(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Merd*, larga!");

        RevisorPtOnlyUseCase.ResultadoPtOnly r = useCase.revisarPasta(dir, false);

        assertEquals(1, r.falasComAsterisco().size());
        assertTrue(r.falasComAsterisco().get(0).contains("Merd*"), "reporta a fala com asterisco");
    }

    @Test
    void pastaInexistenteDevolveZero() {
        RevisorPtOnlyUseCase.ResultadoPtOnly r = useCase.revisarPasta(Path.of("nao_existe_xyz"), true);
        assertEquals(0, r.arquivosAnalisados());
    }
}
