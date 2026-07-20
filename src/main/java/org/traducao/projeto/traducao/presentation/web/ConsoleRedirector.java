package org.traducao.projeto.traducao.presentation.web;
import org.traducao.projeto.core.presentation.web.LogStreamService;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Interceptador global de System.out.
 * Redireciona tudo que é impresso no console padrão para o LogStreamService (SSE)
 * sem deixar de imprimir no console físico (terminal do CMD/PowerShell original).
 * <p>
 * No Spring Boot este bean era instanciado eagerly (singleton), e o redirecionamento
 * acontecia no construtor. No Quarkus/CDI (ARC) beans normais são lazy: como nada
 * injeta {@code ConsoleRedirector}, o bean nunca era construído e o redirecionamento
 * nunca era ativado (o console web parava de receber logs). O fix é o mesmo padrão
 * já usado por {@link BrowserLauncher} no mesmo pacote: reagir a {@link StartupEvent}
 * força a criação do bean na subida do Quarkus e também o protege da remoção de
 * beans não-usados em build-time (beans com método {@code @Observes} nunca são
 * removidos).
 */
@Component
public class ConsoleRedirector {

    private final LogStreamService logStreamService;

    public ConsoleRedirector(LogStreamService logStreamService) {
        this.logStreamService = logStreamService;
    }

    void onStart(@Observes StartupEvent event) {
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new DoubleOutputStream(originalOut, this::publicarLog), true, StandardCharsets.UTF_8));
    }

    private void publicarLog(String logMsg) {
        // Envia a mensagem limpa via Server-Sent Events, no canal da
        // operação em segundo plano que estiver em execução no momento
        // (ver LogStreamService#definirCanalAtual).
        try {
            logStreamService.publicarLog(logMsg);
        } catch (Exception ignored) {
            // Cliente SSE desconectado: não propagar para o servlet/Logback.
        }
    }

    /**
     * OutputStream interno que espelha os bytes gravados no fluxo original
     * e acumula buffers de linhas finalizadas com '\n' para despacho via SSE.
     * Package-private para caracterização em teste do buffer por-thread.
     */
    static class DoubleOutputStream extends OutputStream {
        private final OutputStream original;
        private final java.util.function.Consumer<String> consumer;
        // Buffer de linha POR-THREAD: cada job/thread acumula a SUA própria linha e o flush
        // (consumer.accept) ocorre na thread que iniciou a linha — de modo que o SSE resolva o
        // canal certo (LogStreamService usa um ThreadLocal de canal). Um buffer único misturaria
        // linhas parciais de threads diferentes e as rotearia para o canal errado.
        private final ThreadLocal<ByteArrayOutputStream> buffer =
            ThreadLocal.withInitial(ByteArrayOutputStream::new);

        public DoubleOutputStream(OutputStream original, java.util.function.Consumer<String> consumer) {
            this.original = original;
            this.consumer = consumer;
        }

        // synchronized: serializa a escrita no fluxo físico {@code original} para bytes de
        // threads concorrentes não se entrelaçarem no console. O isolamento de LINHA por canal
        // SSE vem do buffer por-thread acima (não do lock).
        @Override
        public synchronized void write(int b) throws IOException {
            original.write(b);
            if (b == '\n') {
                flushBuffer();
            } else if (b != '\r') {
                buffer.get().write(b);
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            original.write(b, off, len);
            for (int i = 0; i < len; i++) {
                int ch = b[off + i];
                if (ch == '\n') {
                    flushBuffer();
                } else if (ch != '\r') {
                    buffer.get().write(ch);
                }
            }
        }

        private void flushBuffer() {
            ByteArrayOutputStream linhaAtual = buffer.get();
            if (linhaAtual.size() > 0) {
                String line = linhaAtual.toString(StandardCharsets.UTF_8);
                consumer.accept(line);
                linhaAtual.reset();
            }
        }
    }
}
