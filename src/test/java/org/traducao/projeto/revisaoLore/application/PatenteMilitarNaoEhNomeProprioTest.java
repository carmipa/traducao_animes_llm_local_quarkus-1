package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: patente militar e tratamento ({@code Ensign}, {@code Lieutenant},
 * {@code Miss}, {@code Lady}) são CARGO, não nome. Tratá-los como parte do nome próprio fazia a
 * tela 3.2 acusar tradução correta em massa.
 *
 * <h2>O prejuízo, medido em 17/08/2026</h2>
 * Corridas de 0083, Unicorn, Zeta e ZZ: 3.413 falas mandadas ao LLM, e <b>34%</b> dos termos
 * acusados começavam por patente. Os mais repetidos: {@code Lieutenant Quattro}(125),
 * {@code Captain}(81), {@code Lieutenant Emma}(76), {@code Ensign Reccoa}(69),
 * {@code Miss Reccoa}(50), {@code Ensign Uraki}(34).
 * <p>A primeira medição disse "76,8% de acusação falsa" e estava INFLADA: procurava a patente em
 * português em qualquer ponto da fala. Refeita exigindo que ela estivesse colada ao mesmo nome,
 * caiu para 62,8%. As 368 que caíram não estavam provadas.
 *
 * <h2>A armadilha que quase cegou a tela — e por que o teste 3 é o mais importante daqui</h2>
 * Tirar a patente faz {@code "Captain Nouzen"} virar {@code "Nouzen"}, uma palavra só. E a regra
 * que ignora palavra única no INÍCIO da fala (porque o inglês capitaliza a primeira palavra por
 * posição) passaria a engolir justamente o defeito real. O conserto tem duas metades: tirar a
 * patente E mover o "início efetivo" para depois dela — {@code Keith}, em
 * {@code "Ensign Keith, ..."}, nunca esteve na posição inicial.
 *
 * <h2>Invariantes do domínio</h2>
 * Só a patente sai; o nome segue sob conferência. Lacuna declarada: as 10 divergências de
 * PATENTE medidas ({@code Ensign} → {@code Sargento}, {@code Major} → {@code Coronel}) deixam de
 * ser vistas por esta regra — patente não é nome nem lugar, e está fora do escopo da 3.2.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual metade do conserto quebrou e o que o operador veria na tela.
 */
class PatenteMilitarNaoEhNomeProprioTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    @Test
    @DisplayName("'Ensign Keith' -> 'Tenente Keith' deixa de ser acusado")
    void patenteTraduzidaNaoEhNomeQuebrado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Ensign Keith, have you been studying your piloting?",
            "Tenente Keith, voce tem estudado sua pilotagem?");

        assertFalse(r.suspeito(), () ->
            "traducao CORRETA acusada: 'Ensign' vira 'Tenente' e o detector cobra as duas partes "
                + "do 'nome composto'. Este caso sozinho apareceu 34x no 0083. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("patente de DUAS palavras: 'Lt. Commander Gato'")
    void patenteDeDuasPalavrasSaiInteira() {
        // A fala real do 0083 seguia com "The Pegasus-class ship", que rende acusacao PROPRIA
        // ("Pegasus-class" vira "classe Pegasus") e nada tem a ver com patente. Fixture com ruido
        // reprova por motivo errado e manda consertar o codigo certo.
        ResultadoDeteccaoLore r = detector.auditar(
            "Lt. Commander Gato, it's here.",
            "Tenente-Comandante Gato, ela esta aqui.");

        assertFalse(r.suspeito(), () ->
            "a patente composta nao saiu inteira. Se 'Lt.' casar sozinho, sobra 'Commander Gato' "
                + "e a acusacao continua — por isso as de duas palavras vem PRIMEIRO na "
                + "alternancia. Motivos: " + r.motivos());
    }

    /**
     * O nome é <b>{@code Wexler}</b>, e a escolha foi medida. A primeira versão deste
     * contra-teste usava {@code "Captain Nouzen"} e passava nos DOIS mundos — com e sem o
     * deslocamento —, porque {@code Nouzen} está no roster de lore do detector e escapava por
     * outro caminho. Um controle que não distingue os dois mundos não prova nada.
     * <pre>
     *   com deslocamento   Captain Wexler -> acusado      Captain Nouzen -> acusado
     *   SEM deslocamento   Captain Wexler -> NAO acusado  Captain Nouzen -> acusado
     * </pre>
     */
    @Test
    @DisplayName("CONTRA-TESTE: nome trocado NO INICIO da fala continua sendo acusado")
    void nomeTrocadoLogoNoInicioContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Captain Wexler is here.", "Capitao Cha esta aqui.");

        assertTrue(r.suspeito(),
            "a tela ficou CEGA para o defeito que ela existe para pegar: sobrenome trocado por "
                + "outro personagem, logo na primeira palavra. Sem o deslocamento do inicio "
                + "efetivo, tirar a patente deixa 'Wexler' na posicao inicial e a regra da "
                + "maiuscula de frase engole a acusacao.");
    }

    /**
     * A patente SOZINHA, sem nome atrás. Sobraram <b>21</b> destas na corrida do 0083 depois que
     * a patente com nome saiu: o removedor de prefixo exige espaço e algo depois, então não
     * alcança a palavra solta. É o vocativo, e o português resolve com {@code "Tenente"}.
     */
    @Test
    @DisplayName("patente SOZINHA no vocativo tambem deixa de ser acusada")
    void patenteSolteiraNaoEhNomeProprio() {
        ResultadoDeteccaoLore r = detector.auditar(
            "All right! When do we get to see them, Lieutenant?!",
            "Tudo bem! Quando vamos ver eles, Tenente?!");

        assertFalse(r.suspeito(), () ->
            "patente sem nome atras e CARGO, nunca nome proprio. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: nome que sumiu do portugues continua sendo acusado")
    void nomeAusenteNoPortuguesContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "I told Lieutenant Burning about the plan.",
            "Eu contei ao Tenente sobre o plano.");

        assertTrue(r.suspeito(),
            "so a patente pode sair. Aqui o NOME 'Burning' sumiu do portugues — sao as 126 "
                + "acusacoes medidas como 'o nome sumiu do PT', que precisam continuar de pe.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: palavra comum com cara de patente nao vira gatilho")
    void nomeCompostoSemPatenteContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "The Laplace Box was hidden there.", "A Caixa de Laplace estava escondida ali.");

        assertTrue(r.suspeito(),
            "nome composto SEM patente tem de continuar acusado. Sem esta assercao, afrouxar a "
                + "regra inteira passaria nos testes acima.");
    }
}
