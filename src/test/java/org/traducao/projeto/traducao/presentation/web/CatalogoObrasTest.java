package org.traducao.projeto.traducao.presentation.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o agrupamento por franquia, a ordem cronológica e a
 * padronização Gundam do seletor de obras, cobrindo os casos "fujões" que a ordenação
 * alfabética separava (Reconguista, The Super Dimension Fortress Macross).
 *
 * <p>INVARIANTES DO DOMÍNIO: grupo por palavra-chave; título solo tem grupo vazio; ordem
 * de lançamento dentro do grupo; padronização só de exibição.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de grupo, ordem ou rótulo reprova.
 */
class CatalogoObrasTest {

    private final CatalogoObras catalogo = new CatalogoObras();

    @Test
    @DisplayName("grupo: Gundam e Macross capturam ate os fujoes da ordem alfabetica")
    void grupoCapturaFujoes() {
        assertEquals("Gundam", catalogo.grupo("Gundam: Reconguista in G"));
        assertEquals("Gundam", catalogo.grupo("Mobile Suit Zeta Gundam"));
        assertEquals("Macross", catalogo.grupo("The Super Dimension Fortress Macross"));
        assertEquals("Macross", catalogo.grupo("Macross Plus"));
        assertEquals("DanMachi", catalogo.grupo("DanMachi (Season 1)"));
        assertEquals("Evangelion", catalogo.grupo("Evangelion (Série TV Clássica)"));
        assertEquals("Knights of Sidonia", catalogo.grupo("Knights of Sidonia"));
    }

    @Test
    @DisplayName("grupo: titulos solos (86, Guilty Crown) tem grupo vazio")
    void tituloSoloSemGrupo() {
        assertEquals("", catalogo.grupo("86 (Eighty-Six)"));
        assertEquals("", catalogo.grupo("Guilty Crown"));
        assertEquals("", catalogo.grupo(null));
    }

    @Test
    @DisplayName("nomePadronizado: Gundam vira 'Mobile Suit Gundam - X'; demais mantem original")
    void nomePadronizadoGundam() {
        assertEquals("Mobile Suit Gundam - Zeta",
            catalogo.nomePadronizado("gundam_zeta", "Mobile Suit Zeta Gundam"));
        assertEquals("Mobile Suit Gundam - ZZ (Double Zeta)",
            catalogo.nomePadronizado("gundam_zz", "Mobile Suit Gundam ZZ"));
        // Nao-Gundam mantem o nome original.
        assertEquals("Macross Plus", catalogo.nomePadronizado("macross_plus", "Macross Plus"));
        assertEquals("86 (Eighty-Six)", catalogo.nomePadronizado("eight_six", "86 (Eighty-Six)"));
    }

    @Test
    @DisplayName("ordem: cronologica de lancamento dentro do grupo (Zeta antes de ZZ antes de CCA)")
    void ordemCronologica() {
        assertTrue(catalogo.ordem("gundam_0079") < catalogo.ordem("gundam_zeta"));
        assertTrue(catalogo.ordem("gundam_zeta") < catalogo.ordem("gundam_zz"));
        assertTrue(catalogo.ordem("gundam_zz") < catalogo.ordem("gundam_cca"));
        assertEquals(Integer.MAX_VALUE, catalogo.ordem("id_inexistente"));
        assertEquals(Integer.MAX_VALUE, catalogo.ordem(null));
    }

    @Test
    @DisplayName("Break Blade: grupo por 'Break Blade'/'Broken Blade' e os 6 filmes ordenados 1->6")
    void breakBladeGrupoEOrdem() {
        assertEquals("Break Blade", catalogo.grupo("Break Blade - Filme 1 - O Tempo do Despertar"));
        assertEquals("Break Blade", catalogo.grupo("Broken Blade Movie 6: Fortress of Lamentation"));
        // Os 6 filmes reservados saem em ordem de lançamento crescente.
        assertTrue(catalogo.ordem("break_blade_1") < catalogo.ordem("break_blade_2"));
        assertTrue(catalogo.ordem("break_blade_2") < catalogo.ordem("break_blade_3"));
        assertTrue(catalogo.ordem("break_blade_3") < catalogo.ordem("break_blade_4"));
        assertTrue(catalogo.ordem("break_blade_4") < catalogo.ordem("break_blade_5"));
        assertTrue(catalogo.ordem("break_blade_5") < catalogo.ordem("break_blade_6"));
    }
}
