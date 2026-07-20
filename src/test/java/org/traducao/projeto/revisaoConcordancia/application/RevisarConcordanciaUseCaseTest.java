package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private final RevisarConcordanciaUseCase useCase = new RevisarConcordanciaUseCase(
        new LeitorLegendaAss(), new EscritorLegendaAss(), new CorretorConcordanciaGeneroService());

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
    void aplicaCorrigeGeneroComBackup(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Vi o menina.", "Ela está cansado.");

        RevisarConcordanciaUseCase.ResultadoConcordancia r = useCase.revisarPasta(dir, true);

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

        RevisarConcordanciaUseCase.ResultadoConcordancia r = useCase.revisarPasta(dir, false);

        assertEquals(1, r.arquivosAlterados());
        assertFalse(r.aplicado());
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8), "dry-run não pode escrever");
    }

    @Test
    void pastaInexistenteDevolveZero() {
        RevisarConcordanciaUseCase.ResultadoConcordancia r =
            useCase.revisarPasta(Path.of("nao_existe_xyz"), true);
        assertEquals(0, r.arquivosAnalisados());
    }
}
