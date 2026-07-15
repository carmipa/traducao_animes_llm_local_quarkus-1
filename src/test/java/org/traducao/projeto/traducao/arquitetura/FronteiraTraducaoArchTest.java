package org.traducao.projeto.traducao.arquitetura;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a fronteira funcional da fatia vertical Tradução
 * Local ({@code org.traducao.projeto.traducao}). É a Camada A (estática, por
 * bytecode) do harness de fitness da FASE D: prova, a cada build, que a Tradução
 * Local só depende de outras fatias por meio de uma allowlist **estrita por
 * aresta exata** (FQN de origem → FQN de destino), que encolhe subfase a subfase
 * até restar somente o débito dos três controllers bloqueados para a C2. Analisa
 * dependências no bytecode, alcançando o que o import textual não mostra
 * (usos totalmente qualificados no corpo, campos, construtores, herança, genéricos).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li><b>Baseline dupla</b>: a auditoria por import textual encontrou
 *       <b>15</b> arestas funcionais; o bytecode revela <b>17</b> — as 2 extras
 *       são usos por FQN no corpo ({@code TraducaoController → LlmTelemetria} e
 *       {@code TelemetriaController → TelemetriaDatasetService}). Ao fim da FASE D
 *       o esperado é 8 imports / 9 arestas bytecode (só os três controllers C2).</li>
 *   <li>O conjunto real de arestas funcionais de saída da Tradução Local deve ser
 *       <b>exatamente</b> {@link #ARESTAS_FUNCIONAIS_ESPERADAS} (17). Aparecer nova
 *       aresta, mudar o destino para outra classe da mesma fatia, ou trocar import
 *       por FQN reprova o teste.</li>
 *   <li>Aresta técnica temporária de saída para {@code config}: exatamente
 *       {@link #ALLOW_CONFIG_CLI} (removida em D-Config).</li>
 *   <li>Regra reversa: {@code config} depende de {@code traducao} apenas por
 *       {@link #ALLOW_STARTUP_CLI} (removida em D-Config, junto de ALLOW-CONFIG-CLI).</li>
 *   <li>{@code core} é congelado <b>por tipo</b>: só os cinco tipos de
 *       {@link #CORE_TIPOS_CONGELADOS} podem ser usados; nenhum sexto tipo de core entra.</li>
 *   <li>Origem e destino são normalizados à respectiva classe de topo (um tipo
 *       aninhado pertence à mesma classe-alvo já catalogada). A granularidade
 *       permanece de <b>classe</b> exata — nunca de fatia.</li>
 *   <li>Somente classes de produção são analisadas ({@link ImportOption.Predefined#DO_NOT_INCLUDE_TESTS}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência (aresta inesperada, aresta esperada ausente, tipo de core
 * fora dos cinco, violação da regra reversa) reprova o teste listando exatamente
 * o desvio. Se o importador do ArchUnit não conseguir ler o bytecode Java 25
 * (class file major version 69), a importação lança e o teste erra — sinalizando
 * a incompatibilidade para decisão de Paulo, sem contorno.
 */
class FronteiraTraducaoArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String PKG_TRADUCAO = RAIZ + ".traducao";
    private static final String PKG_CONFIG = RAIZ + ".config";
    private static final String FATIA_TRADUCAO = "traducao";
    private static final String FATIA_CORE = "core";
    private static final String FATIA_CONFIG = "config";

    private static final String CLASSE_TRADUTOR_CLI = RAIZ + ".traducao.presentation.TradutorCLI";
    private static final String CLASSE_MODO_STARTUP = RAIZ + ".config.ModoExecucaoStartup";

    // Origens (produção, em traducao)
    private static final String PROCESSAR_ARQUIVO = RAIZ + ".traducao.application.ProcessarArquivoUseCase";
    private static final String PROCESSAR_EPISODIO = RAIZ + ".traducao.application.ProcessarEpisodioUseCase";
    private static final String TRADUCAO_CONTROLLER = RAIZ + ".traducao.presentation.web.TraducaoController";
    private static final String MISTRAL_ADAPTER = RAIZ + ".traducao.infrastructure.adapters.MistralClientAdapter";
    private static final String REST_CLIENT_CONFIG = RAIZ + ".traducao.infrastructure.config.RestClientConfig";
    private static final String CORRECAO_CACHE_CONTROLLER = RAIZ + ".traducao.presentation.web.CorrecaoCacheController";
    private static final String REVISAO_LEGENDAS_CONTROLLER = RAIZ + ".traducao.presentation.web.RevisaoLegendasController";
    private static final String TELEMETRIA_CONTROLLER = RAIZ + ".traducao.presentation.web.TelemetriaController";

    // Destinos (outras fatias)
    private static final String T_LLM_TELEMETRIA = RAIZ + ".telemetria.LlmTelemetria";
    private static final String T_TELEMETRIA_SERVICE = RAIZ + ".telemetria.TelemetriaService";
    private static final String T_TELEMETRIA_RESUMO = RAIZ + ".telemetria.TelemetriaResumo";
    private static final String T_TELEMETRIA_DATASET = RAIZ + ".telemetria.TelemetriaDatasetService";
    private static final String T_PROMPT_REVISAO_LORE = RAIZ + ".revisaoLore.application.PromptRevisaoLore";
    private static final String T_EXTRATOR_VIDEO_PORT = RAIZ + ".legendasExtracao.domain.ports.ExtratorVideoPort";
    private static final String T_EXTRATOR_STRATEGY = RAIZ + ".legendasExtracao.application.strategy.ExtratorStrategy";
    private static final String T_CORRIGIR_COM_GOOGLE = RAIZ + ".raspagemCorrecao.application.CorrigirComGoogleUseCase";
    private static final String T_REVISAR_CACHE = RAIZ + ".raspagemRevisao.application.RevisarCacheUseCase";
    private static final String T_LIMPAR_CACHE = RAIZ + ".traducaoCorrige.application.LimparCacheUseCase";
    private static final String T_RESULTADO_MANUTENCAO = RAIZ + ".traducaoCorrige.domain.ResultadoManutencaoCache";
    private static final String T_RESULTADO_REVISAO_LEG = RAIZ + ".raspagemRevisao.application.ResultadoRevisaoLegendas";
    private static final String T_REVISAR_LEGENDAS = RAIZ + ".raspagemRevisao.application.RevisarLegendasUseCase";

    /** Aresta técnica temporária de saída para config (removida em D-Config). */
    private static final String ALLOW_CONFIG_CLI = CLASSE_TRADUTOR_CLI + " -> " + RAIZ + ".config.ExecucaoCli";

    /** Aresta reversa temporária config → traducao (removida em D-Config). */
    private static final String ALLOW_STARTUP_CLI = CLASSE_MODO_STARTUP + " -> " + CLASSE_TRADUTOR_CLI;

    /**
     * Baseline exata: as 17 arestas funcionais reais no bytecode (origem FQN → destino FQN).
     * Autorização SEMPRE por aresta exata — nunca por nome de fatia.
     */
    private static final Set<String> ARESTAS_FUNCIONAIS_ESPERADAS = Set.of(
        // D-Tel vivo (5) — removidas em D-Tel-4
        aresta(PROCESSAR_ARQUIVO, T_LLM_TELEMETRIA),        // 1
        aresta(PROCESSAR_ARQUIVO, T_TELEMETRIA_SERVICE),    // 2
        aresta(PROCESSAR_EPISODIO, T_TELEMETRIA_SERVICE),   // 3
        aresta(TRADUCAO_CONTROLLER, T_TELEMETRIA_SERVICE),  // 4
        aresta(TRADUCAO_CONTROLLER, T_LLM_TELEMETRIA),      // 5 (FQ no corpo)
        // D-Lore (1) — removida em D-Lore
        aresta(MISTRAL_ADAPTER, T_PROMPT_REVISAO_LORE),     // 6
        // D-Ext (2) — removidas em D-Ext
        aresta(REST_CLIENT_CONFIG, T_EXTRATOR_VIDEO_PORT),  // 7
        aresta(REST_CLIENT_CONFIG, T_EXTRATOR_STRATEGY),    // 8
        // Controllers bloqueados para C2 (9) — permanecem até a C2
        aresta(CORRECAO_CACHE_CONTROLLER, T_CORRIGIR_COM_GOOGLE),  // 9
        aresta(CORRECAO_CACHE_CONTROLLER, T_REVISAR_CACHE),        // 10
        aresta(CORRECAO_CACHE_CONTROLLER, T_LIMPAR_CACHE),         // 11
        aresta(CORRECAO_CACHE_CONTROLLER, T_RESULTADO_MANUTENCAO), // 12
        aresta(REVISAO_LEGENDAS_CONTROLLER, T_RESULTADO_REVISAO_LEG), // 13
        aresta(REVISAO_LEGENDAS_CONTROLLER, T_REVISAR_LEGENDAS),      // 14
        aresta(TELEMETRIA_CONTROLLER, T_TELEMETRIA_RESUMO),   // 15
        aresta(TELEMETRIA_CONTROLLER, T_TELEMETRIA_SERVICE),  // 16
        aresta(TELEMETRIA_CONTROLLER, T_TELEMETRIA_DATASET)   // 17 (FQ no corpo)
    );

    /** Superfície técnica de core congelada POR TIPO (nenhum sexto tipo pode entrar). */
    private static final Set<String> CORE_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".core.util.ArquivoAtomicoUtil",
        RAIZ + ".core.io.DiretorioBaseKronos",
        RAIZ + ".core.execucao.FilaExecucaoPipeline",
        RAIZ + ".core.util.DuracaoUtil",
        RAIZ + ".core.exception.BasePipelineException"
    );

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("ArchUnit importa o bytecode Java 25 (class file major 69) da Tradução Local")
    void importaBytecodeJava25() {
        assertFalse(classesProducao.isEmpty(), "ArchUnit não importou nenhuma classe de produção");
        boolean achouTraducao = classesProducao.stream().anyMatch(c -> c.getName().equals(PROCESSAR_ARQUIVO));
        assertTrue(achouTraducao,
            "ArchUnit deve importar classes Java 25 da Tradução Local (prova de leitura do major 69)");
    }

    @Test
    @DisplayName("Saídas funcionais da Tradução Local == 17 arestas exatas (allowlist estrita por aresta)")
    void saidasFuncionaisBatemComAllowlistExata() {
        Set<String> reais = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                // Destino normalizado à classe de topo que o encerra: um tipo
                // aninhado (enum/record membro) pertence à mesma classe-alvo já
                // catalogada. Granularidade permanece de CLASSE, nunca de fatia.
                String destino = topo(dependencia.getTargetClass().getName());
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                if (fatia == null || fatia.equals(FATIA_TRADUCAO) || fatia.equals(FATIA_CORE)
                    || fatia.equals(FATIA_CONFIG)) {
                    continue; // core, config e interno tratados em testes próprios
                }
                reais.add(aresta(origem, destino));
            }
        }

        Set<String> inesperadas = new TreeSet<>(reais);
        inesperadas.removeAll(ARESTAS_FUNCIONAIS_ESPERADAS);
        Set<String> ausentes = new TreeSet<>(ARESTAS_FUNCIONAIS_ESPERADAS);
        ausentes.removeAll(reais);

        assertTrue(inesperadas.isEmpty() && ausentes.isEmpty(),
            () -> "Divergência na baseline exata de 17 arestas funcionais.\n"
                + "Arestas INESPERADAS (novas/destino trocado): " + inesperadas + "\n"
                + "Arestas ESPERADAS AUSENTES: " + ausentes);
    }

    @Test
    @DisplayName("Saída da Tradução Local para config == somente ALLOW-CONFIG-CLI")
    void saidaParaConfigApenasAllowConfigCli() {
        Set<String> reais = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                if (FATIA_CONFIG.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    reais.add(aresta(origem, topo(dependencia.getTargetClass().getName())));
                }
            }
        }
        assertTrue(reais.equals(Set.of(ALLOW_CONFIG_CLI)),
            () -> "Saída traducao→config deve ser somente ALLOW-CONFIG-CLI. Encontrado: " + reais);
    }

    @Test
    @DisplayName("config depende de traducao somente por ALLOW-STARTUP-CLI (regra reversa exata)")
    void configNaoDependeDeTraducaoExcetoStartupCli() {
        Set<String> reais = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoConfig(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                if (ehDaTraducao(dependencia.getTargetClass())) {
                    reais.add(aresta(origem, topo(dependencia.getTargetClass().getName())));
                }
            }
        }
        assertTrue(reais.equals(Set.of(ALLOW_STARTUP_CLI)),
            () -> "Reversa config→traducao deve ser somente ALLOW-STARTUP-CLI. Encontrado: " + reais);
    }

    @Test
    @DisplayName("core é congelado por tipo: somente os cinco tipos homologados")
    void coreCongeladoPorTipo() {
        List<String> violacoes = new ArrayList<>();
        Set<String> tiposCoreUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (FATIA_CORE.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    tiposCoreUsados.add(destino);
                    if (!CORE_TIPOS_CONGELADOS.contains(destino)) {
                        violacoes.add(origem + " -> " + destino);
                    }
                }
            }
        }
        assertFalse(tiposCoreUsados.isEmpty(),
            "Esperado uso técnico dos tipos de core congelados pela Tradução Local");
        assertTrue(violacoes.isEmpty(),
            () -> "Tipo de core fora dos cinco homologados:\n" + String.join("\n", new TreeSet<>(violacoes)));
    }

    private static String aresta(String origemFqn, String destinoFqn) {
        return origemFqn + " -> " + destinoFqn;
    }

    private static boolean ehDaTraducao(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_TRADUCAO) || pkg.startsWith(PKG_TRADUCAO + ".");
    }

    private static boolean ehDoConfig(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_CONFIG) || pkg.startsWith(PKG_CONFIG + ".");
    }

    /** Nome da classe de topo (normaliza classes internas/aninhadas da ORIGEM; o destino é mantido exato). */
    private static String topo(String nomeCompleto) {
        int cifrao = nomeCompleto.indexOf('$');
        return cifrao < 0 ? nomeCompleto : nomeCompleto.substring(0, cifrao);
    }

    /** Primeiro segmento após {@code org.traducao.projeto.} (a "fatia"), ou {@code null} se externo. */
    private static String fatiaDe(String pkg) {
        if (pkg == null || !pkg.startsWith(PREFIXO)) {
            return null;
        }
        String resto = pkg.substring(PREFIXO.length());
        int ponto = resto.indexOf('.');
        return ponto < 0 ? resto : resto.substring(0, ponto);
    }
}
