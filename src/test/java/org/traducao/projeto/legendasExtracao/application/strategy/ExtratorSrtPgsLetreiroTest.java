package org.traducao.projeto.legendasExtracao.application.strategy;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: E1 da auditoria de 08/10/2026 (reproduzido na aplicação): com a faixa SRT
 * "Signs &amp; Songs" (default + forced) antes da "Full Subtitles", a 1.2 extraía só os 3 letreiros
 * com status de sucesso. O filtro de faixa reduzida existia só na estratégia ASS.
 *
 * <p>INVARIANTES DO DOMÍNIO: faixa reduzida só é escolhida se não houver outra. CONTROLES (A1): só a
 * reduzida existe -> ela volta; nenhuma reduzida -> a regra antiga (default/eng/por) vale intacta.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a faixa errada reprova mostrando o id escolhido.
 */
class ExtratorSrtPgsLetreiroTest {

    private static FaixaLegenda srt(int id, String nome, String idioma, boolean padrao, boolean forcada) {
        return new FaixaLegenda(id, "subtitles", "SubRip/SRT", "S_TEXT/UTF8", idioma, nome, padrao, forcada);
    }

    private static FaixaLegenda pgs(int id, String nome, String idioma, boolean padrao, boolean forcada) {
        return new FaixaLegenda(id, "subtitles", "HDMV PGS", "S_HDMV/PGS", idioma, nome, padrao, forcada);
    }

    @Test
    void srtNaoEscolheAFaixaDeLetreirosDefaultForcada() {
        var escolhida = new ExtratorSrtStrategy().selecionarMelhorFaixa(List.of(
            srt(1, "Signs & Songs", "eng", true, true), srt(2, "Full Subtitles", "eng", false, false)));
        assertEquals(2, escolhida.orElseThrow().id(), "a SRT escolheu a faixa de letreiros");
    }

    @Test
    void pgsNaoEscolheAFaixaDeLetreirosDefault() {
        var escolhida = new ExtratorPgsStrategy().selecionarMelhorFaixa(List.of(
            pgs(3, "Signs", "eng", true, false), pgs(4, "Full", "eng", false, false)));
        assertEquals(4, escolhida.orElseThrow().id(), "a PGS escolheu a faixa de letreiros");
    }

    @Test
    void controleSoAReduzidaExisteEElaVolta() {
        var escolhida = new ExtratorSrtStrategy().selecionarMelhorFaixa(List.of(srt(1, "Signs", "eng", true, true)));
        assertEquals(1, escolhida.orElseThrow().id(), "CONTROLE: sem outra faixa, a reduzida e' melhor que nada");
    }

    @Test
    void controleSemReduzidaARegraAntigaVale() {
        var escolhida = new ExtratorSrtStrategy().selecionarMelhorFaixa(List.of(
            srt(5, "Japanese", "jpn", false, false), srt(6, "English", "eng", false, false)));
        assertEquals(6, escolhida.orElseThrow().id(), "CONTROLE: sem reduzida, vence a eng como antes");
    }
}
