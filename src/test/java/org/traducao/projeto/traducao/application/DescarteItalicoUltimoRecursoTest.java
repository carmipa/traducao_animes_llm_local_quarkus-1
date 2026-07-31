package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a regra do último recurso — quando o LLM perde os
 * marcadores e as únicas tags da fala eram itálico, publicar a TRADUÇÃO sem ênfase é
 * melhor que publicar o ORIGINAL EM INGLÊS com ênfase.
 *
 * <p>Decisão do Paulo em 2026-07-31. As falas deste teste são reais, tiradas dos logs de
 * corrupção: {@code {\i1}Eledore!}, {@code {\i1}Roger!{\i0}},
 * {@code The Apsaras {\i1}will{\i0} be completed.} — hoje todas ficam em inglês.
 *
 * <p>INVARIANTES DO DOMÍNIO: uma única tag que não seja itálico cancela o descarte,
 * porque posição, cor e quebra mudam o que se vê na tela, não apenas a ênfase.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer descarte de tag não-itálica, ou qualquer
 * saída em branco, reprova a suíte.
 */
class DescarteItalicoUltimoRecursoTest {

    private final DescarteItalicoUltimoRecurso descarte = new DescarteItalicoUltimoRecurso();

    @Test
    @DisplayName("Caso real: 'Roger!' deixa de sair em inglês e sai traduzido sem itálico")
    void salvaTraducaoDescartandoItalico() {
        assertEquals("Entendido!",
            descarte.salvarSemItalico(List.of("{\\i1}", "{\\i0}"), "Entendido!"));
    }

    @Test
    @DisplayName("Ênfase no meio da fala: perde o itálico, mantém o sentido traduzido")
    void salvaEnfaseNoMeio() {
        assertEquals("O Apsaras será concluído.",
            descarte.salvarSemItalico(List.of("{\\i1}", "{\\i0}"), "O Apsaras será concluído."));
    }

    @Test
    @DisplayName("Marcador residual na resposta do modelo é limpo")
    void limpaMarcadorResidual() {
        assertEquals("Eledore!",
            descarte.salvarSemItalico(List.of("{\\i1}"), "[[TAG0]]Eledore!"));
        assertEquals("Eledore!",
            descarte.salvarSemItalico(List.of("{\\i1}"), "{\\i1}Eledore!"));
    }

    @Test
    @DisplayName("POSIÇÃO cancela o descarte — mover a legenda na tela não é perder ênfase")
    void naoDescartaPosicionamento() {
        assertNull(descarte.salvarSemItalico(List.of("{\\an8\\i1}"), "Narração"));
        assertNull(descarte.salvarSemItalico(List.of("{\\pos(10,20)}"), "Placa"));
    }

    @Test
    @DisplayName("COR e FONTE cancelam o descarte")
    void naoDescartaCorOuFonte() {
        assertNull(descarte.salvarSemItalico(List.of("{\\c&HFFFFFF&}"), "Texto"));
        assertNull(descarte.salvarSemItalico(List.of("{\\fs140}"), "Título"));
    }

    @Test
    @DisplayName("QUEBRA cancela o descarte — \\N não é bloco de override")
    void naoDescartaQuebra() {
        assertNull(descarte.salvarSemItalico(List.of("{\\i1}", "\\N"), "Uma linha e outra"));
    }

    @Test
    @DisplayName("Uma tag boa não salva a fala se outra for perigosa")
    void bastaUmaTagNaoItalicaParaCancelar() {
        assertNull(descarte.salvarSemItalico(List.of("{\\i1}", "{\\pos(1,2)}"), "Texto"));
    }

    @Test
    @DisplayName("Tradução em branco NÃO é publicada — pior que o inglês")
    void naoPublicaVazio() {
        assertNull(descarte.salvarSemItalico(List.of("{\\i1}"), "   "));
        assertNull(descarte.salvarSemItalico(List.of("{\\i1}"), "[[TAG0]]"));
    }

    @Test
    @DisplayName("Entrada inconsistente devolve null, nunca exceção")
    void entradaInconsistenteDevolveNull() {
        assertNull(descarte.salvarSemItalico(null, "x"));
        assertNull(descarte.salvarSemItalico(List.of(), "x"));
        assertNull(descarte.salvarSemItalico(List.of("{\\i1}"), null));
    }

    @Test
    @DisplayName("ehSomenteItalico distingue itálico puro de composto")
    void classificaTag() {
        assertTrue(descarte.ehSomenteItalico("{\\i1}"));
        assertTrue(descarte.ehSomenteItalico("{\\i}"));
        assertTrue(descarte.ehSomenteItalico("{\\i0}"));
        assertFalse(descarte.ehSomenteItalico("{\\an8\\i1}"));
        assertFalse(descarte.ehSomenteItalico("\\N"));
        assertFalse(descarte.ehSomenteItalico("{}"));
        assertFalse(descarte.ehSomenteItalico(null));
    }
}
