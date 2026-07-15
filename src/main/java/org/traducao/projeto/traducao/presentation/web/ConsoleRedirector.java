package org.traducao.projeto.traducao.presentation.web;

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
     */
    private static class DoubleOutputStream extends OutputStream {
        private final OutputStream original;
        private final java.util.function.Consumer<String> consumer;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        public DoubleOutputStream(OutputStream original, java.util.function.Consumer<String> consumer) {
            this.original = original;
            this.consumer = consumer;
        }

        // synchronized: o buffer de linha é um só para todas as threads que
        // imprimem em System.out (pipeline, threads HTTP, libs). O PrintStream
        // serializa as chamadas na prática, mas isso é detalhe de implementação
        // da JDK — o lock explícito garante que bytes de threads concorrentes
        // não se entrelacem numa mesma linha nem saiam no canal SSE errado.
        @Override
        public synchronized void write(int b) throws IOException {
            original.write(b);
            if (b == '\n') {
                flushBuffer();
            } else if (b != '\r') {
                buffer.write(b);
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
                    buffer.write(ch);
                }
            }
        }

        private void flushBuffer() {
            if (buffer.size() > 0) {
                String line = buffer.toString(StandardCharsets.UTF_8);
                consumer.accept(line);
                buffer.reset();
            }
        }
    }
}
