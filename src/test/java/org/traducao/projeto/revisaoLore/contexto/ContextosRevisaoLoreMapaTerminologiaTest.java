package org.traducao.projeto.revisaoLore.contexto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que os mapas de terminologia da Revisao de Lore
 * (Opção 7 / PT-only) estão enriquecidos — núcleo por franquia + extras por obra/temporada.
 *
 * <p>INVARIANTES DO DOMÍNIO: mapas não vazios; Família→Familia em todo DanMachi;
 * UC herda Traje Móvel→Mobile Suit; extras próprios (0080/0083/08th/CCA/86/Macross).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio ou canônico errado reprova.
 */
class ContextosRevisaoLoreMapaTerminologiaTest {

    @Test
    @DisplayName("Gundam UC (0080/0083/08th/CCA) têm o mapa UC + extras próprios")
    void gundamUcTemMapa() {
        var rev0080 = new ContextoRevisaoLoreGundam0080();
        Map<String, String> m0080 = rev0080.correcoesTerminologia();
        assertEquals("Mobile Suit", m0080.get("Traje Móvel"));
        assertEquals("Gundam Alex", m0080.get("Gundam Alexandre"));
        assertEquals("Kampfer", m0080.get("Kämpfer"));
        assertEquals("War in the Pocket", m0080.get("Guerra no Bolso"));
        assertEquals("Cyclops Team", m0080.get("Equipe Cyclops"));
        assertEquals("Republic of Riah", m0080.get("República de Riah"));
        assertEquals("Antarctic Base", m0080.get("Base Antártica"));
        assertTrue(rev0080.obterPromptSistema().contains("Colonel Killing"));
        assertTrue(rev0080.obterPromptSistema().contains("Libot colony"));
        assertTrue(rev0080.obterPromptSistema().contains("GM Cold Districts Type"));

        var rev0083 = new ContextoRevisaoLoreGundam0083();
        Map<String, String> m0083 = rev0083.correcoesTerminologia();
        assertEquals("Mobile Suit", m0083.get("Traje Móvel"));
        assertEquals("Delaz Fleet", m0083.get("Frota Delaz"));
        assertEquals("Dendrobium", m0083.get("Dendróbio"));
        assertEquals("Neue Ziel", m0083.get("Novo Alvo"));
        assertEquals("Titans", m0083.get("Titãs"));
        assertEquals("Operation Stardust", m0083.get("Operação Stardust"));
        assertEquals("Colony Drop", m0083.get("Queda de Colônia"));
        assertEquals("Physalis", m0083.get("Físalis"));
        assertTrue(rev0083.obterPromptSistema().contains("Nightmare of Solomon"));
        assertTrue(rev0083.obterPromptSistema().contains("La Vie en Rose"));

        Map<String, String> m08th = new ContextoRevisaoLoreGundam08thMSTeam().correcoesTerminologia();
        assertEquals("Mobile Suit", m08th.get("Traje Móvel"), "08th deve herdar o núcleo UC");
        assertEquals("Gouf Custom", m08th.get("Gouf Personalizado"), "08th deve ter o extra Gouf Custom");
        assertEquals("Apsalus", m08th.get("Absalão"));
        assertEquals("Miller's Report", m08th.get("Relatório Miller"));
        assertEquals("08th MS Team", m08th.get("8o Time MS"));
        assertEquals("Gundam Ground Type", m08th.get("Gundam Tipo Terrestre"));
        assertEquals("Hovertruck", m08th.get("Caminhão Hover"));
        assertTrue(new ContextoRevisaoLoreGundam08thMSTeam().obterPromptSistema().contains("Norris Packard"));
        assertTrue(new ContextoRevisaoLoreGundam08thMSTeam().obterPromptSistema().contains("Miller's Report"));

        Map<String, String> cca = new ContextoRevisaoLoreGundamCCA().correcoesTerminologia();
        assertEquals("Mobile Suit", cca.get("Traje Móvel"), "CCA deve herdar o núcleo UC");
        assertEquals("Axis", cca.get("Eixo"), "CCA deve ter o extra Eixo→Axis");
        assertEquals("Nu Gundam", cca.get("Novo Gundam"));
        assertEquals("Psycho-Frame", cca.get("Moldura Psíquica"));
    }

    @Test
    @DisplayName("DanMachi — núcleo enriquecido (Familia/Falna/Dungeon/Excelia/Valis)")
    void danMachiNucleoEnriquecido() {
        Map<String, String> nucleo = CorrecoesTerminologiaDanMachiRevisao.mapa();
        assertEquals("Familia", nucleo.get("Família"));
        assertEquals("Falna", nucleo.get("Fálna"));
        assertEquals("Dungeon", nucleo.get("Masmorra"));
        assertEquals("Excelia", nucleo.get("Excélia"));
        assertEquals("Valis", nucleo.get("Vális"));
        assertEquals("Magic Stone", nucleo.get("Pedra Mágica"));
        assertEquals("War Game", nucleo.get("Jogo de Guerra"));
    }

    @Test
    @DisplayName("DanMachi — todas as temporadas/filme/SO com mapa + extras")
    void danMachiTodasTemporadasTemMapa() {
        assertEquals("Familia", new ContextoRevisaoLoreDanMachi().correcoesTerminologia().get("Família"));
        assertEquals("Familia", new ContextoRevisaoLoreDanMachiS4().correcoesTerminologia().get("Família"));
        assertEquals("Familia", new ContextoRevisaoLoreDanMachiS5().correcoesTerminologia().get("Família"));

        assertEquals("Liliruca Arde", new ContextoRevisaoLoreDanMachiS1().correcoesTerminologia().get("Lilisuka"));
        assertEquals("Bell Cranel", new ContextoRevisaoLoreDanMachiS1().correcoesTerminologia().get("Sino Cranel"));

        assertEquals("Liliruca Arde", new ContextoRevisaoLoreDanMachiS2().correcoesTerminologia().get("Lilisuka"));
        assertEquals("Haruhime Sanjouno", new ContextoRevisaoLoreDanMachiS2().correcoesTerminologia().get("Haruhime Sanjono"));

        assertEquals("Liliruca Arde", new ContextoRevisaoLoreDanMachiS3().correcoesTerminologia().get("Liriruca"));
        assertEquals("Xenos", new ContextoRevisaoLoreDanMachiS4().correcoesTerminologia().get("Alienígenas"));
        assertEquals("Juggernaut", new ContextoRevisaoLoreDanMachiS4().correcoesTerminologia().get("Jugernaut"));

        assertEquals("Freya Familia", new ContextoRevisaoLoreDanMachiS5().correcoesTerminologia().get("Família Freya"));
        assertEquals("Hostess of Fertility", new ContextoRevisaoLoreDanMachiS5().correcoesTerminologia().get("Anfitriã da Fertilidade"));

        assertEquals("Aiz Wallenstein", new ContextoRevisaoLoreDanMachiSwordOratoria().correcoesTerminologia().get("Ais Wallenstein"));
        assertEquals("Sword Princess", new ContextoRevisaoLoreDanMachiSwordOratoria().correcoesTerminologia().get("Princesa Espadachim"));

        assertEquals("Liliruca Arde", new ContextoRevisaoLoreDanMachiOrion().correcoesTerminologia().get("Lilisuka"));
        assertEquals("Liliruca Arde", new ContextoRevisaoLoreDanMachiOrion().correcoesTerminologia().get("Liriruca"));
    }

    @Test
    @DisplayName("86 e Macross base enriquecidos")
    void oitoSeisEMacrossEnriquecidos() {
        Map<String, String> m86 = new ContextoRevisaoLore86().correcoesTerminologia();
        assertEquals("Legion", m86.get("Legião"));
        assertEquals("Shin", m86.get("Canela"));
        assertEquals("Para-RAID", m86.get("Para RAID"));
        assertEquals("Juggernaut", m86.get("Jugernaut"));

        Map<String, String> macross = CorrecoesTerminologiaMacrossRevisao.mapa();
        assertEquals("Valkyrie", macross.get("Valquíria"));
        assertEquals("Protoculture", macross.get("Protocultura"));
        assertEquals("Minmay Attack", macross.get("Ataque Minmay"));
        assertEquals("Meltrandi", macross.get("Meltrandy"));
        assertTrue(macross.size() >= 8);
        assertFalse(new ContextoRevisaoLoreMacrossFrontier().correcoesTerminologia().isEmpty());
    }

    @Test
    @DisplayName("ids DanMachi revisao cobrem S1–S5, SO e filme")
    void idsDanMachiRevisaoCompletos() {
        assertEquals("danmachi_s1", new ContextoRevisaoLoreDanMachiS1().getId());
        assertEquals("danmachi_s2", new ContextoRevisaoLoreDanMachiS2().getId());
        assertEquals("danmachi_s3", new ContextoRevisaoLoreDanMachiS3().getId());
        assertEquals("danmachi_s4", new ContextoRevisaoLoreDanMachiS4().getId());
        assertEquals("danmachi_s5", new ContextoRevisaoLoreDanMachiS5().getId());
        assertEquals("danmachi_so", new ContextoRevisaoLoreDanMachiSwordOratoria().getId());
        assertEquals("danmachi_movie", new ContextoRevisaoLoreDanMachiOrion().getId());
    }
}
