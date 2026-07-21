package org.traducao.projeto.traducao.contextocena;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.application.contextocena.MontadorMensagemContextual;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;
import org.traducao.projeto.traducao.domain.contextocena.RequisicaoTraducaoContextual;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.contextocena.AdaptadorTradutorContextual;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.ChatRequest;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.Choice;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.Mensagem;
import org.traducao.projeto.traducao.infrastructure.dtos.RecordsLlm.RespostaLlm;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: subfase 5 do Plano-Mestre — prova a chamada única contextual do
 * {@link AdaptadorTradutorContextual} SEM rede (o seam {@code enviarChat} é substituído por
 * resposta canned): monta a requisição certa (system+user), limpa a resposta ({@code <think>}
 * e rótulo), aplica o anti-vazamento (pula eco do contexto) e mantém o original quando a
 * resposta é inutilizável.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste puro, sem CDI e sem HTTP; a fala-alvo original é o
 * fallback seguro em toda falha.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de limpeza, anti-vazamento ou fallback reprova.
 */
class AdaptadorTradutorContextualTest {

    private static LinhaAlvoContextual linha(int i, String texto) {
        return new LinhaAlvoContextual(i, "Default", texto);
    }

    private static RequisicaoTraducaoContextual requisicao(String promptSistema) {
        JanelaContextual janela = new JanelaContextual(
            linha(10, "I'm sure..."),
            List.of(linha(9, "Karen, are you ready?")),
            List.of(linha(11, "Let's move out.")));
        return new RequisicaoTraducaoContextual(promptSistema, janela);
    }

    /** Adaptador com o passo HTTP substituído por uma resposta canned. */
    private static final class StubAdapter extends AdaptadorTradutorContextual {
        ChatRequest capturado;
        private final String content;
        private final boolean erro;
        private final boolean semChoices;

        StubAdapter(String content, boolean erro, boolean semChoices) {
            super(new LlmProperties(), new MontadorMensagemContextual(), new ObjectMapper());
            this.content = content;
            this.erro = erro;
            this.semChoices = semChoices;
        }

        @Override
        protected RespostaLlm enviarChat(ChatRequest request) {
            this.capturado = request;
            if (erro) {
                throw new RuntimeException("rede caiu");
            }
            if (semChoices) {
                return new RespostaLlm(List.of());
            }
            return new RespostaLlm(List.of(new Choice(new Mensagem("assistant", content))));
        }
    }

    @Test
    @DisplayName("chamada: monta system+user certos e devolve a traducao de uma linha")
    void chamadaMontaRequisicaoEDevolveTraducao() {
        StubAdapter adapter = new StubAdapter("Estou certa.", false, false);

        String traduzido = adapter.traduzirComContexto(requisicao("PROMPT_SISTEMA_CONGELADO"));

        assertEquals("Estou certa.", traduzido);
        assertEquals("system", adapter.capturado.messages().get(0).role());
        assertEquals("PROMPT_SISTEMA_CONGELADO", adapter.capturado.messages().get(0).content());
        String user = adapter.capturado.messages().get(1).content();
        assertTrue(user.contains("Fala-alvo") && user.contains("I'm sure...")
            && user.contains("Karen, are you ready?"), "mensagem-usuario deve conter alvo e contexto");
    }

    @Test
    @DisplayName("limpeza: remove bloco <think> e rotulo 'Traducao:'")
    void limpaRaciocinioERotulo() {
        assertEquals("Estou certa.",
            new StubAdapter("<think>quem fala? Shiro, homem</think>\nEstou certa.", false, false)
                .traduzirComContexto(requisicao("S")));
        assertEquals("Estou certa.",
            new StubAdapter("Traducao: Estou certa.", false, false)
                .traduzirComContexto(requisicao("S")));
    }

    @Test
    @DisplayName("anti-vazamento: pula linha que ecoa o contexto e devolve a traducao")
    void antiVazamentoPulaEco() {
        String resposta = "Karen, are you ready?\nEstou certa.";
        assertEquals("Estou certa.",
            new StubAdapter(resposta, false, false).traduzirComContexto(requisicao("S")));
    }

    @Test
    @DisplayName("fallback: eco do original, resposta vazia ou erro mantem a fala-alvo original")
    void fallbackMantemOriginal() {
        // modelo apenas ecoou o original em ingles (nao traduziu): mantem original.
        assertEquals("I'm sure...",
            new StubAdapter("I'm sure...", false, false).traduzirComContexto(requisicao("S")));
        // resposta sem choices: mantem original.
        assertEquals("I'm sure...",
            new StubAdapter(null, false, true).traduzirComContexto(requisicao("S")));
        // erro de rede: mantem original.
        assertEquals("I'm sure...",
            new StubAdapter(null, true, false).traduzirComContexto(requisicao("S")));
    }
}
