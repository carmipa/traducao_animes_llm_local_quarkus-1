package org.traducao.projeto.lore.revisao;

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
        var rev0080 = org.traducao.projeto.lore.LoreDeTeste.revisao("gundam_0080");
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

        var rev0083 = org.traducao.projeto.lore.LoreDeTeste.revisao("gundam_0083");
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

        var rev08th = org.traducao.projeto.lore.LoreDeTeste.revisao("gundam_08ms");
        Map<String, String> m08th = rev08th.correcoesTerminologia();
        assertEquals("Mobile Suit", m08th.get("Traje Móvel"), "08th deve herdar o núcleo UC");
        assertEquals("Gouf Custom", m08th.get("Gouf Personalizado"), "08th deve ter o extra Gouf Custom");
        assertEquals("Apsalus", m08th.get("Absalão"));
        assertEquals("Apsalus", m08th.get("Absaras"));
        assertEquals("Miller's Report", m08th.get("Relatório Miller"));
        assertEquals("08th MS Team", m08th.get("8o Time MS"));
        assertEquals("Gundam Ground Type", m08th.get("Gundam Tipo Terrestre"));
        assertEquals("Hovertruck", m08th.get("Caminhão Hover"));
        assertEquals("Kojima Battalion", m08th.get("Batalhão Kojima"));
        String prompt08th = rev08th.obterPromptSistema();
        assertTrue(prompt08th.contains("Norris Packard"));
        assertTrue(prompt08th.contains("Miller's Report"));
        assertTrue(prompt08th.contains("Jidan Nickard"), "roster ampliado: mecanico/tenente da Fed");
        assertTrue(prompt08th.contains("Odessa"), "locais ampliado: Odessa");
        assertTrue(prompt08th.contains("Sakhalin") || prompt08th.contains("Apsaras"),
            "aliases da fansub EN devem constar no prompt");

        Map<String, String> cca = org.traducao.projeto.lore.LoreDeTeste.revisao("gundam_cca").correcoesTerminologia();
        assertEquals("Mobile Suit", cca.get("Traje Móvel"), "CCA deve herdar o núcleo UC");
        assertEquals("Axis", cca.get("Eixo"), "CCA deve ter o extra Eixo→Axis");
        assertEquals("Nu Gundam", cca.get("Novo Gundam"));
        assertEquals("Psycho-Frame", cca.get("Moldura Psíquica"));
    }

    @Test
    @DisplayName("DanMachi — núcleo enriquecido (Familia/Falna/Dungeon/Excelia/Valis)")
    void danMachiNucleoEnriquecido() {
        // Antes isto lia CorrecoesTerminologiaDanMachiRevisao.mapa(), o núcleo NU. Aquela classe
        // não existe mais (a lore virou arquivo em 2026-08-15) e, ao contrário dos outros
        // núcleos, este NUNCA foi usado puro: toda obra DanMachi acrescenta extras. Não há obra
        // cujo mapa SEJA o núcleo. A substituição continua provando o que o teste diz provar
        // porque as asserções abaixo são todas de entradas DO NÚCLEO, e o mapa da obra é
        // superconjunto dele — o que mudou é que agora um extra a mais não passa despercebido
        // aqui, ele simplesmente não é afirmado.
        Map<String, String> nucleo = org.traducao.projeto.lore.LoreDeTeste.terminologiaRevisao("danmachi");
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
        assertEquals("Familia", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi").correcoesTerminologia().get("Família"));
        assertEquals("Familia", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s4").correcoesTerminologia().get("Família"));
        assertEquals("Familia", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s5").correcoesTerminologia().get("Família"));

        assertEquals("Liliruca Arde", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s1").correcoesTerminologia().get("Lilisuka"));
        assertEquals("Bell Cranel", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s1").correcoesTerminologia().get("Sino Cranel"));

        assertEquals("Liliruca Arde", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s2").correcoesTerminologia().get("Lilisuka"));
        assertEquals("Haruhime Sanjouno", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s2").correcoesTerminologia().get("Haruhime Sanjono"));

        assertEquals("Liliruca Arde", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s3").correcoesTerminologia().get("Liriruca"));
        assertEquals("Xenos", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s4").correcoesTerminologia().get("Alienígenas"));
        assertEquals("Juggernaut", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s4").correcoesTerminologia().get("Jugernaut"));

        assertEquals("Freya Familia", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s5").correcoesTerminologia().get("Família Freya"));
        assertEquals("Hostess of Fertility", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s5").correcoesTerminologia().get("Anfitriã da Fertilidade"));

        assertEquals("Aiz Wallenstein", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_so").correcoesTerminologia().get("Ais Wallenstein"));
        assertEquals("Sword Princess", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_so").correcoesTerminologia().get("Princesa Espadachim"));

        assertEquals("Liliruca Arde", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_movie").correcoesTerminologia().get("Lilisuka"));
        assertEquals("Liliruca Arde", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_movie").correcoesTerminologia().get("Liriruca"));
    }

    @Test
    @DisplayName("86 e Macross base enriquecidos")
    void oitoSeisEMacrossEnriquecidos() {
        Map<String, String> m86 = org.traducao.projeto.lore.LoreDeTeste.revisao("eight_six").correcoesTerminologia();
        assertEquals("Legion", m86.get("Legião"));
        assertEquals("Shin", m86.get("Canela"));
        assertEquals("Para-RAID", m86.get("Para RAID"));
        assertEquals("Juggernaut", m86.get("Jugernaut"));

        Map<String, String> macross = org.traducao.projeto.lore.LoreDeTeste.terminologiaRevisao("macross_anime");
        assertEquals("Valkyrie", macross.get("Valquíria"));
        assertEquals("Protoculture", macross.get("Protocultura"));
        assertEquals("Minmay Attack", macross.get("Ataque Minmay"));
        assertEquals("Meltrandi", macross.get("Meltrandy"));
        assertTrue(macross.size() >= 8);
        assertFalse(org.traducao.projeto.lore.LoreDeTeste.revisao("macross_frontier").correcoesTerminologia().isEmpty());
    }

    @Test
    @DisplayName("Break Blade — mapa un-sorcerer/Delphine/Krisna/Valkyrie Squadron")
    void breakBladeMapaEnriquecido() {
        Map<String, String> nucleo = org.traducao.projeto.lore.LoreDeTeste.terminologiaRevisao("break_blade_1");
        assertEquals("un-sorcerer", nucleo.get("Não-feiticeiro"));
        assertEquals("Delphine", nucleo.get("Delfine"));
        assertEquals("Delphine", nucleo.get("Delphing"));
        assertEquals("Kingdom of Krisna", nucleo.get("Reino de Krisna"));
        assertEquals("Athens Commonwealth", nucleo.get("Comunidade de Atenas"));
        assertEquals("Valkyrie Squadron", nucleo.get("Esquadrão Valquíria"));
        assertEquals("Heavy Knight", nucleo.get("Cavaleiro Pesado"));
        assertEquals("Hykelion", nucleo.get("Hykélion"));
        assertEquals("Quartz", nucleo.get("Quartzo"));

        assertEquals("un-sorcerer",
            org.traducao.projeto.lore.LoreDeTeste.revisao("break_blade_1").correcoesTerminologia().get("Sem-magia"));
        assertEquals("Broken Blade",
            org.traducao.projeto.lore.LoreDeTeste.revisao("break_blade_6").correcoesTerminologia().get("Lâmina Quebrada"));
    }

    @Test
    @DisplayName("ids DanMachi revisao cobrem S1–S5, SO e filme")
    void idsDanMachiRevisaoCompletos() {
        assertEquals("danmachi_s1", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s1").getId());
        assertEquals("danmachi_s2", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s2").getId());
        assertEquals("danmachi_s3", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s3").getId());
        assertEquals("danmachi_s4", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s4").getId());
        assertEquals("danmachi_s5", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_s5").getId());
        assertEquals("danmachi_so", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_so").getId());
        assertEquals("danmachi_movie", org.traducao.projeto.lore.LoreDeTeste.revisao("danmachi_movie").getId());
    }
}
