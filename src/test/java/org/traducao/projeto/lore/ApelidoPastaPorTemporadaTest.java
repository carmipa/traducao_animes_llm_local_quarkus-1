package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que uma pasta de TEMPORADA identifica exatamente
 * UMA lore — nem zero (portão cego), nem várias (portão bloqueando por ambiguidade).
 *
 * <h2>O prejuízo que originou</h2>
 * Medido em 07/08/2026: <b>12 das 24 pastas de legenda do acervo têm um nível de
 * temporada</b> entre a obra e a legenda. A obra derivada do caminho saía
 * {@code "Season 05"}, e disso dependem duas coisas:
 * <ul>
 *   <li>o portão obra×contexto, que não reconhecia nada e se declarava CEGO —
 *       avisava e seguia, então uma lore errada passaria sem bloqueio;</li>
 *   <li>o diretório de cache. Já existe {@code cache/Season 1/} com 22 arquivos
 *       do Unicorn, e o DanMachi Season 01 cairia na mesma pasta.</li>
 * </ul>
 *
 * <h2>Por que o apelido é a frase inteira, e não só "DanMachi"</h2>
 * O reconhecimento casa a frase canônica contida no nome da pasta, e o veredicto
 * do portão usa a MAIOR especificidade. Se as oito lores do DanMachi declarassem
 * apenas {@code "DanMachi"}, todas reivindicariam a mesma pasta com o mesmo peso
 * — veredicto AMBÍGUO, que <b>bloqueia</b> a tradução. Seria trocar um portão
 * cego por um portão fechado.
 */
class ApelidoPastaPorTemporadaTest {

    private static final List<ProvedorContexto> DANMACHI = List.of(
        org.traducao.projeto.lore.LoreDeTeste.obra("danmachi"), org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_s1"), org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_s2"),
        org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_s3"), org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_s4"), org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_s5"),
        org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_so"), org.traducao.projeto.lore.LoreDeTeste.obra("danmachi_movie"));

    private static GerenciadorContexto acervo() {
        return new GerenciadorContexto(DANMACHI);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o teste central. Cada pasta de temporada resolve para
     * a lore daquela temporada, e para nenhuma outra.
     */
    @Test
    @DisplayName("cada pasta de temporada resolve para EXATAMENTE uma lore")
    void cadaTemporadaResolveParaUmaLore() {
        GerenciadorContexto g = acervo();

        assertEquals(Set.of("danmachi_s1"), g.idsQueReconhecem("DanMachi Season 01"));
        assertEquals(Set.of("danmachi_s2"), g.idsQueReconhecem("DanMachi Season 02"));
        assertEquals(Set.of("danmachi_s3"), g.idsQueReconhecem("DanMachi Season 03"));
        assertEquals(Set.of("danmachi_s4"), g.idsQueReconhecem("DanMachi Season 04"));
        assertEquals(Set.of("danmachi_s5"), g.idsQueReconhecem("DanMachi Season 05"));
        assertEquals(Set.of("danmachi_so"), g.idsQueReconhecem("DanMachi Sword Oratoria"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE do desenho. Se o apelido fosse genérico,
     * as oito lores reivindicariam a mesma pasta e o portão passaria a BLOQUEAR
     * por ambiguidade — pior que o aviso de hoje.
     */
    @Test
    @DisplayName("nenhuma pasta de temporada e reivindicada por mais de uma lore")
    void nenhumaPastaEhAmbigua() {
        GerenciadorContexto g = acervo();

        for (String pasta : List.of("DanMachi Season 01", "DanMachi Season 02",
                "DanMachi Season 03", "DanMachi Season 04", "DanMachi Season 05",
                "DanMachi Sword Oratoria", "DanMachi Arrow of the Orion")) {
            assertEquals(1, g.idsQueReconhecem(pasta).size(),
                "pasta \"" + pasta + "\" reivindicada por " + g.idsQueReconhecem(pasta)
                    + " — mais de uma lore significa veredicto AMBIGUO, que BLOQUEIA");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o nome antigo continua não sendo reconhecido, e isso
     * é correto — ele não identifica obra nenhuma. É o que torna a renomeação da
     * pasta necessária, em vez de opcional.
     */
    @Test
    @DisplayName("o nome ANTIGO da pasta segue sem identificar obra alguma")
    void nomeAntigoNaoIdentifica() {
        GerenciadorContexto g = acervo();

        assertTrue(g.idsQueReconhecem("Season 05").isEmpty(),
            "\"Season 05\" nao pode identificar lore nenhuma — qualquer obra pode ter essa pasta");
        assertTrue(g.idsQueReconhecem("Season 1").isEmpty());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o Unicorn tinha o mesmo defeito, com agravante — o
     * cache dele JÁ ESTÁ em {@code cache/Season 1/}, a pasta que colidiria.
     */
    @Test
    @DisplayName("o Unicorn identifica a pasta dele, e nao colide com DanMachi")
    void unicornIdentificaSemColidir() {
        List<ProvedorContexto> todos = new java.util.ArrayList<>(DANMACHI);
        todos.add(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_unicorn"));
        GerenciadorContexto g = new GerenciadorContexto(todos);

        assertEquals(Set.of("gundam_unicorn"), g.idsQueReconhecem("Gundam Unicorn Season 1"));
        assertEquals(Set.of("danmachi_s1"), g.idsQueReconhecem("DanMachi Season 01"));
    }

    /**
     * Sanidade do instrumento: uma pasta inventada não pode casar com nada. Sem
     * isto, um reconhecedor quebrado que devolvesse tudo faria os testes acima
     * passarem por acidente.
     */
    @Test
    @DisplayName("instrumento calibrado: pasta inventada nao casa com lore alguma")
    void pastaInventadaNaoCasa() {
        assertTrue(acervo().idsQueReconhecem("Obra Que Nunca Existiu Season 99").isEmpty());
    }
}
