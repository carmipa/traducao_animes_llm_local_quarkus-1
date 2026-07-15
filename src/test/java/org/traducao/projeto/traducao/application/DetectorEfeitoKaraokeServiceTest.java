package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorEfeitoKaraokeServiceTest {

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

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
