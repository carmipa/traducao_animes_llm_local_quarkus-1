package org.traducao.projeto.revisaoLore.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o {@link RevisarLorePtOnlyUseCase} — revisar lore de um
 * {@code .ass} PT-BR sem o inglês, no caminho determinístico (sem LLM): corrige termo inequívoco,
 * não toca homógrafo de uma palavra, dry-run seguro e backup obrigatório.
 *
 * <p>INVARIANTES DO DOMÍNIO: multi-palavra corrigido; homógrafo de 1 palavra intocado sem LLM;
 * dry-run não escreve; aplicar faz backup.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: correção indevida, escrita no dry-run ou backup ausente reprova.
 */
@QuarkusTest
class RevisarLorePtOnlyUseCaseTest {

    @Inject
    RevisarLorePtOnlyUseCase useCase;

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
    void aplicaTermoInequivocoSemEnEComBackup(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        // "Traje Móvel" (multi-palavra) -> corrige; "Eixo" (1 palavra, homógrafo) -> intocado sem LLM.
        escreverAss(ass, "Pilotar o Traje Móvel.", "O Eixo da roda quebrou.");

        RevisarLorePtOnlyUseCase.ResultadoLorePtOnly r =
            useCase.executar(dir, "gundam_zeta", false, true);

        assertEquals(1, r.falasCorrigidas());
        assertEquals(1, r.backups().size());
        assertTrue(Files.exists(r.backups().get(0)));
        String depois = Files.readString(ass, StandardCharsets.UTF_8);
        assertTrue(depois.contains("Pilotar o Mobile Suit."), "termo inequívoco corrigido sem o EN");
        assertTrue(depois.contains("O Eixo da roda quebrou."), "homógrafo de 1 palavra intocado sem LLM");
    }

    @Test
    void dryRunNaoEscreve(@TempDir Path dir) throws IOException {
        Path ass = dir.resolve("ep_PT-BR.ass");
        escreverAss(ass, "Pilotar o Traje Móvel.");
        String antes = Files.readString(ass, StandardCharsets.UTF_8);

        RevisarLorePtOnlyUseCase.ResultadoLorePtOnly r =
            useCase.executar(dir, "gundam_zeta", false, false);

        assertEquals(1, r.arquivosAlterados());
        assertFalse(r.aplicado());
        assertEquals(antes, Files.readString(ass, StandardCharsets.UTF_8), "dry-run não pode escrever");
    }

    @Test
    void pastaInexistenteDevolveZero() {
        RevisarLorePtOnlyUseCase.ResultadoLorePtOnly r =
            useCase.executar(Path.of("nao_existe_xyz"), "gundam_zeta", false, true);
        assertEquals(0, r.arquivosAnalisados());
    }
}
