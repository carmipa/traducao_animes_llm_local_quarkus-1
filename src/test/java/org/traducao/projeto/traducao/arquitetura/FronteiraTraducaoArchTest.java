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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a fronteira funcional da fatia vertical Tradução
 * Local ({@code org.traducao.projeto.traducao}). É a Camada A (estática, por
 * bytecode) do harness de fitness da FASE D: prova, a cada build, que a Tradução
 * Local só depende de outras fatias por meio de uma allowlist nominal explícita,
 * que encolhe subfase a subfase até restar somente o débito dos três controllers
 * bloqueados para a C2. Cobre o que o grep não vê (imports, herança, campos,
 * parâmetros de construtor, genéricos), pois analisa dependências no bytecode.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nenhuma classe de produção em {@code ..traducao..} depende de outra fatia
 *       (qualquer pacote sob {@code org.traducao.projeto} que não seja
 *       {@code traducao} nem {@code core}), exceto pelas arestas nominalmente
 *       registradas na allowlist.</li>
 *   <li>Allowlist inicial (baseline homologada): as 15 arestas funcionais
 *       catalogadas + a aresta técnica temporária ALLOW-CONFIG-CLI
 *       ({@code TradutorCLI → config.ExecucaoCli}, removida em D-Config).</li>
 *   <li>{@code core} é infraestrutura técnica congelada, classificada à parte —
 *       não conta como acoplamento funcional.</li>
 *   <li>Regra reversa: nenhuma classe de {@code ..config..} depende de
 *       {@code ..traducao..}, exceto ALLOW-STARTUP-CLI
 *       ({@code ModoExecucaoStartup → TradutorCLI}, removida em D-Config).</li>
 *   <li>Somente classes de produção são analisadas ({@link ImportOption.Predefined#DO_NOT_INCLUDE_TESTS}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer dependência fora da allowlist reprova o teste, listando cada aresta
 * violada (origem → alvo, fatia). Se o importador do ArchUnit não conseguir ler
 * o bytecode Java 25 (class file major version 69), a importação lança e o teste
 * erra — sinalizando a incompatibilidade para decisão de Paulo, sem contorno.
 */
class FronteiraTraducaoArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";
    private static final String PKG_TRADUCAO = RAIZ + ".traducao";
    private static final String PKG_CONFIG = RAIZ + ".config";
    private static final String FATIA_TRADUCAO = "traducao";
    private static final String FATIA_CORE = "core";

    private static final String CLASSE_TRADUTOR_CLI = RAIZ + ".traducao.presentation.TradutorCLI";
    private static final String CLASSE_MODO_STARTUP = RAIZ + ".config.ModoExecucaoStartup";

    /**
     * Allowlist nominal de SAÍDA (origem de produção em traducao → fatia de destino
     * tolerada temporariamente). As 15 arestas funcionais catalogadas colapsam em
     * pares (classe de origem → fatia), granularidade robusta a usos totalmente
     * qualificados que o import não mostra. ALLOW-CONFIG-CLI é a única aresta
     * técnica temporária. Cada subfase da FASE D remove daqui apenas o que eliminar.
     */
    private static final Map<String, Set<String>> ALLOWLIST_SAIDA = new LinkedHashMap<>();

    static {
        // --- D-Tel (removidas em D-Tel-4) ---
        // 1. ProcessarArquivoUseCase -> telemetria.LlmTelemetria
        // 2. ProcessarArquivoUseCase -> telemetria.TelemetriaService
        permitir(RAIZ + ".traducao.application.ProcessarArquivoUseCase", "telemetria");
        // 3. ProcessarEpisodioUseCase -> telemetria.TelemetriaService
        permitir(RAIZ + ".traducao.application.ProcessarEpisodioUseCase", "telemetria");
        // 4. TraducaoController -> telemetria.TelemetriaService (+ LlmTelemetria, uso FQ)
        permitir(RAIZ + ".traducao.presentation.web.TraducaoController", "telemetria");

        // --- D-Lore (removida em D-Lore) ---
        // 5. MistralClientAdapter -> revisaoLore.PromptRevisaoLore
        permitir(RAIZ + ".traducao.infrastructure.adapters.MistralClientAdapter", "revisaoLore");

        // --- D-Ext (removida em D-Ext) ---
        // 6. RestClientConfig -> legendasExtracao.ExtratorVideoPort
        // 7. RestClientConfig -> legendasExtracao.ExtratorStrategy
        permitir(RAIZ + ".traducao.infrastructure.config.RestClientConfig", "legendasExtracao");

        // --- Três controllers bloqueados (arestas 8-15; movidos na C2) ---
        // 8. CorrecaoCacheController -> raspagemCorrecao.CorrigirComGoogleUseCase
        // 9. CorrecaoCacheController -> raspagemRevisao.RevisarCacheUseCase
        // 10-11. CorrecaoCacheController -> traducaoCorrige.{LimparCacheUseCase, ResultadoManutencaoCache}
        permitir(RAIZ + ".traducao.presentation.web.CorrecaoCacheController", "raspagemCorrecao");
        permitir(RAIZ + ".traducao.presentation.web.CorrecaoCacheController", "raspagemRevisao");
        permitir(RAIZ + ".traducao.presentation.web.CorrecaoCacheController", "traducaoCorrige");
        // 12-13. RevisaoLegendasController -> raspagemRevisao.{ResultadoRevisaoLegendas, RevisarLegendasUseCase}
        permitir(RAIZ + ".traducao.presentation.web.RevisaoLegendasController", "raspagemRevisao");
        // 14-15. TelemetriaController -> telemetria.{TelemetriaResumo, TelemetriaService}
        permitir(RAIZ + ".traducao.presentation.web.TelemetriaController", "telemetria");

        // --- ALLOW-CONFIG-CLI (aresta técnica temporária; removida em D-Config) ---
        permitir(CLASSE_TRADUTOR_CLI, "config");
    }

    /** Pacotes de {@code core} usados hoje pela Tradução Local: superfície técnica congelada. */
    private static final Set<String> PACOTES_CORE_CONGELADOS = Set.of(
        RAIZ + ".core.exception",
        RAIZ + ".core.io",
        RAIZ + ".core.util",
        RAIZ + ".core.execucao"
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
        boolean achouTraducao = classesProducao.stream()
            .anyMatch(c -> c.getName().equals(RAIZ + ".traducao.application.ProcessarArquivoUseCase"));
        assertTrue(achouTraducao,
            "ArchUnit deve importar classes Java 25 da Tradução Local (prova de leitura do major 69)");
    }

    @Test
    @DisplayName("Tradução Local não depende de outras fatias além da allowlist nominal")
    void traducaoNaoDependeDeOutrasFatias() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String fatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                if (fatia == null || fatia.equals(FATIA_TRADUCAO) || fatia.equals(FATIA_CORE)) {
                    continue;
                }
                Set<String> toleradas = ALLOWLIST_SAIDA.get(origem);
                if (toleradas != null && toleradas.contains(fatia)) {
                    continue;
                }
                violacoes.add(origem + " -> " + dependencia.getTargetClass().getName() + "  [fatia: " + fatia + "]");
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "Saídas funcionais NÃO autorizadas na Tradução Local (fora da allowlist):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("config não depende de traducao, exceto ALLOW-STARTUP-CLI (regra reversa)")
    void configNaoDependeDeTraducao() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDoConfig(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                JavaClass alvo = dependencia.getTargetClass();
                if (!ehDaTraducao(alvo)) {
                    continue;
                }
                // ALLOW-STARTUP-CLI: única exceção nominal, some junto com ALLOW-CONFIG-CLI em D-Config.
                if (origem.equals(CLASSE_MODO_STARTUP) && alvo.getName().equals(CLASSE_TRADUTOR_CLI)) {
                    continue;
                }
                violacoes.add(origem + " -> " + alvo.getName());
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "config não pode depender da Tradução Local (fora de ALLOW-STARTUP-CLI):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("core é técnico congelado, classificado separadamente das fatias")
    void coreEhTecnicoCongeladoEClassificadoSeparadamente() {
        Set<String> pacotesCoreUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String pkg = dependencia.getTargetClass().getPackageName();
                if (pkg.equals(RAIZ + ".core") || pkg.startsWith(RAIZ + ".core.")) {
                    pacotesCoreUsados.add(pkg);
                }
            }
        }
        assertFalse(pacotesCoreUsados.isEmpty(),
            "Esperado uso técnico de core pela Tradução Local (superfície congelada)");
        assertTrue(PACOTES_CORE_CONGELADOS.containsAll(pacotesCoreUsados),
            () -> "Novo pacote de core fora da superfície congelada conhecida: " + pacotesCoreUsados);
    }

    private static void permitir(String origemFqn, String fatiaDestino) {
        ALLOWLIST_SAIDA.computeIfAbsent(origemFqn, k -> new TreeSet<>()).add(fatiaDestino);
    }

    private static boolean ehDaTraducao(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_TRADUCAO) || pkg.startsWith(PKG_TRADUCAO + ".");
    }

    private static boolean ehDoConfig(JavaClass classe) {
        String pkg = classe.getPackageName();
        return pkg.equals(PKG_CONFIG) || pkg.startsWith(PKG_CONFIG + ".");
    }

    /** Nome da classe de topo (remove sufixos de classes internas/aninhadas). */
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
