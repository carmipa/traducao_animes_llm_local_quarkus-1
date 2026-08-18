package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a correção determinística troca UM termo na legenda entregue. Tudo o
 * mais — tags de posicionamento, quebra {@code \N}, pontuação, o resto da frase — tem de sair
 * byte a byte igual. É o invariante que separa "corrigir" de "reescrever".
 *
 * <h2>O prejuízo que justifica</h2>
 * Em 18/08/2026, na primeira corrida em que a tela escreveu, a via do LLM produziu duas falas
 * alteradas além do termo:
 * <pre>
 *   "Até a meia-noite..."            -> "Até a meia-noite... Zulu."   (apendice do modelo)
 *   "...as Anti Corpos sejam..."     -> "...as [[Anti Bodies]] sejam..." (sentinela na tela)
 * </pre>
 * A via determinística gravou 45 falas no mesmo dia e não produziu nenhuma — mas isso era
 * observação, não garantia. Este teste transforma a observação em invariante.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Tag ASS intacta: {@code \pos}, {@code \i1} e afins não podem ser tocados — legenda com
 *       typesetting perdido é dano visível.</li>
 *   <li>{@code \N} preservado: achatar a quebra muda o desenho da legenda na tela.</li>
 *   <li>Só o termo muda; o comprimento do resto não se altera.</li>
 *   <li>Contra-teste: sem o termo canônico no INGLÊS da fala, o corretor não mexe — é a regra
 *       que deixa "Londres" virar "Londenion" só onde a fala fala de Londenion.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem mostra a fala inteira antes e depois, para o que sobrou de diferente ficar visível.
 */
class CorrecaoPreservaTudoMenosOTermoTest {

    private final CorretorLoreDeterministico corretor =
        new CorretorLoreDeterministico(new EnforcadorTermosLore());

    private static final Map<String, String> CORRECOES = Map.of(
        "Voide", "Void",
        "Cárdeas", "Cardeas");

    @Test
    @DisplayName("tag ASS de posicionamento atravessa intacta")
    void tagDePosicionamentoNaoEhTocada() {
        String pt = "{\\pos(960,975)\\i1}Essa era a Voide de Inori.";
        Optional<String> saida = corretor.corrigir("{\\pos(960,975)\\i1}That was Inori's Void.",
            pt, CORRECOES);

        assertTrue(saida.isPresent(), "a correcao devia acontecer: 'Voide' esta no mapa e 'Void' "
            + "esta no ingles da fala");
        assertEquals("{\\pos(960,975)\\i1}Essa era a Void de Inori.", saida.get(),
            "so o termo pode mudar. Typesetting perdido e dano visivel na tela.");
    }

    @Test
    @DisplayName("a quebra \\N sobrevive")
    void quebraDeLinhaSobrevive() {
        Optional<String> saida = corretor.corrigir(
            "I'm the head of the family,\\NCardeas Vist.",
            "Eu sou o chefe da\\Nfamília, Cárdeas Vist.", CORRECOES);

        assertTrue(saida.isPresent());
        assertEquals("Eu sou o chefe da\\Nfamília, Cardeas Vist.", saida.get(),
            "achatar a quebra muda o desenho da legenda na tela — este caso REAL foi gravado no "
                + "Unicorn em 18/08/2026");
    }

    @Test
    @DisplayName("nada mais na fala se mexe: o resto sai igual, caractere a caractere")
    void soOTermoMuda() {
        String pt = "Então essa e a forca de um Voide? Sério mesmo?!";
        Optional<String> saida = corretor.corrigir("So that's the power of a Void? Really?!",
            pt, CORRECOES);

        assertTrue(saida.isPresent());
        String antes = pt.replace("Voide", "");
        String depois = saida.get().replace("Void", "");
        assertEquals(antes, depois,
            () -> "sobrou diferenca fora do termo.\n  antes : " + pt + "\n  depois: " + saida.get());
    }

    @Test
    @DisplayName("CONTRA-TESTE: sem o canonico no INGLES da fala, nao mexe")
    void semOCanonicoNoInglesNaoMexe() {
        Optional<String> saida = corretor.corrigir(
            "I visited London last year.",
            "Eu visitei a Voide no ano passado.", CORRECOES);

        assertFalse(saida.isPresent(),
            "o corretor so restaura quando o texto ORIGINAL contem o canonico. E essa regra que "
                + "deixa 'Londres' virar 'Londenion' numa fala sobre Londenion e permanecer "
                + "'Londres' numa fala sobre a capital inglesa. Sem ela a troca seria cega.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: fala sem a forma ruim atravessa sem correcao")
    void falaLimpaNaoEhTocada() {
        Optional<String> saida = corretor.corrigir("That was Inori's Void.",
            "Essa era a Void de Inori.", CORRECOES);

        assertFalse(saida.isPresent(),
            "fala ja correta nao pode ser 'corrigida' — isso geraria escrita infinita a cada "
                + "corrida e o operador nunca saberia quando parou de mudar");
    }
}
