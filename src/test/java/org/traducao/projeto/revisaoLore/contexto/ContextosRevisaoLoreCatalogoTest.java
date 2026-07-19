package org.traducao.projeto.revisaoLore.contexto;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        String prompt = new ContextoRevisaoLoreGundamUnicorn().obterPromptSistema();

        assertTrue(prompt.contains("Unicorn Gundam"));
        assertTrue(prompt.contains("Full Frontal nao vira"));
        assertTrue(prompt.contains("Laplace's Box"));
        assertTrue(prompt.contains("Nahel Argama"));
    }

    @Test
    void promptZetaProtegeTermosCriticosDaObra() {
        String prompt = new ContextoRevisaoLoreGundamZeta().obterPromptSistema();

        assertTrue(prompt.contains("A.E.U.G."));
        assertTrue(prompt.contains("Hyaku Shiki nao vira"));
        assertTrue(prompt.contains("The O nao vira"));
        assertTrue(prompt.contains("Titans nao vira"));
        assertTrue(prompt.contains("Quattro nao vira"));
        assertTrue(prompt.contains("Audhumla"));
        assertTrue(prompt.contains("Bask Om"));
        assertTrue(prompt.contains("Gryps Conflict"));
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
    void contextosMacrossRevisaoExistemComMapa() {
        var delta = new ContextoRevisaoLoreMacrossDelta();
        assertEquals("macross_delta", delta.getId());
        assertTrue(delta.getNomeExibicao().contains("Revisao de Lore"));
        assertEquals("Valkyrie", delta.correcoesTerminologia().get("Valquíria"));
        assertTrue(delta.obterPromptSistema().contains("Walküre")
            || delta.obterPromptSistema().contains("Valkyrie"));
    }

    @Test
    void promptGuiltyCrownProtegeTermosCriticosDaObra() {
        String prompt = new ContextoRevisaoLoreGuiltyCrown().obterPromptSistema();

        assertTrue(prompt.contains("Guilty Crown"));
        assertTrue(prompt.contains("Void Genome"));
        assertTrue(prompt.contains("Funeral Parlor"));
        assertTrue(prompt.contains("Apocalypse Virus"));
    }
}
