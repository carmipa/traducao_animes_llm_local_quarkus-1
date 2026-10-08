package org.traducao.projeto.analisadorMidia.presentation.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a frase final da 1.1 Análise de Mídia. M1 da auditoria de 08/10/2026:
 * um lote com 0 analisados e 1 falha terminava em "🎉 [SUCESSO] ... FINALIZADA COM SUCESSO!", e a
 * tela vira toda linha "[SUCESSO]" em toast verde.
 *
 * <p>INVARIANTES DO DOMÍNIO: "[SUCESSO]" só sem falha; a contagem de falhas sempre aparece quando
 * existe. O CONTROLE (A1) é o lote limpo: esse continua dizendo sucesso.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: um banner de sucesso com falha reprova mostrando a frase.
 */
class AnaliseMidiaControllerBannerTest {

    @Test
    void loteSoComFalhaNaoDizSucesso() {
        String banner = AnaliseMidiaController.bannerFinal(0, 1);
        assertFalse(banner.contains("[SUCESSO]"), "0 analisados e 1 falha nao e sucesso: " + banner);
        assertTrue(banner.contains("[FALHA]") && banner.contains("1 falha"), banner);
    }

    @Test
    void loteComAlgumaFalhaAvisaEContaAsFalhas() {
        String banner = AnaliseMidiaController.bannerFinal(5, 2);
        assertFalse(banner.contains("[SUCESSO]"), banner);
        assertTrue(banner.contains("5 analisado") && banner.contains("2 falha"), banner);
    }

    @Test
    void controleLoteLimpoContinuaSucesso() {
        assertTrue(AnaliseMidiaController.bannerFinal(3, 0).contains("[SUCESSO]"),
            "CONTROLE: lote sem falha continua sendo sucesso");
    }
}
