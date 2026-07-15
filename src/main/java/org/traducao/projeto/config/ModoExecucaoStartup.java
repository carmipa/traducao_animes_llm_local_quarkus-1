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
import org.traducao.projeto.traducao.presentation.TradutorCLI;
import org.traducao.projeto.traducaoCorrige.CorretorCacheCLI;

/**
 * Dispara o modo CLI configurado em {@code app.modo}. No modo WEB nenhuma CLI e executada.
 */
@ApplicationScoped
public class ModoExecucaoStartup {

    private static final Logger log = LoggerFactory.getLogger(ModoExecucaoStartup.class);

    @ConfigProperty(name = "app.modo", defaultValue = "WEB")
    String modo;

    @Inject Instance<TradutorCLI> tradutorCli;
    @Inject Instance<ExtratorCLI> extratorCli;
    @Inject Instance<CorretorCacheCLI> corretorCacheCli;
    @Inject Instance<CorretorRaspagemCLI> corretorRaspagemCli;
    @Inject Instance<RevisorRaspagemCLI> revisorRaspagemCli;
    @Inject Instance<RevisorLegendasCLI> revisorLegendasCli;
    @Inject Instance<AnalisadorMidiaCLI> analisadorMidiaCli;
    @Inject Instance<RemuxerCLI> remuxerCli;
    @Inject Instance<MapaProjetoCLI> mapaProjetoCli;

    void onStart(@Observes StartupEvent event) {
        if ("WEB".equalsIgnoreCase(modo)) {
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
            case "TRADUZIR" -> tradutorCli.get();
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
