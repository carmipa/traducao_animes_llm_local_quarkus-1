package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: uma palavra SOZINHA no início da fala só conta como indício de lore se
 * estiver num roster ESTREITO — não basta a lore da obra mencioná-la. Este teste existe para
 * impedir que o roster seja "limpado" por parecer duplicata.
 *
 * <h2>O engano que este teste previne, medido em 18/08/2026</h2>
 * Olhando de fora, {@code TERMOS_LORE_SOLTEIROS_RELEVANTES} parece dívida: 93 dos seus 94 termos
 * também estão no {@code lore.yaml}, e a checagem é
 * {@code roster.contains(t) && loreMenciona(t)}. A conclusão fácil — e errada — é que o roster
 * duplica o catálogo e podia sair.
 *
 * <p>Ele não duplica: ele ESTREITA. O campo de lore da obra inclui a PROSA DO PROMPT. Medido no
 * bloco do Zeta:
 * <pre>
 *   488 palavras distintas de 4+ letras na lore
 *   470 delas FORA do roster
 *   entre elas: "abaixo", "aceite", "adjetivos", "ajustar", "altere", "anglicizados",
 *               e os comuns "guerra", "terra", "base", "colonia"
 * </pre>
 * Trocar o roster por "a lore menciona" transformaria 470 palavras de instrução em indicadores
 * de lore válidos, e as acusações EXPLODIRIAM. O caminho certo, se um dia se quiser tirar o
 * roster do código, é um campo PRÓPRIO no yaml — nunca reusar o texto inteiro da lore.
 *
 * <h2>Invariantes do domínio</h2>
 * O par de testes tem de discriminar: a palavra de prosa cala, e a palavra do roster acusa,
 * com a MESMA lore e a mesma forma de fala. Um teste que só verificasse o lado que cala ficaria
 * verde com a regra inteira apagada.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual dos dois lados quebrou e o que aconteceria numa corrida real.
 */
class RosterSolteiroEhEstreitoDePropositoTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    /** Lore com PROSA de prompt, que é como o campo real chega — não uma lista limpa de nomes. */
    private static final String LORE_COM_PROSA =
        "Traduza preservando os nomes canonicos. Nao altere termos anglicizados. "
            + "Ajuste os adjetivos ao portugues. Premissa: a guerra entre a Terra e as colonias; "
            + "a base de Jaburo; o piloto Amuro e o mobile suit Gundam.";

    @Test
    @DisplayName("palavra de PROSA da lore no inicio da fala nao vira indicio de lore")
    void palavraDeProsaNaoEhIndicio() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Terra firma operations are underway.",
            "As operacoes em solo firme estao em andamento.",
            LORE_COM_PROSA);

        assertFalse(r.suspeito(), () ->
            "\"Terra\" aparece na PROSA do prompt desta obra, e passou a contar como indicio de "
                + "lore. No bloco do Zeta sao 470 palavras assim — a tela acusaria instrucao de "
                + "prompt como nome proprio. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: palavra DO ROSTER no inicio da fala continua acusando")
    void palavraDoRosterContinuaSendoIndicio() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Gundam is our only hope.",
            "O Amuro e nossa unica esperanca.",
            LORE_COM_PROSA);

        assertTrue(r.suspeito(),
            "a regra do indicio solteiro foi apagada: \"Gundam\" esta no roster E na lore da "
                + "obra, e o portugues o trocou por outro nome. Sem esta assercao, calar tudo "
                + "passaria no teste acima.");
    }
}
