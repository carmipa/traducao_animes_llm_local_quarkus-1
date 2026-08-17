package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.raspagemRevisao.domain.ports.TelemetriaRevisaoPort;
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
        new RevisorPtOnlyService(new NormalizadorAcentosComuns(), new CorretorDeterministicoConcordanciaService(), new org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda()),
        new TelemetriaNoOp(),
        new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()));

    /** Telemetria no-op para o teste unitário: não persiste em disco (não chama super). */
    /**
     * Telemetria em memoria. Com a porta da FASE 2, o teste deixou de precisar da fatia
     * {@code telemetria} para existir -- criterio de saida da fase.
     */
    private static final class TelemetriaNoOp implements TelemetriaRevisaoPort {
        @Override public void registrarComRelatorio(String operacao, String detalhe,
            String prefixoRelatorio, Path pastaAlvo, long duracaoMs, int arquivosProcessados,
            int itensDetectados, int itensCorrigidos, String relatorio) { }

        @Override public void registrar(String operacao, String detalhe, long duracaoMs,
            int arquivosProcessados, int itensDetectados, int itensCorrigidos) { }

        @Override public Path pastaDeRelatorios(Path pastaEntrada) { return pastaEntrada; }
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

    private void escreverAssComEstilo(Path arquivo, String estilo, String... falas) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (String f : falas) {
            sb.append("Dialogue: 0,0:00:01.00,0:00:03.00,").append(estilo)
                .append(",,0,0,0,,").append(f).append("\n");
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * O VETO DE MÚSICA, achado na auditoria método a método de 17/08 e fechado com PREJUÍZO
     * MEDIDO, não hipótese: em dry-run com as classes de produção sobre {@code 86 Part 1}, este
     * caso de uso alteraria <b>65 falas, e as 65 são estilo {@code Ending}</b> — 65 cópias do
     * fragmento {@code "choes!"} (pedaço de {@code "echoes!"} pintado pelo gradiente) viradas em
     * {@code "chões!"}. Zero diálogo.
     *
     * <p>Ele não está em menu nenhum hoje — e foi exatamente assim que a ponte do cache ficou
     * dormente até reescrever 687 linhas de ED no Gundam 08th. <b>"Inalcançável" não é proteção.</b>
     */
    @Test
    void estiloMusicalNaoEhTocadoPeloCorretorPtOnly(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAssComEstilo(ass, "Ending", "Nao vou tambem.");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        RevisorPtOnlyUseCase.ResultadoPtOnly r = useCase.revisarPasta(dir, true);

        assertEquals(0, r.arquivosAlterados(),
            "música é veto absoluto: a letra fica como está até a 4.1 tratá-la");
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8),
            "o arquivo não podia ser tocado");
    }

    /** O contra-teste: a MESMA fala em estilo de diálogo continua sendo corrigida. */
    @Test
    void aMesmaFalaEmDialogoContinuaSendoCorrigida(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAssComEstilo(ass, "Default", "Nao vou tambem.");

        RevisorPtOnlyUseCase.ResultadoPtOnly r = useCase.revisarPasta(dir, true);

        assertEquals(1, r.arquivosAlterados(), "diálogo é o trabalho deste corretor");
        assertTrue(Files.readString(ass, StandardCharsets.UTF_8).contains("Não vou também."));
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

