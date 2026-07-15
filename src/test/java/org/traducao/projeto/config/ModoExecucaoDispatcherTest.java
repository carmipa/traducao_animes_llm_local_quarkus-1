package org.traducao.projeto.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o contrato do dispatcher compartilhado
 * {@link ModoExecucaoStartup} após a extração do modo TRADUZIR (D-Config). Fixa que
 * o modo TRADUZIR deixou de ser roteado aqui — passando a ser tratado como um
 * short-circuit, e nunca como "modo desconhecido" — sem afetar o roteamento dos
 * demais modos nem a rejeição de modos inválidos.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code WEB} e {@code TRADUZIR} retornam sem efeito (nenhuma CLI roteada por
 *       este dispatcher; TRADUZIR tem ciclo de vida próprio em {@code traducao}).</li>
 *   <li>Um modo inválido continua sendo rejeitado com {@link IllegalStateException}.</li>
 *   <li>Estes três caminhos (WEB, TRADUZIR, inválido) nunca chamam {@code .get()} nos
 *       beans injetados — por isso a caracterização dispensa cabeamento CDI.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer regressão (TRADUZIR voltando a ser tratado como desconhecido, ou modo
 * inválido deixando de lançar) reprova a suíte.
 */
class ModoExecucaoDispatcherTest {

    @Test
    @DisplayName("Modo WEB: dispatcher não roteia nenhuma CLI (short-circuit)")
    void modoWebNaoRoteia() {
        ModoExecucaoStartup dispatcher = new ModoExecucaoStartup();
        dispatcher.modo = "WEB";
        assertDoesNotThrow(() -> dispatcher.onStart(null));
    }

    @Test
    @DisplayName("Modo TRADUZIR: delegado à Tradução Local, não é tratado como desconhecido")
    void modoTraduzirEhDelegadoNaoDesconhecido() {
        ModoExecucaoStartup dispatcher = new ModoExecucaoStartup();
        dispatcher.modo = "TRADUZIR";
        // Após D-Config, TRADUZIR não é roteado por este dispatcher e NÃO deve lançar
        // "Modo de execucao desconhecido" — seu ciclo de vida vive em TraducaoStartup.
        assertDoesNotThrow(() -> dispatcher.onStart(null));
    }

    @Test
    @DisplayName("Modo TRADUZIR minúsculo também é reconhecido (case-insensitive)")
    void modoTraduzirCaseInsensitive() {
        ModoExecucaoStartup dispatcher = new ModoExecucaoStartup();
        dispatcher.modo = "traduzir";
        assertDoesNotThrow(() -> dispatcher.onStart(null));
    }

    @Test
    @DisplayName("Modo inválido continua sendo rejeitado com IllegalStateException")
    void modoInvalidoEhRejeitado() {
        ModoExecucaoStartup dispatcher = new ModoExecucaoStartup();
        dispatcher.modo = "MODO_INEXISTENTE";
        assertThrows(IllegalStateException.class, () -> dispatcher.onStart(null));
    }
}
