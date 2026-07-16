package org.traducao.projeto.legenda.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.exception.BasePipelineException;
import org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException;
import org.traducao.projeto.traducao.domain.exceptions.TradutorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a hierarquia de exceções extraída na E5b —
 * {@code ExcecaoLegenda} (raiz do módulo {@code legenda}) e
 * {@code ArquivoLegendaException} sob ela, com {@code EntradaJaTraduzidaException}
 * permanecendo em {@code traducao} mas reparentada para a hierarquia de legenda.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Construtores preservam mensagem e causa.</li>
 *   <li>{@code ArquivoLegendaException} IS-A {@code ExcecaoLegenda} IS-A {@code BasePipelineException}.</li>
 *   <li>{@code EntradaJaTraduzidaException} IS-A {@code ArquivoLegendaException} (logo IS-A
 *       {@code ExcecaoLegenda} e {@code BasePipelineException}).</li>
 *   <li>Provas NEGATIVAS: nenhuma das duas é mais {@code TradutorException} — é exatamente
 *       por isso que o {@code TradutorCLI} precisou de um {@code catch (ExcecaoLegenda)}
 *       equivalente ao ramo {@code TradutorException}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer regressão da hierarquia (positiva ou negativa) reprova o teste — também é a
 * garantia de que o {@code BasePipelineExceptionMapper} (genérico sobre
 * {@code BasePipelineException}) continua cobrindo toda a família.
 */
@DisplayName("E5b: hierarquia de exceções de legenda (ExcecaoLegenda/ArquivoLegendaException)")
class HierarquiaExcecaoLegendaTest {

    @Test
    @DisplayName("ExcecaoLegenda preserva mensagem e causa")
    void excecaoLegendaPreservaMensagemECausa() {
        ExcecaoLegenda semCausa = new ExcecaoLegenda("mensagem");
        assertEquals("mensagem", semCausa.getMessage());

        Throwable causa = new IllegalStateException("raiz");
        ExcecaoLegenda comCausa = new ExcecaoLegenda("mensagem", causa);
        assertEquals("mensagem", comCausa.getMessage());
        assertSame(causa, comCausa.getCause());
    }

    @Test
    @DisplayName("ArquivoLegendaException preserva mensagem e causa")
    void arquivoLegendaExceptionPreservaMensagemECausa() {
        ArquivoLegendaException semCausa = new ArquivoLegendaException("falha io");
        assertEquals("falha io", semCausa.getMessage());

        Throwable causa = new IllegalStateException("raiz");
        ArquivoLegendaException comCausa = new ArquivoLegendaException("falha io", causa);
        assertEquals("falha io", comCausa.getMessage());
        assertSame(causa, comCausa.getCause());
    }

    @Test
    @DisplayName("provas POSITIVAS: ArquivoLegendaException IS-A ExcecaoLegenda IS-A BasePipelineException")
    void arquivoLegendaExceptionHerdaDaRaizDeLegenda() {
        ArquivoLegendaException ex = new ArquivoLegendaException("falha io");
        assertTrue(ex instanceof ExcecaoLegenda);
        assertTrue(ex instanceof BasePipelineException);
    }

    @Test
    @DisplayName("provas POSITIVAS: EntradaJaTraduzidaException IS-A ArquivoLegendaException/ExcecaoLegenda/BasePipelineException")
    void entradaJaTraduzidaHerdaDaHierarquiaDeLegenda() {
        EntradaJaTraduzidaException ex = new EntradaJaTraduzidaException("ja traduzido");
        assertTrue(ex instanceof ArquivoLegendaException);
        assertTrue(ex instanceof ExcecaoLegenda);
        assertTrue(ex instanceof BasePipelineException);
    }

    @Test
    @DisplayName("provas NEGATIVAS: nem ArquivoLegendaException nem EntradaJaTraduzidaException são TradutorException")
    void naoSaoMaisTradutorException() {
        // Referências tipadas como BasePipelineException: a checagem instanceof
        // TradutorException compila (poderia ser, estaticamente) e avalia em runtime.
        // Tipar como ArquivoLegendaException tornaria o instanceof um erro de compilação —
        // prova ainda mais forte, mas inutilizável como assertiva runtime.
        BasePipelineException arquivo = new ArquivoLegendaException("falha io");
        BasePipelineException entrada = new EntradaJaTraduzidaException("ja traduzido");
        assertFalse(arquivo instanceof TradutorException,
            "ArquivoLegendaException NAO pode mais ser TradutorException apos a E5b");
        assertFalse(entrada instanceof TradutorException,
            "EntradaJaTraduzidaException NAO pode mais ser TradutorException apos a E5b");
    }
}
