package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o {@link RevisarConcordanciaUseCase} — corrigir gênero num
 * {@code .ass} PT-BR, com dry-run seguro e backup obrigatório.
 *
 * <p>INVARIANTES DO DOMÍNIO: dry-run não escreve; aplicar faz backup antes de gravar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: escrita no dry-run ou backup ausente reprova.
 */
class RevisarConcordanciaUseCaseTest {

    private final TelemetriaSpy telemetria = new TelemetriaSpy();
    private final RevisarConcordanciaUseCase useCase = new RevisarConcordanciaUseCase(
        new LeitorLegendaAss(), new EscritorLegendaAss(), new CorretorConcordanciaGeneroService(), telemetria,
        new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()));

    /** Captura a operação registrada sem persistir em disco (não chama super). */
    static class TelemetriaSpy extends TelemetriaService {
        OperacaoTelemetria ultima;
        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria op) {
            this.ultima = op;
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

    private void escreverAssComEstilo(Path arquivo, String estilo, String... falas) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (String f : falas) {
            sb.append("Dialogue: 0,0:00:01.00,0:00:03.00,").append(estilo)
                .append(",,0,0,0,,").append(f).append("\n");
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * O VETO DE MÚSICA — o 🔴 que faltava nesta tela, e o pré-requisito para ela receber a
     * concordância que a 3.1 passou a encaminhar para cá (decisão de Paulo, 2026-08-16: cada menu
     * numerado é uma etapa).
     *
     * <p>MEDIDO ANTES DE A GUARDA EXISTIR, no 86: esta tela via <b>22.568 de 26.524</b> eventos na
     * Part 1 (85,1%) e <b>49.458 de 53.175</b> na Part 2 (93,0%) — quase tudo sílaba solta de
     * karaokê. E ela mexe em GÊNERO, que é onde a heurística mais erra. As outras duas telas já
     * vetavam música; só esta não.
     */
    @Test
    void estiloMusicalNaoEhTocadoPorEstaTela(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAssComEstilo(ass, "Opening", "Vi o menina.");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        ResultadoConcordancia r = useCase.revisarPasta(dir, true);

        assertEquals(0, r.falasCorrigidas(),
            "música é veto ABSOLUTO: karaokê pertence à fatia traducaoKaraoke, não a esta tela");
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8),
            "o arquivo não podia ser tocado");
    }

    /**
     * O CONTRA-TESTE, e ele é o que separa "vetou música" de "parou de funcionar": a MESMA fala,
     * em estilo de diálogo, continua sendo corrigida. Sem ele, um veto largo demais passaria
     * despercebido como se fosse a guarda funcionando.
     */
    @Test
    void aMesmaFalaEmDialogoContinuaSendoCorrigida(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAssComEstilo(ass, "Default", "Vi o menina.");

        ResultadoConcordancia r = useCase.revisarPasta(dir, true);

        assertEquals(1, r.falasCorrigidas(), "diálogo é o trabalho desta tela e não pode ser vetado");
        assertTrue(Files.readString(ass, StandardCharsets.UTF_8).contains("Vi a menina."));
    }

    @Test
    void aplicaCorrigeGeneroComBackup(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Vi o menina.", "Ela está cansado.");

        ResultadoConcordancia r = useCase.revisarPasta(dir, true);

        assertEquals(2, r.falasCorrigidas());
        assertEquals(1, r.backups().size());
        assertTrue(Files.exists(r.backups().get(0)));
        String depois = Files.readString(ass, StandardCharsets.UTF_8);
        assertTrue(depois.contains("Vi a menina."), "flip do artigo");
        assertTrue(depois.contains("Ela está cansada."), "flip do predicativo");
        assertFalse(depois.contains("Vi o menina."));
    }

    @Test
    void dryRunNaoEscreve(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Vi o menina.");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        ResultadoConcordancia r = useCase.revisarPasta(dir, false);

        assertEquals(1, r.arquivosAlterados());
        assertFalse(r.aplicado());
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8), "dry-run não pode escrever");
    }

    @Test
    void pastaInexistenteDevolveZero() {
        ResultadoConcordancia r =
            useCase.revisarPasta(Path.of("nao_existe_xyz"), true);
        assertEquals(0, r.arquivosAnalisados());
    }

    @Test
    void registraOperacaoNaTelemetria(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Vi o menina.");

        useCase.revisarPasta(dir, true);

        assertNotNull(telemetria.ultima, "deve registrar a operação na telemetria");
        assertEquals("Revisão de Concordância", telemetria.ultima.tipo());
        assertEquals(1, telemetria.ultima.arquivosProcessados());
        assertEquals(1, telemetria.ultima.itensCorrigidos());
    }
}
