package org.traducao.projeto.contexto.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.exception.BasePipelineException;
import org.traducao.projeto.traducao.domain.exceptions.TradutorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a hierarquia de exceções extraída na E7a —
 * {@code ExcecaoContexto} (raiz do módulo {@code contexto}) com
 * {@code ContextoNaoEncontradoException} sob ela, ambas movidas de {@code traducao}
 * e reparentadas para deixarem de ser {@code TradutorException}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Construtores preservam mensagem e causa.</li>
 *   <li>{@code ExcecaoContexto} IS-A {@code BasePipelineException}.</li>
 *   <li>{@code ContextoNaoEncontradoException} IS-A {@code ExcecaoContexto} (logo IS-A
 *       {@code BasePipelineException}).</li>
 *   <li>Prova NEGATIVA: {@code ContextoNaoEncontradoException} não é mais
 *       {@code TradutorException}. Como o fluxo desta exceção NÃO alcança o
 *       {@code catch (TradutorException)} do {@code TradutorCLI} (só é lançada por
 *       {@code GerenciadorContexto.definirContextoAtivo}, fora do caminho do CLI), a
 *       E7a não precisou de {@code catch (ExcecaoContexto)} no CLI.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer regressão da hierarquia (positiva ou negativa) reprova o teste — também é a
 * garantia de que o {@code BasePipelineExceptionMapper} (genérico sobre
 * {@code BasePipelineException}) continua cobrindo toda a família.
 */
@DisplayName("E7a: hierarquia de exceções de contexto (ExcecaoContexto/ContextoNaoEncontradoException)")
class HierarquiaExcecaoContextoTest {

    @Test
    @DisplayName("ExcecaoContexto preserva mensagem e causa")
    void excecaoContextoPreservaMensagemECausa() {
        ExcecaoContexto semCausa = new ExcecaoContexto("mensagem");
        assertEquals("mensagem", semCausa.getMessage());

        Throwable causa = new IllegalStateException("raiz");
        ExcecaoContexto comCausa = new ExcecaoContexto("mensagem", causa);
        assertEquals("mensagem", comCausa.getMessage());
        assertSame(causa, comCausa.getCause());
    }

    @Test
    @DisplayName("ContextoNaoEncontradoException preserva mensagem")
    void contextoNaoEncontradoPreservaMensagem() {
        ContextoNaoEncontradoException ex = new ContextoNaoEncontradoException("contexto x desconhecido");
        assertEquals("contexto x desconhecido", ex.getMessage());
    }

    @Test
    @DisplayName("provas POSITIVAS: ContextoNaoEncontradoException IS-A ExcecaoContexto IS-A BasePipelineException")
    void contextoNaoEncontradoHerdaDaRaizDeContexto() {
        ContextoNaoEncontradoException ex = new ContextoNaoEncontradoException("x");
        assertTrue(ex instanceof ExcecaoContexto);
        assertTrue(ex instanceof BasePipelineException);
    }

    @Test
    @DisplayName("prova POSITIVA: ExcecaoContexto IS-A BasePipelineException")
    void excecaoContextoHerdaDeBase() {
        ExcecaoContexto ex = new ExcecaoContexto("x");
        assertTrue(ex instanceof BasePipelineException);
    }

    @Test
    @DisplayName("prova NEGATIVA: ContextoNaoEncontradoException não é mais TradutorException")
    void naoEhMaisTradutorException() {
        // Referências tipadas como BasePipelineException: a checagem instanceof
        // TradutorException compila (poderia ser, estaticamente) e avalia em runtime.
        // Tipar como ExcecaoContexto tornaria o instanceof um erro de compilação —
        // prova ainda mais forte, mas inutilizável como assertiva runtime.
        BasePipelineException contexto = new ContextoNaoEncontradoException("x");
        BasePipelineException raiz = new ExcecaoContexto("x");
        assertFalse(contexto instanceof TradutorException,
            "ContextoNaoEncontradoException NAO pode mais ser TradutorException apos a E7a");
        assertFalse(raiz instanceof TradutorException,
            "ExcecaoContexto NAO pode ser TradutorException (hierarquia irmã sob BasePipelineException)");
    }
}
