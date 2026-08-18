package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o encanamento do pipeline e do modelo não pode aparecer na tela do
 * espectador. Sentinela de mascaramento, negrito de markdown e token de template são transporte,
 * nunca legenda.
 *
 * <h2>O prejuízo, medido em 18/08/2026 — na PRIMEIRA corrida em que a tela escreveu de verdade</h2>
 * Depois de um dia inteiro fechando ruído, a Revisão de Lore passou a corrigir: 58 falas gravadas
 * em quatro obras. Varrendo o acervo entregue logo depois, uma delas estava assim:
 * <pre>
 *   arquivo: Guilty Crown - 07_Track4_PT-BR.ass, evento 183
 *   EN : it's only natural that the Anti Bodies be dropped.
 *   PT : É só questão de tempo até que as [[Anti Bodies]] sejam retiradas.
 * </pre>
 * Os colchetes iriam para a tela. Uma linha em 128 mil entregues — e uma basta, porque é a que o
 * espectador vê.
 *
 * <h2>Por que a validação que já existia não pegou</h2>
 * {@code ValidadorCandidatoLoreService} compara TOKENS, e a tokenização olha palavras: ela
 * ignora colchetes e asteriscos. Então {@code "[[Anti Bodies]]"} tokenizava idêntico a
 * {@code "Anti Bodies"}, o diff dava "termo inserido que existe no inglês", e a proposta era
 * aprovada. A guarda enxergava exatamente o que não importava aqui.
 *
 * <p>Foi a via do LLM ({@code CORRIGIDA}) que produziu as duas falhas do dia; as 45 escritas da
 * via determinística ({@code CORRIGIDA_REGRA_LORE}) saíram limpas.
 *
 * <h2>Invariantes do domínio</h2>
 * O veto roda ANTES de qualquer análise de termo — não adianta discutir se o termo é de lore
 * quando o texto traz encanamento. Os contra-testes garantem que colchete e asterisco de USO
 * LEGÍTIMO na legenda continuem passando.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A proposta é recusada com o resíduo exato citado na mensagem, e a legenda fica intacta.
 */
class ResiduoDeTransporteNaoVaiParaLegendaTest {

    private static final String LORE = "anti bodies, funeral parlor, void, undertaker";

    private static Optional<String> validar(String en, String pt, String proposta) {
        return ValidadorCandidatoLoreService.validar(en, pt, proposta, LORE);
    }

    @Test
    @DisplayName("o caso REAL: [[Anti Bodies]] nao pode ser gravado")
    void sentinelaDeMascaramentoEhRecusada() {
        Optional<String> veto = validar(
            "it's only natural that the Anti Bodies be dropped.",
            "é só questão de tempo até que as Anti Corpos sejam retiradas.",
            "É só questão de tempo até que as [[Anti Bodies]] sejam retiradas.");

        assertTrue(veto.isPresent(),
            "esta proposta FOI GRAVADA no Guilty Crown ep07 em 18/08/2026. Os colchetes do "
                + "sentinela de mascaramento chegariam a tela do espectador.");
        assertTrue(veto.get().contains("[[Anti Bodies]]"),
            () -> "a recusa tem de citar o residuo achado, senao quem le o log nao sabe o que "
                + "houve. Veio: " + veto.get());
    }

    @Test
    @DisplayName("negrito de markdown tambem e recusado")
    void markdownEhRecusado() {
        assertTrue(validar("Oh, so, what are the other Egoist members like?",
            "como são os outros membros do Egoísta?",
            "como são os outros membros do **Funeral Parlor**?").isPresent(),
            "o modelo emitiu markdown em 13 propostas do dia; asterisco nao e legenda");
    }

    @Test
    @DisplayName("token de template de chat tambem e recusado")
    void tokenDeTemplateEhRecusado() {
        assertTrue(validar("That was Inori's Void.", "Essa era a Voide de Inori.",
            "Essa era a Void de Inori.<|END_OF_TURN_TOKEN|>").isPresent(),
            "o token de controle ja custou 115 de 116 falas numa execucao da traducao");
    }

    @Test
    @DisplayName("CONTRA-TESTE: a troca limpa equivalente continua sendo aceita")
    void trocaLimpaContinuaPassando() {
        Optional<String> veto = validar(
            "it's only natural that the Anti Bodies be dropped.",
            "é só questão de tempo até que as Anti Corpos sejam retiradas.",
            "É só questão de tempo até que as Anti Bodies sejam retiradas.");

        assertFalse(veto.isPresent(), () ->
            "a MESMA proposta sem os colchetes e legitima e tem de passar. Um veto que recusasse "
                + "as duas seria guarda comendo o conserto certo. Veio: " + veto);
    }

    @Test
    @DisplayName("CONTRA-TESTE: colchete simples de legenda nao e sentinela")
    void colcheteSimplesContinuaPassando() {
        // Troca MINIMA de proposito: mexer no artigo junto ("A" -> "O") faria a proposta ser
        // recusada por outra regra ("o" nao esta no ingles), e o contra-teste ficaria verde
        // pelo motivo errado — provando nada sobre o veto de residuo.
        Optional<String> veto = validar(
            "[Radio] The Void is here.",
            "[Rádio] A Alma está aqui.",
            "[Rádio] A Void está aqui.");

        assertFalse(veto.isPresent(), () ->
            "colchete SIMPLES e uso legitimo em legenda (indicacao de fonte de audio). O veto "
                + "mira o par duplo [[...]], que e o sentinela do pipeline. Veio: " + veto);
    }
}
