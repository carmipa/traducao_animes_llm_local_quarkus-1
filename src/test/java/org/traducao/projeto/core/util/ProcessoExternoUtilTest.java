package org.traducao.projeto.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
     * PROPÓSITO DE NEGÓCIO: prova que o timeout não só sinaliza, mas de fato ENCERRA o
     * processo-filho — a garantia de que um ffmpeg/mkvmerge travado não fica órfão consumindo
     * recursos após o {@link TimeoutException}.
     *
     * <p>INVARIANTES DO DOMÍNIO: o filho grava o próprio PID em arquivo antes de dormir; após
     * o timeout, o PID observado via {@link ProcessHandle} não pode estar mais vivo. A
     * verificação é determinística (arquivo + polling com prazo total estrito), sem usar
     * {@code sleep} como sincronização.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: PID não gravado no prazo, ou processo ainda vivo no
     * prazo de observação, reprova o teste.
     */
    @Test
    void timeoutEncerraDeFatoOProcessoFilho(@TempDir Path tempDir) throws Exception {
        Path fonte = tempDir.resolve("ProcessoPid.java");
        Path arquivoPid = tempDir.resolve("pid.txt");
        Files.writeString(fonte,
            "public class ProcessoPid {"
            + " public static void main(String[] a) throws Exception {"
            + "  java.nio.file.Files.writeString(java.nio.file.Path.of(a[0]),"
            + "    String.valueOf(ProcessHandle.current().pid()));"
            + "  Thread.sleep(30000); } }");
        List<String> cmd = List.of(javaBin(), fonte.toString(), arquivoPid.toString());

        assertThrows(TimeoutException.class,
            () -> ProcessoExternoUtil.executar(cmd, Duration.ofMillis(1500)));

        long pid = lerPidComPrazo(arquivoPid, Duration.ofSeconds(5));
        assertFalse(processoVivoComPrazo(pid, Duration.ofSeconds(5)),
            "o processo-filho deveria ter sido encerrado à força após o timeout");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lê o PID que o processo-filho gravou, tolerando a janela entre o
     * spawn e a primeira escrita do arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: só observa; faz busy-wait com {@link Thread#onSpinWait()} até
     * um prazo total estrito, sem {@code sleep} como sincronização.
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
            Thread.onSpinWait();
        }
        throw new AssertionError("processo-filho não gravou o PID dentro do prazo de observação");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: observa se um PID ainda corresponde a um processo vivo, dentro de
     * um prazo total estrito.
     *
     * <p>INVARIANTES DO DOMÍNIO: só observa via {@link ProcessHandle}; PID ausente ou não vivo
     * conta como encerrado; busy-wait com {@link Thread#onSpinWait()}, sem {@code sleep}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se ainda vivo ao fim do prazo, devolve {@code true}
     * para o teste reprovar.
     */
    private static boolean processoVivoComPrazo(long pid, Duration prazo) {
        long fim = System.nanoTime() + prazo.toNanos();
        while (System.nanoTime() < fim) {
            Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isEmpty() || !handle.get().isAlive()) {
                return false;
            }
            Thread.onSpinWait();
        }
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        return handle.isPresent() && handle.get().isAlive();
    }
}
