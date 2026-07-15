package org.traducao.projeto.analisadorMidia.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o dado VITAL da análise: a classificação do tipo de legenda (codec →
 * tipo) e o veredicto de traduzibilidade (texto = traduzível; bitmap = OCR;
 * nenhuma = RAW/hardsub). Decide se um episódio segue no pipeline de tradução.
 * A lógica vive em {@link ClassificadorLegendaService} (extraído do use case).
 */
class AnalisarMidiaClassificacaoTest {

    private final ClassificadorLegendaService classificador = new ClassificadorLegendaService();

    @Test
    void classificaOsCodecsDeLegendaConhecidos() {
        assertEquals("ASS", tipoCurto("ass", "ASS"));
        assertEquals("SSA", tipoCurto("ssa", "SSA"));
        assertEquals("SRT", tipoCurto("subrip", "SUBRIP"));
        assertEquals("PGS", tipoCurto("hdmv_pgs_subtitle", "HDMV_PGS_SUBTITLE"));
        assertEquals("VOBSUB", tipoCurto("dvd_subtitle", "DVD_SUBTITLE"));
        assertEquals("DVB", tipoCurto("dvb_subtitle", "DVB_SUBTITLE"));
        assertEquals("WEBVTT", tipoCurto("webvtt", "WEBVTT"));
        assertEquals("MOV_TEXT", tipoCurto("mov_text", "MOV_TEXT"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: PGS e VobSub são legendas BITMAP (imagem), não
     * hardsub — a terminologia importa para o operador não confundir uma faixa
     * bitmap embutida (extraível via OCR) com conteúdo queimado no vídeo.
     * INVARIANTES DO DOMÍNIO: o rótulo de PGS/VobSub cita "Bitmap" e NÃO "Hardsub".
     * COMPORTAMENTO EM CASO DE FALHA: regressão de terminologia falha aqui.
     */
    @Test
    void pgsEVobSubSaoRotuladosComoBitmapNaoHardsub() {
        String[] pgs = classificador.classificar("hdmv_pgs_subtitle", "HDMV_PGS_SUBTITLE");
        assertTrue(pgs[0].contains("Bitmap"), pgs[0]);
        assertFalse(pgs[0].toLowerCase().contains("hardsub"), "PGS não deve ser rotulado como Hardsub: " + pgs[0]);

        String[] vobsub = classificador.classificar("dvd_subtitle", "DVD_SUBTITLE");
        assertTrue(vobsub[0].contains("Bitmap"), vobsub[0]);
        assertFalse(vobsub[0].toLowerCase().contains("hardsub"), "VobSub não deve ser rotulado como Hardsub: " + vobsub[0]);
    }

    @Test
    void codecDesconhecidoCaiEmDesconhecido() {
        assertEquals("DESCONHECIDO", tipoCurto("algo_estranho", "XYZ"));
    }

    @Test
    void veredictoDeTextoEhTraduzivel() {
        assertTrue(classificador.verdictTraducao(List.of(leg("ASS"))).startsWith("SIM"));
        assertTrue(classificador.verdictTraducao(List.of(leg("SRT"))).startsWith("SIM"));
    }

    @Test
    void veredictoDeBitmapNaoEhTraduzivel() {
        assertTrue(classificador.verdictTraducao(List.of(leg("PGS"))).startsWith("NAO"));
        assertTrue(classificador.verdictTraducao(List.of(leg("VOBSUB"))).startsWith("NAO"));
    }

    @Test
    void veredictoSemLegendaEhNaoAplicavel() {
        assertTrue(classificador.verdictTraducao(List.of()).startsWith("N/A"));
    }

    @Test
    void faixaDeTextoTemPrioridadeSobreBitmap() {
        // Um arquivo com PGS + ASS ainda é traduzível pela faixa de texto.
        String veredicto = classificador.verdictTraducao(List.of(leg("PGS"), leg("ASS")));
        assertTrue(veredicto.startsWith("SIM"), veredicto);
    }

    private String tipoCurto(String codecId, String formato) {
        return classificador.classificar(codecId, formato)[1];
    }

    private static LegendaInfo leg(String tipoCurto) {
        return new LegendaInfo(0, 0, "eng", "ASS", "ass", "(Sem titulo)",
            null, tipoCurto, null, false, false, false, false, false, false, null, null);
    }
}
