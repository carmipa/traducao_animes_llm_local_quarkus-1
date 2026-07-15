package org.traducao.projeto.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.analisadorMidia.presentation.AnalisadorMidiaCLI;
import org.traducao.projeto.legendasExtracao.presentation.ExtratorCLI;
import org.traducao.projeto.mapaProjeto.presentation.MapaProjetoCLI;
import org.traducao.projeto.raspagemCorrecao.CorretorRaspagemCLI;
import org.traducao.projeto.raspagemRevisao.RevisorLegendasCLI;
import org.traducao.projeto.raspagemRevisao.RevisorRaspagemCLI;
import org.traducao.projeto.remuxer.presentation.RemuxerCLI;
import org.traducao.projeto.traducaoCorrige.CorretorCacheCLI;

/**
 * Dispara o modo CLI configurado em {@code app.modo}. No modo WEB nenhuma CLI e
 * executada. O modo {@code TRADUZIR} possui ciclo de vida proprio na fatia Traducao
 * Local ({@code traducao.presentation.bootstrap.TraducaoStartup}) e nao e roteado
 * aqui — por isso este dispatcher nao conhece nenhuma classe de {@code traducao}.
 */
@ApplicationScoped
public class ModoExecucaoStartup {

    private static final Logger log = LoggerFactory.getLogger(ModoExecucaoStartup.class);

    @ConfigProperty(name = "app.modo", defaultValue = "WEB")
    String modo;

    @Inject Instance<ExtratorCLI> extratorCli;
    @Inject Instance<CorretorCacheCLI> corretorCacheCli;
    @Inject Instance<CorretorRaspagemCLI> corretorRaspagemCli;
    @Inject Instance<RevisorRaspagemCLI> revisorRaspagemCli;
    @Inject Instance<RevisorLegendasCLI> revisorLegendasCli;
    @Inject Instance<AnalisadorMidiaCLI> analisadorMidiaCli;
    @Inject Instance<RemuxerCLI> remuxerCli;
    @Inject Instance<MapaProjetoCLI> mapaProjetoCli;

    void onStart(@Observes StartupEvent event) {
        if ("WEB".equalsIgnoreCase(modo) || "TRADUZIR".equalsIgnoreCase(modo)) {
            // WEB: nenhuma CLI. TRADUZIR: ciclo de vida proprio na fatia Traducao
            // Local (TraducaoStartup); nao e roteado por este dispatcher compartilhado.
            return;
        }

        ExecucaoCli cli = resolverModo(modo);
        if (cli == null) {
            log.error("Modo de execucao desconhecido: {}", modo);
            throw new IllegalStateException("Modo de execucao desconhecido: " + modo);
        }

        try {
            log.info("Iniciando modo CLI: {}", modo);
            cli.executar();
        } catch (Exception e) {
            log.error("Falha ao executar modo CLI {}", modo, e);
            throw new RuntimeException(e);
        }
    }

    private ExecucaoCli resolverModo(String modoAtual) {
        return switch (modoAtual.toUpperCase()) {
            case "EXTRAIR" -> extratorCli.get();
            case "CORRIGIR_CACHE" -> corretorCacheCli.get();
            case "RASPAGEM_CORRECAO" -> corretorRaspagemCli.get();
            case "RASPAGEM_REVISAO" -> revisorRaspagemCli.get();
            case "RASPAGEM_REVISAO_LEGENDAS" -> revisorLegendasCli.get();
            case "ANALISAR" -> analisadorMidiaCli.get();
            case "REMUXAR" -> remuxerCli.get();
            case "MAPEAR" -> mapaProjetoCli.get();
            default -> null;
        };
    }
}
