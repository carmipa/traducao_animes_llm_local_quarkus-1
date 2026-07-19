package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link CorretorLoreDeterministico} — restaurar
 * terminologia canônica de lore SEM LLM, combinando casos herdados (Shin/"Canela";
 * "dud rounds") com o mapa genérico da obra, sem nunca deixar a linha pior.
 *
 * <p>INVARIANTES DO DOMÍNIO: o mapa só restaura quando o original EN contém o canônico na
 * grafia exata; forma-ruim casa por fronteira de palavra ignorando caixa.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de guarda ou de fronteira reprova.
 */
class CorretorLoreDeterministicoTest {

    private final CorretorLoreDeterministico corretor = new CorretorLoreDeterministico();

    @Test
    @DisplayName("corrige Shin traduzido como Canela quando o original tem Shin")
    void corrigeShinTraduzidoComoCanelaSemLlm() {
        var corrigida = corretor.corrigir(
            "Shin! Shinei Nouzen!",
            "Canela! Shinei Nouzen!",
            Map.of());

        assertTrue(corrigida.isPresent());
        assertEquals("Shin! Shinei Nouzen!", corrigida.get());
    }

    @Test
    @DisplayName("NÃO troca Canela quando o original não tem Shin")
    void naoTrocaCanelaQuandoOriginalNaoTemShin() {
        var corrigida = corretor.corrigir(
            "My leg hurts.",
            "Minha canela dói.",
            Map.of());

        assertTrue(corrigida.isEmpty());
    }

    @Test
    @DisplayName("corrige dud rounds traduzido literalmente para munições falhas")
    void corrigeDudRoundsTraduzidoLiteralmente() {
        var corrigida = corretor.corrigir(
            "Those are dud rounds.",
            "Essas são rodadas aleatórias.",
            Map.of());
        var cache = corretor.corrigir(
            "Those dud rounds landed around there.",
            "Aquelas rodadas fracassadas caíram ali perto.",
            Map.of());

        assertTrue(corrigida.isPresent());
        assertEquals("Essas são munições falhas.", corrigida.get());
        assertTrue(cache.isPresent());
        assertEquals("Aquelas munições falhas caíram ali perto.", cache.get());
    }

    @Test
    @DisplayName("mapa restaura termo quando o original tem o canônico exato")
    void mapaLoreRestauraTermoQuandoOriginalTemCanonico() {
        var corrigida = corretor.corrigir(
            "She was killed by the Titans.",
            "Ela foi morta pelos Titãs.",
            Map.of("Titãs", "Titans", "Titas", "Titans"));

        assertTrue(corrigida.isPresent());
        assertEquals("Ela foi morta pelos Titans.", corrigida.get());
    }

    @Test
    @DisplayName("mapa NÃO restaura sem o canônico exato no original (Titãs mitológico)")
    void mapaLoreNaoRestauraSemCanonicoNoOriginal() {
        var corrigida = corretor.corrigir(
            "The Greek titans rose against Olympus.",
            "Os titãs gregos se ergueram contra o Olimpo.",
            Map.of("Titãs", "Titans", "Titas", "Titans"));

        assertTrue(corrigida.isEmpty(),
            "sem 'Titans' na grafia exata no original, a forma comum nao pode ser tocada");
    }

    @Test
    @DisplayName("caso hardcoded (Shin) convive com mapa que não casa")
    void mapaLoreConviveComCasoHardcodedShin() {
        var corrigida = corretor.corrigir(
            "Shin! Shinei Nouzen!",
            "Canela! Shinei Nouzen!",
            Map.of("Legião", "Legion"));

        assertTrue(corrigida.isPresent());
        assertEquals("Shin! Shinei Nouzen!", corrigida.get());
    }

    @Test
    @DisplayName("NÃO troca 'canela' por Shin quando o nome Shin já está correto no PT (evita cinnamon)")
    void naoTrocaCanelaQuandoShinJaPresenteNoPt() {
        // EN tem o nome "Shin" (correto) e "canela" aqui é a especiaria, não o nome mal traduzido.
        // Como o PT já tem "Shin" na grafia certa, o gatilho não deve disparar.
        var corrigida = corretor.corrigir(
            "Shin, add cinnamon.",
            "Shin, adicione canela.",
            Map.of());

        assertTrue(corrigida.isEmpty(),
            "com 'Shin' já correto no PT, 'canela' é a especiaria e não pode virar Shin");
    }
}
