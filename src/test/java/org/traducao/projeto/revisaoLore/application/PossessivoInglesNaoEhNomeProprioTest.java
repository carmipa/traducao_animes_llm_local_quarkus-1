package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o possessivo inglês ({@code Our}, {@code His}…) é capitalizado por
 * POSIÇÃO na frase, não por ser nome. Tratá-lo como parte do nome próprio fazia a tela 3.2
 * acusar tradução correta — e, pior, <b>impedir um conserto real</b>.
 *
 * <h2>Os dois danos, medidos na corrida do 86 em 17/08/2026</h2>
 * <ul>
 *   <li><b>Bloqueava conserto.</b> ep10 ev.149: EN {@code "Our Reaper was lonely"}, PT
 *       {@code "Nosso Juggernaut estava solitário"} — o apelido do piloto trocado pelo nome do
 *       MECHA. O corretor determinístico consertava para {@code "Nosso Reaper"}, a re-auditoria
 *       acusava de novo ({@code "Our Reaper"} tem duas partes e o PT só tem uma), e a correção era
 *       DESCARTADA. A legenda ficava errada com a tela funcionando "como projetada".</li>
 *   <li><b>Criava falso positivo.</b> {@code "His Juggernaut"} → {@code "Seu Juggernaut"} está
 *       certo e era acusado pelo mesmo caminho.</li>
 * </ul>
 * Medido: 82 acusações de composto no 86, <b>2</b> por possessivo — uma de cada tipo.
 *
 * <h2>Invariantes do domínio</h2>
 * O contra-teste é obrigatório e está no mesmo arquivo: o nome próprio composto SEM possessivo
 * tem de continuar sendo acusado. Sem ele, apagar a regra inteira passaria neste teste.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual dos dois lados quebrou e o que o operador veria na legenda.
 */
class PossessivoInglesNaoEhNomeProprioTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    @Test
    @DisplayName("o conserto de 'Nosso Juggernaut' para 'Nosso Reaper' passa a ser aceito")
    void possessivoNaoBloqueiaConsertoReal() {
        ResultadoDeteccaoLore depoisDoConserto = detector.auditar(
            "Our Reaper was lonely, wasn't he?", "Nosso Reaper estava solitario, nao foi?");

        assertFalse(depoisDoConserto.suspeito(), () ->
            "a fala JA CONSERTADA continua acusada, entao a tela descarta a correcao e a legenda "
                + "fica com 'Nosso Juggernaut' — o apelido do piloto no lugar do nome do mecha. "
                + "Motivos: " + depoisDoConserto.motivos());
    }

    @Test
    @DisplayName("'His Juggernaut' -> 'Seu Juggernaut' deixa de ser falso positivo")
    void possessivoNaoCriaFalsoPositivo() {
        ResultadoDeteccaoLore r = detector.auditar(
            "His Juggernaut's still here,", "Seu Juggernaut ainda esta aqui,");

        assertFalse(r.suspeito(), () ->
            "traducao CORRETA sendo acusada: 'His' vira 'Seu' e o detector cobra as duas partes "
                + "do 'nome composto'. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: nome composto SEM possessivo continua sendo acusado")
    void nomeCompostoDeVerdadeContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Captain Nouzen is here.", "Capitao Cha esta aqui.");

        assertTrue(r.suspeito(),
            "o detector ficou CEGO para nome composto quebrado — 'Captain Nouzen' com o sobrenome "
                + "trocado tem de acusar. Sem esta assercao, apagar a regra inteira passaria nos "
                + "dois testes acima.");
    }
}
