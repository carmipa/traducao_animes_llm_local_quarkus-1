package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.application.NormalizadorAcentosComuns;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link RevisorPtOnlyService} — corrigir uma fala PT
 * só com o que é seguro sem o inglês, e apenas SINALIZAR o asterisco.
 *
 * <p>INVARIANTES DO DOMÍNIO: acentos inequívocos, \N reposicionado, concordância PT-only;
 * asterisco reportado, não escondido.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer correção indevida/faltante ou flag errado reprova.
 */
class RevisorPtOnlyServiceTest {

    private final RevisorPtOnlyService svc = new RevisorPtOnlyService(
        new NormalizadorAcentosComuns(), new CorretorDeterministicoConcordanciaService());

    @Test
    @DisplayName("repõe acentos inequívocos sem o inglês")
    void corrigeAcentos() {
        RevisorPtOnlyService.ResultadoFala r = svc.revisarFala("Nao vou tambem ate la");
        assertEquals("Não vou também até la", r.texto());
        assertTrue(r.alterado());
    }

    @Test
    @DisplayName("move o \\N para depois da pontuação")
    void moveQuebraOrfa() {
        RevisorPtOnlyService.ResultadoFala r = svc.revisarFala("executar a equipe\\N, se preciso");
        assertEquals("executar a equipe,\\N se preciso", r.texto());
    }

    @Test
    @DisplayName("aplica concordância PT-only (graças a Deus e possessivo de parentesco)")
    void aplicaConcordanciaPtOnly() {
        assertEquals("Graças a Deus!", svc.revisarFala("Graças ao Deus!").texto());
        assertEquals("meu pai chegou", svc.revisarFala("minha pai chegou").texto());
    }

    @Test
    @DisplayName("sinaliza asterisco sem reescrever a fala")
    void sinalizaAsterisco() {
        RevisorPtOnlyService.ResultadoFala r = svc.revisarFala("Merd*, me larga!");
        assertTrue(r.temAsterisco(), "deve sinalizar o asterisco");
        assertTrue(r.texto().contains("*"), "não deve esconder/remover o asterisco");
    }

    @Test
    @DisplayName("fala já correta não é alterada")
    void falaCorretaIntacta() {
        RevisorPtOnlyService.ResultadoFala r = svc.revisarFala("Tudo certo por aqui.");
        assertEquals("Tudo certo por aqui.", r.texto());
        assertFalse(r.alterado());
        assertFalse(r.temAsterisco());
    }
}
