package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa por regressão a política de reuso de cache e a validação
 * final do {@link AvaliadorTraducaoCache} (FASE F, R5), garantindo que a decomposição não
 * altere quando uma tradução pode ser reusada ou por que uma fala permanece pendente.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa os colaboradores reais ({@link MascaradorTags},
 * {@link DetectorTraducaoIdenticaService} sobre uma lore vazia, {@link ValidadorTraducaoService})
 * com entradas escolhidas para exercitar cada ramo; sem rede, LM Studio, sleep ou dependência
 * temporal.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de reuso/motivo reprova a suíte.
 */
class AvaliadorTraducaoCacheTest {

    private static final class LoreVazia implements LoreAtivaPort {
        @Override public Set<String> termosProtegidosAtivos() { return Set.of(); }
        @Override public String obterLoreAtiva() { return ""; }
    }

    private final AvaliadorTraducaoCache avaliador = new AvaliadorTraducaoCache(
        new MascaradorTags(),
        new DetectorTraducaoIdenticaService(new LoreVazia()),
        new ValidadorTraducaoService());

    /**
     * PROPÓSITO DE NEGÓCIO: tradução ausente nunca é reaproveitada.
     * <p>INVARIANTES DO DOMÍNIO: {@code null} e branco degradam para não-reuso.
     * <p>COMPORTAMENTO EM CASO DE FALHA: reuso de vazio reprova.
     */
    @Test
    void traducaoNulaOuVaziaNaoEhReaproveitavel() {
        assertFalse(avaliador.isCacheReaproveitavel("Hello", null));
        assertFalse(avaliador.isCacheReaproveitavel("Hello", "   "));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tradução que perdeu/alterou tags do original não é reusada.
     * <p>INVARIANTES DO DOMÍNIO: divergência estrutural de tags invalida o cache.
     * <p>COMPORTAMENTO EM CASO DE FALHA: reuso com tags divergentes reprova.
     */
    @Test
    void tagsDivergentesNaoSaoReaproveitaveis() {
        assertFalse(avaliador.isCacheReaproveitavel("{\\i1}Oi", "Oi"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tradução PT-BR bem-formada e distinta do original é reusável.
     * <p>INVARIANTES DO DOMÍNIO: estrutura preservada + validação limpa habilitam o reuso.
     * <p>COMPORTAMENTO EM CASO DE FALHA: negar reuso de tradução válida reprova.
     */
    @Test
    void traducaoValidaEhReaproveitavel() {
        assertTrue(avaliador.isCacheReaproveitavel("Hello", "Olá"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando a tradução é idêntica ao original, o reuso só ocorre se o
     * {@link DetectorTraducaoIdenticaService} autorizar (nome próprio) — não para palavra
     * conversacional em inglês.
     * <p>INVARIANTES DO DOMÍNIO: "Naruto" idêntico é mantido; "hello" idêntico é rejeitado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: inverter qualquer um dos casos reprova.
     */
    @Test
    void originalIdenticoReaproveitavelSomenteQuandoDetectorAutoriza() {
        assertTrue(avaliador.isCacheReaproveitavel("Naruto", "Naruto"),
            "nome próprio idêntico deve ser mantido");
        assertFalse(avaliador.isCacheReaproveitavel("hello", "hello"),
            "palavra inglesa comum idêntica não é tradução");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tradução que o validador acusa como resíduo gringo não é reusada.
     * <p>INVARIANTES DO DOMÍNIO: {@code AlucinacaoDetectadaException} do validador vira não-reuso.
     * <p>COMPORTAMENTO EM CASO DE FALHA: reusar resíduo reprova.
     */
    @Test
    void residuoDetectadoPeloValidadorNaoEhReaproveitado() {
        assertFalse(avaliador.isCacheReaproveitavel("algo", "the same"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: {@code motivoFalhaFinal} devolve a justificativa correta para cada
     * classe de falha e {@code null} para uma tradução válida.
     * <p>INVARIANTES DO DOMÍNIO: vazio, tags divergentes, texto não traduzido e resíduo têm
     * motivos distintos e estáveis.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer motivo divergente reprova.
     */
    @Test
    void motivoFalhaFinalRetornaMotivosEsperados() {
        assertEquals("resposta vazia", avaliador.motivoFalhaFinal("Hello", ""));
        assertEquals("tags ASS/SSA ou quebras de linha divergentes do original",
            avaliador.motivoFalhaFinal("{\\i1}Oi", "Oi"));
        assertEquals("modelo devolveu o texto original sem tradução",
            avaliador.motivoFalhaFinal("hello", "hello"));
        assertTrue(avaliador.motivoFalhaFinal("algo", "the same").startsWith("Resíduo"),
            "resíduo gringo deve devolver o diagnóstico do validador");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma tradução válida não tem motivo de falha.
     * <p>INVARIANTES DO DOMÍNIO: estrutura preservada + validação limpa devolvem {@code null}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivo não-nulo para tradução válida reprova.
     */
    @Test
    void traducaoValidaRetornaNullComoMotivo() {
        assertNull(avaliador.motivoFalhaFinal("Hello", "Olá"));
    }
}
