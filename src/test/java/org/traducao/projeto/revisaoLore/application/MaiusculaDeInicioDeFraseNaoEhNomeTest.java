package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o inglês capitaliza a primeira palavra da frase. Quando o candidato a
 * nome próprio ABRE a fala, sua primeira palavra está maiúscula por POSIÇÃO — não por ser nome.
 *
 * <h2>O prejuízo, medido em 18/08/2026 nas sete obras</h2>
 * <pre>
 *   3.563 acusacoes com termo nomeado
 *     707 (19,8%) eram compostos que COMECAM a frase
 * </pre>
 * O que a tela chamava de "nome próprio composto preservado apenas em parte":
 * <pre>
 *   "But Lieutenant Uraki"   "Only Nina"   "Even Gundam Unit"   "Damn Cima"
 *   "Tell Kamille"   "Inform Burning"   "Think Chara"   "Will Roux"   "And Aina"
 * </pre>
 *
 * <h2>A terceira encarnação do mesmo defeito, no mesmo dia</h2>
 * Primeiro o possessivo ({@code "Our Reaper"}), depois a patente ({@code "Ensign Keith"}), agora
 * a abertura de frase. As duas primeiras foram resolvidas com alternância de palavras — e
 * alternância sempre tem a próxima palavra que falta ({@code But}, {@code Damn}, {@code Only},
 * {@code Inform}… não acabam). A POSIÇÃO não tem cauda: ou o candidato abre a frase, ou não.
 *
 * <h2>Invariantes do domínio</h2>
 * O contra-teste do {@code "Even Gundam Unit"} é o que impede a regra de virar dano: a palavra
 * que a lore conhece FICA. Sem ele, a regra comeria o nome exatamente na fala que mais importa —
 * a que começa com ele.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz se a tela voltou a acusar abertura de frase, ou se passou a comer nome de lore.
 */
class MaiusculaDeInicioDeFraseNaoEhNomeTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    private static final String LORE_UC =
        "gundam, uraki, kou uraki, nina purpleton, cima garahau, gato, anavel gato, "
            + "unidade gundam, gundam unit, jaburo, albion, dendrobium, kamille, haman";

    @Test
    @DisplayName("'But Lieutenant Uraki' deixa de ser nome proprio composto")
    void conjuncaoNaAberturaSai() {
        ResultadoDeteccaoLore r = detector.auditar(
            "But Lieutenant Uraki escaped safely.",
            "Mas o Tenente Uraki escapou em seguranca.",
            LORE_UC);

        assertFalse(r.suspeito(), () ->
            "\"But\" e maiuscula de POSICAO, nao nome. Casos assim eram 707 das 3.563 acusacoes "
                + "de 18/08. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("'Damn Cima' e 'Only Nina' tambem saem")
    void adverbioEXclamacaoNaAberturaSaem() {
        ResultadoDeteccaoLore damn = detector.auditar(
            "Damn Cima, she failed!", "Maldita Cima, ela falhou!", LORE_UC);
        assertFalse(damn.suspeito(), () -> "Motivos: " + damn.motivos());

        ResultadoDeteccaoLore only = detector.auditar(
            "Only Nina and the captain know what it really is.",
            "So a Nina e o capitao sabem o que e de verdade.", LORE_UC);
        assertFalse(only.suspeito(), () -> "Motivos: " + only.motivos());
    }

    /**
     * A fixture é escolhida para DISCRIMINAR: o português preserva a segunda palavra e perde a
     * primeira, que é a de lore.
     * <pre>
     *   com a protecao da lore   termo = "Gundam Alex", o PT nao tem "Gundam"  -> ACUSA
     *   SEM a protecao           termo = "Alex", e o PT tem "Alex"             -> cala
     * </pre>
     * A primeira versão deste contra-teste usava {@code "Gundam Unit 03"} e passava nos DOIS
     * mundos — com e sem a proteção —, por caminhos diferentes. Controle que não distingue os
     * dois mundos não prova nada, e já me enganou uma vez hoje (o {@code "Captain Nouzen"}).
     */
    @Test
    @DisplayName("CONTRA-TESTE: palavra que a LORE conhece na abertura FICA")
    void nomeDeLoreNaAberturaNaoEhComido() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Gundam Alex is here.", "O Alex esta aqui.",
            LORE_UC + ", alex, gundam alex");

        assertTrue(r.suspeito(),
            "a regra comeu o nome de lore na abertura da fala: sobrou \"Alex\", que o portugues "
                + "preservou, e a acusacao morreu — mas foi o \"Gundam\" que se perdeu. So a "
                + "maiuscula de POSICAO sai, e ela nunca e um termo catalogado.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: no MEIO da frase nada muda")
    void noMeioDaFraseNadaMuda() {
        ResultadoDeteccaoLore r = detector.auditar(
            "I saw that Kou Uraki was there.", "Eu vi que o Chuck estava la.", LORE_UC);

        assertTrue(r.suspeito(),
            "fora da abertura, a maiuscula so pode vir de nome — e aqui o sobrenome foi trocado. "
                + "A regra vale para a POSICAO INICIAL e mais nada.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: nome de duas palavras na abertura, com a 1a fora da lore, ainda acusa a 2a")
    void restoDoNomeContinuaSendoConferido() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Damn Kamille, where did he go?", "Maldito Amuro, para onde ele foi?", LORE_UC);

        assertTrue(r.suspeito(),
            "sai so \"Damn\"; \"Kamille\" continua sob conferencia — e aqui foi trocado por outro "
                + "personagem. A regra tira a maiuscula de posicao, nao a vigilancia sobre o nome.");
    }
}
