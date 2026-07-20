package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o detector conservador de idioma-fonte — garante que
 * falas já em português sejam reconhecidas (para não reenviá-las ao LLM) SEM nunca classificar
 * inglês como já-traduzido (o erro perigoso: deixaria inglês na saída).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Evidência de PT (stopword ou ≥2 diacríticos) + ausência de inglês ⇒ {@code true}.</li>
 *   <li>Qualquer sinal de inglês, alvo não-PT, texto nulo ou curto ⇒ {@code false}.</li>
 * </ul>
 */
@DisplayName("DetectorIdiomaFonteService: reconhece PT sem confundir inglês")
class DetectorIdiomaFonteServiceTest {

    private final DetectorIdiomaFonteService detector = new DetectorIdiomaFonteService();
    private static final String PT = "pt-BR";

    @Test
    @DisplayName("linha com stopword PT (não/você) → já no alvo")
    void stopwordPortuguesa() {
        assertTrue(detector.jaNoIdiomaAlvo("Não é que ele fosse desagradável.", PT));
        assertTrue(detector.jaNoIdiomaAlvo("Você quer que a gente perca?", PT));
    }

    @Test
    @DisplayName("linha só com diacríticos PT (≥2), sem stopword → já no alvo")
    void diacriticosPortugueses() {
        assertTrue(detector.jaNoIdiomaAlvo("A força nacional será derrotada.", PT));
    }

    @Test
    @DisplayName("linha inglesa nunca é confundida com PT")
    void inglesNaoEhAlvo() {
        assertFalse(detector.jaNoIdiomaAlvo("What happened to that guy you had?", PT));
        assertFalse(detector.jaNoIdiomaAlvo("It is because the Principality of Zeon.", PT));
        assertFalse(detector.jaNoIdiomaAlvo("Move it! Move it!", PT));
    }

    @Test
    @DisplayName("mistura EN+PT: sinal de inglês vence → manda traduzir")
    void misturaMandaTraduzir() {
        assertFalse(detector.jaNoIdiomaAlvo("Cuidado com o seu mobile suit when we land!", PT));
    }

    @Test
    @DisplayName("tags ASS são ignoradas antes da checagem")
    void tagsAssIgnoradas() {
        assertTrue(detector.jaNoIdiomaAlvo("{\\an8\\i1}Não! É um novo começo!{\\i}", PT));
    }

    @Test
    @DisplayName("linha curta é ambígua → não pula (false)")
    void curtaNaoPula() {
        assertFalse(detector.jaNoIdiomaAlvo("Sim.", PT));
        assertFalse(detector.jaNoIdiomaAlvo("Oi!", PT));
    }

    @Test
    @DisplayName("alvo não-PT nunca pula (heurística só cobre PT)")
    void alvoNaoPtNuncaPula() {
        assertFalse(detector.jaNoIdiomaAlvo("Não é que ele fosse desagradável.", "en"));
        assertFalse(detector.jaNoIdiomaAlvo("Não é que ele fosse desagradável.", "es"));
    }

    @Test
    @DisplayName("nulo/vazio → false, sem lançar")
    void nuloEhFalse() {
        assertFalse(detector.jaNoIdiomaAlvo(null, PT));
        assertFalse(detector.jaNoIdiomaAlvo("", PT));
    }
}
