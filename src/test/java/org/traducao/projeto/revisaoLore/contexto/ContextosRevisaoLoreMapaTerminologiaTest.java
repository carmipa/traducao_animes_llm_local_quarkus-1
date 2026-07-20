package org.traducao.projeto.revisaoLore.contexto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * PROPÓSITO DE NEGÓCIO: garante que as obras que estavam com o mapa de terminologia VAZIO
 * (corretor determinístico de lore inerte) agora têm mapa real — as 4 Gundam UC e as 3 DanMachi.
 * Sem isso, a Revisão de Lore só corrigia via LLM (ou nada, no modo PT-only determinístico).
 *
 * <p>INVARIANTES DO DOMÍNIO: cada uma dessas obras devolve um mapa NÃO vazio; as UC herdam o
 * núcleo UC (Traje Móvel→Mobile Suit) e seus extras próprios; DanMachi fixa Família→Familia.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio ou termo canônico errado reprova.
 */
class ContextosRevisaoLoreMapaTerminologiaTest {

    @Test
    @DisplayName("Gundam UC (0080/0083/08th/CCA) têm o mapa UC + extras próprios")
    void gundamUcTemMapa() {
        assertFalse(new ContextoRevisaoLoreGundam0080().correcoesTerminologia().isEmpty());
        assertFalse(new ContextoRevisaoLoreGundam0083().correcoesTerminologia().isEmpty());

        Map<String, String> m08th = new ContextoRevisaoLoreGundam08thMSTeam().correcoesTerminologia();
        assertEquals("Mobile Suit", m08th.get("Traje Móvel"), "08th deve herdar o núcleo UC");
        assertEquals("Gouf Custom", m08th.get("Gouf Personalizado"), "08th deve ter o extra Gouf Custom");

        Map<String, String> cca = new ContextoRevisaoLoreGundamCCA().correcoesTerminologia();
        assertEquals("Mobile Suit", cca.get("Traje Móvel"), "CCA deve herdar o núcleo UC");
        assertEquals("Axis", cca.get("Eixo"), "CCA deve ter o extra Eixo→Axis");
    }

    @Test
    @DisplayName("DanMachi (base/S4/S5) fixam Família→Familia")
    void danMachiTemMapa() {
        assertEquals("Familia", new ContextoRevisaoLoreDanMachi().correcoesTerminologia().get("Família"));
        assertEquals("Familia", new ContextoRevisaoLoreDanMachiS4().correcoesTerminologia().get("Família"));
        assertEquals("Familia", new ContextoRevisaoLoreDanMachiS5().correcoesTerminologia().get("Família"));
    }
}
