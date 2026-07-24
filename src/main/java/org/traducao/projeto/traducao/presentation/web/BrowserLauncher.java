package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Abre o navegador apos a inicializacao do Quarkus quando {@code app.modo=WEB}.
 */
@Component
public class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    /** Teto de espera pelo comando de abertura, para nao segurar a inicializacao. */
    private static final int TIMEOUT_ABERTURA_SEGUNDOS = 10;

    @ConfigProperty(name = "app.modo", defaultValue = "")
    String modoExecucao;

    @ConfigProperty(name = "quarkus.http.host", defaultValue = "127.0.0.1")
    String httpHost;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int httpPort;

    @ConfigProperty(name = "app.browser.auto-open", defaultValue = "true")
    boolean abrirNavegadorAutomatico;

    void onStart(@Observes StartupEvent event) {
        if (!"WEB".equals(modoExecucao)) {
            return;
        }

        String host = "0.0.0.0".equals(httpHost) ? "localhost" : httpHost;
        String url = "http://" + host + ":" + httpPort;

        System.out.println("\n==============================================================");
        System.out.println("   SERVIDOR WEB INICIADO COM SUCESSO!");
        System.out.println("   Acesse a interface visual em: \u001B[36m" + url + "\u001B[0m");
        System.out.println("==============================================================\n");

        if (abrirNavegadorAutomatico) {
            abrirNavegador(url);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: abre a interface no navegador padrão do operador assim que o servidor
     * sobe, para ele não precisar copiar a URL do console.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>No Windows o {@code start} recebe um TÍTULO VAZIO antes da URL. É obrigatório: o
     *       {@code start} interpreta o primeiro argumento entre aspas como título de janela, e o
     *       Java envolve a URL em aspas ao montar a linha de comando (por causa de {@code :} e
     *       {@code /}). Sem o {@code ""}, o Windows abre uma janela de console vazia intitulada
     *       {@code http://127.0.0.1:8099} e o navegador nunca sobe — que era o defeito.</li>
     *   <li>O sucesso é CONFIRMADO pelo código de saída do processo, não presumido. A versão
     *       anterior logava "Navegador aberto automaticamente" sempre que {@code exec} não
     *       lançasse — e {@code exec} só lança quando o processo não INICIA. O {@code cmd.exe}
     *       inicia sempre, então o log afirmava sucesso mesmo com o navegador fechado, o que
     *       manteve a falha invisível.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Nunca propaga: a aplicação já está no ar e a URL foi impressa no console. Falha de I/O,
     * código de saída diferente de zero ou espera interrompida viram WARN com a URL, para o
     * operador abrir à mão. A espera é limitada a {@value #TIMEOUT_ABERTURA_SEGUNDOS}s para não
     * segurar a inicialização se o navegador demorar a responder.
     */
    private void abrirNavegador(String url) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] comando;
        if (os.contains("win")) {
            // O "" e o titulo da janela: sem ele o start engole a URL como titulo.
            comando = new String[]{"cmd.exe", "/c", "start", "", url};
        } else if (os.contains("mac")) {
            comando = new String[]{"open", url};
        } else {
            comando = new String[]{"xdg-open", url};
        }
        try {
            Process processo = new ProcessBuilder(comando).start();
            if (!processo.waitFor(TIMEOUT_ABERTURA_SEGUNDOS, TimeUnit.SECONDS)) {
                processo.destroy();
                log.warn("O comando de abrir navegador nao respondeu a tempo. Abra manualmente: {}", url);
                return;
            }
            if (processo.exitValue() != 0) {
                log.warn("O comando de abrir navegador falhou (codigo {}). Abra manualmente: {}",
                    processo.exitValue(), url);
                return;
            }
            log.info("Navegador aberto automaticamente na URL: {}", url);
        } catch (IOException e) {
            log.warn("Nao foi possivel abrir o navegador automaticamente ({}). Abra manualmente: {}",
                e.getMessage(), url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Espera pela abertura do navegador interrompida. Abra manualmente: {}", url);
        }
    }
}
