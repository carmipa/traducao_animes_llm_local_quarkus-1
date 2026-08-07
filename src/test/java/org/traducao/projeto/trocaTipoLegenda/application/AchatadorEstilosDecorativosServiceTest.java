package org.traducao.projeto.trocaTipoLegenda.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService;
import org.traducao.projeto.trocaTipoLegenda.infrastructure.ClassificadorCamadaMusicalLegendaAdapter;
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
        // O achatador não conhece mais o peer `legenda`: recebe a porta. O adaptador real é
        // montado aqui só porque este teste quer o comportamento ponta a ponta; um teste de
        // regra pura pode passar uma classificação fixa, sem levantar o peer.
        DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();
        achatador = new AchatadorEstilosDecorativosService(
            new AuditoriaFontesService(),
            new ClassificadorCamadaMusicalLegendaAdapter(detector, new ProtecaoCamadasMusicaisService(detector)));
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

    @Test
    @DisplayName("romaji NÃO é achatado, mesmo com fonte própria — e o letreiro comum continua sendo")
    void naoAchataRomajiMasContinuaAchatandoLetreiro(@TempDir Path dir) throws IOException {
        DocumentoLegenda doc = lerKaraoke(dir);

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(doc);

        EventoLegenda romaji = r.documento().eventos().get(1);
        assertEquals("OP_S2_roma", romaji.estilo(),
            "achatar o romaji o joga no estilo do diálogo: rodapé, em cima da fala, e some a pista "
                + "que separa as duas camadas do karaokê");
        assertTrue(romaji.prefixo().contains(",OP_S2_roma,"), "o prefixo do romaji deve ficar intacto");
        assertFalse(r.estilosDecorativos().contains("OP_S2_roma"));

        EventoLegenda letreiro = r.documento().eventos().get(3);
        assertEquals("Default", letreiro.estilo(), "letreiro comum continua sendo achatado");
        assertTrue(r.estilosDecorativos().contains("Sign"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o arquivo achatado tem de DIZER que foi achatado e o
     * que existia antes.
     *
     * <p>O PREJUÍZO: auditado o acervo em 07/08/2026, o Gundam 0080 tinha
     * {@code Default 226 · Signs 150 · OP 21} na origem e {@code Default 397} na
     * saída. Depois de achatar, nada no arquivo distingue diálogo de letreiro ou
     * música. Foi assim que 18.431 falas foram classificadas como resíduo de
     * tradução numa auditoria quando eram letra de música com o estilo apagado.
     */
    @Test
    @DisplayName("o arquivo achatado registra no cabecalho o que foi colapsado")
    void carimbaNoCabecalho(@TempDir Path dir) throws IOException {
        DocumentoLegenda doc = lerAss(dir);

        String cabecalho = achatador.achatar(doc).documento().cabecalho();

        assertTrue(cabecalho.contains("; KRONOS achatou:"), "o carimbo tem de existir");
        assertTrue(cabecalho.contains("backups/"), "tem de apontar onde esta o original");

        // A asserção olha a LINHA do carimbo, não o cabeçalho inteiro. Procurar
        // "OPL2" no todo passava sozinha: o nome também aparece em [V4+ Styles],
        // e a primeira versão deste teste sobreviveu à mutação que apagava a
        // listagem — mesmo defeito de assertiva larga demais que já custou caro
        // nesta base.
        String linhaEstilos = cabecalho.lines()
            .filter(l -> l.startsWith("; KRONOS estilos originais:"))
            .findFirst()
            .orElse("");
        assertTrue(linhaEstilos.contains("OPL2"),
            "o estilo OPL2 tem de estar NA LINHA do carimbo: [" + linhaEstilos + "]");
        assertTrue(linhaEstilos.contains("Sign"),
            "o estilo Sign tem de estar NA LINHA do carimbo: [" + linhaEstilos + "]");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o carimbo é comentário do formato, não conteúdo.
     * Player e libass ignoram linha iniciada por {@code ;} — o Aegisub usa a mesma
     * convenção no cabeçalho que ele próprio gera.
     */
    @Test
    @DisplayName("o carimbo entra como COMENTARIO, dentro de [Script Info]")
    void carimboEhComentarioNaSecaoCerta(@TempDir Path dir) throws IOException {
        DocumentoLegenda doc = lerAss(dir);

        String cabecalho = achatador.achatar(doc).documento().cabecalho();

        for (String linha : cabecalho.split("\n")) {
            if (linha.contains("KRONOS")) {
                assertTrue(linha.startsWith(";"),
                    "linha do carimbo que nao comeca com ';' vira conteudo para o player: " + linha);
            }
        }
        int scriptInfo = cabecalho.indexOf("[Script Info]");
        int carimbo = cabecalho.indexOf("; KRONOS achatou:");
        int estilos = cabecalho.indexOf("[V4+ Styles]");
        assertTrue(scriptInfo < carimbo && carimbo < estilos,
            "o carimbo tem de ficar DENTRO de [Script Info], nao solto entre secoes");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: achatar duas vezes não pode empilhar carimbo. Sem
     * isto o cabeçalho cresceria a cada execução e o carimbo antigo — já falso —
     * continuaria no arquivo ao lado do novo.
     */
    @Test
    @DisplayName("achatar arquivo que JA tem carimbo substitui, nao empilha")
    void carimboEhIdempotente(@TempDir Path dir) throws IOException {
        // A primeira versão deste teste reachatava o resultado da primeira
        // passada — e um documento já achatado sai por "nada a achatar" ANTES de
        // carimbar, então o caminho da idempotência nunca era exercitado e a
        // mutação que a removia passava verde.
        //
        // O caso real: um arquivo com carimbo antigo que AINDA tem estilo
        // decorativo a achatar — acontece quando a lista de decorativos muda ou
        // quando o operador reprocessa uma pasta com arquivos em estados
        // diferentes.
        Path arquivo = dir.resolve("ja-carimbado.ass");
        String comCarimboAntigo = ASS.replace("[Script Info]", String.join("\n",
            "[Script Info]",
            "; KRONOS achatou: 999 fala(s) para o estilo \"Antigo\"",
            "; KRONOS estilos originais: EstiloQueNaoExisteMais",
            "; KRONOS original preservado em backups/ desta execucao"));
        Files.writeString(arquivo, comCarimboAntigo, StandardCharsets.UTF_8);

        String cabecalho = achatador.achatar(leitor.ler(arquivo)).documento().cabecalho();

        int ocorrencias = cabecalho.split("; KRONOS achatou:", -1).length - 1;
        assertEquals(1, ocorrencias,
            "o cabecalho ficou com " + ocorrencias + " carimbos; o antigo tinha de ser substituido");
        assertFalse(cabecalho.contains("EstiloQueNaoExisteMais"),
            "o carimbo ANTIGO ficou no arquivo ao lado do novo, afirmando algo que nao e mais verdade");
        assertFalse(cabecalho.contains("999 fala(s)"),
            "a contagem antiga sobreviveu e agora mente sobre o arquivo");
    }

    private DocumentoLegenda lerAss(Path dir) throws IOException {
        Path arquivo = dir.resolve("unicorn.ass");
        Files.writeString(arquivo, ASS, StandardCharsets.UTF_8);
        return leitor.ler(arquivo);
    }

    /** Estrutura real do OP: romaji e tradução no MESMO tempo, cada um com sua fonte. */
    private DocumentoLegenda lerKaraoke(Path dir) throws IOException {
        String ass = String.join("\n",
            "[Script Info]",
            "ScriptType: v4.00+",
            "",
            "[V4+ Styles]",
            "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, "
                + "BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, "
                + "BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding",
            "Style: Default,Gandhi Sans,70,&H00FFFFFF,&H000000FF,&H00020713,&H00000000,-1,0,0,0,"
                + "100,100,0,0,1,2,1,2,0,0,30,1",
            "Style: OP_S2_roma,Amienne,60,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,"
                + "100,100,0,0,1,2,1,8,10,10,10,1",
            "Style: OP_S2,Amienne,60,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,"
                + "100,100,0,0,1,2,1,8,10,10,40,1",
            "Style: Sign,Althea,150,&H00FFFFFE,&H000000FF,&H00000000,&H7C1D1D1D,0,0,0,0,"
                + "100,100,0,0,1,0,5,5,10,10,10,1",
            "",
            "[Events]",
            "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
            "Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,Ola mundo",
            "Dialogue: 0,0:01:23.45,0:01:26.78,OP_S2_roma,,0,0,0,,Sonna sekai ni nokosareta boku wa",
            "Dialogue: 0,0:01:23.40,0:01:26.90,OP_S2,,0,0,0,,In this world I was left behind",
            "Dialogue: 0,0:03:00.00,0:03:04.00,Sign,,0,0,0,,{\\pos(500,200)}CARTAZ",
            "");
        Path arquivo = dir.resolve("karaoke.ass");
        Files.writeString(arquivo, ass, StandardCharsets.UTF_8);
        return leitor.ler(arquivo);
    }
}
