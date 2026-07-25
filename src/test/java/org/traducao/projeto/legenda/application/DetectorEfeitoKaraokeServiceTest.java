package org.traducao.projeto.legenda.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorEfeitoKaraokeServiceTest {

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

    // Linhas reais extraídas do cache versionado do projeto (Guilty Crown ep13, estilo ED_S2_roma,
    // e o karaokê do 0083), usadas como conjunto-ouro do escore de romaji.
    private static final String ROMAJI_PURO = "Sonna sekai ni nokosareta boku wa";
    private static final String ROMAJI_COM_INGLES = "mugen no Energy yobisamasu";
    private static final String KARAOKE_INGLES = "But no matter how bad a fight we'd have";

    @Test
    void preservaRomajiQuandoEstiloSeparaOMarcadorPorSublinhado() {
        // Estilo real do ED do Guilty Crown. Com fronteira \b o sublinhado é caractere de palavra,
        // então "ED_S2_roma" NÃO casava e as 29 linhas de romaji do ED/OP foram traduzidas.
        assertTrue(detector.devePreservarKaraokeOriginal("ED_S2_roma", "{\\k30}But {\\k25}why"));
        assertTrue(detector.devePreservarKaraokeOriginal("OP_S2_roma", "{\\k30}But {\\k25}why"));
        // A forma com espaço, que já funcionava, continua valendo.
        assertTrue(detector.devePreservarKaraokeOriginal("OP Roma", "{\\k30}But {\\k25}why"));
    }

    @Test
    void naoConfundeMarcadorDeRomajiComPalavraQueApenasComeceIgual() {
        // "Romance"/"Roman" não podem ligar a proteção só por começarem com "roma".
        assertFalse(detector.devePreservarKaraokeOriginal("Romance", "{\\k30}But {\\k25}why"));
        assertFalse(detector.devePreservarKaraokeOriginal("Roman Signs", "{\\k30}But {\\k25}why"));
    }

    @Test
    void estiloQueSeDeclaraRomajiVirouIndicadorDeMusicaPorSiSo() {
        // Sem tag \k e sem nome musical reconhecível: antes da Fase 1 a linha nem chegava a ser
        // tratada como música e o romaji ia para o LLM. É o caso das 29 linhas do Guilty Crown ep13.
        String romajiSemTagK = "{\\3c&HF497D3&\\blur4.5\\fad(200,150)}Sonna sekai ni nokosareta boku wa";
        assertTrue(detector.devePreservarKaraokeOriginal("ED_S2_roma", romajiSemTagK));
        assertTrue(detector.devePreservarKaraokeOriginal("OP_S2_roma", romajiSemTagK));
        // E não vale o contrário: nome sem marcador de romaji e texto em inglês segue traduzível.
        assertFalse(detector.devePreservarKaraokeOriginal("ED_S2", "In this world I was left behind"));
    }

    @Test
    void inventarioDeEstilosReaisDoAcervoEstaCongelado() {
        // Os 45 estilos distintos do cache versionado; aqui os representativos de cada decisão.
        for (String musical : new String[] {
            "Opening", "Ending", "Ending 2", "OP", "ED", "Song ENG", "Karaoke Simples",
            "Insert", "Gundam 0083 ED3 Lyrics", "Copy of OP", "OP Roma", "ED Roma L1"}) {
            assertTrue(detector.eEstiloDeMusica(musical), "deveria ser música: " + musical);
        }
        for (String comum : new String[] {
            "Dialogue", "Default", "Default - Alt", "Signs", "Titles", "EG", "nextep", "Next Ep",
            "Ep Titles", "Zeta Episode Title", "08thMS", "Axis", "preludezz", "Copy of label2",
            "Gundam Narrative Cage"}) {
            assertFalse(detector.eEstiloDeMusica(comum), "não deveria ser música: " + comum);
        }
        // CONHECIDO E DELIBERADO: as camadas em inglês com sublinhado/dígito (494 linhas no acervo)
        // seguem fora do conjunto musical. Alargar o padrão de NOME as arrastaria de uma vez para o
        // simplificador de karaokê e para o fluxo de correção — pertence à fase do karaokê simples.
        for (String pendente : new String[] {"OP2", "ED_S2", "OP_S2"}) {
            assertFalse(detector.eEstiloDeMusica(pendente),
                "mudança consciente de fase futura, não regressão: " + pendente);
        }
    }

    @Test
    void proporcaoRomajiSeparaRomajiDeKaraokeEmIngles() {
        // Medido no cache real: estilo romaji tem mediana 100; música não-romaji, 22.
        assertEquals(100, detector.proporcaoRomaji(ROMAJI_PURO));
        assertTrue(detector.proporcaoRomaji(ROMAJI_COM_INGLES) >= 70,
            "romaji com palavra inglesa solta precisa continuar acima do limiar de preservação");
        assertTrue(detector.proporcaoRomaji(KARAOKE_INGLES) < 50,
            "karaokê em inglês precisa ficar bem abaixo do limiar");
    }

    @Test
    void proporcaoRomajiIgnoraTagsEQuebrasDeLinha() {
        // A mesma linha do ED, como vive no arquivo: uma tag de cor por caractere (KFX).
        String comKfx = "{\\3c&HF497D3&\\blur4.5}S{\\3c&HF49ED0&}o{\\3c&HF4A2CF&}nna"
            + "{\\3c&HF5B2CA&} {\\3c&HF6BCC7&}sekai\\N{\\3c&HF5A7CE&}ni nokosareta boku wa";
        assertEquals(detector.proporcaoRomaji(ROMAJI_PURO), detector.proporcaoRomaji(comKfx));
    }

    @Test
    void proporcaoRomajiDegradaSemLancar() {
        assertEquals(0, detector.proporcaoRomaji(null));
        assertEquals(0, detector.proporcaoRomaji("   "));
        assertEquals(0, detector.proporcaoRomaji("{\\pos(10,10)}"));
        // Kana/kanji não pontuam aqui de propósito: são reconhecidos por escrita japonesa.
        assertEquals(0, detector.proporcaoRomaji("そんな世界に"));
    }

    @Test
    void detectaKaraokeCruComTagsDeTiming() {
        assertTrue(detector.eEfeitoKaraoke("{\\k50}Ka {\\k30}ra {\\k42}o {\\k28}ke"));
        assertTrue(detector.eEfeitoKaraoke("{\\kf20}sora {\\ko35}wo"));
    }

    @Test
    void detectaSaidaDeTemplateKaraokePorLetra() {
        // Linha real que escapou da revisão: letra "I" afogada em transformações.
        assertTrue(detector.eEfeitoKaraoke(
            "{\\r\\pos(369,23)\\t(1160,1450,\\frx-50\\fry50\\bord6\\blur5\\3c&HFFE7C7&"
                + "\\fad(50,50))\\t(1450,1450,\\frx0\\fry0\\bord3\\blur0\\3c&HFEA32F&)}I"));
    }
 
     @Test
     void detectaLetreiroFrameAFramePorDensidadeTags() {
         // Linha com \pos e fscx/fscy onde o texto visível é curto em relação às tags.
         String letreiroFrame = "{\\fscx100\\fscy100\\blur0.8\\fs60\\c&H010101&\\pos(452,444)}THE 08TH MS TEAM";
         assertTrue(detector.eEfeitoKaraoke(letreiroFrame));
     }

     @Test
    void naoSinalizaDialogoComum() {
        assertFalse(detector.eEfeitoKaraoke("What are you doing here?!"));
        assertFalse(detector.eEfeitoKaraoke("{\\i1}Bell, cuidado!{\\i0}"));
    }

    @Test
    void naoSinalizaFalaCurtaComPosicionamentoSimples() {
        assertFalse(detector.eEfeitoKaraoke("{\\pos(100,100)}Sai!"));
    }

    @Test
    void naoSinalizaDialogoComEfeitoPontualETextoLongo() {
        // \t presente, mas o texto visível domina a linha: é fala, não karaokê.
        assertFalse(detector.eEfeitoKaraoke(
            "{\\fad(200,200)\\t(0,300,\\fscx110)}Eu nunca vou desistir deste sonho, aconteça o que acontecer!"));
    }

    @Test
    void naoSinalizaNuloOuVazio() {
        assertFalse(detector.eEfeitoKaraoke(null));
        assertFalse(detector.eEfeitoKaraoke("   "));
    }

    @Test
    void temTagKaraokeSoDetectaTagsDeTimingCruas() {
        assertTrue(detector.temTagKaraoke("{\\k50}Ka {\\k30}ra"));
        // Letreiro/título com \t e texto curto (caso real DanMachi: "Prólogo"):
        // eEfeitoKaraoke sinaliza (revisão pula), temTagKaraoke não (tradução traduz).
        String tituloDeTela = "{\\pos(1565.5,822.5)\\c&H000000&\\blur0.7\\t(4188,0,1,\\1a&HFF&)}Prologue";
        assertTrue(detector.eEfeitoKaraoke(tituloDeTela));
        assertFalse(detector.temTagKaraoke(tituloDeTela));
    }

    @Test
    void preservaKaraokeEmJaponesOuRomaji() {
        assertTrue(detector.devePreservarKaraokeOriginal("Song JP", "{\\k30}君 {\\k20}の名は"));
        assertTrue(detector.devePreservarKaraokeOriginal("Romaji", "{\\k30}kimi {\\k20}no na wa"));
        assertFalse(detector.eKaraokeOuMusicaTraduzivel("Song JP", "{\\k30}君 {\\k20}の名は"));
        assertFalse(detector.eKaraokeOuMusicaTraduzivel("Romaji", "{\\k30}kimi {\\k20}no na wa"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o estilo "OP Roma"/"ED Roma" (abreviação de romaji) tem de ser
     * preservado pelo NOME, sem depender da heurística de texto. Caso real (Guilty Crown ep4):
     * o OP "My Dearest" mistura romaji e inglês, a heurística por palavra não reconhecia como
     * romaji, e a linha era TRADUZIDA — virava salada "Eusouumcaminhonissosagueyou" com tags de
     * cor injetadas por token. "OP Roma" não casava com o regex que só aceitava "romaji".
     * <p>INVARIANTES DO DOMÍNIO: só age em linha que já tem indicador de música; diálogo intacto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar a não reconhecer "Roma", o romaji é traduzido.
     */
    @Test
    void preservaRomajiPeloNomeAbreviadoDoEstilo() {
        // Mesmo com inglês misturado (que derruba a heurística de palavra), o NOME salva.
        assertTrue(detector.devePreservarKaraokeOriginal("OP Roma", "{\\k30}boku {\\k20}wa you"));
        assertTrue(detector.devePreservarKaraokeOriginal("ED Roma L1", "{\\k30}kimi {\\k20}dake"));
        assertTrue(detector.devePreservarKaraokeOriginal("Roma", "{\\k30}sekai"));
        assertFalse(detector.eKaraokeOuMusicaTraduzivel("OP Roma", "{\\k30}boku {\\k20}wa you"));
        // Guarda: sem indicador de música, "Roma" no nome NÃO protege diálogo comum.
        assertFalse(detector.devePreservarKaraokeOriginal("Default", "Vamos a Roma amanhã."));
    }

    @Test
    void permiteKaraokeEmInglesOuOutroIdiomaLatino() {
        assertFalse(detector.devePreservarKaraokeOriginal("Song EN", "{\\k30}Fly {\\k20}me to the moon"));
        assertTrue(detector.eKaraokeOuMusicaTraduzivel("Song EN", "{\\k30}Fly {\\k20}me to the moon"));
        assertTrue(detector.eKaraokeOuMusicaTraduzivel("Karaoke", "{\\k30}Bonjour {\\k20}mon amour"));
    }

    @Test
    void preservaRomajiSemMarcadorDeEstiloOuKanji() {
        // Caso real do 86 T1 (ED, estilo "Opening"): romaji com tags leves
        // passava pela densidade e o LLM alucinava uma "tradução" por frame.
        String linhaRomaji = "{\\pos(1143,40)\\bord0\\blur0.5\\clip(0,70,1920,86.5)}fuminijirareru dake no hana";
        assertTrue(detector.devePreservarKaraokeOriginal("Opening", linhaRomaji));
        assertFalse(detector.eKaraokeOuMusicaTraduzivel("Opening", linhaRomaji));
        assertTrue(detector.devePreservarKaraokeOriginal("OP", "{\\k30}kimi {\\k20}no na wa"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: romaji que MISTURA palavras em inglês (comum em J-pop, ex.: o OP
     * "My Dearest" do Guilty Crown) deve ser preservado. Antes a detecção era tudo-ou-nada — uma
     * única palavra inglesa a derrubava —, e o romaji vazava para o LLM virando salada
     * "Eusouumcaminhonissosagueyou". Agora é por PROPORÇÃO: maioria de sílabas japonesas basta.
     * <p>INVARIANTES DO DOMÍNIO: só age em linha de música; karaokê em inglês fica abaixo do
     * limiar e continua traduzível.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se voltar ao tudo-ou-nada, o romaji misturado é traduzido.
     */
    @Test
    void preservaRomajiQueMisturaPalavrasEmIngles() {
        assertTrue(detector.devePreservarKaraokeOriginal("Opening",
            "{\\pos(500,40)}sekai no naka de you know"), "5/6 sílabas japonesas: é romaji");
        assertTrue(detector.devePreservarKaraokeOriginal("OP",
            "{\\k30}kimi {\\k20}wa {\\k18}boku {\\k15}no love"), "maioria romaji, 'love' solto");
    }

    @Test
    void naoPreservaLetraOcidentalEmEstiloMusical() {
        // Letra já em PT (dano da era Gemma no 86) e letra em inglês continuam
        // elegíveis para tradução/revisão mesmo em estilo musical.
        assertFalse(detector.devePreservarKaraokeOriginal("Opening",
            "{\\pos(970,40)\\bord0\\blur0.5\\clip(0,37,1920,53.5)}Uma flor floresce apenas para ser esmagada"));
        assertFalse(detector.devePreservarKaraokeOriginal("Ending",
            "{\\pos(100,40)}Levado e disperso pelo vento"));
        assertFalse(detector.devePreservarKaraokeOriginal("Opening",
            "{\\pos(500,40)}You are my reason to fight"));
    }
}
