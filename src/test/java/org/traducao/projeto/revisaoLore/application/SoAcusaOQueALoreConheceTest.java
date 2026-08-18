package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a tela 3.2 só tem o direito de acusar o que a LORE DA OBRA conhece.
 * Acusação que ninguém consegue decidir não é achado — é ruído, e ruído esconde achado.
 *
 * <h2>A decisão de Paulo, 18/08/2026: inverter o padrão da tela</h2>
 * Antes ela acusava tudo que parecia nome próprio e pedia ao LLM que consertasse. Os números do
 * dia, já com os consertos de patente e letreiro aplicados:
 * <pre>
 *   4.177  acusacoes com termo nomeado
 *   1.509  (36,1%)  a lore da obra conhece o termo  -> da para decidir
 *   2.668  (63,9%)  a lore nao conhece             -> nao da, por construcao
 * </pre>
 * Entre os que a lore não conhece estava {@code "Then I'll"} — uma contração inglesa tratada
 * como nome próprio.
 *
 * <p>E do lado do modelo, na mesma corrida: sete obras, <b>zero</b> correções aceitas, e quando
 * ele tentava, piorava — propôs {@code "Equipe 08"} onde o inglês dizia {@code "06th Team"}, e
 * {@code "Terry Sanders Jr."} onde só havia {@code "Sanders"}. O portão recusou as duas. Mais
 * LLM não produz conserto de lore; a pergunta é que estava errada.
 *
 * <h2>Invariantes do domínio</h2>
 * Os contra-testes são a metade que importa. Sem eles, "não acusar nada" passaria em todos os
 * testes positivos — e uma tela muda é exatamente o que a regra dos três estados proíbe.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz se a tela ficou surda (deixou de ver lore de verdade) ou tagarela (voltou a
 * acusar o que ninguém decide).
 */
class SoAcusaOQueALoreConheceTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    /** Lore mínima de uma obra, no formato que o catálogo entrega: texto corrido em minúsculas. */
    private static final String LORE_DO_86 =
        "nomes canonicos: shinei nouzen, vladilena milize, spearhead squadron, juggernaut, "
            + "san magnolia, republica de san magnolia, legion, handler one";

    @Test
    @DisplayName("nome composto que a lore NAO conhece deixa de ser acusado")
    void nomeForaDaLoreNaoEhAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "We opened the Laplace Box together.",
            "Nos abrimos a Caixa de Laplace juntos.",
            LORE_DO_86);

        assertFalse(r.suspeito(), () ->
            "a tela acusou um termo que a lore desta obra nao conhece. Isso e ruido por "
                + "construcao: ninguem — nem o modelo — tem como decidir. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("contracao inglesa nunca mais e tratada como nome proprio")
    void contracaoInglesaNaoEhNomeProprio() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Then I'll check it out myself.",
            "Entao eu mesmo vou conferir.",
            LORE_DO_86);

        assertFalse(r.suspeito(), () ->
            "\"Then I'll\" foi acusado como nome proprio 19 vezes na corrida de 18/08. Motivos: "
                + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: nome que a lore CONHECE continua sendo acusado")
    void nomeDaLoreContinuaAcusado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "That was Shinei Nouzen over there.",
            "Aquele era o Shin Cha ali.",
            LORE_DO_86);

        assertTrue(r.suspeito(),
            "a tela ficou SURDA: o sobrenome de um personagem catalogado foi trocado e ela nao "
                + "viu. Sem esta assercao, calar a regra inteira passaria nos testes acima.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: basta UMA parte na lore — o composto pode quebrar justo nela")
    void bastaUmaParteConhecida() {
        ResultadoDeteccaoLore r = detector.auditar(
            "The Spearhead Squadron is moving out.",
            "O Esquadrao de Ponta esta saindo.",
            LORE_DO_86);

        assertTrue(r.suspeito(),
            "a lore conhece 'Spearhead'; exigir o composto INTEIRO cegaria a tela justamente "
                + "quando a parte catalogada e a que foi traduzida.");
    }

    @Test
    @DisplayName("CONTRA-TESTE: sem lore informada, o comportamento antigo continua")
    void semLoreMantemComportamentoAnterior() {
        ResultadoDeteccaoLore r = detector.auditar(
            "We opened the Laplace Box together.",
            "Nos abrimos a Caixa de Laplace juntos.");

        assertTrue(r.suspeito(),
            "sem lore informada a tela nao pode ficar muda — quem chama sem obra ativa nao esta "
                + "declarando que nada importa. E a mesma convencao de loreMenciona().");
    }
}
