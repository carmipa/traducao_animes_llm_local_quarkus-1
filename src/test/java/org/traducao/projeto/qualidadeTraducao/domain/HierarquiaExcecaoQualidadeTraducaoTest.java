package org.traducao.projeto.qualidadeTraducao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.exception.BasePipelineException;
import org.traducao.projeto.traducao.domain.exceptions.TradutorException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a hierarquia de exceções extraída na E8b —
 * {@code ExcecaoQualidadeTraducao} (raiz do peer {@code qualidadeTraducao}) com
 * {@code AlucinacaoDetectadaException} sob ela, movidas de {@code traducao} e
 * reparentadas para deixarem de ser {@code TradutorException}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code ExcecaoQualidadeTraducao} IS-A {@code BasePipelineException}.</li>
 *   <li>{@code AlucinacaoDetectadaException} IS-A {@code ExcecaoQualidadeTraducao} (logo
 *       IS-A {@code BasePipelineException}).</li>
 *   <li>Prova NEGATIVA: {@code AlucinacaoDetectadaException} NÃO é mais
 *       {@code TradutorException} — por isso os sítios que a capturavam por herança
 *       (ProcessarEpisodioUseCase, TradutorCLI) passaram a multi-catch explícito na E8b.</li>
 *   <li>Construtores preservam mensagem (e causa, na base).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer regressão da hierarquia (positiva ou negativa) reprova o teste — também é a
 * garantia de que o {@code BasePipelineExceptionMapper} (genérico sobre
 * {@code BasePipelineException}) continua cobrindo toda a família.
 */
@DisplayName("E8b: hierarquia de exceções de qualidade (ExcecaoQualidadeTraducao/AlucinacaoDetectadaException)")
class HierarquiaExcecaoQualidadeTraducaoTest {

    @Test
    @DisplayName("ExcecaoQualidadeTraducao preserva mensagem e causa da base")
    void excecaoBasePreservaMensagemECausa() {
        ExcecaoQualidadeTraducao semCausa = new ExcecaoQualidadeTraducao("mensagem");
        assertEquals("mensagem", semCausa.getMessage());

        Throwable causa = new IllegalStateException("raiz");
        ExcecaoQualidadeTraducao comCausa = new ExcecaoQualidadeTraducao("mensagem", causa);
        assertEquals("mensagem", comCausa.getMessage());
        assertSame(causa, comCausa.getCause());
    }

    @Test
    @DisplayName("AlucinacaoDetectadaException preserva a mensagem concreta")
    void alucinacaoPreservaMensagem() {
        AlucinacaoDetectadaException ex = new AlucinacaoDetectadaException("marcadores corrompidos");
        assertEquals("marcadores corrompidos", ex.getMessage());
    }

    @Test
    @DisplayName("provas POSITIVAS: AlucinacaoDetectadaException IS-A ExcecaoQualidadeTraducao IS-A BasePipelineException")
    void provasPositivasDaHierarquia() {
        ExcecaoQualidadeTraducao base = new ExcecaoQualidadeTraducao("x");
        assertTrue(base instanceof BasePipelineException,
            "ExcecaoQualidadeTraducao deve ser BasePipelineException");

        AlucinacaoDetectadaException alucinacao = new AlucinacaoDetectadaException("y");
        assertTrue(alucinacao instanceof ExcecaoQualidadeTraducao,
            "AlucinacaoDetectadaException deve ser ExcecaoQualidadeTraducao");
        assertTrue(alucinacao instanceof BasePipelineException,
            "AlucinacaoDetectadaException deve ser BasePipelineException");
    }

    @Test
    @DisplayName("prova NEGATIVA: AlucinacaoDetectadaException NÃO é mais TradutorException")
    void provaNegativaNaoEhMaisTradutorException() {
        // instanceof direto não compila (tipos já não relacionados) — a própria prova de que
        // o reparenting funcionou; usa-se reflexão para asseverar a NÃO-herança.
        assertFalse(TradutorException.class.isAssignableFrom(AlucinacaoDetectadaException.class),
            "após a E8b, AlucinacaoDetectadaException NÃO pode mais ser TradutorException");
    }
}
