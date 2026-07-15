package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.legenda.DocumentoLegenda;
import org.traducao.projeto.traducao.domain.legenda.EventoLegenda;
import org.traducao.projeto.traducao.infrastructure.cache.EntradaCache;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que as correções da Opção 5 chegam à Opção 6 sem
 * apagar pendências que o Google não conseguiu resolver.
 *
 * <p>INVARIANTES DO DOMÍNIO: índice liga cache e diálogo; vazio é sempre
 * preservação, nunca comando de exclusão.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: mudança indevida no texto reprova o teste.
 */
class SincronizadorLegendaCacheServiceTest {

    /**
     * PROPÓSITO DE NEGÓCIO: materializa uma correção válida e mantém a fala cuja
     * entrada continuou vazia após `SEM_ALTERACAO` do Google.
     * <p>INVARIANTES DO DOMÍNIO: somente o evento 1 é alterado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: apagamento do evento 2 reprova o teste.
     */
    @Test
    void aplicaCorrecaoNaoVaziaESeguraPendenciaVazia() {
        DocumentoLegenda documento = new DocumentoLegenda("[Events]\n", List.of(
            new EventoLegenda(1, "Dialogue", "Default", "prefixo1,", "Texto antigo"),
            new EventoLegenda(2, "Dialogue", "Default", "prefixo2,", "Fransson!")), "\n", false);
        List<EntradaCache> entradas = List.of(
            new EntradaCache(1, "Default", "Help!", "Ajude!", "en", "pt-br"),
            new EntradaCache(2, "Default", "Fransson!", "", "en", "pt-br"));

        var resultado = new SincronizadorLegendaCacheService().sincronizar(documento, entradas, true);

        assertEquals(1, resultado.total());
        assertEquals("Ajude!", resultado.documento().eventos().get(0).texto());
        assertEquals("Fransson!", resultado.documento().eventos().get(1).texto());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede cache antigo de desfazer uma revisão posterior.
     * <p>INVARIANTES DO DOMÍNIO: autorização falsa mantém a mesma instância.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer sincronização reprova o teste.
     */
    @Test
    void naoAplicaCacheQuandoComparacaoTemporalNaoAutoriza() {
        DocumentoLegenda documento = new DocumentoLegenda("", List.of(
            new EventoLegenda(1, "Dialogue", "Default", "", "Revisão nova")), "\n", false);
        EntradaCache antiga = new EntradaCache(1, "Default", "Original", "Cache antigo", "en", "pt-br");

        var resultado = new SincronizadorLegendaCacheService().sincronizar(
            documento, List.of(antiga), false);

        assertEquals(0, resultado.total());
        assertEquals("Revisão nova", resultado.documento().eventos().get(0).texto());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recupera do banco persistente uma fala que voltou ao
     * inglês por restauração de backup, mesmo quando o ASS restaurado é mais novo.
     * <p>INVARIANTES DO DOMÍNIO: a recuperação exige igualdade exata com o
     * original EN; uma revisão PT-BR diferente continua soberana.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência da recuperação reprova o teste.
     */
    @Test
    void recuperaRegressaoAoOriginalMesmoComCacheMaisAntigo() {
        DocumentoLegenda documento = new DocumentoLegenda("", List.of(
            new EventoLegenda(1, "Dialogue", "Default", "", "Help me, Jona!"),
            new EventoLegenda(2, "Dialogue", "Default", "", "Revisão humana melhor")), "\n", false);
        List<EntradaCache> entradas = List.of(
            new EntradaCache(1, "Default", "Help me, Jona!", "Ajude-me, Jona!", "en", "pt-br"),
            new EntradaCache(2, "Default", "Original", "Tradução antiga", "en", "pt-br"));

        var resultado = new SincronizadorLegendaCacheService().sincronizar(documento, entradas, false);

        assertEquals(1, resultado.total());
        assertEquals(List.of(1), resultado.indicesRecuperadosDoOriginal());
        assertEquals("Ajude-me, Jona!", resultado.documento().eventos().get(0).texto());
        assertEquals("Revisão humana melhor", resultado.documento().eventos().get(1).texto());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que cache antigo remova um nome canônico
     * restaurado pela Opção 7 só porque a fala válida coincide com o inglês.
     * <p>INVARIANTES DO DOMÍNIO: índice protegido não é recuperado; outra
     * regressão verdadeira continua elegível pelo mesmo algoritmo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorno a `Michele!` reprova o teste.
     */
    @Test
    void naoConfundeFalaCanonicaComRegressaoAoIngles() {
        DocumentoLegenda documento = new DocumentoLegenda("", List.of(
            new EventoLegenda(59, "Dialogue", "Default", "", "Jona! Michele!"),
            new EventoLegenda(60, "Dialogue", "Default", "", "Help me!")), "\n", false);
        List<EntradaCache> entradas = List.of(
            new EntradaCache(59, "Default", "Jona! Michele!", "Michele!", "en", "pt-br"),
            new EntradaCache(60, "Default", "Help me!", "Ajude-me!", "en", "pt-br"));

        var resultado = new SincronizadorLegendaCacheService().sincronizar(
            documento, entradas, false, Set.of(59));

        assertEquals(1, resultado.total());
        assertEquals(List.of(60), resultado.indicesRecuperadosDoOriginal());
        assertEquals("Jona! Michele!", resultado.documento().eventos().get(0).texto());
        assertEquals("Ajude-me!", resultado.documento().eventos().get(1).texto());
    }
}
