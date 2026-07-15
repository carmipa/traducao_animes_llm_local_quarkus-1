package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassificadorLetraKaraokeServiceTest {

    private ClassificadorLetraKaraokeService classificador; // instância real, sem CDI

    @BeforeEach
    void setUp() {
        classificador = new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());
    }

    @Test
    void dialogoComumNaoEMusica() {
        assertEquals(ClasseLinhaKaraoke.FORA_DE_MUSICA,
            classificador.classificar("Default", "Ela disse que voltaria amanhã cedo."));
    }

    @Test
    void silabaKfxCruaEPreservadaComoEfeito() {
        assertEquals(ClasseLinhaKaraoke.EFEITO_KFX,
            classificador.classificar("OP - Romaji", "{\\k25}ki{\\k30}mi{\\k20}no"));
    }

    @Test
    void estiloRomajiSemprePreservaMesmoComInglesNoMeio() {
        // O caso central do módulo: cantor japonês mistura inglês na letra.
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("OP - Romaji", "kimi no heart ni fly away"));
    }

    @Test
    void estiloAbreviadoRomPreservaOriginal() {
        // Estilo real do 86: "ED-ROM".
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("ED-ROM", "boku wa itsumo doko ka he"));
    }

    @Test
    void escritaJaponesaSemprePreservada() {
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Song", "君のハートに届け"));
    }

    @Test
    void estiloGenericoComParticulasRomajiPreservaMesmoComIngles() {
        // Sem rótulo no estilo, as partículas japonesas decidem: "kimi" + "ni"
        // vencem o "heart"/"fly"/"away" misturados.
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Opening", "kimi no heart ni fly away"));
    }

    @Test
    void estiloInglesTraduz() {
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("OP - English", "Even if the world ends tomorrow"));
    }

    @Test
    void estiloGenericoComGramaticaInglesaTraduz() {
        assertEquals(ClasseLinhaKaraoke.TRADUZIVEL_INGLES,
            classificador.classificar("Song", "You are the light of my world"));
    }

    @Test
    void refraoOriginalEmInglesSilabavelEPreservadoNoDesempate() {
        // "One more time, one more chance": inglês cantado no original, sem
        // gramática inequívoca — o desempate pela fração silábica preserva.
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Insert Song", "One more time, one more chance"));
    }

    @Test
    void letraJaEmPortuguesNaoRetraduz() {
        assertEquals(ClasseLinhaKaraoke.JA_PORTUGUES,
            classificador.classificar("OP - English", "Mesmo que o mundo acabe amanhã, não vou te deixar"));
    }

    @Test
    void letraPortuguesaSemAcentosReconhecidaPorPalavrasFortes() {
        assertEquals(ClasseLinhaKaraoke.JA_PORTUGUES,
            classificador.classificar("Song", "voce sabe que eu nunca vou desistir"));
    }

    @Test
    void romajiPuroEmEstiloGenericoEPreservado() {
        // Caso real do 86 T1 (ED no estilo "Opening"): a partícula "dake" e a
        // fração silábica preservam a linha sem depender do nome do estilo.
        assertEquals(ClasseLinhaKaraoke.ORIGINAL_JAPONES,
            classificador.classificar("Opening", "fuminijirareru dake no hana"));
    }

    @Test
    void linhaSoDeTagsEmEstiloDeMusicaEEfeito() {
        assertEquals(ClasseLinhaKaraoke.EFEITO_KFX,
            classificador.classificar("Song", "{\\pos(100,200)\\fad(300,300)}"));
    }
}
