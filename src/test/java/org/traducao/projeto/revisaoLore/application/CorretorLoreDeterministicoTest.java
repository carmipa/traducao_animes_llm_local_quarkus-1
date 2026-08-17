package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

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

    private final CorretorLoreDeterministico corretor = new CorretorLoreDeterministico(new EnforcadorTermosLore());

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

    /**
     * PROPÓSITO DE NEGÓCIO: prende os três defeitos que existiam SÓ nesta classe, quando ela
     * carregava uma cópia própria do algoritmo do enforcer. A cópia foi removida e o mapa passou
     * a ser delegado ao {@link EnforcadorTermosLore}; estes casos provam que a delegação de fato
     * mudou o comportamento — sem eles, alguém pode reintroduzir a cópia local e a suíte não vê.
     *
     * <p>Todos usam entradas REAIS dos mapas de lore em produção.
     */
    @Test
    @DisplayName("teto de ocorrências: 'Quatro' vira Quattro só onde o EN sustenta, não no número")
    void tetoDeOcorrenciasNaoCorrompeONumeroQuatro() {
        // "Quatro" é ao mesmo tempo o nome do piloto (Quattro Bajeena, em Zeta/ZZ) e o NÚMERO.
        // A cópia antiga usava replaceAll e devolvia "Quattro, há Quattro inimigos".
        var corrigida = corretor.corrigir(
            "Quattro, there are four enemies.",
            "Quatro, há quatro inimigos.",
            Map.of("Quatro", "Quattro"));

        assertTrue(corrigida.isPresent());
        assertEquals("Quattro, há quatro inimigos.", corrigida.get(),
            "o canônico aparece 1x no EN, então só UMA restauração é autorizada — e é a capitalizada");
    }

    @Test
    @DisplayName("ordem por comprimento: a frase longa é aplicada antes da curta que a contém")
    void ordemPorComprimentoPreservaOTermoComposto() {
        // A cópia antiga iterava o mapa sem ordem e devolvia "O Genoma do Void despertou."
        // O mapa é ORDENADO com a chave curta PRIMEIRO de propósito: com Map.of a ordem de
        // iteração depende do hash e o teste passaria por sorte metade das vezes, sem provar nada.
        var corrigida = corretor.corrigir(
            "The Void Genome awakened.",
            "O Genoma do Vazio despertou.",
            curtaPrimeiro("Vazio", "Void", "Genoma do Vazio", "Void Genome"));

        assertTrue(corrigida.isPresent());
        assertEquals("O Void Genome despertou.", corrigida.get());
    }

    @Test
    @DisplayName("canônico multi-palavra: o EN em caixa baixa ainda conta como o termo da lore")
    void canonicoMultiPalavraReconheceCaixaDiferenteNoIngles() {
        // A cópia antiga exigia grafia exata para TODO canônico, então "mobile suits" no EN não
        // reconhecia "Mobile Suits" e a restauração nunca disparava.
        var corrigida = corretor.corrigir(
            "Two mobile suits approaching.",
            "Dois trajes móveis se aproximando.",
            Map.of("Trajes Móveis", "Mobile Suits"));

        assertTrue(corrigida.isPresent());
        assertEquals("Dois Mobile Suits se aproximando.", corrigida.get());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o mapa com ordem de iteração GARANTIDA e hostil — a chave curta
     * antes da longa. Sem isso o teste de ordenação é decidido pelo hash das chaves e passa por
     * acaso, deixando de provar que a ordenação existe.
     */
    private static Map<String, String> curtaPrimeiro(
            String chaveCurta, String valorCurta, String chaveLonga, String valorLonga) {
        Map<String, String> mapa = new java.util.LinkedHashMap<>();
        mapa.put(chaveCurta, valorCurta);
        mapa.put(chaveLonga, valorLonga);
        return mapa;
    }
}
