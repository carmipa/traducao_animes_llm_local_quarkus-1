package org.traducao.projeto.traducao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a imutabilidade do registro de telemetria — o estado gravado
 * não pode divergir do momento do registro por aliasing das listas passadas pelo chamador.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@code errosOcorridos} e {@code pendenciasPorCausa} são cópias
 * defensivas imutáveis; {@code null} vira lista vazia.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer vazamento de referência mutável reprova.
 */
class TelemetriaTraducaoTest {

    @Test
    @DisplayName("#20: listas são cópias defensivas imutáveis; mutação externa não afeta o registro")
    void copiaDefensivaDasListas() {
        List<String> erros = new ArrayList<>(List.of("erro1"));
        TelemetriaTraducao t = new TelemetriaTraducao("ep.ass", "m", 1, 1, 0, 1L,
            erros, "Anime", "Temporada Única", "2026-01-01T00:00:00Z", "lore", "CONCLUIDO", null);

        erros.add("erro2"); // muta a lista original passada ao construtor

        assertEquals(1, t.errosOcorridos().size(),
            "cópia defensiva: mutação externa não pode vazar para o registro");
        assertThrows(UnsupportedOperationException.class, () -> t.errosOcorridos().add("x"),
            "a lista do registro deve ser imutável");
    }

    @Test
    @DisplayName("#20: listas nulas viram listas vazias imutáveis")
    void nullViraListaVazia() {
        TelemetriaTraducao t = new TelemetriaTraducao("ep.ass", "m", 1, 1, 0, 1L,
            null, "Anime", "Temporada Única", "2026-01-01T00:00:00Z", "lore", "CONCLUIDO", null);

        assertNotNull(t.errosOcorridos());
        assertNotNull(t.pendenciasPorCausa());
        assertTrue(t.errosOcorridos().isEmpty());
        assertTrue(t.pendenciasPorCausa().isEmpty());
    }
}
