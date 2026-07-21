package org.traducao.projeto.trocaTipoLegenda.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
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
 * PROPÓSITO DE NEGÓCIO: caracteriza o achatamento de estilos decorativos usando o
 * leitor/escritor .ass REAIS sobre um cabeçalho fiel ao do Gundam Unicorn (o caso
 * que motivou a correção: a fala "Nós não vimos todo o seu significado" no estilo
 * OPL2/Dash Horizon com {@code \pos}/{@code \fad}).
 *
 * <p>INVARIANTES DO DOMÍNIO: falas de estilo decorativo (fonte ≠ Default) viram
 * Default e perdem o override inicial; diálogo comum e a saída "Karaoke Simples"
 * ficam intactos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: uma regressão que deixe de reatribuir o estilo
 * ou de remover o {@code \{...\}} inicial reprova o teste com o valor observado.
 */
class AchatadorEstilosDecorativosServiceTest {

    private static final String ASS = String.join("\n",
        "[Script Info]",
        "Title: Teste",
        "ScriptType: v4.00+",
        "PlayResX: 1920",
        "PlayResY: 1080",
        "",
        "[V4+ Styles]",
        "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, "
            + "BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, "
            + "BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding",
        "Style: Default,Gandhi Sans,70,&H00FFFFFF,&H000000FF,&H00020713,&H00000000,-1,0,0,0,"
            + "100,100,0,0,1,2,1,2,0,0,30,1",
        "Style: Sign,Althea,150,&H00FFFFFE,&H000000FF,&H00000000,&H7C1D1D1D,0,0,0,0,"
            + "100,100,0,0,1,0,5,5,10,10,10,1",
        "Style: OPL2,Dash Horizon,130,&H00000000,&HFF363636,&H00FFFFFF,&H00FFFFFF,0,0,0,0,"
            + "100,100,0,0,1,2,0,2,10,10,30,1",
        "Style: Karaoke Simples,Arial,49,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,-1,0,0,"
            + "100,100,0,0,1,2,1,8,30,30,20,1",
        "",
        "[Events]",
        "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
        "Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,Ola mundo",
        "Dialogue: 0,0:01:56.51,0:02:01.68,OPL2,,0,0,0,fx,"
            + "{\\fad(50,200)\\bord0\\c&HFFFFFF&\\1a&H00&\\3c&H000000&\\pos(960,1050)}"
            + "Nós não vimos todo o seu significado",
        "Dialogue: 0,0:03:00.00,0:03:04.00,Sign,,0,0,0,,{\\pos(500,200)}CARTAZ",
        "Dialogue: 0,0:04:00.00,0:04:03.00,Karaoke Simples,,0,0,0,,Romaji da musica",
        "");

    private final LeitorLegendaAss leitor = new LeitorLegendaAss();
    private final EscritorLegendaAss escritor = new EscritorLegendaAss();
    private AchatadorEstilosDecorativosService achatador;

    @BeforeEach
    void setUp() {
        achatador = new AchatadorEstilosDecorativosService(new AuditoriaFontesService());
    }

    @Test
    @DisplayName("acha estilos decorativos (OPL2/Sign) e reatribui ao Default sem override inicial")
    void achataDecorativosParaDefault(@TempDir Path dir) throws IOException {
        DocumentoLegenda doc = lerAss(dir);

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(doc);

        assertTrue(r.houveAchatamento(), "deveria ter achatado ao menos uma fala");
        assertEquals(2, r.falasAchatadas(), "OPL2 e Sign devem ser achatados; Default e Karaoke Simples não");
        assertTrue(r.estilosDecorativos().contains("OPL2"));
        assertTrue(r.estilosDecorativos().contains("Sign"));

        EventoLegenda opl2 = r.documento().eventos().get(1);
        assertEquals("Default", opl2.estilo(), "estilo OPL2 deve virar Default");
        assertEquals("Nós não vimos todo o seu significado", opl2.texto(),
            "o bloco {\\fad...\\pos...} inicial deve ser removido, preservando o texto");
        assertTrue(opl2.prefixo().contains(",Default,"), "a coluna Style do prefixo deve ser Default");
        assertFalse(opl2.prefixo().contains("OPL2"), "não pode sobrar OPL2 no prefixo");

        EventoLegenda sign = r.documento().eventos().get(2);
        assertEquals("Default", sign.estilo());
        assertEquals("CARTAZ", sign.texto());
    }

    @Test
    @DisplayName("preserva diálogo comum e a saída protegida Karaoke Simples")
    void preservaDialogoEKaraokeSimples(@TempDir Path dir) throws IOException {
        DocumentoLegenda doc = lerAss(dir);

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(doc);

        EventoLegenda dialogo = r.documento().eventos().get(0);
        assertEquals("Default", dialogo.estilo());
        assertEquals("Ola mundo", dialogo.texto(), "diálogo comum não deve ser tocado");

        EventoLegenda karaoke = r.documento().eventos().get(3);
        assertEquals("Karaoke Simples", karaoke.estilo(), "a saída Karaoke Simples é protegida");
        assertEquals("Romaji da musica", karaoke.texto());
    }

    @Test
    @DisplayName("a legenda regravada tem a fala OPL2 sem estilo decorativo nem override")
    void regravaSemFrescura(@TempDir Path dir) throws IOException {
        DocumentoLegenda doc = lerAss(dir);

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(doc);
        Path saida = dir.resolve("saida.ass");
        escritor.escrever(saida, r.documento());
        String texto = Files.readString(saida, StandardCharsets.UTF_8);

        assertTrue(texto.contains(
                "Dialogue: 0,0:01:56.51,0:02:01.68,Default,,0,0,0,fx,Nós não vimos todo o seu significado"),
            "a linha OPL2 regravada deve estar em Default e sem o bloco {...}");
    }

    private DocumentoLegenda lerAss(Path dir) throws IOException {
        Path arquivo = dir.resolve("unicorn.ass");
        Files.writeString(arquivo, ASS, StandardCharsets.UTF_8);
        return leitor.ler(arquivo);
    }
}
