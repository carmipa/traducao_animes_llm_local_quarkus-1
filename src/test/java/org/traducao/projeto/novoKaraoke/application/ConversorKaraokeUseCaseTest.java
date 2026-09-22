package org.traducao.projeto.novoKaraoke.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;
import org.traducao.projeto.novoKaraoke.domain.ports.TelemetriaKaraokePort;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversorKaraokeUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void arquivoSemMusicaEhCopiadoByteIdentico() throws Exception {
        Path origem = tempDir.resolve("sem-musica.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        byte[] original = """
            [Script Info]\r
            PlayResY: 1080\r
            \r
            [V4+ Styles]\r
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding\r
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1\r
            \r
            [Events]\r
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text\r
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,template preservado\r
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\r
            """.getBytes(StandardCharsets.UTF_8);
        Files.write(origem, original);

        novoConversor().converterArquivo(origem, destino, true);

        assertEquals(new String(original, StandardCharsets.UTF_8),
            Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o ORIGINAL fica em cima e a tradução embaixo — na música inteira, sem
     * trocar de posição no meio. O espectador não pode ver o japonês ora acima ora abaixo.
     *
     * <p>O critério de ordem era booleano e exigia 100% das palavras serem sílaba Hepburn. Romaji
     * REAL reprova nisso: {@code hitoribocchi}, {@code tsudzuku}, {@code kekkyoku} e
     * {@code icchatte} têm geminada/dígrafo fora da regex de sílaba, e uma palavra em seis derruba
     * a linha. As duas camadas empatavam em "não é romaji", o sort ESTÁVEL preservava a ordem do
     * arquivo, e a linha inglesa subia.
     *
     * <p>Medido no Guilty Crown em 2026-08-03: 33 de 508 versos pareados (6%), sempre os MESMOS
     * versos em todos os episódios — o defeito é do texto, não do arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar a comparar por sim/não, estas linhas invertem.
     */
    @Test
    void romajiComGeminadaFicaAcimaDaTraducaoMesmoNaoSendoSilabaPura() throws Exception {
        Path origem = tempDir.resolve("ordem-camadas.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        // A linha INGLESA vem PRIMEIRO no arquivo de propósito: é o caso real do ED do Guilty
        // Crown e o que fazia o sort estável manter a ordem errada.
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,80)}Now I am all alone\n"
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,40)}Soshite watashi wa koushite hitoribocchi de\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Soshite watashi wa koushite hitoribocchi de\\NNow I am all alone"),
            () -> "o romaji tem de ficar ACIMA da traducao mesmo com 'hitoribocchi' fora da regex "
                + "de silaba (83% romaji, nao 100%): " + saida);
    }

    @Test
    void preservaAsDuasCamadasNoMesmoTempoELimpaTagsVisiveis() throws Exception {
        Path origem = tempDir.resolve("karaoke.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,80)}[]Não importa o quanto eu deseje, nada muda [![TAG1]]\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        // REGRA DE NEGÓCIO (Paulo, 2026-07-25): o romaji é a LÍNGUA ORIGINAL e fica; o que se
        // remove é a frescura visual. As duas camadas viram UM evento com \N — original em cima,
        // tradução embaixo — porque a saída usa um único estilo e eventos separados no mesmo tempo
        // imprimiriam um sobre o outro. Antes desta data o romaji era justamente o descartado.
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,"
            + "aigan shitemo kongan shitemo kawaranai ya, mou"
            + "\\NNão importa o quanto eu deseje, nada muda"), saida);
        assertFalse(saida.contains("[]"));
        assertFalse(saida.contains("TAG1"));
    }

    @Test
    void preservaEventoCurtoSemCoberturaRealMesmoPertoDaLinhaPrincipal() throws Exception {
        Path origem = tempDir.resolve("curta.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\pos(100,40)}Linha principal da música\n"
            + "Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:10.00,0:00:12.00,Karaoke Simples,,0,0,0,,Linha principal da música"));
        assertTrue(saida.contains("Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a abertura {@code OPL2} do Gundam Unicorn é karaokê — {@code Effect="fx"}
     * em todas as linhas, {@code \pos} por sílaba, ZERO {@code \k} — mas o NOME {@code OPL2} não casa
     * o padrão musical ({@code op} seguido de letra derrota a fronteira). Sem a evidência do campo
     * Effect, os 155 eventos KFX por episódio saíam byte a byte, a animação inteira misturada à letra
     * na tela. Caso real medido em 2026-09-21 no {@code C:\animes\ANIMES-TESTES}.
     *
     * <h2>Caso-controle de fronteira (A1)</h2>
     * O MESMO arquivo traz um cartão {@code Sign} e diálogo {@code Default} com o campo Effect VAZIO.
     * Eles NÃO podem ser arrastados pela nova evidência: o carimbo {@code fx} separa o karaokê do
     * resto, ao contrário da assinatura de template ({@code \t} + densidade), que pegaria o letreiro
     * animado. É o que torna o carimbo seguro na classificação inicial, onde a assinatura não seria.
     *
     * <h2>Comportamento em caso de falha</h2>
     * Sem a evidência do Effect, o {@code assertTrue} da linha simples e o {@code assertFalse} de
     * "sobrou OPL2" reprovam — o KFX teria passado intacto.
     */
    @Test
    void aberturaOpl2ComEffectFxEhAchatadaMasEffectVazioFicaIntacto() throws Exception {
        Path origem = tempDir.resolve("opl2.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            // OPL2: nome NAO casa o padrao musical, ZERO \k, so \pos, Effect=fx -> so entra por fx
            + "Dialogue: 0,0:01:37.00,0:01:39.80,OPL2,,0,0,0,fx,{\\pos(640,60)}Do you feel alone\n"
            + "Dialogue: 0,0:01:37.00,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(560,60)}Do\n"
            + "Dialogue: 0,0:01:37.15,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(600,60)}you\n"
            + "Dialogue: 0,0:01:37.24,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(640,60)}feel\n"
            + "Dialogue: 0,0:01:37.61,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(700,60)}alone\n"
            // CONTROLE: Effect VAZIO -> NAO pode ser arrastado pela nova evidencia
            + "Dialogue: 0,0:07:02.61,0:07:05.00,Sign,,0,0,0,,{\\pos(960,540)}DEPARTURE 0096\n"
            + "Dialogue: 0,0:00:00.52,0:00:02.00,Default,,0,0,0,,Fear not.\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Karaoke Simples,,0,0,0,,Do you feel alone"),
            () -> "a abertura OPL2 tinha de virar linha simples pelo carimbo Effect=fx:\n" + saida);
        assertFalse(saida.contains("OPL2"),
            () -> "nenhum evento/estilo OPL2 pode sobrar apos o achatamento:\n" + saida);
        // CONTROLE de fronteira: Effect vazio nao vira musica e sai byte-identico
        assertTrue(saida.contains("Sign,,0,0,0,,{\\pos(960,540)}DEPARTURE 0096"),
            () -> "o cartao Sign de Effect VAZIO tinha de sair intacto:\n" + saida);
        assertTrue(saida.contains("Default,,0,0,0,,Fear not."),
            () -> "o dialogo de Effect VAZIO tinha de sair intacto:\n" + saida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando o Passo 1 (Tradução de Karaokê) já entregou o par bilíngue
     * {@code original\Ntradução} num evento ÚNICO — o caso da abertura OPL2 do Unicorn, que é inglês
     * ORIGINAL cantado, preservado em cima com o PT embaixo —, o achatamento tem de manter as DUAS
     * linhas. Antes deste conserto, {@code textoVisivel()} trocava o {@code \N} por espaço e a saída
     * colava "inglês PT" numa linha só (a mistura que o Paulo apontou em 21/09, medida no .ass real).
     *
     * <h2>Caso-controle de fronteira (A1)</h2>
     * O par bilíngue num evento vira DUAS linhas ({@code \N} preservado); o KFX puro silábico (sem
     * {@code \N}, coberto por {@code kfxApenasSilabicoViraLinhaSimplesENaoArquivoGrande}) continua UMA
     * linha, sem {@code \N} INVENTADO. Os dois lados carregam o mesmo sinal superficial (evento
     * musical achatável), e a régua é só a presença do {@code \N} deliberado.
     *
     * <h2>Comportamento em caso de falha</h2>
     * Se a saída voltar a usar o texto achatado (sem {@code \N}), o inglês e o PT colam numa linha.
     */
    @Test
    void parBilingueNumEventoUnicoViraDuasLinhasComAQuebraPreservada() throws Exception {
        Path origem = tempDir.resolve("opl2-bilingue.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            // frase OPL2 que JA traz original\Ntraducao (como sai do Passo 1) + silabas do KFX
            + "Dialogue: 0,0:01:37.00,0:01:39.80,OPL2,,0,0,0,fx,{\\pos(640,60)}Do you feel alone\\NVocê se sente sozinho?\n"
            + "Dialogue: 0,0:01:37.00,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(560,60)}Do\n"
            + "Dialogue: 0,0:01:37.15,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(600,60)}you\n"
            + "Dialogue: 0,0:01:37.24,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(640,60)}feel\n"
            + "Dialogue: 0,0:01:37.61,0:01:39.90,OPL2,,0,0,0,fx,{\\pos(700,60)}alone\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Karaoke Simples,,0,0,0,,Do you feel alone\\NVocê se sente sozinho?"),
            () -> "o par bilingue tinha de sair em DUAS linhas (\\N preservado), nao colado:\n" + saida);
        assertFalse(saida.contains("Do you feel alone Você se sente sozinho?"),
            () -> "o \\N nao pode virar espaco (ingles e PT colados numa linha):\n" + saida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o outro lado da fronteira (A1) do fix #2 — a quebra {@code \N} de um
     * verso MONOLÍNGUE (o fansub quebrou uma linha de letra em duas, mesma língua, SEM tradução PT
     * do outro lado) NÃO é par bilíngue e tem de ACHATAR para uma linha, o comportamento histórico.
     *
     * <h2>O prejuízo que a revisão adversarial mediu (21/09/2026)</h2>
     * Preservar toda quebra {@code \N} musical mudava a saída de 44 {@code Song JP} + 154 {@code ED - EN} +
     * 13 {@code ED - Romaji} do acervo (verso de 1 linha virava 2) e poluía o contador
     * {@code pareadas} da telemetria. Só o par {@code original\NtraduçãoPT} (a abertura OPL2) vira
     * duas linhas — ver {@link #parBilingueNumEventoUnicoViraDuasLinhasComAQuebraPreservada}.
     */
    @Test
    void versoMonolingueComQuebraAchataParaUmaLinha() throws Exception {
        Path origem = tempDir.resolve("verso-monolingue.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        // ED - EN do Unicorn: ingles\Ningles (mesma lingua, sem PT) — o fansub quebrou o verso
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:22:16.00,0:22:20.00,OPL2,,0,0,0,fx,{\\pos(640,60)}Have a little break\\NWe are running through the lights\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Karaoke Simples,,0,0,0,,Have a little break We are running through the lights"),
            () -> "verso monolingue (ingles\\Ningles, sem PT) tinha de ACHATAR para uma linha:\n" + saida);
        assertFalse(saida.contains("Have a little break\\NWe are running"),
            () -> "verso monolingue nao pode virar 2 linhas — nao é par bilingue original\\NPT:\n" + saida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: par bilíngue em que a tradução PT é CORRETA mas SEM acento nenhum
     * ({@code "E estou chamando seu nome novamente"}) tem de ser reconhecido e virar 2 linhas. O
     * diacrítico sozinho não pega; o DICIONÁRIO Hunspell reconhece {@code estou}/{@code chamando}/
     * {@code seu}/{@code nome}/{@code novamente} como português e o inglês do outro lado como inglês.
     * (No teste, o dublê {@code CorretorOrtograficoDeTeste} dá esses vereditos; em produção é o
     * hunspell real.) Medido no e2e de 21/09: 2 das 16 linhas da abertura colavam por falta disso.
     */
    @Test
    void parBilingueComTraducaoSemAcentoPreservaAsDuasLinhas() throws Exception {
        Path origem = tempDir.resolve("opl2-sem-acento.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:02:04.00,0:02:09.00,OPL2,,0,0,0,fx,{\\pos(640,60)}And Im calling out your name again\\NE estou chamando seu nome novamente\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("And Im calling out your name again\\NE estou chamando seu nome novamente"),
            () -> "PT sem acento (chamando/novamente/estou/seu) tinha de ser reconhecido e manter 2 linhas:\n" + saida);
        assertFalse(saida.contains("name again E estou"),
            () -> "o par nao pode colar numa linha so:\n" + saida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso "para valer" que era o resíduo de 1/16 — par bilíngue em que a
     * tradução PT é CURTA e SEM diacrítico ({@code "Deixe a luz passar"}). O que resolve não é uma
     * lista de palavras à mão (colidiria com o inglês), e sim o DICIONÁRIO: {@code deixe}/{@code luz}/
     * {@code passar} são português e {@code Let}/{@code light}/{@code shine}/{@code through} são
     * inglês — a pontuação de cada lado decide. (Ordem do Paulo, 21/09: usar os dicionários do
     * projeto, não léxico à mão.) O dublê dá esses vereditos; em produção é o hunspell real.
     */
    @Test
    void parBilingueSemDiacriticoReconhecidoPeloDicionario() throws Exception {
        Path origem = tempDir.resolve("opl2-sinal-ingles.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:02:18.00,0:02:21.00,OPL2,,0,0,0,fx,{\\pos(640,60)}Let light shine through\\NDeixe a luz passar\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Let light shine through\\NDeixe a luz passar"),
            () -> "o dicionario (deixe/luz/passar=PT, let/light/through=EN) devia manter o par em 2 linhas:\n" + saida);
        assertFalse(saida.contains("through Deixe"),
            () -> "o par nao pode colar numa linha so:\n" + saida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: FALHA FECHADA do discriminador — dicionário INDISPONÍVEL (hunspell fora
     * do ar) não pode quebrar o achatador. Cai no fallback do diacrítico: o par com acento
     * ({@code "Você"}) ainda vira 2 linhas. É a garantia de que o achatador roda mesmo sem o
     * dicionário, com degradação declarada (o PT sem acento aí achataria).
     */
    @Test
    void semDicionarioDisponivelCaiNoFallbackDoDiacritico() throws Exception {
        ConversorKaraokeUseCase conversor = new ConversorKaraokeUseCase();
        conversor.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conversor.logStream = new LogStreamSilencioso();
        conversor.telemetriaKaraoke = new TelemetriaKaraokeSilenciosa();
        conversor.corretorOrtografico = new CorretorOrtograficoLegenda() {
            @Override
            public Map<String, VeredictoPalavra> classificarPalavras(Collection<String> palavras) {
                return Map.of(); // dicionario indisponivel: nenhum veredicto
            }
        };
        Path origem = tempDir.resolve("fallback.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:01:00.00,0:01:04.00,OPL2,,0,0,0,fx,{\\pos(640,60)}Do you feel alone\\NVocê se sente sozinho?\n",
            StandardCharsets.UTF_8);

        conversor.converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Do you feel alone\\NVocê se sente sozinho?"),
            () -> "sem dicionario, o diacritico (ê) ainda preserva o par acentuado em 2 linhas:\n" + saida);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma quebra {@code \N} no FIM da fala (comum no fansub) NÃO pode virar
     * uma linha VAZIA na tela. Medido pela revisão adversarial de 21/09 no {@code ED - EN} do
     * Unicorn ({@code "...love I felt\N"}): o {@code .strip()} não remove {@code \N}, e a linha
     * simples terminava em {@code \N} → linha em branco sob a letra. {@code normalizarQuebras} apara.
     */
    @Test
    void quebraNoFimNaoViraLinhaVazia() throws Exception {
        Path origem = tempDir.resolve("quebra-final.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:23:14.00,0:23:19.00,OPL2,,0,0,0,fx,{\\pos(640,60)}You do never know that love I felt\\N\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Karaoke Simples,,0,0,0,,You do never know that love I felt"),
            () -> "a linha tinha de sair limpa:\n" + saida);
        assertFalse(saida.contains("love I felt\\N"),
            () -> "o \\N final foi aparado — nao pode sobrar quebra que renderiza linha vazia:\n" + saida);
    }

    @Test
    void kfxApenasSilabicoViraLinhaSimplesENaoArquivoGrande() throws Exception {
        Path origem = tempDir.resolve("kfx-silabico.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 1,0:00:01.00,0:00:01.30,Opening,,0,0,0,,{\\pos(100,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}fu\n"
            + "Dialogue: 1,0:00:01.02,0:00:01.28,Opening,,0,0,0,,{\\pos(101,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}fu\n"
            + "Dialogue: 1,0:00:01.30,0:00:01.60,Opening,,0,0,0,,{\\pos(130,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}mi\n"
            + "Dialogue: 1,0:00:01.60,0:00:01.90,Opening,,0,0,0,,{\\pos(160,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}ni\n"
            + "Dialogue: 1,0:00:01.90,0:00:02.30,Opening,,0,0,0,,{\\pos(190,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}hana\n",
            StandardCharsets.UTF_8);

        var resultado = novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:02.30,Karaoke Simples,,0,0,0,,fu mi ni hana"));
        assertFalse(saida.contains("\\t("));
        assertFalse(saida.contains("\\clip("));
        assertFalse(saida.contains("Style: Opening"));
        assertEquals(5, resultado.getEventosKaraokeRemovidos());
        assertEquals(0, resultado.getEventosPreservadosPorSeguranca());
        assertTrue(resultado.getTamanhoNovoBytes() < resultado.getTamanhoOriginalBytes());
    }

    @Test
    void kfxComLayersDuplicadosPrefereLegendaOcidentalSimples() throws Exception {
        Path origem = tempDir.resolve("kfx-duas-faixas.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,30,100,30,0,3000)\\t(0,3000,\\frz1)}ki\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,30,130,30,0,3000)\\t(0,3000,\\frz1)}mi\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(160,30,160,30,0,3000)\\t(0,3000,\\frz1)}no\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,30,100,30,0,3000)\\t(0,3000,\\frz1)}ki\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,30,130,30,0,3000)\\t(0,3000,\\frz1)}mi\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(160,30,160,30,0,3000)\\t(0,3000,\\frz1)}no\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,1050,100,1050,0,3000)\\t(0,3000,\\frz1)}O\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,1050,130,1050,0,3000)\\t(0,3000,\\frz1)}i\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        // O romaji pulverizado pelo KFX é reconstruído ("ki mi no") e PRESERVADO acima da
        // tradução, em vez de descartado: continua sendo a língua original.
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,ki mi no\\NOi"), saida);
        assertFalse(saida.contains("\\move("));
        assertFalse(saida.contains("\\t("));
    }

    @Test
    void kfxLetraPorLetraContinuoEhCortadoPorFraseComEspacos() throws Exception {
        // KFX real (86): cada letra é um evento, a frase inteira fica na tela ao
        // mesmo tempo e a frase seguinte começa EXATAMENTE quando a anterior
        // termina — o gap nunca separa; o corte tem que vir do vale de concorrência.
        Path origem = tempDir.resolve("kfx-letra-a-letra.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        StringBuilder corpo = new StringBuilder(cabecalho());
        corpo.append(eventosPorLetra("0:00:01.00", "0:00:05.00", "Voce pode"));
        corpo.append(eventosPorLetra("0:00:05.00", "0:00:09.00", "Nada muda"));
        Files.writeString(origem, corpo.toString(), StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke Simples,,0,0,0,,Voce pode"), saida);
        assertTrue(saida.contains("Dialogue: 0,0:00:05.00,0:00:09.00,Karaoke Simples,,0,0,0,,Nada muda"), saida);
        assertFalse(saida.contains("VocepodeNadamuda"), saida);
    }

    @Test
    void deduplicaCamadasComJanelasQuaseIdenticasENaoSoIguais() throws Exception {
        // romaji e tradução simultâneos raramente terminam no MESMO centésimo;
        // a deduplicação precisa agrupar por sobreposição, não por janela exata
        Path origem = tempDir.resolve("janelas-quase-iguais.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.96,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:05.00,Opening,,0,0,0,,{\\pos(100,80)}Não importa o quanto eu deseje, nada muda\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        // Agrupar por sobreposição continua valendo (as janelas diferem em 4 centésimos); o que
        // mudou é o desfecho: as duas camadas são preservadas juntas, não uma descartada.
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke Simples,,0,0,0,,"
            + "aigan shitemo kongan shitemo kawaranai ya, mou"
            + "\\NNão importa o quanto eu deseje, nada muda"), saida);
    }

    @Test
    void blocoKfxQueViraLinhaImplausivelEhPreservadoIntacto() throws Exception {
        // sem vale de concorrência não há como separar as frases: melhor manter o
        // efeito original do que emitir uma parede de texto de 29 segundos
        Path origem = tempDir.resolve("kfx-irreconstruivel.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + eventosPorLetra("0:00:01.00", "0:00:30.00", "Frase longa demais"),
            StandardCharsets.UTF_8);

        var resultado = novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertFalse(saida.contains("Karaoke Simples"), saida);
        assertTrue(saida.contains("{\\pos(100.0,40)\\t(0,100,\\blur4\\fscx120)}F"), saida);
        assertEquals(0, resultado.getEventosKaraokeRemovidos());
        assertTrue(resultado.getEventosPreservadosPorSeguranca() > 0);
    }

    /** Um evento Dialogue por letra visível, todos na janela inteira da frase (KFX letra-por-letra). */
    private static String eventosPorLetra(String inicio, String fim, String frase) {
        StringBuilder eventos = new StringBuilder();
        double x = 100;
        for (char letra : frase.toCharArray()) {
            if (letra == ' ') {
                x += 40; // espaço não vira evento: só o salto em X marca a palavra
                continue;
            }
            eventos.append("Dialogue: 1,").append(inicio).append(',').append(fim)
                .append(",Opening,,0,0,0,,{\\pos(").append(x).append(",40)\\t(0,100,\\blur4\\fscx120)}")
                .append(letra).append('\n');
            x += 20;
        }
        return eventos.toString();
    }

    @Test
    void ignoraArquivosAuxiliaresQuandoHaEpisodiosPrincipais() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86_-_NCOP01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Opening,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86 Special Edition Senya_-_SP_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Especial.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), processados);
    }

    @Test
    void processaAuxiliaresQuandoPastaTemApenasAuxiliares() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Ending,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), processados);
    }

    private static ConversorKaraokeUseCase novoConversor() {
        ConversorKaraokeUseCase conversor = new ConversorKaraokeUseCase();
        conversor.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conversor.logStream = new LogStreamSilencioso();
        conversor.telemetriaKaraoke = new TelemetriaKaraokeSilenciosa();
        conversor.corretorOrtografico = new CorretorOrtograficoDeTeste();
        return conversor;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dublê do classificador de idioma por dicionário. O hunspell real não
     * está na máquina de teste, então o veredicto de cada palavra é dado por uma tabela conhecida —
     * o suficiente para exercitar a decisão inglês×português do achatador de forma determinística.
     * Palavra fora da tabela vira {@code DESCONHECIDA} (o mesmo que o dicionário faria com nome
     * próprio/termo de lore).
     */
    private static final class CorretorOrtograficoDeTeste extends CorretorOrtograficoLegenda {
        private static final Map<String, VeredictoPalavra> VEREDITO = new LinkedHashMap<>();
        static {
            String pt = "voce se sente sozinho pode me ouvir agora sua mente ainda esta tao distante presa "
                + "terra muitas vezes esta machucando nao apenas uma vida prateleira somente pilotar novo "
                + "ceu sempre assim machuca facas estou chamando seu nome novamente estiver preso medo "
                + "soubesse cegos podem abrir deixe luz passar digo do da de que a e o os as com para "
                + "eu sei todas mentiras tornaram pedra coracao pergunto quanto tempo vai sobreviver";
            String en = "do you feel alone can hear now mind is so far away still on earth many times are "
                + "hurting yourself cant be just life shelf its only that fly this new unicorn into the sky "
                + "and every time hurt with knives im calling out your name again if holding onto fear i knew "
                + "blind open let light shine through we say why stop all sacrifice know lies became stone in "
                + "heart wonder how long gonna survive didnt see meaning have little break running lights "
                + "take off my sought idol then breathe deep dress crown fall sound asleep";
            for (String w : pt.split(" ")) VEREDITO.put(w, VeredictoPalavra.PORTUGUES_OK);
            for (String w : en.split(" ")) VEREDITO.putIfAbsent(w, VeredictoPalavra.RESIDUO_INGLES);
        }

        @Override
        public Map<String, VeredictoPalavra> classificarPalavras(Collection<String> palavras) {
            Map<String, VeredictoPalavra> fora = new LinkedHashMap<>();
            for (String p : palavras) {
                fora.put(p, VEREDITO.getOrDefault(p.toLowerCase(Locale.ROOT), VeredictoPalavra.DESCONHECIDA));
            }
            return fora;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a medição não pode participar do resultado — a suíte usa uma
     * implementação inerte para provar isso. Se um teste passar a depender do que a telemetria
     * faz, a porta deixou de ser unidirecional.
     *
     * <p>É esta a razão de a medição entrar por PORTA e não pelo serviço concreto: a fatia é
     * testável sem a fatia {@code telemetria} existir.
     */
    private static final class TelemetriaKaraokeSilenciosa implements TelemetriaKaraokePort {
        @Override
        public void publicarArquivo(String arquivo, int eventosEntrada, int eventosSaida,
            int camadasPareadas, int camadasInvertidas,
            java.util.List<org.traducao.projeto.novoKaraoke.domain.MedicaoEstiloKaraoke> porEstilo) {
            // inerte de propósito
        }

        @Override
        public void publicarOperacao(String operacao, java.nio.file.Path pastaOrigem,
            java.nio.file.Path pastaDestino, long duracaoMs, int arquivosProcessados) {
            // inerte de propósito
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a tipografia EMPILHADA (halo + sombra + texto, no mesmo instante) é
     * achatada para o evento legível; fala repetida NA MESMA LAYER continua intacta.
     *
     * <h2>O caso real que originou isto</h2>
     * Cartão de data do 86, três eventos na janela EXATA {@code 0:00:00.00 → 0:00:02.98}:
     * <pre>
     *   layer 0  \1a&amp;HFF&amp;  \blur5      -> preenchimento transparente = halo
     *   layer 1  \c&amp;H000000&amp;  \blur0.5  -> preto = sombra
     *   layer 2  (sem cor)               -> herda a cor do estilo = TEXTO legível
     * </pre>
     * Os três iam ao LLM separadamente e em quatro deles o modelo escreveu {@code 2049} no lugar
     * de {@code 2149}, virando pendência. Sobrevive a de MAIOR layer, que é a convenção do ASS —
     * guardar a primeira renderizaria o halo invisível; a segunda, texto preto.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Layer DIFERENTE é obrigatório: duas pessoas dizendo a mesma coisa no mesmo instante
     *       ficam na MESMA layer, e achatar isso apagaria fala. Medido no 86: com layer distinta,
     *       {@code Default} dá ZERO; sem ela, daria 2.</li>
     *   <li>O evento sobrevivente mantém tags e posição — não se remonta nada.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Ou o cartão sai triplicado na tela, ou uma fala legítima some.
     */
    @Test
    void achataTipografiaEmpilhadaMasPreservaFalaRepetidaNaMesmaLayer() throws Exception {
        Path origem = tempDir.resolve("empilhado.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            // musica: sem ela o conversor copia o arquivo intacto e nao ha o que medir
            + "Dialogue: 0,0:01:00.00,0:01:04.00,Opening,,0,0,0,,{\\pos(100,40)}kimi no koe ga kikoeru\n"
            // cartao de data: TRES layers, mesma janela, mesmo texto -> colapsa para a layer 2
            + "Dialogue: 0,0:00:00.00,0:00:02.98,Signs,,0,0,0,,{\\1a&HFF&\\blur5}30 de julho, Ano Estelar 2149\n"
            + "Dialogue: 1,0:00:00.00,0:00:02.98,Signs,,0,0,0,,{\\c&H000000&\\blur0.5}30 de julho, Ano Estelar 2149\n"
            + "Dialogue: 2,0:00:00.00,0:00:02.98,Signs,,0,0,0,,{\\fs80}30 de julho, Ano Estelar 2149\n"
            // fala repetida na MESMA layer e no mesmo instante: NAO pode ser achatada
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,Sim, senhor!\n"
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,Sim, senhor!\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);
        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);

        long cartao = saida.lines().filter(l -> l.contains("Ano Estelar 2149")).count();
        assertEquals(1, cartao,
            () -> "as tres camadas do cartao tinham de virar UMA:\n" + saida);
        assertTrue(saida.contains("{\\fs80}30 de julho, Ano Estelar 2149"),
            () -> "tem de sobreviver a de MAIOR layer (a legivel), nao o halo nem a sombra:\n" + saida);

        long falaRepetida = saida.lines().filter(l -> l.contains("Sim, senhor!")).count();
        assertEquals(2, falaRepetida,
            () -> "mesma layer NAO e empilhamento: as duas falas tinham de sobreviver:\n" + saida);
    }

    private static String cabecalho() {
        return """
            [Script Info]
            PlayResY: 1080

            [V4+ Styles]
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1

            [Events]
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
            """;
    }

    private static final class LogStreamSilencioso extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // Testes unitarios nao precisam de SSE nem arquivo de log.
        }
    }
}
