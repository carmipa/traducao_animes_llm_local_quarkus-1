package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;

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

        var resultado = new SincronizadorLegendaCacheService(new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()), new org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico()).sincronizar(documento, entradas, true);

        assertEquals(1, resultado.total());
        assertEquals("Ajude!", resultado.documento().eventos().get(0).texto());
        assertEquals("Fransson!", resultado.documento().eventos().get(1).texto());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: esta ponte é o ÚNICO ponto da 3.1 que traz texto de FORA da
     * legenda — ela escreve o {@code traduzido} do cache. O cache gravado ANTES da regra do
     * itálico (22/08/2026) tem {@code \i1}, então sem a regra aqui a Revisão de Legendas
     * REINTRODUZIRIA na legenda exatamente o que a Tradução acabou de tirar.
     *
     * <p>É o pior formato possível de defeito para quem opera: o operador passa a tela que
     * existe para CORRIGIR e vê o problema VOLTAR.
     *
     * <p>INVARIANTES DO DOMÍNIO: o resto do texto e as demais tags chegam intactos; só o
     * itálico não passa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: itálico na legenda depois da sincronização reprova.
     */
    @Test
    @DisplayName("cache anterior a regra nao reintroduz italico na legenda ja limpa")
    void cacheComItalicoNaoReintroduzItalicoNaLegenda() {
        DocumentoLegenda documento = new DocumentoLegenda("[Events]\\n", List.of(
            new EventoLegenda(1, "Dialogue", "Default", "prefixo1,", "Texto antigo"),
            new EventoLegenda(2, "Dialogue", "Default", "prefixo2,", "Outro antigo")), "\\n", false);
        List<EntradaCache> entradas = List.of(
            new EntradaCache(1, "Default", "Help!", "{\\i1}Ajude!{\\i0}", "en", "pt-br"),
            new EntradaCache(2, "Default", "Look.", "{\\q2\\i1}Olhe.", "en", "pt-br"));

        var resultado = new SincronizadorLegendaCacheService(
            new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()),
            new org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico())
            .sincronizar(documento, entradas, true);

        assertEquals(2, resultado.total());
        assertEquals("Ajude!", resultado.documento().eventos().get(0).texto(),
            "o italico do cache antigo nao pode voltar para a legenda");
        assertEquals("{\\q2}Olhe.", resultado.documento().eventos().get(1).texto(),
            "bloco misto: sai o italico e o \\q2 (quebra automatica) sobrevive");
    }

    /**
     * O VETO DE MÚSICA NESTA PONTE — e ele nasceu de dano REAL no acervo, não de hipótese.
     *
     * <p>Em 17/08/2026, uma corrida da 3.1 no Gundam 08th MS Team reescreveu <b>693 linhas, das
     * quais 687 eram estilo {@code Song ENG}</b> (99,1%): a letra inteira do ED de 13 episódios,
     * restaurada de um cache da era ANTERIOR ao veto e com erro visível
     * ({@code "you were watching"} virou {@code "Estamos assistindo"}). O console anunciava
     * {@code [CACHE/RECUPERADO]}, que soa como coisa boa.
     *
     * <p>A causa: aqui só se perguntava {@code evento.isDialogo()}, que responde "a linha é
     * {@code Dialogue:}" — e NÃO "o estilo é diálogo". A tela declarava veto absoluto de música na
     * auditoria e furava a própria invariante nesta ponte, que roda ANTES dela.
     */
    @Test
    void naoRestauraDoCacheUmaFalaDeEstiloMusical() {
        DocumentoLegenda documento = new DocumentoLegenda("", List.of(
            new EventoLegenda(1, "Dialogue", "Song ENG", "", "I was watching you as you,"),
            new EventoLegenda(2, "Dialogue", "Default", "", "Texto antigo")), "\n", false);
        List<EntradaCache> entradas = List.of(
            new EntradaCache(1, "Song ENG", "I was watching you as you,", "Eu estava observando você", "en", "pt-br"),
            new EntradaCache(2, "Default", "Texto antigo", "Texto novo", "en", "pt-br"));

        var r = new SincronizadorLegendaCacheService(
            new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()),
            new org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico())
            .sincronizar(documento, entradas, true);

        assertEquals("I was watching you as you,", r.documento().eventos().get(0).texto(),
            "música é veto absoluto: a letra fica como está no espelho em inglês até a 4.1 tratá-la");
        assertEquals("Texto novo", r.documento().eventos().get(1).texto(),
            "e o diálogo ao lado continua sendo sincronizado — o veto é de música, não paralisia");
        assertEquals(1, r.total(), "só a fala de diálogo pode contar como sincronizada");
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

        var resultado = new SincronizadorLegendaCacheService(new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()), new org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico()).sincronizar(
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

        var resultado = new SincronizadorLegendaCacheService(new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()), new org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico()).sincronizar(documento, entradas, false);

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

        var resultado = new SincronizadorLegendaCacheService(new org.traducao.projeto.legenda.domain.PoliticaEstiloMusical(java.util.List.of()), new org.traducao.projeto.qualidadeTraducao.application.RemovedorItalico()).sincronizar(
            documento, entradas, false, Set.of(59));

        assertEquals(1, resultado.total());
        assertEquals(List.of(60), resultado.indicesRecuperadosDoOriginal());
        assertEquals("Jona! Michele!", resultado.documento().eventos().get(0).texto());
        assertEquals("Ajude-me!", resultado.documento().eventos().get(1).texto());
    }
}
