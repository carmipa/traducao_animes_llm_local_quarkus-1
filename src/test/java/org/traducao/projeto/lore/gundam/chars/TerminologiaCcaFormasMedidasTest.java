package org.traducao.projeto.lore.gundam.chars;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela as formas-ruim REAIS que o LLM produziu ao traduzir
 * Char's Counterattack em 2026-07-30 — já com a lore completa e os termos em
 * {@code termosProtegidos()}. Cada caso aqui saiu de uma fala do filme, não de suposição.
 *
 * <p>Existe porque a medição mostrou que {@code termosProtegidos()} NÃO impede a
 * localização: aquele conjunto é permissivo (os validadores removem o termo do texto
 * antes de procurar resíduo em inglês), e quem garante a grafia canônica é o
 * {@link EnforcadorTermosLore} lendo {@code correcoesTerminologia()}.
 *
 * <p>INVARIANTES DO DOMÍNIO: o enforcer só restaura quando o texto ORIGINAL (EN) contém
 * o canônico — por isso "Londres" pode virar "Londenion" numa fala que fale de Londenion
 * e permanece Londres numa que fale da capital inglesa.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer forma-ruim medida que volte a passar
 * reprova a suíte.
 */
class TerminologiaCcaFormasMedidasTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();
    private final Map<String, String> correcoes =
        org.traducao.projeto.lore.LoreDeTeste.obra("gundam_cca").correcoesTerminologia();

    private String reforcar(String original, String traduzido) {
        return enforcador.reforcar(original, traduzido, correcoes);
    }

    @Test
    @DisplayName("Fifth Luna: perdido em 7 de 7 falas — o asteroide virava substantivo comum")
    void restauraFifthLuna() {
        assertEquals("Ângulo de entrada da Fifth Luna, confirmado.",
            reforcar("Entry angle of Fifth Luna, check.",
                "Ângulo de entrada da Quinta Lua, confirmado."));
    }

    @Test
    @DisplayName("Luna II: perdido em 10 de 18 falas")
    void restauraLunaII() {
        assertEquals("Estou indo para a Luna II para preparar o desarmamento deles.",
            reforcar("I'm going to Luna II to prepare for their disarmament.",
                "Estou indo para a Lua II para preparar o desarmamento deles."));
    }

    @Test
    @DisplayName("Side 1 e Side 2: aglomerados coloniais viravam 'Lado'")
    void restauraSides() {
        assertEquals("Entre a Lua e o Side 1. Droga, Char!",
            reforcar("And between the Moon and Side 1. Damn Char!",
                "Entre a Lua e o Lado 1. Droga, Char!"));
        assertEquals("Side 2 iniciou seu ataque a laser.",
            reforcar("Side 2 has commenced their laser attack.",
                "Lado 2 iniciou seu ataque a laser."));
    }

    @Test
    @DisplayName("Earthsphere: as duas formas que apareceram nas 2 falas")
    void restauraEarthsphere() {
        assertEquals("Por serem a origem de todas as guerras na Earthsphere,",
            reforcar("Because they are the source of all wars in the Earthsphere,",
                "Por serem a origem de todas as guerras na Esfera da Terra,"));
        assertEquals("os fragmentos do Axis serão lançados para fora da Earthsphere",
            reforcar("the fragments of Axis will be blown clear out of the Earthsphere",
                "os fragmentos do Axis serão lançados para fora da Esfera Terrestre"));
    }

    @Test
    @DisplayName("Psyco-frame: a grafia do filme é SEM h e o modelo colava tudo")
    void restauraPsycoFrame() {
        assertEquals("É um Psyco-frame.",
            reforcar("It's a Psyco-frame.", "É um psycoframe."));
    }

    @Test
    @DisplayName("Sieg Zeon: ordem invertida em 4 de 5 falas")
    void restauraOrdemSiegZeon() {
        assertEquals("Sieg Zeon!", reforcar("Sieg Zeon!", "Zeon Sieg!"));
    }

    @Test
    @DisplayName("Londenion: três invenções distintas numa execução só")
    void restauraLondenion() {
        assertEquals("Londenion é o destino, certo?",
            reforcar("Destination is Londenion, right?", "Londem é o destino, certo?"));
        assertEquals("Chegaremos a tempo em Londenion.",
            reforcar("As long as we get to Londenion on time.",
                "Chegaremos a tempo em Londresão."));
        assertEquals("Londenion é um prazer.",
            reforcar("I'd like to welcome you to Londenion.", "Londres é um prazer."));
    }

    @Test
    @DisplayName("Metade da troca É corrigível: 'Londenion' que deveria ser 'Londo Bell'")
    void desfazLondenionQueDeveriaSerLondoBell() {
        assertEquals("Mande para o Londo Bell!",
            reforcar("Send it to the Londo Bell!", "Mande para o Londenion!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela um LIMITE do mecanismo, não um comportamento desejado.
     *
     * <p>Na fala medida {@code "Side 1's Londenion."} o modelo devolveu
     * {@code "Londo Bell's Londenion."} — o {@code "Londo Bell"} errado veio de
     * {@code "Side 1"}, não de {@code "Londenion"}. Como o {@link EnforcadorTermosLore}
     * decide pela FORMA-RUIM e não pela origem, uma entrada
     * {@code "Londo Bell" -> "Londenion"} produziria {@code "Londenion's Londenion."}:
     * troca um erro por outro. Por isso essa entrada NÃO existe no mapa.
     *
     * <p>Este teste falha se alguém a adicionar achando que completa o par.
     */
    @Test
    @DisplayName("LIMITE: substituição de Side 1 por Londo Bell não é corrigível pelo mapa")
    void trocaSide1PorLondoBellNaoEhCorrigivelPeloMapa() {
        String saida = reforcar("Side 1's Londenion.", "Londo Bell's Londenion.");
        assertEquals("Londo Bell's Londenion.", saida,
            "o mapa deve deixar a fala como está — corrigir aqui exigiria saber a origem");
        assertTrue(!correcoes.containsKey("Londo Bell"),
            "entrada 'Londo Bell' -> 'Londenion' produz 'Londenion's Londenion.'");
    }

    @Test
    @DisplayName("Londres continua Londres quando a fala é sobre a cidade")
    void naoTocaLondresForaDeContexto() {
        String fala = "Estive em Londres no ano passado.";
        assertEquals(fala, reforcar("I was in London last year.", fala),
            "sem 'Londenion' no inglês, o enforcer não pode tocar 'Londres'");
    }

    @Test
    @DisplayName("Earth Federation NÃO entra no mapa — 'Federação Terrestre' é decisão de produto")
    void federacaoTerrestrePermanece() {
        String fala = "O governo da Federação Terrestre governa as colônias.";
        assertEquals(fala,
            reforcar("The Earth Federation government rules the colonies.", fala));
        assertTrue(!correcoes.containsKey("Federação Terrestre"),
            "traduzir Earth Federation é consistente nas 7 falas e no acervo do Unicorn");
    }
}
