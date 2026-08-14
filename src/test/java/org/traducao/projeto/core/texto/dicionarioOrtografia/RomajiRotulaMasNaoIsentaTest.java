package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o dicionário de romaji passa a ROTULAR japonês em alfabeto latino sem
 * ISENTAR nenhuma palavra da checagem de nome próprio.
 *
 * <h2>O trade-off que este teste congela, medido antes de ligar</h2>
 * O {@code ja_ROMAJI.dic} vem do IPADIC — 129.745 formas — e é morfológico, então inclui nome
 * próprio à beça. Medido com o hunspell real em 14/08/2026:
 * <pre>
 *   kimi, kokoro, watashi -&gt; CONHECE   (romaji comum, é o que se quer rotular)
 *   Aoshima               -&gt; CONHECE   (personagem do Memories — o problema)
 *   sasageyo, yakusoku    -&gt; nao conhece (a cobertura é parcial)
 * </pre>
 * O arquivo abre em {@code aarajima}, {@code aatsukawa}: são sobrenomes. Se {@code ROMAJI}
 * significasse "palavra de idioma conhecido, logo não é nome inventado da obra", ligar o
 * dicionário CEGARIA o detector de nome próprio exatamente no caso mais frequente do acervo —
 * nome japonês. Por isso o veredicto rotula e o detector continua acusando.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell ou sem o {@code ja_ROMAJI}, PULA por {@link Assumptions}.
 */
@DisplayName("romaji: rotula, mas não isenta da checagem de nome próprio")
class RomajiRotulaMasNaoIsentaTest {

    private static Map<String, VeredictoPalavra> classificar(Set<String> palavras) {
        return new CorretorOrtograficoLegenda().classificar(palavras);
    }

    private static void exigeRomaji(Map<String, VeredictoPalavra> v) {
        Assumptions.assumeFalse(v.isEmpty(), "hunspell ausente — NÃO VERIFICADO");
        Assumptions.assumeTrue(v.values().stream().noneMatch(x -> x == VeredictoPalavra.NAO_VERIFICADO),
            "algum dicionário indisponível — NÃO VERIFICADO");
    }

    @Test
    @DisplayName("romaji comum ganha rótulo próprio, em vez de cair em DESCONHECIDA")
    void romajiComumEhRotulado() {
        var v = classificar(Set.of("kimi", "kokoro", "watashi"));
        exigeRomaji(v);
        for (String p : Set.of("kimi", "kokoro", "watashi")) {
            assertEquals(VeredictoPalavra.ROMAJI, v.get(p),
                "'" + p + "' é romaji e devia ser rotulado como tal; veio " + v.get(p));
        }
    }

    /**
     * O TESTE QUE IMPEDE A CEGUEIRA. Se um dia alguém "otimizar" tratando ROMAJI como palavra
     * conhecida, este assert cai — e com ele voltaria o buraco de não acusar nome japonês
     * traduzido, que é o caso mais comum do acervo.
     */
    @Test
    @DisplayName("Aoshima é reconhecido pelo romaji e MESMO ASSIM continua acusável")
    void nomeJaponesContinuaAcusavel() {
        var v = classificar(Set.of("Aoshima"));
        exigeRomaji(v);

        assertNotEquals(VeredictoPalavra.PORTUGUES_OK, v.get("Aoshima"));
        assertTrue(v.get("Aoshima") == VeredictoPalavra.ROMAJI
                || v.get("Aoshima") == VeredictoPalavra.DESCONHECIDA,
            "Aoshima precisa cair num veredicto que o detector de nome próprio trata como "
                + "candidato. Veio: " + v.get("Aoshima"));
    }

    /** O português continua vencendo: ligar mais um dicionário não pode atropelar o alvo. */
    @Test
    @DisplayName("português tem precedência sobre o romaji")
    void portuguesVenceRomaji() {
        var v = classificar(Set.of("mora", "cara", "sara"));
        Assumptions.assumeFalse(v.isEmpty(), "hunspell ausente — NÃO VERIFICADO");
        for (String p : Set.of("mora", "cara", "sara")) {
            assertEquals(VeredictoPalavra.PORTUGUES_OK, v.get(p),
                "'" + p + "' é palavra portuguesa e o romaji a roubou: " + v.get(p));
        }
    }

    /** E a função original do subpacote — repor acento — não pode ter mudado. */
    @Test
    @DisplayName("a correção de acento continua intacta com cinco dicionários")
    void correcaoDeAcentoIntacta() {
        var c = new CorretorOrtograficoLegenda();
        String saida = c.corrigir("A situacao da colonia e critica.");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");
        assertTrue(saida.contains("situação"), "a correção de acento quebrou: " + saida);
    }
}
