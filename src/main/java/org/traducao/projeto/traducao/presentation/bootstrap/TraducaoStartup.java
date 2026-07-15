package org.traducao.projeto.traducao.presentation.bootstrap;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.presentation.TradutorCLI;

/**
 * PROPÓSITO DE NEGÓCIO: é o ponto de partida (bootstrap) próprio da fatia vertical
 * Tradução Local. Observa a subida do Quarkus e, quando o operador seleciona o modo
 * {@code TRADUZIR}, dispara a CLI de tradução ({@link TradutorCLI}). Substitui o
 * roteamento que antes vinha do dispatcher compartilhado {@code config.ModoExecucaoStartup},
 * de modo que o ciclo de vida do modo TRADUZIR pertença exclusivamente à Tradução
 * Local — sem que a fatia {@code config} conheça qualquer classe de {@code traducao}.
 * Segue o mesmo molde dos demais observadores de {@code StartupEvent} já existentes
 * no slice ({@code BrowserLauncher}, {@code ConsoleRedirector}).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Age única e exclusivamente quando {@code app.modo == TRADUZIR}
 *       (case-insensitive); em qualquer outro modo (WEB ou demais CLIs) retorna
 *       sem efeito, preservando a exclusividade mútua com o dispatcher compartilhado.</li>
 *   <li>Não é o bootstrap global da aplicação: o container é iniciado implicitamente
 *       pelo Quarkus/CDI. Nenhum {@code Application.main()}, {@code @QuarkusMain} ou
 *       Composition Root artificial é introduzido.</li>
 *   <li>Delega integralmente a lógica de tradução a {@link TradutorCLI}; não duplica
 *       nem antecipa qualquer regra de negócio da tradução.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * <ul>
 *   <li>Se o modo for TRADUZIR mas {@link TradutorCLI} não estiver disponível no
 *       contexto CDI, lança {@link IllegalStateException} — falha explícita de
 *       inicialização, jamais silenciosa.</li>
 *   <li>Qualquer exceção lançada por {@link TradutorCLI#executar()} é registrada e
 *       re-propagada como {@link RuntimeException}, abortando a subida — preservando
 *       exatamente o contrato de falha que antes vinha de {@code ModoExecucaoStartup}.</li>
 * </ul>
 */
@Component
public class TraducaoStartup {

    private static final Logger log = LoggerFactory.getLogger(TraducaoStartup.class);
    private static final String MODO_TRADUZIR = "TRADUZIR";

    @ConfigProperty(name = "app.modo", defaultValue = "WEB")
    String modo;

    @Inject
    Instance<TradutorCLI> tradutorCli;

    /**
     * PROPÓSITO DE NEGÓCIO: reage à subida do Quarkus disparando a CLI de tradução
     * quando o modo configurado é TRADUZIR; nos demais modos não faz nada.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhum efeito fora do modo TRADUZIR; a resolução do
     * {@link TradutorCLI} ocorre apenas dentro do modo correto (o bean é condicional
     * a {@code app.modo=TRADUZIR}).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link IllegalStateException} se o bean da
     * CLI não for resolvível no modo TRADUZIR; {@link RuntimeException} envolvendo a
     * causa se {@code executar()} falhar (aborta o startup).
     *
     * @param event evento de inicialização observado do container CDI
     */
    void onStart(@Observes StartupEvent event) {
        if (!MODO_TRADUZIR.equalsIgnoreCase(modo)) {
            return;
        }

        if (!tradutorCli.isResolvable()) {
            log.error("Modo TRADUZIR ativo, mas TradutorCLI nao esta disponivel no contexto CDI");
            throw new IllegalStateException("TradutorCLI indisponivel para o modo TRADUZIR");
        }

        try {
            log.info("Iniciando modo CLI: {}", modo);
            tradutorCli.get().executar();
        } catch (Exception e) {
            log.error("Falha ao executar modo CLI {}", modo, e);
            throw new RuntimeException(e);
        }
    }
}
