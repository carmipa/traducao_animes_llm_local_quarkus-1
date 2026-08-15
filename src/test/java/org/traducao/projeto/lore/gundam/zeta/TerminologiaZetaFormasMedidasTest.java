package org.traducao.projeto.lore.gundam.zeta;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela as formas-ruim REAIS medidas nas 16.778 falas de
 * Zeta Gundam — a obra com maior volume absoluto de perdas do acervo (433).
 *
 * <p>Quinta obra a receber o mapa. O caso mais grave aqui não é grafia, é SENTIDO:
 * {@code G-Defenser} (a unidade de apoio) saiu como "Super Gundam" (o Mk-II já acoplado
 * a ela) em 8 de 19 falas — dois mechas diferentes, ambos existentes na obra.
 *
 * <p>INVARIANTES DO DOMÍNIO: o {@link EnforcadorTermosLore} só age quando o texto
 * ORIGINAL contém o canônico, e foi verificado que nenhuma fala traz "G-Defenser" e
 * "Super Gundam" juntos no inglês.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer forma-ruim medida que volte a passar
 * reprova a suíte.
 */
class TerminologiaZetaFormasMedidasTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();
    private final Map<String, String> correcoes = org.traducao.projeto.lore.LoreDeTeste.obra("gundam_zeta").correcoesTerminologia();

    private String reforcar(String original, String traduzido) {
        return enforcador.reforcar(original, traduzido, correcoes);
    }

    @Test
    @DisplayName("G-Defenser não é Super Gundam — erro de sentido em 8 de 19")
    void restauraGDefenser() {
        assertEquals("Como está o G-Defenser?",
            reforcar("How's the G-Defenser coming along?", "Como está o Super Gundam?"));
    }

    @Test
    @DisplayName("Super Gundam continua Super Gundam quando é ele no inglês")
    void naoTocaSuperGundamLegitimo() {
        String fala = "O Super Gundam está pronto.";
        assertEquals(fala, reforcar("The Super Gundam is ready.", fala),
            "sem 'G-Defenser' no inglês, a guarda impede a troca");
    }

    @Test
    @DisplayName("Gate of Zedan: perdido em 30 de 30")
    void restauraGateOfZedan() {
        assertEquals("Gate of Zedan?", reforcar("Gate of Zedan?", "Portão de Zedan?"));
    }

    @Test
    @DisplayName("One Year War: 7 de 7 — o mapa do 08th já tinha, o Zeta não")
    void restauraOneYearWar() {
        assertEquals("Assumo desde os dias da One Year War.",
            reforcar("I assume from the days of the One Year War.",
                "Assumo desde os dias da Guerra de Um Ano."));
    }

    @Test
    @DisplayName("Mega Particle Cannon nas duas formas que apareceram")
    void restauraMegaParticleCannon() {
        assertEquals("Ele está usando um poderoso Mega Particle Cannon!",
            reforcar("It's using a powerful mega particle cannon!",
                "Ele está usando um poderoso mega canhão de partículas!"));
    }

    @Test
    @DisplayName("Colony 30 Incident: 4 de 4")
    void restauraColony30Incident() {
        assertEquals("O Colony 30 Incident?",
            reforcar("The Colony 30 Incident?", "O Incidente da Colônia 30?"));
    }

    @Test
    @DisplayName("Palace Athene é mobile suit, não residência")
    void restauraPalaceAthene() {
        assertEquals("Palace Athene, lançando!",
            reforcar("Palace Athene, launching!", "Palácio Atena, lançando!"));
    }

    @Test
    @DisplayName("Four é o NOME da personagem, não o número quatro")
    void restauraFourMurasame() {
        assertEquals("Four Murasame.", reforcar("Four Murasame.", "Quatro Murasame."));
    }

    @Test
    @DisplayName("A.E.U.G. NÃO entra — 'AEUG' sem pontos é consistente nas 178")
    void aeugSemPontosEhDecisaoDoPaulo() {
        String fala = "Não conhece o AEUG?";
        assertEquals(fala, reforcar("You don't know of the A.E.U.G.?!", fala),
            "178 de 211 usam a forma sem pontos; mudar reescreveria falas publicadas");
        assertTrue(!correcoes.containsKey("AEUG"),
            "é formatação, não erro de sentido — decisão de produto");
    }

    @Test
    @DisplayName("Colony Laser NÃO entra — entrada seria inerte pela caixa da fonte")
    void colonyLaserSeriaInerte() {
        assertTrue(!correcoes.containsKey("Laser de colônia"),
            "a legenda escreve 'colony Laser' com c minúsculo nas 24 ocorrências e o "
                + "enforcer exige o canônico na grafia exata — mesmo caso do Bio-Computer");
    }

    /**
     * A fala REAL que a correção online do cache devolveu em 2026-08-11, e que motivou a
     * entrada no núcleo UC. "Bright Noa" já estava no termosProtegidos e não impediu nada:
     * o texto quebrado traz "Bright" sozinho, que não casa a forma composta.
     */
    @Test
    @DisplayName("Bright é o capitão, não o adjetivo — ep02 da correção online")
    void restauraBrightDoIncidenteDaCorrecaoOnline() {
        assertEquals("Comandante Bright!",
            reforcar("Commander Bright!", "Comandante Brilhante!"));
    }

    /**
     * O OUTRO defeito do mesmo incidente (ep33) e o motivo de ele NÃO ser tarefa deste mapa:
     * o inglês diz "Right", não "Bright" — quem confundiu as duas palavras foi o tradutor
     * online. Sem o canônico no original, a condicionante manda o enforcer não encostar, e é
     * isso que tem de acontecer: restaurar aqui inventaria um personagem que a fala não cita.
     */
    @Test
    @DisplayName("ep33 fica intocado — o inglês é 'Right', a confusão foi do tradutor online")
    void naoInventaBrightOndeOOriginalDizRight() {
        String defeituoso = "- Certo. [Brilhante.";
        assertEquals(defeituoso, reforcar("{\\i1}- Right.\\N- Right.", defeituoso),
            "sem 'Bright' no original o mapa não age — este defeito é de outra etapa");
    }

    /**
     * CASO-CONTROLE da entrada acima: a forma-ruim "Brilhante" é comparada ignorando caixa,
     * então o que impede o estrago é a condicionante do original. Sem "Bright" no inglês, o
     * adjetivo legítimo tem de sair intacto — em maiúscula ou minúscula.
     */
    @Test
    @DisplayName("adjetivo 'brilhante' fica intacto quando não há Bright no inglês")
    void naoTocaBrilhanteLegitimo() {
        String fala = "Que ideia brilhante.";
        assertEquals(fala, reforcar("What a brilliant idea.", fala),
            "sem 'Bright' no original a guarda impede a troca");
        String inicioDeFrase = "Brilhante, Kamille!";
        assertEquals(inicioDeFrase, reforcar("Brilliant, Kamille!", inicioDeFrase),
            "maiúscula por posição não é o personagem");
    }
}
