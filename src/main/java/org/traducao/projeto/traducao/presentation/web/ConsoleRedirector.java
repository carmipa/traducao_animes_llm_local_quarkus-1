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

    /**
     * Onde fica guardado o {@code System.out} ORIGINAL — o console físico, antes de qualquer
     * espelho.
     *
     * <p>Mora em {@link System#getProperties()} e não em campo estático porque o live reload do
     * Quarkus troca o classloader da aplicação: um {@code static} nasce vazio a cada reload, que
     * é exatamente quando ele precisaria ter valor. {@code Properties} é um
     * {@code Hashtable<Object,Object>} e aceita o objeto; a leitura é por {@code get}, nunca por
     * {@code getProperty} (que só enxerga String).
     */
    static final String CHAVE_STDOUT_ORIGINAL = "kronos.console.stdout-original";

    /**
     * PROPÓSITO DE NEGÓCIO: instalar UMA vez o espelho do console para o painel web.
     *
     * <h2>O defeito que este guard fecha — medido em 17/08/2026</h2>
     * A versão anterior fazia {@code System.setOut(new PrintStream(... System.out ...))} a cada
     * {@link StartupEvent}. No dev mode, cada live reload dispara o evento de novo, e o
     * {@code System.out} de então <b>já é o redirecionador anterior</b> — então as camadas se
     * EMPILHAM, e cada linha passa por todas elas, publicando uma vez por camada.
     *
     * <p>Medido numa corrida da 3.2 no 86, depois de várias recompilações na mesma sessão:
     * <pre>
     *   14.512 linhas no canal [console]  +  7.256 em [revisao-lore]  =  3 copias por linha
     *   linhas com prefixo [UTC (do log.info): 0  -> a duplicacao NAO vinha do logger
     * </pre>
     * É a mesma anomalia que a tela 3.1 registrou como "console duplica linha (7×, cresce dentro
     * do processo)" sem achar a causa: ela cresce porque cada reload acrescenta uma camada.
     *
     * <h2>E por que a primeira correção, de "instalar uma vez só", estava errada — 17/08/2026</h2>
     * A versão que barrava a reinstalação por uma marca matava o console web INTEIRO depois do
     * primeiro live reload. Duas coisas acontecem no reload e as duas quebram o espelho preso:
     * <ul>
     *   <li>o {@code System.out} volta ao console físico ao desligar a aplicação, e a marca
     *       impedia recolocar o espelho;</li>
     *   <li>mesmo que sobrevivesse, o espelho aponta para {@code this::publicarLog} do bean
     *       ANTIGO, cujo {@code LogStreamService} morreu junto com o contexto — inclusive o
     *       {@code ThreadLocal} do canal, que o código novo não teria como preencher.</li>
     * </ul>
     * Medido: {@code console-web.log} recebeu a última linha às 22:26 e ficou MUDO enquanto uma
     * revisão de lore de 15 arquivos rodava e escrevia normalmente no log de execução. Silêncio,
     * sem erro — o pior desfecho para quem acompanha pela tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: o espelho envolve SEMPRE o {@code System.out} original guardado
     * em {@link #CHAVE_STDOUT_ORIGINAL} — nunca o corrente. Daí saem as duas propriedades ao
     * mesmo tempo: não empilha (cada instalação substitui a anterior em vez de embrulhá-la) e não
     * envelhece (cada arranque reata o espelho ao bean vivo).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o original guardado não for utilizável, cai no
     * {@code System.out} corrente — o painel pode duplicar linha, e duplicar é muito melhor que
     * emudecer.
     */
    void onStart(@Observes StartupEvent event) {
        System.setOut(new PrintStream(
            new DoubleOutputStream(stdoutOriginal(), this::publicarLog), true, StandardCharsets.UTF_8));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o console físico, guardando-o na primeira vez.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda uma vez só por PROCESSO; do segundo arranque em diante
     * devolve o mesmo objeto, mesmo com o classloader trocado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor guardado que não seja {@link PrintStream} é
     * descartado e o {@code System.out} corrente assume.
     */
    private static PrintStream stdoutOriginal() {
        Object guardado = System.getProperties().get(CHAVE_STDOUT_ORIGINAL);
        if (guardado instanceof PrintStream original) {
            return original;
        }
        PrintStream atual = System.out;
        System.getProperties().put(CHAVE_STDOUT_ORIGINAL, atual);
        return atual;
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
