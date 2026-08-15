package org.traducao.projeto.contexto.lore.gundam.zeta;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regressão dos nomes canônicos observados nas legendas reais de Zeta. */
class ContextoGundamZetaLoreTest {

    @Test
    void protegeTermosRecorrentesMedidosNosCinquentaEpisodios() {
        org.traducao.projeto.contexto.domain.ProvedorContexto contexto = org.traducao.projeto.contexto.LoreDeTeste.obra("gundam_zeta");
        Set<String> protegidos = contexto.termosProtegidos();

        Set.of(
            "Mont Blanc", "Dogosse Gier", "Von Braun City", "Green Oasis", "Green Noa",
            "Bosnia", "Sudori", "Haro", "Shinta", "Qum", "Rosammy", "Manack",
            "Operation Apollo", "Operation Maelstrom", "Baund Doc"
        ).forEach(termo -> assertTrue(protegidos.contains(termo), termo));

        assertTrue(contexto.obterPromptSistema().contains("Dogosse Gier"));
        assertFalse(contexto.obterPromptSistema().contains("Dogosse Giar"));
    }

    @Test
    void naoProtegeAliasesCurtosAmbiguos() {
        Set<String> protegidos = org.traducao.projeto.contexto.LoreDeTeste.obra("gundam_zeta").termosProtegidos();

        assertFalse(protegidos.contains("Four"));
        assertFalse(protegidos.contains("Fa"));
        assertFalse(protegidos.contains("Bright"));
    }

    @Test
    void corrigeSomenteVariantesMedidasQuandoOCanonicoExistirNoIngles() {
        var correcoes = org.traducao.projeto.contexto.LoreDeTeste.obra("gundam_zeta").correcoesTerminologia();

        assertTrue("Dogosse Gier".equals(correcoes.get("Dogosse Giar")));
        assertTrue("Rosammy".equals(correcoes.get("Rosamia")));
        assertTrue("Qum".equals(correcoes.get("Quem")));
        assertTrue("Manack".equals(correcoes.get("Mancack")));
        assertTrue("Ramsus".equals(correcoes.get("Ramus")));
        assertTrue("Batch".equals(correcoes.get("Lote")));
        assertTrue("Green Noa".equals(correcoes.get("Verde Noa")));
        assertTrue("Von Braun City".equals(correcoes.get("cidade de Von Braun")));
    }
}
