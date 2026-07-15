package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que a Revisão de Legendas não seja usada como
 * retradutor acidental de um ASS restaurado parcialmente em inglês.
 * <p>INVARIANTES DO DOMÍNIO: pequenos resíduos continuam revisáveis; regressão
 * ampla é bloqueada antes de chamadas em massa ao LLM ou Google.
 * <p>COMPORTAMENTO EM CASO DE FALHA: mudança indevida do limiar reprova os testes.
 */
class RevisarLegendasProtecaoMassaTest {

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia o caso real de centenas de falas inglesas.
     * <p>INVARIANTES DO DOMÍNIO: 360 de 1.230 excede ambos os limites.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorno falso reprova o teste.
     */
    @Test
    void bloqueiaRegressaoRealDeGundamNarrative() {
        assertTrue(RevisarLegendasUseCase.excedeLimiarRetraducaoEmMassa(1230, 360));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite corrigir resíduos isolados no fluxo normal.
     * <p>INVARIANTES DO DOMÍNIO: três pendências em 1.230 falas não são massa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorno verdadeiro reprova o teste.
     */
    @Test
    void permitePoucasPendencias() {
        assertFalse(RevisarLegendasUseCase.excedeLimiarRetraducaoEmMassa(1230, 3));
        assertFalse(RevisarLegendasUseCase.excedeLimiarRetraducaoEmMassa(100, 10));
    }
}
