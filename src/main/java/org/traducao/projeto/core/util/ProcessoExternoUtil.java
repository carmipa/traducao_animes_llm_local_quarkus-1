package org.traducao.projeto.core.util;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executa processos externos (ffmpeg, ffprobe, mkvmerge, mkvextract) de forma segura:
 * drena stdout e stderr em threads separadas (evita o deadlock classico de ProcessBuilder,
 * em que o processo filho trava escrevendo em um pipe cujo buffer do SO enche enquanto o
 * pai ainda le o outro stream) e aplica um timeout que mata o processo (destroyForcibly)
 * caso ele nao termine a tempo, em vez de travar o pipeline indefinidamente.
 */
public final class ProcessoExternoUtil {

    private ProcessoExternoUtil() {}

    public record Resultado(int codigoSaida, byte[] stdout, byte[] stderr) {}

    public static Resultado executar(List<String> comando, Duration timeout) throws IOException, InterruptedException, TimeoutException {
        return executar(comando, timeout, false);
    }

    public static Resultado executar(List<String> comando, Duration timeout, boolean mesclarErro)
            throws IOException, InterruptedException, TimeoutException {
        Process processo = new ProcessBuilder(comando).redirectErrorStream(mesclarErro).start();

        ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "processo-externo-drain");
            t.setDaemon(true);
            return t;
        });
        try {
            Future<byte[]> stdoutFuture = pool.submit(() -> processo.getInputStream().readAllBytes());
            Future<byte[]> stderrFuture = mesclarErro ? null : pool.submit(() -> processo.getErrorStream().readAllBytes());

            boolean terminou = processo.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!terminou) {
                processo.destroyForcibly();
                throw new TimeoutException("Processo excedeu o tempo limite de " + timeout.toSeconds()
                        + "s e foi encerrado: " + String.join(" ", comando));
            }

            byte[] stdout = lerResultado(stdoutFuture);
            byte[] stderr = stderrFuture != null ? lerResultado(stderrFuture) : new byte[0];
            return new Resultado(processo.exitValue(), stdout, stderr);
        } finally {
            pool.shutdownNow();
            if (processo.isAlive()) {
                processo.destroyForcibly();
            }
        }
    }

    private static byte[] lerResultado(Future<byte[]> future) throws IOException, InterruptedException {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IOException("Falha ao ler saída do processo externo", e.getCause());
        } catch (TimeoutException e) {
            future.cancel(true);
            return new byte[0];
        }
    }
}
