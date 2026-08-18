package org.traducao.projeto.core.texto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o token de template do servidor de LLM não pode chegar ao domínio. Duas
 * fatias já perderam trabalho por isso, com sete dias de intervalo.
 *
 * <h2>Os dois prejuízos medidos</h2>
 * <ul>
 *   <li><b>11/08/2026, tradução:</b> 115 das 116 falas perdidas numa execução (99%) eram
 *       {@code <|END_OF_TURN_TOKEN|>} colado numa tradução CORRETA.</li>
 *   <li><b>18/08/2026, Revisão de Lore:</b> 4.903 propostas recusadas, 100% com o token, e em
 *       2.616 delas (53,4%) o token era a ÚNICA diferença — o modelo não mudara nada e o
 *       validador leu o token como "termo que não existe no inglês". Sete obras fecharam com
 *       "Falas corrigidas: 0".</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * Os contra-testes são a metade que importa: {@code </s>} e texto com {@code <} legítimo têm de
 * atravessar intactos. Guarda que apaga conteúdo legítimo é pior que guarda nenhuma.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz qual família de token escapou ou qual conteúdo legítimo foi comido.
 */
class TokenDeControleLlmTest {

    @Test
    @DisplayName("o caso REAL da Revisao de Lore: o token era a unica diferenca")
    void tiraOTokenQueEraAUnicaDiferenca() {
        String daAuditoria = "Desista, Chuck.<|END_OF_TURN_TOKEN|>";

        assertEquals("Desista, Chuck.", TokenDeControleLlm.limpar(daAuditoria),
            "esta linha veio da auditoria de 18/08. Com o token, o validador de escopo ve um "
                + "termo inserido que nao existe no ingles e recusa a proposta — foram 2.616 assim "
                + "num dia so.");
    }

    @Test
    @DisplayName("as familias de token que os modelos em uso emitem")
    void tiraAsFamiliasConhecidas() {
        assertEquals("ok", TokenDeControleLlm.limpar("ok<|END_OF_TURN_TOKEN|>"), "Cohere/aya");
        assertEquals("ok", TokenDeControleLlm.limpar("ok<|im_end|>"), "ChatML");
        assertEquals("ok", TokenDeControleLlm.limpar("ok<|eot_id|>"), "Llama");
        assertEquals("ok", TokenDeControleLlm.limpar("ok<|endoftext|>"), "GPT");
        assertEquals("ok", TokenDeControleLlm.limpar("<start_of_turn>ok<end_of_turn>"), "Gemma");
    }

    @Test
    @DisplayName("CONTRA-TESTE: </s> e conteudo legitimo com < atravessam intactos")
    void naoComeConteudoLegitimo() {
        assertEquals("fim</s>", TokenDeControleLlm.limpar("fim</s>"),
            "</s> e ambiguo com marcacao de texto e nenhum modelo em uso o emite. Apagar aqui "
                + "seria guarda comendo conteudo.");
        assertEquals("5 < 7 e verdade", TokenDeControleLlm.limpar("5 < 7 e verdade"),
            "texto com sinal de menor tem de sobreviver");
        assertEquals("a <b> c", TokenDeControleLlm.limpar("a <b> c"),
            "marcacao comum nao e token de controle");
    }

    @Test
    @DisplayName("CONTRA-TESTE: texto sem token volta igual, e null volta null")
    void naoMexeNoQueNaoPrecisa() {
        assertEquals("Tenente Uraki?!", TokenDeControleLlm.limpar("Tenente Uraki?!"));
        assertNull(TokenDeControleLlm.limpar(null));
        assertFalse(TokenDeControleLlm.contemToken("Tenente Uraki?!"));
        assertTrue(TokenDeControleLlm.contemToken("Tenente Uraki?!<|END_OF_TURN_TOKEN|>"));
    }
}
