package org.traducao.projeto.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PROPÓSITO DE NEGÓCIO: executa com segurança as ferramentas externas de que o KRONOS
 * depende (ffmpeg, ffprobe, mkvmerge, mkvextract, git), servindo de único ponto de spawn de
 * processos. Drena stdout e stderr em paralelo para nunca travar o pipeline por um pipe cheio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>stdout e stderr são drenados concorrentemente — evita o deadlock clássico do
 *       {@link ProcessBuilder}, em que o filho bloqueia escrevendo num pipe cujo buffer do
 *       SO enche enquanto o pai ainda lê o outro fluxo.</li>
 *   <li>Um processo que excede o timeout é morto com {@code destroyForcibly}, em vez de
 *       prender o pipeline indefinidamente.</li>
 *   <li>A drenagem é I/O puro: roda em Virtual Threads (Java 21+), sem manter um pool de
 *       threads de plataforma por invocação nem competir com a GPU/LLM sequencial.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de spawn ou de leitura vira {@link IOException}; estouro do limite lança
 * {@link TimeoutException} após matar o processo; interrupção da thread chamadora propaga
 * {@link InterruptedException}.
 */
public final class ProcessoExternoUtil {

    private static final Logger log = LoggerFactory.getLogger(ProcessoExternoUtil.class);

    // Virtual threads: a drenagem de stdout/stderr é I/O puro e efêmera. Nomeadas para
    // diagnóstico; sempre daemon por natureza (não seguram a JVM viva).
    private static final ThreadFactory FABRICA_DRENAGEM =
        Thread.ofVirtual().name("processo-externo-drain-", 0).factory();

    // Espera curta e explícita pela confirmação do término após destroyForcibly. Curta
    // porque um kill do SO é quase imediato; limitada para nunca travar o pipeline caso
    // o SO demore a recolher o processo.
    private static final Duration ESPERA_ENCERRAMENTO = Duration.ofSeconds(2);

    private ProcessoExternoUtil() {}

    public record Resultado(int codigoSaida, byte[] stdout, byte[] stderr) {}

    /**
     * PROPÓSITO DE NEGÓCIO: executa o comando externo capturando stdout e stderr separados,
     * para o chamador inspecionar diagnósticos sem misturar os fluxos.
     *
     * <p>INVARIANTES DO DOMÍNIO: delega ao fluxo completo sem mesclar erro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}, {@link InterruptedException}
     * e {@link TimeoutException} do fluxo completo.
     */
    public static Resultado executar(List<String> comando, Duration timeout) throws IOException, InterruptedException, TimeoutException {
        return executar(comando, timeout, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa o comando externo com controle explícito de mescla de
     * erro e timeout, isolando o KRONOS de travas e vazamentos de processos-filho.
     *
     * <p>INVARIANTES DO DOMÍNIO: stdout e stderr são drenados concorrentemente em Virtual
     * Threads; com {@code mesclarErro=true}, stderr vai para stdout e o stderr devolvido é
     * vazio; o processo é sempre morto se ainda estiver vivo ao final.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha ao ler um fluxo vira {@link IOException};
     * exceder o {@code timeout} lança {@link TimeoutException} depois de {@code destroyForcibly};
     * interrupção propaga {@link InterruptedException}.
     */
    public static Resultado executar(List<String> comando, Duration timeout, boolean mesclarErro)
            throws IOException, InterruptedException, TimeoutException {
        Process processo = new ProcessBuilder(comando).redirectErrorStream(mesclarErro).start();

        ExecutorService pool = Executors.newThreadPerTaskExecutor(FABRICA_DRENAGEM);
        try {
            Future<byte[]> stdoutFuture = pool.submit(() -> processo.getInputStream().readAllBytes());
            Future<byte[]> stderrFuture = mesclarErro ? null : pool.submit(() -> processo.getErrorStream().readAllBytes());

            boolean terminou = processo.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!terminou) {
                encerrarForcadamenteEAguardar(processo);
                throw new TimeoutException("Processo excedeu o tempo limite de " + timeout.toSeconds()
                        + "s e foi encerrado: " + String.join(" ", comando));
            }

            byte[] stdout = lerResultado(stdoutFuture);
            byte[] stderr = stderrFuture != null ? lerResultado(stderrFuture) : new byte[0];
            return new Resultado(processo.exitValue(), stdout, stderr);
        } finally {
            pool.shutdownNow();
            encerrarForcadamenteEAguardar(processo);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que um processo externo estourado ou abandonado seja de
     * fato encerrado antes do método retornar, em vez de só disparar o kill e seguir — evita
     * que um ffmpeg/mkvmerge órfão continue consumindo CPU/disco depois do timeout.
     *
     * <p>INVARIANTES DO DOMÍNIO: se ainda vivo, chama {@code destroyForcibly()} e aguarda a
     * confirmação do término por um limite curto e explícito
     * ({@link #ESPERA_ENCERRAMENTO}), nunca indefinidamente; não altera stdout/stderr nem o
     * código de saída; não introduz concorrência no LLM.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o SO não confirmar o término dentro do limite,
     * registra um aviso e retorna assim mesmo (o kill já foi enviado e o SO conclui de forma
     * assíncrona), sem prender o pipeline; uma interrupção durante a espera restaura o flag de
     * interrupção da thread e encerra a espera imediatamente.
     */
    private static void encerrarForcadamenteEAguardar(Process processo) {
        if (!processo.isAlive()) {
            return;
        }
        processo.destroyForcibly();
        try {
            boolean encerrou = processo.waitFor(ESPERA_ENCERRAMENTO.toMillis(), TimeUnit.MILLISECONDS);
            if (!encerrou) {
                log.warn("Processo externo não confirmou o término {} ms após destroyForcibly; "
                    + "o SO concluirá o encerramento de forma assíncrona.", ESPERA_ENCERRAMENTO.toMillis());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
