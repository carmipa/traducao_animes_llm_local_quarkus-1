package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: garante que o painel da Opção 6 diferencie conclusão
 * integral de uma execução estável que ainda deixou falas sem solução.
 *
 * <p>INVARIANTES DO DOMÍNIO: qualquer pendência impede banner verde.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: status divergente reprova o teste.
 */
class ResultadoRevisaoLegendasTest {

    /**
     * PROPÓSITO DE NEGÓCIO: representa revisão sem problemas remanescentes.
     * <p>INVARIANTES DO DOMÍNIO: zero pendências equivale a conclusão integral.
     * <p>COMPORTAMENTO EM CASO DE FALHA: status diferente reprova o teste.
     */
    @Test
    void semPendenciasConcluiIntegralmente() {
        assertEquals("CONCLUIDO", new ResultadoRevisaoLegendas(1, 3, 3, 0).status());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém visível a fala detectada que nenhum motor corrigiu.
     * <p>INVARIANTES DO DOMÍNIO: pendência tem precedência mesmo sem falha técnica.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso sucesso reprova o teste.
     */
    @Test
    void pendenciaImpedeFalsoSucesso() {
        assertEquals("CONCLUIDO_COM_PENDENCIAS",
            new ResultadoRevisaoLegendas(1, 0, 1, 1).status());
    }
}
