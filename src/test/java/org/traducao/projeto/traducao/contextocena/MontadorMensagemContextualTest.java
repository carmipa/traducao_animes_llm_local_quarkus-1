package org.traducao.projeto.traducao.contextocena;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.application.contextocena.MontadorMensagemContextual;
import org.traducao.projeto.traducao.domain.contextocena.JanelaContextual;
import org.traducao.projeto.traducao.domain.contextocena.LinhaAlvoContextual;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: subfase 4 do Plano-Mestre — prova o FORMATO da mensagem-usuário
 * contextual: contexto rotulado "NAO traduzir", fala-alvo isolada, ordem anterior→alvo→
 * posterior e instrução final (uma linha, não vazar contexto, neutro em caso de dúvida).
 * É o contrato de formato que a subfase 5 vai enviar ao LLM; testado sem tocar o modelo.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste puro e determinístico; seções de contexto só aparecem
 * quando há vizinhas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de rótulo, ordem ou instrução reprova.
 */
class MontadorMensagemContextualTest {

    private final MontadorMensagemContextual montador = new MontadorMensagemContextual();

    private static LinhaAlvoContextual linha(int i, String texto) {
        return new LinhaAlvoContextual(i, "Default", texto);
    }

    @Test
    @DisplayName("mensagem: contexto rotulado 'NAO traduzir', alvo isolado, ordem antes->alvo->depois")
    void formatoCompleto() {
        JanelaContextual janela = new JanelaContextual(
            linha(10, "I'm sure..."),
            List.of(linha(8, "Karen, are you ready?"), linha(9, "Almost.")),
            List.of(linha(11, "Let's move out."), linha(12, "Roger.")));

        String msg = montador.montarMensagemUsuario(janela);

        assertTrue(msg.contains("Contexto anterior (NAO traduzir"), "faltou rotular o contexto anterior");
        assertTrue(msg.contains("Contexto posterior (NAO traduzir"), "faltou rotular o contexto posterior");
        assertTrue(msg.contains("Fala-alvo (traduza SOMENTE esta linha):"), "faltou destacar a fala-alvo");
        assertTrue(msg.contains("I'm sure..."), "faltou a fala-alvo");
        assertTrue(msg.contains("Karen, are you ready?") && msg.contains("Roger."), "faltou alguma vizinha");
        assertTrue(msg.contains("uma unica linha"), "faltou a instrucao de uma linha");
        assertTrue(msg.toLowerCase().contains("neutra"), "faltou a instrucao de formulacao neutra");
        assertTrue(msg.contains("[[TAG"), "faltou a instrucao de preservar os marcadores [[TAGn]]");

        int iAntes = msg.indexOf("Karen, are you ready?");
        int iAlvo = msg.indexOf("I'm sure...");
        int iDepois = msg.indexOf("Let's move out.");
        assertTrue(iAntes >= 0 && iAntes < iAlvo && iAlvo < iDepois,
            "ordem deve ser anterior -> alvo -> posterior");
    }

    @Test
    @DisplayName("mensagem: sem vizinhas, nenhuma secao de contexto aparece")
    void semVizinhas() {
        JanelaContextual janela = new JanelaContextual(linha(5, "Thank you, Norris."), List.of(), List.of());
        String msg = montador.montarMensagemUsuario(janela);

        assertFalse(msg.contains("Contexto anterior"), "nao deveria haver contexto anterior");
        assertFalse(msg.contains("Contexto posterior"), "nao deveria haver contexto posterior");
        assertTrue(msg.contains("Fala-alvo (traduza SOMENTE esta linha):"));
        assertTrue(msg.contains("Thank you, Norris."));
        assertTrue(msg.contains("uma unica linha"));
    }

    @Test
    @DisplayName("mensagem: so contexto anterior (fala no fim da cena)")
    void soContextoAnterior() {
        JanelaContextual janela = new JanelaContextual(
            linha(5, "Are you mad?!"), List.of(linha(4, "Calm down.")), List.of());
        String msg = montador.montarMensagemUsuario(janela);

        assertTrue(msg.contains("Contexto anterior (NAO traduzir"));
        assertFalse(msg.contains("Contexto posterior"));
        assertTrue(msg.indexOf("Calm down.") < msg.indexOf("Are you mad?!"));
    }

    @Test
    @DisplayName("mensagem: janela ou alvo nulos lancam NPE")
    void nulosLancam() {
        assertThrows(NullPointerException.class, () -> montador.montarMensagemUsuario(null));
        assertThrows(NullPointerException.class,
            () -> montador.montarMensagemUsuario(new JanelaContextual(null, List.of(), List.of())));
    }
}
