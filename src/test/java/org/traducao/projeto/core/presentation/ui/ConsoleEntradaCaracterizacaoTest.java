package org.traducao.projeto.core.presentation.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a saída atual de
 * {@link ConsoleEntrada#imprimirErroSaida()} — o único método efetivamente
 * consumido pelas fatias CLI — para blindar a subfase E2 (movimentação de
 * {@code ConsoleEntrada} para o {@code core.presentation.ui}). Prova que o move
 * é puramente de pacote: mensagens, cores ANSI e enquadramento por linhas em
 * branco permanecem byte a byte idênticos.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A saída é exatamente: linha em branco, a mensagem de erro em VERMELHO/negrito,
 *       a dica de recuperação em AMARELO e uma linha em branco final — cada uma via
 *       {@code println}, com {@link System#lineSeparator()}.</li>
 *   <li>O esperado é reconstruído a partir do próprio {@link AnsiCores}, garantindo
 *       que qualquer troca de mensagem, cor ou ordem reprove o teste.</li>
 *   <li>O {@code System.out} original é SEMPRE restaurado (via {@link AfterEach}),
 *       nunca deixando o stream substituído vazar para outros testes.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência na saída capturada reprova com {@code assertEquals},
 * exibindo o esperado versus o real.
 */
class ConsoleEntradaCaracterizacaoTest {

    private final PrintStream saidaOriginal = System.out;

    @AfterEach
    void restaurarSaida() {
        System.setOut(saidaOriginal);
    }

    @Test
    @DisplayName("imprimirErroSaida() preserva mensagens, cores ANSI e enquadramento")
    void imprimirErroSaidaPreservaSaidaAtual() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));

        ConsoleEntrada.imprimirErroSaida();

        String ls = System.lineSeparator();
        String esperado = ls
            + AnsiCores.colorir("ERRO: interrupção ou erro no console.", AnsiCores.RED, true) + ls
            + AnsiCores.colorir("Rode:  .\\gradlew.bat bootRun --console=plain", AnsiCores.YELLOW) + ls
            + ls;

        String real = buffer.toString(StandardCharsets.UTF_8);
        assertEquals(esperado, real, "A saída de imprimirErroSaida() divergiu da baseline caracterizada");
    }
}
