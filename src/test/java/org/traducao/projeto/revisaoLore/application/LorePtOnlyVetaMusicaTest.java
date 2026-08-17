package org.traducao.projeto.revisaoLore.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: provar que a aba <b>PT-only</b> da tela 3.2 corrige o nome na fala de
 * diálogo e NÃO toca na letra de música — as duas coisas, no mesmo arquivo, na mesma corrida.
 *
 * <h2>O prejuízo que originou</h2>
 * Esta aba grava no {@code .ass} ({@code aplicar=true}, e "Apenas simular" nasce desmarcado) e
 * filtrava apenas {@code temTexto()}: sem juiz de estilo musical, sem {@code isDialogo()}, sem
 * karaokê. Medido no acervo em 17/08/2026 ({@code MedicaoExposicaoMusicalRevisaoLorePtOnlyIT},
 * calibrado contra o próprio caso de uso em dry-run, 22 obras batendo): <b>246.246 linhas de
 * estilo musical ao alcance dela</b>. É a mesma forma da porta que reescreveu 687 linhas
 * {@code Song ENG} no Gundam 08th MS Team.
 *
 * <h2>Por que o contra-teste está no MESMO método</h2>
 * Um teste que só afirma "não mexeu na música" fica verde se a aba parar de funcionar por
 * completo. O par diálogo-corrigido + música-intacta é o que separa <i>"vetou música"</i> de
 * <i>"quebrou a tela"</i> — foi assim que o veto da 3.3 e o da ponte do cache foram provados.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Assertiva de calibração primeiro: se o juiz de produção não considerar {@code Opening} musical,
 * o teste REPROVA ali, antes de medir qualquer coisa — fixture que não é música não prova veto.
 */
@QuarkusTest
class LorePtOnlyVetaMusicaTest {

    /** Par real do mapa de terminologia de {@code gundam_zeta}: {@code Espada de Raio → Beam Saber}. */
    private static final String FORMA_RUIM = "Espada de Raio";
    private static final String CANONICO = "Beam Saber";
    private static final String CONTEXTO = "gundam_zeta";

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1
        Style: Opening,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    @Inject
    RevisarLorePtOnlyUseCase useCase;

    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    @Test
    @DisplayName("corrige o nome no dialogo e deixa a MESMA forma-ruim intacta na letra de musica")
    void corrigeDialogoENaoTocaNaMusica(@TempDir Path pasta) throws IOException {
        // CALIBRACAO: a fixture so prova veto se o juiz de PRODUCAO disser que ela e musica.
        assertTrue(politicaEstiloMusical.estiloIgnorado("Opening"),
            "fixture invalida: a producao NAO considera o estilo Opening musical, entao este "
                + "teste nao estaria medindo veto nenhum");
        assertFalse(politicaEstiloMusical.estiloIgnorado("Default"),
            "controle negativo invalido: a producao considera Default musical, entao a linha de "
                + "dialogo seria vetada por motivo errado");

        Path arquivo = pasta.resolve("episodio_PT-BR.ass");
        Files.writeString(arquivo, CABECALHO
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Pegue a " + FORMA_RUIM + " agora!\n"
            + "Dialogue: 0,0:00:04.00,0:00:06.00,Opening,,0,0,0,,A " + FORMA_RUIM + " brilha no ceu\n",
            StandardCharsets.UTF_8);

        RevisarLorePtOnlyUseCase.ResultadoLorePtOnly resultado =
            useCase.executar(pasta, CONTEXTO, false, true);

        String gravado = Files.readString(arquivo, StandardCharsets.UTF_8);
        String linhaDialogo = linhaDoEstilo(gravado, "Default");
        String linhaMusica = linhaDoEstilo(gravado, "Opening");

        assertTrue(linhaDialogo.contains(CANONICO),
            "a tela existe para isto: o nome canonico devia ter entrado no dialogo. Linha: " + linhaDialogo);
        assertFalse(linhaDialogo.contains(FORMA_RUIM),
            "a forma-ruim continua no dialogo: " + linhaDialogo);

        assertTrue(linhaMusica.contains(FORMA_RUIM),
            "A LETRA DE MUSICA FOI REESCRITA. Foi assim que 687 linhas Song ENG do 08th MS Team "
                + "sumiram em 17/08/2026. Linha: " + linhaMusica);
        assertFalse(linhaMusica.contains(CANONICO),
            "o canonico entrou na letra de musica: " + linhaMusica);

        assertEquals(1, resultado.falasCorrigidas(),
            "exatamente UMA fala devia ter sido corrigida (a de dialogo); a contagem tambem nao "
                + "pode incluir a musica");
    }

    private static String linhaDoEstilo(String conteudo, String estilo) {
        return conteudo.lines()
            .filter(l -> l.startsWith("Dialogue:") && l.contains("," + estilo + ","))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "linha de estilo " + estilo + " sumiu do arquivo gravado — a tela NUNCA pode "
                    + "perder fala:\n" + conteudo));
    }
}
