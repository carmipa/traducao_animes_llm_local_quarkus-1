package org.traducao.projeto.traducao.presentation.ui;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Wrapper thread-safe em torno da barra de progresso (estilo tqdm). Todo
 * acesso a {@code pb} e sincronizado porque mensagens podem chegar
 * durante a tradução de um episódio.
 * <p>
 * No modo web, {@code System.out} já é espelhado pelo ConsoleRedirector para
 * SSE e {@code logs/console-web.log}. As mensagens visuais não são repetidas no
 * SLF4J, evitando que a mesma linha apareça duas vezes no terminal e no painel.
 */
@Component
public class ConsoleUILogger {

    private static final Logger log = LoggerFactory.getLogger(ConsoleUILogger.class);

    // Cores ANSI — ver AnsiCores
    private static final String ANSI_RESET = AnsiCores.RESET;
    private static final String ANSI_RED = AnsiCores.RED;
    private static final String ANSI_GREEN = AnsiCores.GREEN;
    private static final String ANSI_YELLOW = AnsiCores.YELLOW;
    private static final String ANSI_CYAN = AnsiCores.CYAN;

    // O apagar-linha ANSI (ESC[K) só faz sentido num terminal interativo real; em modo web
    // (System.out redirecionado, sem console anexado) ele vazaria como lixo no painel SSE e
    // no logs/console-web.log. Resolvido uma vez na carga da classe.
    private static final boolean TERMINAL_INTERATIVO = System.console() != null;

    private ProgressBar pb;

    private int totalFalasCache = 0;
    private int totalFalasNovas = 0;
    private int totalAvisos = 0;

    public synchronized void iniciarLotes(int totalLotes, String nomeEpisodio) {
        fecharBarraComSeguranca();
        try {
            pb = new ProgressBarBuilder()
                    .setTaskName("Traduzindo " + nomeEpisodio)
                    .setInitialMax(totalLotes)
                    .setStyle(ProgressBarStyle.ASCII) // Fallback seguro para Windows Terminal e CMD
                    .setUpdateIntervalMillis(100)
                    .build();
        } catch (RuntimeException e) {
            log.warn("Não foi possível iniciar a barra de progresso (terminal incompatível); continuando sem ela: {}", e.getMessage());
            pb = null;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: imprime um marco visual entre episódios para que o
     * acompanhamento da tradução não misture lotes de arquivos diferentes.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma única emissão em {@code System.out} alimenta
     * terminal, SSE e log web, sem duplicação via SLF4J.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fecha a barra anterior com segurança e
     * continua emitindo o cabeçalho textual.
     */
    public synchronized void tituloEpisodio(String nomeEpisodio, int indiceAtual, int totalEpisodios) {
        fecharBarraComSeguranca();
        String cabecalho = String.format("EPISÓDIO %d/%d: %s", indiceAtual, totalEpisodios, nomeEpisodio);
        String linha = "=".repeat(Math.max(cabecalho.length() + 8, 70));

        System.out.println();
        System.out.println(ANSI_CYAN + linha + ANSI_RESET);
        System.out.println(AnsiCores.BOLD + AnsiCores.YELLOW + ">>> " + cabecalho + ANSI_RESET);
        System.out.println(ANSI_CYAN + linha + ANSI_RESET);

    }

    /**
     * PROPÓSITO DE NEGÓCIO: publica uma mensagem operacional colorida no canal
     * acompanhado pelo Paulo durante traduções e correções.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada mensagem é emitida exatamente uma vez;
     * avisos incrementam o contador visual; falha cosmética da barra não afeta o job.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: desativa a barra incompatível e escreve
     * diretamente no console, sem propagar exceção para o pipeline.
     */
    public synchronized void log(String mensagem) {
        // INFO fica sem cor (herda o foreground padrão do terminal): cor é
        // reservada para o que precisa de atenção (sucesso/aviso/erro) e para
        // cabeçalhos, evitando fadiga visual em telas com muita linha de info.
        String cor = null;

        if (mensagem.contains("[ FAIL ]") || mensagem.contains("Erro") || mensagem.contains("Falha")) {
            cor = ANSI_RED;
        } else if (mensagem.contains("[ OK ]") || mensagem.contains("Sucesso") || mensagem.contains("Concluido") || mensagem.contains("concluido")) {
            cor = ANSI_GREEN;
        } else if (mensagem.contains("[ WARN ]")) {
            cor = ANSI_YELLOW;
            totalAvisos++;
        }

        // Aplica a cor na string final para o console visual do usuário
        String msgVisual = cor != null ? cor + mensagem + ANSI_RESET : mensagem;

        if (pb == null) {
            System.out.println(msgVisual);
            return;
        }

        // Emula o tqdm.write(): pausa o redesenho automático da barra (que corre
        // numa thread própria a cada 100ms) para que ele não escreva por cima
        // desta mensagem no meio da impressão — a versão anterior só dava um
        // "tick" (stepBy(0)) sem nenhuma garantia de exclusão mútua com aquela
        // thread, o que corrompia a barra ou a deixava com leitura desatualizada
        // (parecia "travada" numa porcentagem antiga).
        //
        // A biblioteca de terceiros (me.tongfei:progressbar) pode lançar
        // exceções de renderização dependendo do terminal/console (ex.:
        // `--console=plain` do Gradle). Isso é puramente cosmético e NUNCA deve
        // abortar a tradução em andamento — por isso qualquer falha aqui apenas
        // desativa a barra para o resto do episódio, em vez de propagar.
        boolean mensagemImpressa = false;
        try {
            pb.pause();
            if (TERMINAL_INTERATIVO) {
                System.out.print("\r\033[K"); // apaga a linha da barra só no terminal real
            }
            System.out.println(msgVisual);
            mensagemImpressa = true;
            pb.resume();
            pb.refresh();
        } catch (RuntimeException e) {
            log.warn("Barra de progresso falhou ao renderizar; desativando-a para o restante deste episódio: {}", e.getMessage());
            fecharBarraComSeguranca();
            if (!mensagemImpressa) {
                System.out.println(msgVisual);
            }
        }
    }

    public synchronized void passoConcluido(int lotes) {
        if (pb == null) {
            return;
        }
        try {
            pb.stepBy(lotes);
            pb.refresh();
        } catch (RuntimeException e) {
            log.warn("Barra de progresso falhou ao avançar; desativando-a para o restante deste episódio: {}", e.getMessage());
            fecharBarraComSeguranca();
        }
    }

    public synchronized void finalizar() {
        fecharBarraComSeguranca();
    }

    private void fecharBarraComSeguranca() {
        if (pb == null) {
            return;
        }
        try {
            pb.close();
        } catch (RuntimeException e) {
            log.warn("Falha ao fechar a barra de progresso (ignorada): {}", e.getMessage());
        } finally {
            pb = null;
        }
    }

    public synchronized void registrarFalasCache(int quantidade) {
        totalFalasCache += quantidade;
    }

    public synchronized void registrarFalasNovas(int quantidade) {
        totalFalasNovas += quantidade;
    }

    public synchronized int totalFalasCache() {
        return totalFalasCache;
    }

    public synchronized int totalFalasNovas() {
        return totalFalasNovas;
    }

    public synchronized int totalAvisos() {
        return totalAvisos;
    }
}
