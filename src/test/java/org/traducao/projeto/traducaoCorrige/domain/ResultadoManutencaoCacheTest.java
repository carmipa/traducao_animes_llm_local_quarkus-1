package org.traducao.projeto.traducaoCorrige.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: garante que o painel diferencie conclusão integral de
 * uma execução tecnicamente estável que ainda deixou itens sem correção.
 *
 * <p>INVARIANTES DO DOMÍNIO: itens detectados e não corrigidos são pendências,
 * não sucesso completo nem falha técnica.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: status ou contagem divergente reprova o teste.
 */
class ResultadoManutencaoCacheTest {

    /**
     * PROPÓSITO DE NEGÓCIO: reproduz o caso real de 350 candidatos, 348
     * corrigidos e duas respostas Google sem alteração.
     * <p>INVARIANTES DO DOMÍNIO: o resultado expõe exatamente duas pendências.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sucesso verde indevido reprova o teste.
     */
    @Test
    void informaConclusaoComPendencias() {
        ResultadoManutencaoCache resultado = new ResultadoManutencaoCache(
            1, 1, 350, 348, 2, 2, 0, false);

        assertEquals(2, resultado.itensPendentes());
        assertEquals("CONCLUIDO_COM_PENDENCIAS", resultado.status());
    }
}
