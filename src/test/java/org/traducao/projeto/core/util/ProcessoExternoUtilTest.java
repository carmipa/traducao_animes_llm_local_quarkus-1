package org.traducao.projeto.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: rede de segurança do contrato de {@link ProcessoExternoUtil} — o
 * único ponto por onde o KRONOS fala com ferramentas externas (ffmpeg, ffprobe, mkvmerge,
 * git). Trava o comportamento observável (captura de saída, mescla de erro, código de saída
 * e timeout que mata o processo) ANTES da troca da drenagem para Virtual Threads (FASE I),
 * para que a modernização não altere o que os adapters de mídia enxergam.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>stdout e stderr são capturados sem se contaminarem quando {@code mesclarErro=false}.</li>
 *   <li>{@code mesclarErro=true} redireciona stderr para stdout e devolve stderr vazio.</li>
 *   <li>O código de saída do processo externo é propagado fielmente.</li>
 *   <li>Um processo que excede o timeout é encerrado à força e vira {@link TimeoutException}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Usa a própria JVM em execução como processo externo portátil ({@code java --version} em
 * stdout, {@code java -version} em stderr), evitando dependência de ferramentas do SO; falha
 * de I/O ou de spawn propaga e reprova o teste.
 */
class ProcessoExternoUtilTest {

    /**
     * PROPÓSITO DE NEGÓCIO: localiza o executável Java corrente para servir de processo
     * externo determinístico e portátil entre sistemas operacionais.
     *
     * <p>INVARIANTES DO DOMÍNIO: prefere o caminho exato do processo em execução; só cai
     * para {@code java.home/bin} quando a informação do processo não está disponível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem o comando do processo atual, monta o caminho a
     * partir de {@code java.home}, ajustando a extensão {@code .exe} no Windows.
     */
    private static String javaBin() {
        return ProcessHandle.current().info().command().orElseGet(() -> {
            String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
            return Path.of(System.getProperty("java.home"), "bin", exe).toString();
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova a captura de stdout com saída bem-sucedida e sem
     * vazamento para stderr.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code java --version} escreve o banner em stdout, sai com 0
     * e nada em stderr.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: stdout vazio, stderr não vazio ou código diferente de
     * zero reprova o teste.
     */
    @Test
    void capturaStdoutComExitZeroESemStderr() throws Exception {
        ProcessoExternoUtil.Resultado r =
            ProcessoExternoUtil.executar(List.of(javaBin(), "--version"), Duration.ofSeconds(30));

        assertEquals(0, r.codigoSaida());
        assertTrue(new String(r.stdout(), StandardCharsets.UTF_8).length() > 0);
        assertEquals(0, r.stderr().length);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que stderr é drenado num canal separado, sem se misturar
     * ao stdout — o motivo original das threads de drenagem.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code java -version} (um traço) escreve o banner em stderr,
     * deixando stdout vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: stdout não vazio ou stderr vazio reprova o teste.
     */
    @Test
    void capturaStderrSeparadoDoStdout() throws Exception {
        ProcessoExternoUtil.Resultado r =
            ProcessoExternoUtil.executar(List.of(javaBin(), "-version"), Duration.ofSeconds(30));

        assertEquals(0, r.codigoSaida());
        assertEquals(0, r.stdout().length);
        assertTrue(r.stderr().length > 0);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova a mescla de stderr em stdout quando solicitada, usada
     * pelos adapters que só querem um fluxo combinado (mkvmerge, git).
     *
     * <p>INVARIANTES DO DOMÍNIO: com {@code mesclarErro=true}, o banner de {@code -version}
     * chega em stdout e stderr volta vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: stdout vazio ou stderr não vazio reprova o teste.
     */
    @Test
    void mesclarErroRedirecionaStderrParaStdout() throws Exception {
        ProcessoExternoUtil.Resultado r =
            ProcessoExternoUtil.executar(List.of(javaBin(), "-version"), Duration.ofSeconds(30), true);

        assertEquals(0, r.codigoSaida());
        assertTrue(r.stdout().length > 0);
        assertEquals(0, r.stderr().length);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que um código de saída de erro do processo externo é
     * propagado, permitindo aos adapters distinguir sucesso de falha.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma opção inválida faz a JVM sair com código diferente de zero.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: código de saída igual a zero reprova o teste.
     */
    @Test
    void propagaCodigoDeSaidaDeErro() throws Exception {
        ProcessoExternoUtil.Resultado r = ProcessoExternoUtil.executar(
            List.of(javaBin(), "--opcao-inexistente-kronos"), Duration.ofSeconds(30), true);

        assertNotEquals(0, r.codigoSaida());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que um processo travado além do timeout é encerrado à
     * força e sinalizado, em vez de prender o pipeline indefinidamente.
     *
     * <p>INVARIANTES DO DOMÍNIO: excedido o limite, {@link ProcessoExternoUtil#executar}
     * lança {@link TimeoutException} e mata o processo filho.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de {@link TimeoutException} reprova o teste.
     * Usa o modo single-file source ({@code java Sleeper.java}) para não depender do
     * {@code classpath}, que sob Quarkus estoura o limite de linha de comando do Windows.
     */
    @Test
    void timeoutEncerraProcessoQueExcedeLimite(@TempDir Path tempDir) throws Exception {
        Path fonte = tempDir.resolve("ProcessoLento.java");
        Files.writeString(fonte,
            "public class ProcessoLento {"
            + " public static void main(String[] a) throws Exception { Thread.sleep(30000); } }");
        List<String> cmd = List.of(javaBin(), fonte.toString());

        assertThrows(TimeoutException.class,
            () -> ProcessoExternoUtil.executar(cmd, Duration.ofMillis(1500)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que {@link ProcessoExternoUtil#executar} AGUARDA o
     * encerramento do processo-filho antes de retornar — não basta disparar o kill e seguir.
     * Distingue "kill solicitado" de "término confirmado".
     *
     * <p>INVARIANTES DO DOMÍNIO: executar roda numa thread separada; o PID do filho é lido
     * enquanto executar ainda está bloqueado; assim que executar termina (por
     * {@link TimeoutException}), o processo JÁ deve estar morto — verificação imediata, SEM
     * janela de tolerância posterior nem busy-wait. A implementação antiga (só
     * {@code destroyForcibly} e retorno) não garante isso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: processo ainda vivo no instante do retorno reprova o
     * teste; recursos são liberados no {@code finally}.
     */
    @Test
    void timeoutAguardaEncerramentoAntesDeRetornar(@TempDir Path tempDir) throws Exception {
        Path fonte = tempDir.resolve("ProcessoPid.java");
        Path arquivoPid = tempDir.resolve("pid.txt");
        Files.writeString(fonte,
            "public class ProcessoPid {"
            + " public static void main(String[] a) throws Exception {"
            + "  java.nio.file.Files.writeString(java.nio.file.Path.of(a[0]),"
            + "    String.valueOf(ProcessHandle.current().pid()));"
            + "  Thread.sleep(60000); } }");
        List<String> cmd = List.of(javaBin(), fonte.toString(), arquivoPid.toString());

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> futuro = executor.submit(() -> ProcessoExternoUtil.executar(cmd, Duration.ofMillis(2000)));

            // PID lido ENQUANTO executar ainda está bloqueado no processo (antes do retorno).
            long pid = lerPidComPrazo(arquivoPid, Duration.ofSeconds(15));

            // Sincronização determinística pelo término de executar (não por sleep/janela).
            ExecutionException ee = assertThrows(ExecutionException.class,
                () -> futuro.get(30, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, ee.getCause(),
                "o estouro de tempo deve emergir como TimeoutException");

            // Imediatamente após o retorno: o método aguardou, então o filho não pode seguir vivo.
            assertFalse(processoVivo(pid),
                "executar() deve aguardar o encerramento; o filho não pode continuar vivo ao retornar");
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que uma interrupção da thread chamadora durante
     * {@link ProcessoExternoUtil#executar} PROPAGA {@link InterruptedException} ao chamador
     * (não vira {@link TimeoutException}), dando ao cancelamento precedência sobre o timeout.
     *
     * <p>INVARIANTES DO DOMÍNIO: executar roda numa thread com timeout longo (30s) para ficar
     * bloqueado na espera do processo; interrompida a thread, executar sai por
     * {@link InterruptedException}; o processo-filho é limpo pelo {@code finally} da própria
     * {@code executar}. Determinístico, sem {@code sleep} como sincronização.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceção capturada de tipo diferente de
     * {@link InterruptedException}, ou processo-filho ainda vivo, reprova o teste.
     */
    @Test
    void interrupcaoDuranteEsperaPropagaInterruptedException(@TempDir Path tempDir) throws Exception {
        Path fonte = tempDir.resolve("ProcessoPid.java");
        Path arquivoPid = tempDir.resolve("pid.txt");
        Files.writeString(fonte,
            "public class ProcessoPid {"
            + " public static void main(String[] a) throws Exception {"
            + "  java.nio.file.Files.writeString(java.nio.file.Path.of(a[0]),"
            + "    String.valueOf(ProcessHandle.current().pid()));"
            + "  Thread.sleep(60000); } }");
        List<String> cmd = List.of(javaBin(), fonte.toString(), arquivoPid.toString());

        AtomicReference<Throwable> capturado = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                ProcessoExternoUtil.executar(cmd, Duration.ofSeconds(30));
            } catch (Throwable t) {
                capturado.set(t);
            }
        }, "processo-externo-interrupcao-teste");
        worker.setDaemon(true);
        worker.start();
        try {
            long pid = lerPidComPrazo(arquivoPid, Duration.ofSeconds(15));

            worker.interrupt();
            worker.join(10_000);
            assertFalse(worker.isAlive(), "a thread do executar deveria ter encerrado após a interrupção");
            assertInstanceOf(InterruptedException.class, capturado.get(),
                "a interrupção deve propagar InterruptedException, não TimeoutException");
            assertFalse(processoVivo(pid),
                "o processo-filho deve ser encerrado pela limpeza de executar()");
        } finally {
            worker.interrupt();
            worker.join(5_000);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê o PID que o processo-filho gravou, tolerando a janela entre o
     * spawn e a primeira escrita do arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: só observa, com poll de intervalo curto e prazo total estrito;
     * o pequeno {@code sleep} é apenas espaçamento do poll (não sincronização principal, que é
     * o término de {@code executar}) e não queima um núcleo como um busy-wait faria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: esgotado o prazo sem PID legível, lança
     * {@link AssertionError}.
     */
    private static long lerPidComPrazo(Path arquivoPid, Duration prazo) throws Exception {
        long fim = System.nanoTime() + prazo.toNanos();
        while (System.nanoTime() < fim) {
            if (Files.exists(arquivoPid)) {
                String texto = Files.readString(arquivoPid).trim();
                if (!texto.isEmpty()) {
                    return Long.parseLong(texto);
                }
            }
            Thread.sleep(10);
        }
        throw new AssertionError("processo-filho não gravou o PID dentro do prazo de observação");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: observa, num único instante, se um PID ainda corresponde a um
     * processo vivo — sem prazo de tolerância, para provar que o encerramento já ocorreu.
     *
     * <p>INVARIANTES DO DOMÍNIO: consulta única via {@link ProcessHandle}; PID ausente conta
     * como encerrado; não espera, não faz poll.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code true} apenas se o processo estiver
     * efetivamente vivo no instante da consulta.
     */
    private static boolean processoVivo(long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }
}
