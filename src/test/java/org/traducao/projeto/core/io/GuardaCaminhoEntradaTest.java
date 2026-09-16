package org.traducao.projeto.core.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o aviso anti-baseline da revisão (Achado 0, 2026-09-16). A revisão
 * sobrescreve no lugar a pasta que lê; apontá-la para um baseline de comparação apaga a referência
 * de um experimento em silêncio. O aviso torna isso visível SEM bloquear o uso legítimo.
 *
 * <p>INVARIANTES DO DOMÍNIO: só o NOME da pasta decide; alvo genérico da revisão não dispara.
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer aviso indevido no alvo normal reprova o teste.
 */
class GuardaCaminhoEntradaTest {

    private final GuardaCaminhoEntrada guarda = new GuardaCaminhoEntrada();

    /** DEFEITO/HAZARD: revisar um baseline de comparação avisa. */
    @Test
    void avisaAoRevisarBaselineDeComparacao() {
        assertTrue(guarda.avisoRevisaoSobrescreveBaseline("animes/86/traducao_aya").isPresent(),
            "traducao_aya e baseline de comparacao");
        assertTrue(guarda.avisoRevisaoSobrescreveBaseline("animes/86/traducao_mistral").isPresent(),
            "traducao_mistral e baseline de comparacao");
        assertTrue(guarda.avisoRevisaoSobrescreveBaseline("animes/86/legenda-simplificada").isPresent(),
            "legenda-simplificada foi a cicatriz dos 17 arquivos");
    }

    /**
     * CONTRA-CASO (A1): o alvo NORMAL da revisao e uma pasta comum NAO disparam — avisar neles seria
     * o alarme falso que ensina a ignorar o aviso.
     */
    @Test
    void naoAvisaNoAlvoNormalNemEmPastaComum() {
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline("animes/86/traducao_ptbr").isPresent(),
            "traducao_ptbr e o alvo NORMAL da revisao; avisar nele seria ruido");
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline("animes/86/legendas_extraidas_ass").isPresent(),
            "pasta comum nao avisa");
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline("").isPresent());
        assertFalse(guarda.avisoRevisaoSobrescreveBaseline(null).isPresent());
    }
}
