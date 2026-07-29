package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: sela o REPARO da troca de entidade — desfazer em vez de descartar.
 *
 * <h2>O que custava descartar</h2>
 * Retradução do Zeta em 2026-07-29: das 59 falas que ficaram vazias, <b>22 eram troca de
 * entidade</b>, e a tradução descartada estava correta em tudo menos numa palavra. Entre elas, a
 * cena da morte da Four Murasame — o Kamille gritando o nome dela — saiu em branco:
 * <pre>
 *   EN "Answer me, Four!"       descartado "Responda-me, Quattro!"
 *   EN "Wake up, Four!"         descartado "Acorda, Quattro!"
 *   EN "Open your eyes, Four!!" descartado "Abra seus olhos, Quattro!!!"
 * </pre>
 * O portão já sabia o termo certo, o errado e a fala inteira. Faltava trocar uma palavra.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O reparo só alcança o que {@code trocou} acusaria — mesma condição, mesma guarda de
 *       preservação. Fala que o portão deixaria passar não é tocada, e é o que o teste do
 *       acréscimo legítimo afirma.</li>
 *   <li>Reverte nas DUAS direções do par, porque a proibição é simétrica.</li>
 *   <li>Reparar não é aceitar: quem chama tem de revalidar. Aqui só se testa o TEXTO produzido;
 *       a revalidação obrigatória vive em {@code AvaliadorTraducaoCache.repararSePossivel}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem o reparo, volta a apagar tradução boa por uma palavra. Com reparo largo demais, o portão
 * vira porta dos fundos e entidade trocada entra no cache disfarçada de conserto.
 */
class ValidadorReparoTrocaDeEntidadeTest {

    @Test
    @DisplayName("desfaz a troca preservando o resto da fala — os casos reais do Zeta")
    void desfazTrocaPreservandoOResto() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Four", "Quattro")));

        assertEquals("Responda-me, Four!",
            v.repararTrocaDeEntidade("Answer me, Four!", "Responda-me, Quattro!"));
        assertEquals("Acorda, Four!",
            v.repararTrocaDeEntidade("Wake up, Four!", "Acorda, Quattro!"));
        assertEquals("Abra seus olhos, Four!!!",
            v.repararTrocaDeEntidade("Open your eyes, Four!!", "Abra seus olhos, Quattro!!!"));
    }

    /** Todas as ocorrências, não só a primeira: "Four! Don't go! Four!" tem duas. */
    @Test
    @DisplayName("troca TODAS as ocorrências na mesma fala")
    void trocaTodasAsOcorrencias() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Four", "Quattro")));

        assertEquals("Four! Não vá! Four!",
            v.repararTrocaDeEntidade("Four! Don't go! Four!", "Quattro! Não vá! Quattro!"));
    }

    @Test
    @DisplayName("reverte nas duas direções do par")
    void reverteNasDuasDirecoes() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Char", "Quattro")));

        assertEquals("Rendezvous com Char",
            v.repararTrocaDeEntidade("Rendezvous with Char", "Rendezvous com Quattro"));
        assertEquals("Quattro Bajeena chegou",
            v.repararTrocaDeEntidade("Quattro Bajeena has arrived", "Char Bajeena chegou"));
    }

    /**
     * O vizinho que impede o reparo de virar porta dos fundos: quando o nome do original
     * SOBREVIVEU, não houve troca, e mexer no texto seria inventar defeito onde não há.
     * "Sanders the Reaper" -> "Sanders, o Ceifador" é tradução certa.
     */
    @Test
    @DisplayName("acréscimo legítimo não é reparado — o nome do original sobreviveu")
    void acrescimoLegitimoNaoEhReparado() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Sanders", "Ceifador")));

        assertNull(v.repararTrocaDeEntidade(
            "So, you're the team killer, Sanders the Reaper.", "Sanders, o Ceifador."));
    }

    /**
     * "Nahel Argama" CONTÉM "Argama". O reparo herda a guarda de preservação de {@code trocou},
     * então a menção legítima à sucessora não vira "conserto" que a rebaixa para a nave anterior.
     */
    @Test
    @DisplayName("termo que é substring do outro não é reparado a si mesmo")
    void substringNaoEhReparadaASiMesma() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Argama", "Nahel Argama")));

        assertNull(v.repararTrocaDeEntidade("Repair the Nahel Argama.", "Reparem a Nahel Argama."));
        // E a troca de verdade continua sendo revertida.
        assertEquals("Reparem a Argama.",
            v.repararTrocaDeEntidade("Repair the Argama.", "Reparem a Nahel Argama."));
    }

    @Test
    @DisplayName("sem par declarado, nada é reparado")
    void semParDeclaradoNadaEhReparado() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.vazia());

        assertNull(v.repararTrocaDeEntidade("Answer me, Four!", "Responda-me, Quattro!"));
    }

    /**
     * LIMITE DECLARADO, e o teste existe para que ele seja uma decisão e não uma surpresa: o
     * reparo troca a palavra, não a concordância em volta. Four Murasame é mulher, e o artigo
     * masculino que o modelo pôs por causa de "Quattro" permanece.
     *
     * <p>Fica errado de gênero e certo de identidade — antes ficava vazio. Consertar o artigo
     * exigiria o gênero de cada termo do par, que a lore declara em texto livre e não em campo.
     */
    @Test
    @DisplayName("LIMITE: o reparo não corrige o artigo em volta do nome")
    void reparoNaoCorrigeArtigo() {
        var v = new ValidadorTraducaoService(LoreAtivaFake.comPares(List.of("Four", "Quattro")));

        assertEquals("Somente o Four pode abrir aquela porta.",
            v.repararTrocaDeEntidade(
                "Only Four can open that door.", "Somente o Quattro pode abrir aquela porta."),
            "o nome fica certo e o artigo continua masculino — limite conhecido e aceito");
    }
}
