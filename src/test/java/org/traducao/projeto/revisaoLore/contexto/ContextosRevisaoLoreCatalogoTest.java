package org.traducao.projeto.revisaoLore.contexto;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextosRevisaoLoreCatalogoTest {

    @Test
    void novosContextosPossuemIdsUnicosENomesDeRevisao() {
        List<ProvedorPromptRevisaoLore> provedores = List.of(
            new ContextoRevisaoLoreGundamUnicorn(),
            new ContextoRevisaoLoreGundamZeta(),
            new ContextoRevisaoLoreGundamZZ(),
            new ContextoRevisaoLoreGuiltyCrown()
        );

        Set<String> ids = provedores.stream()
            .map(ProvedorPromptRevisaoLore::getId)
            .collect(Collectors.toSet());

        assertEquals(provedores.size(), ids.size());
        assertTrue(ids.contains("gundam_unicorn"));
        assertTrue(ids.contains("gundam_zeta"));
        assertTrue(ids.contains("gundam_zz"));
        assertTrue(ids.contains("guilty_crown"));
        provedores.forEach(provedor -> assertTrue(provedor.getNomeExibicao().contains("Revisao de Lore")));
    }

    @Test
    void promptUnicornProtegeTermosCriticosDaObra() {
        var unicorn = new ContextoRevisaoLoreGundamUnicorn();
        String prompt = unicorn.obterPromptSistema();

        assertTrue(prompt.contains("Unicorn Gundam"));
        assertTrue(prompt.contains("Full Frontal nao vira"));
        assertTrue(prompt.contains("Laplace's Box"));
        assertTrue(prompt.contains("Nahel Argama"));
        assertFalse(prompt.contains("Black Tri-Stars"));
        assertTrue(prompt.contains("Magallanica"));
        assertTrue(prompt.contains("Garencieres"));
        assertTrue(prompt.contains("Gilboa Sant"));
        assertTrue(prompt.contains("Ra Cailum"));
        assertEquals("Destroy Mode", unicorn.correcoesTerminologia().get("Modo Destruição"));
        assertEquals("Laplace Incident", unicorn.correcoesTerminologia().get("Incidente de Laplace"));
    }

    @Test
    void promptZetaProtegeTermosCriticosDaObra() {
        var zeta = new ContextoRevisaoLoreGundamZeta();
        String prompt = zeta.obterPromptSistema();

        assertTrue(prompt.contains("A.E.U.G."));
        assertTrue(prompt.contains("Hyaku Shiki nao vira"));
        assertTrue(prompt.contains("The O nao vira"));
        assertTrue(prompt.contains("Titans nao vira"));
        assertTrue(prompt.contains("Quattro nao vira"));
        assertTrue(prompt.contains("Audhumla"));
        assertTrue(prompt.contains("Bask Om"));
        assertTrue(prompt.contains("Gryps Conflict"));
        assertTrue(prompt.contains("Mineva Lao Zabi"));
        assertTrue(prompt.contains("Gate of Zedan"));
        assertTrue(prompt.contains("Psycho Gundam"));
        assertTrue(prompt.contains("Dakar Speech"));
        assertEquals("AEUG", zeta.correcoesTerminologia().get("União Anti-Terra"));
        assertEquals("Gryps Conflict", zeta.correcoesTerminologia().get("Conflito de Gryps"));
        assertEquals("Psycho Gundam", zeta.correcoesTerminologia().get("Psico Gundam"));
        assertEquals("Gundam Mk-II", zeta.correcoesTerminologia().get("Gundam Mark II"));
    }

    @Test
    void promptZzProtegeTermosCriticosDaObra() {
        String prompt = new ContextoRevisaoLoreGundamZZ().obterPromptSistema();

        assertTrue(prompt.contains("Double Zeta nao vira"));
        assertTrue(prompt.contains("Quin Mantha nao vira"));
        assertTrue(prompt.contains("Lady Haman"));
        assertTrue(prompt.contains("Blue Corps"));
        assertTrue(prompt.contains("Axis nao vira"));
        assertEquals("Axis", new ContextoRevisaoLoreGundamZZ().correcoesTerminologia().get("Eixo"));
    }

    @Test
    void contextosUcEMacrossClasseRevisaoExcepcionais() {
        var origin = new ContextoRevisaoLoreGundamOrigin();
        var zeta = new ContextoRevisaoLoreGundamZeta();
        var zz = new ContextoRevisaoLoreGundamZZ();
        var unicorn = new ContextoRevisaoLoreGundamUnicorn();
        var nt = new ContextoRevisaoLoreGundamNT();
        var m2 = new ContextoRevisaoLoreMacross2();
        var dyrl = new ContextoRevisaoLoreMacrossFilme1();

        assertEquals("gundam_origin", origin.getId());
        assertTrue(origin.obterPromptSistema().contains("Casval Rem Deikun"));
        assertTrue(origin.obterPromptSistema().contains("Black Tri-Stars"));
        assertTrue(origin.obterPromptSistema().contains("Don Teabolo Mass"));
        assertTrue(origin.obterPromptSistema().contains("Battle of Loum"));
        assertEquals("White Base", origin.correcoesTerminologia().get("Base Branca"));
        assertEquals("Black Tri-Stars", origin.correcoesTerminologia().get("Triângulo Negro"));
        assertEquals("Edouard Mass", origin.correcoesTerminologia().get("Eduardo Mass"));
        assertEquals("One Year War", origin.correcoesTerminologia().get("Guerra de Um Ano"));

        assertEquals("Hyaku Shiki", zeta.correcoesTerminologia().get("Cem Estilos"));
        assertEquals("Titans", zeta.correcoesTerminologia().get("Titãs"));

        assertEquals("Quin Mantha", zz.correcoesTerminologia().get("Rainha Mansa"));
        assertEquals("Ple", zz.correcoesTerminologia().get("Plê"));

        assertEquals("Sleeves", unicorn.correcoesTerminologia().get("Mangas"));
        assertEquals("Laplace's Box", unicorn.correcoesTerminologia().get("Caixa de Laplace"));
        assertTrue(unicorn.obterPromptSistema().contains("Full Frontal nao vira"));

        assertEquals("Phenex", nt.correcoesTerminologia().get("Fênix"));
        assertEquals("Miracle Children", nt.correcoesTerminologia().get("Crianças Milagrosas"));
        assertTrue(nt.obterPromptSistema().contains("Operation Phoenix Hunt"));

        assertEquals("Emulator", m2.correcoesTerminologia().get("Emulador"));
        assertEquals("Minmay Attack", m2.correcoesTerminologia().get("Ataque Minmay"));

        assertEquals("Protoculture", dyrl.correcoesTerminologia().get("Protocultura"));
        assertTrue(dyrl.obterPromptSistema().contains("Meltrandi"));
        assertEquals("Protoculture", new ContextoRevisaoLoreMacrossDYRL().correcoesTerminologia().get("Protocultura"));
    }

    @Test
    void contextosMacrossDeltaRevisaoExcepcionais() {
        var tv = new ContextoRevisaoLoreMacrossDelta();
        var f1 = new ContextoRevisaoLoreMacrossDeltaFilme1();
        var f2 = new ContextoRevisaoLoreMacrossDeltaFilme2();

        assertEquals("macross_delta", tv.getId());
        assertEquals("macross_delta_filme1", f1.getId());
        assertEquals("macross_delta_filme2", f2.getId());

        assertTrue(tv.obterPromptSistema().contains("Aerial Knights"));
        assertTrue(f1.obterPromptSistema().contains("Passionate Walküre"));
        assertTrue(f2.obterPromptSistema().contains("Yami_Q_Ray"));
        assertTrue(f2.obterPromptSistema().contains("Heimdall"));
        assertFalse(f1.obterPromptSistema().contains("Yami_Q_Ray"));

        assertEquals("Walküre", tv.correcoesTerminologia().get("Walkure"));
        assertEquals("Var Syndrome", tv.correcoesTerminologia().get("Síndrome Var"));
        assertEquals("Delta Flight", f1.correcoesTerminologia().get("Esquadrão Delta"));
        assertEquals("Fold Waves", f2.correcoesTerminologia().get("Ondas Fold"));
    }

    @Test
    void promptGuiltyCrownProtegeTermosCriticosDaObra() {
        var ctx = new ContextoRevisaoLoreGuiltyCrown();
        String prompt = ctx.obterPromptSistema();

        assertTrue(prompt.contains("Guilty Crown"));
        assertTrue(prompt.contains("Void Genome"));
        assertTrue(prompt.contains("Funeral Parlor"));
        assertTrue(prompt.contains("Apocalypse Virus"));
        assertTrue(prompt.contains("Shuichiro Keido"));
        assertTrue(prompt.contains("Shibungi"));
        assertTrue(prompt.contains("Argo Tsukishima"));
        assertTrue(prompt.contains("Oogumo"));
        assertTrue(prompt.contains("Crow"));
        assertTrue(prompt.contains("Second Hand"));
        assertTrue(prompt.contains("Roppongi Fort"));
        assertEquals("Funeral Parlor", ctx.correcoesTerminologia().get("Funerária"));
        assertEquals("Void Genome", ctx.correcoesTerminologia().get("Genoma do Vazio"));
        assertEquals("Undertaker", ctx.correcoesTerminologia().get("Coveiro"));
        assertEquals("Genomic Resonance", ctx.correcoesTerminologia().get("Ressonância Genômica"));
        assertEquals("Second Hand", ctx.correcoesTerminologia().get("Segunda Mão"));
        assertEquals("Endlave", ctx.correcoesTerminologia().get("Endslave"));
        assertEquals("Anti Bodies", ctx.correcoesTerminologia().get("Anticorpos"));
    }
}
