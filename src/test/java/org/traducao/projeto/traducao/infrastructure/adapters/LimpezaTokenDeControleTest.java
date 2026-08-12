package org.traducao.projeto.traducao.infrastructure.adapters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * PROPÓSITO DE NEGÓCIO: garante que uma tradução CORRETA não seja descartada porque o servidor
 * deixou escapar o token do template de chat dentro do texto.
 *
 * <h2>O prejuízo, medido em 11/08/2026</h2>
 * Na comparação de quatro modelos sobre DanMachi S01 (conteúdo virgem, mesmo insumo), a
 * {@code aya-expanse-8b} devolveu traduções boas com {@code <|END_OF_TURN_TOKEN|>} colado no
 * fim. O token quebrava a checagem de marcadores {@code [[TAGn]]}; a fala era retentada três
 * vezes com temperatura crescente, falhava pelo mesmo motivo, e caía no tradutor de máquina.
 *
 * <p><b>115 das 116 falas perdidas naquela execução — 99% — eram este token.</b> O modelo foi
 * reprovado por um defeito que era do adaptador, não dele. Sem esta limpeza, qualquer modelo
 * da família Cohere fica inutilizável aqui por um motivo que não tem nada a ver com tradução.
 */
class LimpezaTokenDeControleTest {

    @Test
    @DisplayName("remove o token da Cohere que reprovou a Aya, preservando a traducao")
    void removeTokenDaCohere() {
        assertEquals("Bell, você não faz ideia de quão sortudo você é.",
            LlmClientAdapter.limparTokensDeControle(
                "Bell, você não faz ideia de quão sortudo você é.<|END_OF_TURN_TOKEN|>"));
    }

    @Test
    @DisplayName("cobre as familias de template, nao so a que doeu")
    void cobreAsOutrasFamilias() {
        // Buscar UMA forma mede a forma, não o invariante: o próximo modelo carregado traz
        // outro template, e o defeito voltaria com outro nome.
        assertEquals("Olá", LlmClientAdapter.limparTokensDeControle("Olá<|im_end|>"));
        assertEquals("Olá", LlmClientAdapter.limparTokensDeControle("Olá<|eot_id|>"));
        assertEquals("Olá", LlmClientAdapter.limparTokensDeControle("Olá<|endoftext|>"));
        assertEquals("Olá", LlmClientAdapter.limparTokensDeControle("<|START_OF_TURN_TOKEN|>Olá"));
        assertEquals("Olá", LlmClientAdapter.limparTokensDeControle("Olá<end_of_turn>"));
        assertEquals("Olá", LlmClientAdapter.limparTokensDeControle("<start_of_turn>Olá"));
    }

    @Test
    @DisplayName("caso-controle: NAO apaga conteudo legitimo da legenda")
    void naoApagaConteudoLegitimo() {
        // Guarda que apaga o certo é pior que guarda nenhuma. Uma legenda traz seta, sinal de
        // menor, marcação HTML de SRT e a tag ASS entre chaves — nada disso pode sumir.
        for (String legitimo : new String[]{
            "Vai por ali -> a leste",
            "5 < 10 e 10 > 5",
            "<i>sussurrando</i>",
            "{\\i1}Não acredito{\\i0}",
            "[[TAG0]]Bell[[TAG1]]",
            "Ele gritou: <<Corram!>>"
        }) {
            assertEquals(legitimo, LlmClientAdapter.limparTokensDeControle(legitimo),
                "a limpeza mexeu em conteudo legitimo: " + legitimo);
        }
    }

    @Test
    @DisplayName("texto sem token volta INTACTO, sem custo e sem copia")
    void textoSemTokenVoltaIntacto() {
        String original = "Uma fala comum, sem nada de estranho.";
        // assertSame: o atalho do indexOf('<') tem de evitar até a alocação. Se um dia alguém
        // trocar por um replaceAll incondicional, este teste avisa.
        assertSame(original, LlmClientAdapter.limparTokensDeControle(original));
        assertSame(null, LlmClientAdapter.limparTokensDeControle(null));
    }

    @Test
    @DisplayName("nao remove </s>, e isso e decisao declarada")
    void naoRemoveBarraS() {
        // </s> é token de fim em várias famílias, mas é ambíguo com marcação de texto e
        // nenhum modelo em uso o emite. Fica de fora DE PROPÓSITO; se um dia doer, entra com
        // o prejuízo medido junto — não por precaução.
        assertEquals("fim</s>", LlmClientAdapter.limparTokensDeControle("fim</s>"));
    }
}
