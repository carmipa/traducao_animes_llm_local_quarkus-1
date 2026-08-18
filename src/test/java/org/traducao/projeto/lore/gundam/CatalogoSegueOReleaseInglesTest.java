package org.traducao.projeto.lore.gundam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: quando o catálogo e o release em inglês discordam sobre a grafia de um
 * nome, quem manda é o release — é dele que a tradução parte e é ele que o espectador tem como
 * original.
 *
 * <h2>O caso que originou este teste — 18/08/2026</h2>
 * A tela acusou {@code Kelley} 15 vezes no Gundam 0083 e a leitura fácil foi "a tradução
 * escreveu errado". Medindo o acervo, o contrário:
 * <pre>
 *   "Kelley"  no release EN: 26 falas   no PT entregue:  4
 *   "Kelly"   no release EN:  0 falas   no PT entregue: 21
 * </pre>
 * O {@code lore.yaml} declarava {@code "Kelly Layzner"}, a tradução seguiu o catálogo, e o
 * catálogo é que divergia da fonte. O defeito estava três camadas acima de onde apareceu.
 *
 * <p>Corrigido na raiz — {@code termosProtegidos} e prompt — mais uma entrada em
 * {@code correcoesTerminologia} para as 21 falas já entregues.
 *
 * <h2>Por que uma guarda, e não só o conserto</h2>
 * O nome aparecia em TRÊS lugares do yaml (lista de termos, prosa do prompt, elenco). Corrigir um
 * e esquecer os outros deixa o catálogo discordando de si mesmo, e a próxima corrida acusa de
 * novo sem ninguém entender por quê.
 *
 * <h2>Invariantes do domínio</h2>
 * O contra-teste fixa a forma ERRADA como ausente: sem ele, apagar o nome inteiro do catálogo
 * passaria neste teste.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual grafia voltou e onde o release em inglês foi conferido.
 */
class CatalogoSegueOReleaseInglesTest {

    private static final String OBRA = "gundam_0083";

    @Test
    @DisplayName("o 0083 declara 'Kelley Layzner', a grafia do release em ingles")
    void declaraAGrafiaDoRelease() {
        Set<String> protegidos =
            org.traducao.projeto.lore.LoreDeTeste.obra(OBRA).termosProtegidos();

        assertTrue(protegidos.contains("Kelley Layzner"),
            "o release em ingles do 0083 escreve \"Kelley\" em 26 falas e \"Kelly\" em nenhuma. "
                + "O catalogo declarava \"Kelly Layzner\" e a traducao o seguiu — 21 falas "
                + "entregues com a grafia errada, e a tela acusando a traducao por um defeito "
                + "que era do catalogo.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: a grafia antiga nao pode voltar")
    void aGrafiaAntigaNaoVolta() {
        Set<String> protegidos =
            org.traducao.projeto.lore.LoreDeTeste.obra(OBRA).termosProtegidos();

        assertFalse(protegidos.contains("Kelly Layzner"),
            "as duas grafias no mesmo catalogo fazem o corretor deterministico trocar uma pela "
                + "outra conforme a ordem — e o nome aparecia em TRES lugares do yaml, entao "
                + "corrigir um e esquecer os outros deixa o catalogo discordando de si mesmo");
    }

    @Test
    @DisplayName("as falas ja entregues sao corrigidas por regra")
    void asFalasEntreguesSaoCorrigidas() {
        Map<String, String> correcoes =
            org.traducao.projeto.lore.LoreDeTeste.obra(OBRA).correcoesTerminologia();

        assertTrue("Kelley".equals(correcoes.get("Kelly")),
            "sem esta entrada, as 21 falas entregues com \"Kelly\" ficam como estao e a tela "
                + "acusa para sempre. O corretor so dispara onde o INGLES da fala tem \"Kelley\", "
                + "entao a troca nao alcanca outro Kelly que porventura exista.");
    }
}
