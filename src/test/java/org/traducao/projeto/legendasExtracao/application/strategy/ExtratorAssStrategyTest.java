package org.traducao.projeto.legendasExtracao.application.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garantir que a extração pegue a faixa de DIÁLOGO, e não a faixa
 * reduzida de letreiros e músicas que acompanha lançamento com áudio dublado.
 *
 * <h2>O caso-controle é real, não inventado</h2>
 * O Break Blade (release {@code [Lulu]}, Dual-Audio) tem exatamente duas faixas ASS:
 * <pre>
 *   id=3  nome="[Coalgirls]"    -> dialogo completo, 646 falas
 *   id=4  nome="Signs/Songs"    -> letreiros e musica, 65 falas
 * </pre>
 * Nenhum dos dois nomes casa com as palavras-chave de "faixa completa", então a escolha caía
 * na regra de posição — "a última é a completa" — e pegava a 4. Os <b>seis filmes foram
 * traduzidos inteiros da faixa errada</b>: 373 falas contra as 3.457 do diálogo real.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se estes testes ficarem verdes com o filtro negativo removido, a catraca é decorativa —
 * cada um foi visto reprovando a versão anterior da estratégia.
 */
class ExtratorAssStrategyTest {

    private final ExtratorAssStrategy strategy = new ExtratorAssStrategy();

    private static FaixaLegenda ass(int id, String nome, boolean forcada) {
        return new FaixaLegenda(id, "subtitles", "SubStationAlpha", "S_TEXT/ASS", "eng", nome, false, forcada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o caso Break Blade, com os nomes e a ORDEM reais do arquivo.
     * <p>INVARIANTES DO DOMÍNIO: a faixa de letreiro vem por último e nenhum nome declara
     * "completa" — é a combinação exata que derrotava a heurística de posição.
     * <p>COMPORTAMENTO EM CASO DE FALHA: escolher a id 4 significa traduzir 65 linhas de placa
     * achando que é o filme inteiro.
     */
    @Test
    @DisplayName("Break Blade: escolhe o dialogo mesmo com Signs/Songs por ULTIMO")
    void escolheDialogoQuandoLetreiroVemPorUltimo() {
        List<FaixaLegenda> faixas = List.of(
            ass(3, "[Coalgirls]", false),
            ass(4, "Signs/Songs", false));

        Optional<FaixaLegenda> escolhida = strategy.selecionarMelhorFaixa(faixas);

        assertTrue(escolhida.isPresent(), "deveria escolher alguma faixa");
        assertEquals(3, escolhida.get().id(),
            "escolheu a faixa de letreiros; a regra de posicao voltou a decidir sozinha");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a ordem inversa também tem de funcionar — não se troca um viés
     * de posição por outro.
     * <p>COMPORTAMENTO EM CASO DE FALHA: se só passar com o letreiro por último, a correção
     * apenas mudou de lado o mesmo erro.
     */
    @Test
    @DisplayName("letreiro PRIMEIRO: continua escolhendo o dialogo")
    void escolheDialogoQuandoLetreiroVemPrimeiro() {
        List<FaixaLegenda> faixas = List.of(
            ass(2, "Signs & Songs", false),
            ass(3, "[Coalgirls]", false));

        assertEquals(3, strategy.selecionarMelhorFaixa(faixas).orElseThrow().id());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: faixa marcada como {@code forced} é, por convenção do formato, o
     * subconjunto exibido sobre áudio dublado — mesmo quando o nome não diz nada.
     */
    @Test
    @DisplayName("faixa forced e despriorizada mesmo sem nome revelador")
    void despriorizaFaixaForcada() {
        List<FaixaLegenda> faixas = List.of(
            ass(2, "Portuguese", false),
            ass(3, "Portuguese", true));

        assertEquals(2, strategy.selecionarMelhorFaixa(faixas).orElseThrow().id());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quando o contêiner SÓ tem faixa reduzida, extrair a reduzida é
     * melhor do que falhar — recusar aqui transformaria arquivo pobre em erro de operação.
     * <p>INVARIANTES DO DOMÍNIO: é o contra-teste do filtro negativo. Sem ele, "filtrar
     * letreiro" poderia virar "nunca extrair nada" e ninguém notaria.
     */
    @Test
    @DisplayName("so ha faixa de letreiro: extrai ela mesmo assim")
    void devolveLetreiroQuandoNaoHaAlternativa() {
        List<FaixaLegenda> faixas = List.of(ass(4, "Signs/Songs", false));

        Optional<FaixaLegenda> escolhida = strategy.selecionarMelhorFaixa(faixas);

        assertTrue(escolhida.isPresent(),
            "recusou a unica faixa existente — o filtro negativo virou um bloqueio");
        assertEquals(4, escolhida.get().id());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o comportamento que já funcionava não pode regredir — o Gundam
     * 0080 tem faixa única chamada "English" e sempre foi extraído certo.
     */
    @Test
    @DisplayName("faixa unica declarada English continua sendo escolhida")
    void mantemComportamentoDeFaixaUnicaDeclarada() {
        assertEquals(2, strategy.selecionarMelhorFaixa(List.of(ass(2, "English", false)))
            .orElseThrow().id());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: palavra-chave de faixa completa vence a posição, e o filtro
     * negativo não pode engoli-la.
     */
    @Test
    @DisplayName("nome que declara 'Full Dialogue' vence a posicao")
    void palavraChaveVenceAPosicao() {
        List<FaixaLegenda> faixas = List.of(
            ass(2, "Full Dialogue", false),
            ass(3, "Commentary", false));

        assertEquals(2, strategy.selecionarMelhorFaixa(faixas).orElseThrow().id());
    }

    /** Sem faixa ASS nenhuma, não há o que escolher. */
    @Test
    @DisplayName("sem faixa ASS devolve vazio")
    void semFaixaAssDevolveVazio() {
        List<FaixaLegenda> pgs = List.of(
            new FaixaLegenda(1, "subtitles", "HDMV PGS", "S_HDMV/PGS", "eng", "English", false, false));

        assertTrue(strategy.selecionarMelhorFaixa(pgs).isEmpty());
    }
}
