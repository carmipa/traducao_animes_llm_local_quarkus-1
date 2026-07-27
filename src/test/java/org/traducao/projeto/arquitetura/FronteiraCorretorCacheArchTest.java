package org.traducao.projeto.arquitetura;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela o estado ATUAL da área de correção de cache — as quatro fatias
 * {@code traducaoCorrige}, {@code raspagemCorrecao}, {@code raspagemRevisao} e
 * {@code correcaoLegendas} — como rede de segurança da FASE 0 do Plano-Mestre do corretor.
 *
 * <p>Diferente das sete fronteiras já existentes, este teste <b>não afirma que a arquitetura está
 * certa</b>. Ele afirma o contrário: registra a dívida medida em 2026-07-26 (6.723 linhas, 29
 * arestas cross-fatia, 2 ciclos, 4 acessos cruzados a {@code infrastructure}) e impede que ela
 * cresça em silêncio enquanto a refatoração acontece. É o "teste de caracterização antes de mexer"
 * que o contrato arquitetural exige.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O inventário de arestas cross-fatia é <b>nominal e exato</b>, por FQN completo. Aresta
 *       nova reprova nomeando o par; aresta que SOME também reprova, porque a dívida encolheu e a
 *       lista tem de registrar o progresso.</li>
 *   <li>Os dois ciclos conhecidos são declarados. Um ciclo NOVO reprova; quebrar um dos dois
 *       reprova também, forçando a atualização quando a FASE 3 acontecer.</li>
 *   <li>Dependências para {@code core} e para os cinco peers ({@code legenda},
 *       {@code cachetraducao}, {@code contexto}, {@code qualidadeTraducao}, {@code llm}) são
 *       livres — é o contrato do projeto e não conta como dívida.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem separa <b>arestas novas</b> (regressão: alguém acoplou mais) de <b>arestas que
 * sumiram</b> (progresso: atualizar a lista). Sem essa distinção, o teste vira ruído e é
 * desligado na primeira refatoração.
 */
class FronteiraCorretorCacheArchTest {

    private static final String RAIZ = "org.traducao.projeto";
    private static final String PREFIXO = RAIZ + ".";

    /** As quatro fatias que juntas formam a correção de cache. */
    private static final Set<String> AREA = Set.of(
        "traducaoCorrige", "raspagemCorrecao", "raspagemRevisao", "correcaoLegendas");

    /** Consumo livre por contrato: kernel técnico e os cinco peers compartilhados. */
    private static final Set<String> LIVRES = Set.of(
        "core", "legenda", "cachetraducao", "contexto", "qualidadeTraducao", "llm");

    /**
     * Inventário CONGELADO das arestas para outras fatias funcionais, medido em 2026-07-26.
     * Cada linha é dívida declarada, não aprovação.
     */
    private static final Set<String> ARESTAS_CONGELADAS = Set.of(
        "correcaoLegendas.application.CorrigirLegendasUseCase -> telemetria.TelemetriaService",
        "correcaoLegendas.domain.CorrecaoLegendasRelatorioJson -> telemetria.OperacaoTelemetria",
        "correcaoLegendas.infrastructure.CorrecaoLegendasLogPersistencia -> telemetria.TelemetriaService",
        "raspagemCorrecao.CorretorRaspagemCLI -> config.ExecucaoCli",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> telemetria.TelemetriaService",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.application.ClassificadorEntradaCacheService",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.application.ContextoManutencaoCacheService",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.domain.ResultadoManutencaoCache",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.infrastructure.CorrecaoCacheAuditoria",
        "raspagemRevisao.RevisorLegendasCLI -> config.ExecucaoCli",
        "raspagemRevisao.RevisorRaspagemCLI -> config.ExecucaoCli",
        "raspagemRevisao.application.RevisarCacheUseCase -> telemetria.TelemetriaService",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.application.ClassificadorEntradaCacheService",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.application.ContextoManutencaoCacheService",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.domain.ResultadoManutencaoCache",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.infrastructure.CorrecaoCacheAuditoria",
        "raspagemRevisao.application.RevisarLegendasUseCase -> correcaoLegendas.application.SanitizadorTagsService",
        "raspagemRevisao.application.RevisarLegendasUseCase -> raspagemCorrecao.application.ProtetorTermosLoreService",
        "raspagemRevisao.application.RevisarLegendasUseCase -> raspagemCorrecao.infrastructure.GoogleTranslateScraper",
        "raspagemRevisao.application.RevisarLegendasUseCase -> raspagemCorrecao.infrastructure.ResultadoRaspagem",
        "raspagemRevisao.application.RevisarLegendasUseCase -> telemetria.TelemetriaService",
        "raspagemRevisao.application.RevisorPtOnlyUseCase -> telemetria.OperacaoTelemetria",
        "raspagemRevisao.application.RevisorPtOnlyUseCase -> telemetria.TelemetriaService",
        "traducaoCorrige.CorretorCacheCLI -> config.ExecucaoCli",
        "traducaoCorrige.application.LimparCacheUseCase -> telemetria.TelemetriaService",
        "traducaoCorrige.presentation.web.CorrecaoCacheController -> raspagemCorrecao.application.CorrigirComGoogleUseCase",
        "traducaoCorrige.presentation.web.CorrecaoCacheController -> raspagemRevisao.application.RevisarCacheUseCase");

    /**
     * Os dois CICLOS conhecidos, em forma canônica (par ordenado alfabeticamente). A fase C2 do
     * projeto quebrou o {@code config ⇄ traducao} e registrou isso como conquista; estes nunca
     * foram quebrados. A FASE 3 do Plano-Mestre existe para eliminá-los.
     */
    private static final Set<String> CICLOS_CONGELADOS = Set.of(
        "raspagemCorrecao <-> traducaoCorrige",
        "raspagemRevisao <-> traducaoCorrige");

    /**
     * Acessos de uma fatia ao {@code infrastructure} de OUTRA. Violam a regra de camada do
     * contrato — {@code application} depende de {@code domain.ports}, nunca de
     * {@code infrastructure} — e são o alvo direto da FASE 2 (portas).
     */
    private static final Set<String> INFRA_CRUZADA_CONGELADA = Set.of(
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.infrastructure.CorrecaoCacheAuditoria",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.infrastructure.CorrecaoCacheAuditoria",
        "raspagemRevisao.application.RevisarLegendasUseCase -> raspagemCorrecao.infrastructure.GoogleTranslateScraper",
        "raspagemRevisao.application.RevisarLegendasUseCase -> raspagemCorrecao.infrastructure.ResultadoRaspagem");

    /**
     * Violações de CAMADA dentro da própria fatia: {@code application} alcançando o
     * {@code infrastructure} do mesmo módulo. O contrato manda depender de {@code domain.ports}.
     *
     * <p>Esta catraca nasceu de um ponto cego real. O inventário de arestas e o
     * {@link #acessoCruzadoAInfrastructureCongelado()} só enxergam o que atravessa a FRONTEIRA
     * ENTRE fatias — dentro da própria fatia, tudo passava. Um caso de uso novo escrito nesta
     * sessão declarava no Javadoc que evitava a telemetria "porque o ArchUnit reprovaria" e, na
     * mesma classe, injetava o {@code CorrecaoCacheAuditoria} concreto: a fronteira testada foi
     * respeitada, a não testada foi furada. Enquanto a regra não existir aqui, o teste ensina que
     * o certo é o que ele mede.
     */
    private static final Set<String> CAMADA_INTERNA_CONGELADA = Set.of(
        "correcaoLegendas.application.CorrigirLegendasUseCase -> correcaoLegendas.infrastructure.CorrecaoLegendasLogPersistencia",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> raspagemCorrecao.infrastructure.GoogleTranslateScraper",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> raspagemCorrecao.infrastructure.ResultadoRaspagem",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> raspagemCorrecao.infrastructure.StatusRaspagem",
        "traducaoCorrige.application.LimparCacheUseCase -> traducaoCorrige.infrastructure.CorrecaoCacheAuditoria");

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("FASE 0: inventário exato das arestas cross-fatia da área de correção (29, congeladas)")
    void arestasCrossFatiaCongeladas() {
        Set<String> vivas = arestasVivas();
        Set<String> novas = new TreeSet<>(vivas);
        novas.removeAll(ARESTAS_CONGELADAS);
        Set<String> sumiram = new TreeSet<>(ARESTAS_CONGELADAS);
        sumiram.removeAll(vivas);

        assertTrue(novas.isEmpty() && sumiram.isEmpty(),
            () -> "A área de correção de cache está congelada na FASE 0 do Plano-Mestre.\n"
                + (novas.isEmpty() ? "" : "\nARESTAS NOVAS (regressão — acoplou mais):\n  "
                    + String.join("\n  ", novas))
                + (sumiram.isEmpty() ? "" : "\nARESTAS QUE SUMIRAM (progresso — atualize a lista):\n  "
                    + String.join("\n  ", sumiram))
                + "\n\nvivas=" + vivas.size() + " congeladas=" + ARESTAS_CONGELADAS.size());
    }

    @Test
    @DisplayName("FASE 0: exatamente os 2 ciclos conhecidos (FASE 3 do plano existe para quebrá-los)")
    void ciclosConhecidos() {
        Set<String> paresVivos = new TreeSet<>();
        for (String aresta : arestasVivas()) {
            String origem = fatiaDaEntrada(aresta, true);
            String destino = fatiaDaEntrada(aresta, false);
            if (AREA.contains(origem) && AREA.contains(destino)) {
                paresVivos.add(origem + "->" + destino);
            }
        }

        Set<String> ciclos = new TreeSet<>();
        for (String par : paresVivos) {
            String[] p = par.split("->");
            if (paresVivos.contains(p[1] + "->" + p[0])) {
                String a = p[0].compareTo(p[1]) <= 0 ? p[0] : p[1];
                String b = p[0].compareTo(p[1]) <= 0 ? p[1] : p[0];
                ciclos.add(a + " <-> " + b);
            }
        }

        assertEquals(CICLOS_CONGELADOS, ciclos,
            "Ciclo NOVO entre fatias da área é regressão; ciclo quebrado é progresso e exige "
                + "atualizar CICLOS_CONGELADOS. Vivos: " + ciclos);
    }

    @Test
    @DisplayName("FASE 0: acesso ao infrastructure de OUTRA fatia — 4 casos, alvo da FASE 2")
    void acessoCruzadoAInfrastructureCongelado() {
        Set<String> vivos = new TreeSet<>();
        for (String aresta : arestasVivas()) {
            String destino = aresta.substring(aresta.indexOf("-> ") + 3);
            if (destino.contains(".infrastructure.")) {
                vivos.add(aresta);
            }
        }
        assertEquals(new TreeSet<>(INFRA_CRUZADA_CONGELADA), vivos,
            "Uma fatia não pode alcançar o infrastructure de outra: o contrato manda depender de "
                + "domain.ports. Os 4 casos atuais são dívida declarada e a FASE 2 os remove. "
                + "Caso novo é regressão.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha o ponto cego das outras três catracas — elas só enxergam o que
     * ATRAVESSA a fronteira entre fatias, então {@code application} chamando o
     * {@code infrastructure} do MESMO módulo passava livre, embora seja a mesma violação de
     * camada que o contrato proíbe ("application depende de domain.ports, nunca de
     * infrastructure").
     *
     * <p>INVARIANTES DO DOMÍNIO: inventário exato dos 5 casos legados, medido em 2026-07-26. Caso
     * NOVO é regressão e reprova nomeando o tipo; caso que SUMIR também reprova, porque a FASE 2
     * do Plano-Mestre existe para eliminá-los com portas e o progresso tem de ser registrado.
     * A checagem olha o pacote de origem terminando em {@code .application} e o de destino
     * contendo {@code .infrastructure.} DA MESMA fatia — o caso cross-fatia já é coberto por
     * {@link #acessoCruzadoAInfrastructureCongelado()} e não é duplicado aqui.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista a diferença separando regressão de progresso.
     */
    @Test
    @DisplayName("FASE 0: application -> infrastructure da PRÓPRIA fatia — 5 casos legados, alvo da FASE 2")
    void camadaInternaApplicationParaInfrastructureCongelada() {
        Set<String> vivos = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            String fatia = fatiaDe(classe.getPackageName());
            if (fatia == null || !AREA.contains(fatia)
                || !classe.getPackageName().endsWith(".application")) {
                continue;
            }
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String pacoteAlvo = dependencia.getTargetClass().getPackageName();
                if (!pacoteAlvo.startsWith(PREFIXO + fatia + ".infrastructure")) {
                    continue;
                }
                vivos.add(curto(classe.getName()) + " -> "
                    + curto(dependencia.getTargetClass().getName()));
            }
        }

        Set<String> novos = new TreeSet<>(vivos);
        novos.removeAll(CAMADA_INTERNA_CONGELADA);
        Set<String> sumiram = new TreeSet<>(CAMADA_INTERNA_CONGELADA);
        sumiram.removeAll(vivos);

        assertTrue(novos.isEmpty() && sumiram.isEmpty(),
            () -> "application não pode alcançar infrastructure — nem o da própria fatia. Os 5 casos "
                + "atuais são dívida declarada que a FASE 2 remove com portas.\n"
                + (novos.isEmpty() ? "" : "\nNOVOS (regressão — use uma porta em domain/ports):\n  "
                    + String.join("\n  ", novos))
                + (sumiram.isEmpty() ? "" : "\nSUMIRAM (progresso — atualize a lista):\n  "
                    + String.join("\n  ", sumiram)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: calcula, pelo bytecode, as arestas da área para outras fatias
     * funcionais — a mesma leitura que as sete fronteiras existentes fazem.
     *
     * <p>INVARIANTES DO DOMÍNIO: ignora {@code core} e os cinco peers (consumo livre por
     * contrato) e ignora arestas internas à própria fatia; nomes reduzidos ao tipo top-level
     * (classe aninhada normaliza para a externa) e sem o prefixo da raiz, para a lista congelada
     * ficar legível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; ausência de classes devolve conjunto vazio,
     * o que faz o teste reprovar mostrando que o inventário sumiu inteiro.
     */
    private static Set<String> arestasVivas() {
        Set<String> arestas = new LinkedHashSet<>();
        for (JavaClass classe : classesProducao) {
            String origemFatia = fatiaDe(classe.getPackageName());
            if (origemFatia == null || !AREA.contains(origemFatia)) {
                continue;
            }
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoFatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                if (destinoFatia == null
                    || LIVRES.contains(destinoFatia)
                    || destinoFatia.equals(origemFatia)) {
                    continue;
                }
                arestas.add(curto(classe.getName()) + " -> " + curto(dependencia.getTargetClass().getName()));
            }
        }
        return new TreeSet<>(arestas);
    }

    private static String fatiaDaEntrada(String aresta, boolean origem) {
        String lado = origem ? aresta.substring(0, aresta.indexOf(" ->")) : aresta.substring(aresta.indexOf("-> ") + 3);
        int ponto = lado.indexOf('.');
        return ponto < 0 ? lado : lado.substring(0, ponto);
    }

    private static String curto(String nomeCompleto) {
        int cifrao = nomeCompleto.indexOf('$');
        String topo = cifrao < 0 ? nomeCompleto : nomeCompleto.substring(0, cifrao);
        return topo.startsWith(PREFIXO) ? topo.substring(PREFIXO.length()) : topo;
    }

    private static String fatiaDe(String pkg) {
        if (pkg == null || !pkg.startsWith(PREFIXO)) {
            return null;
        }
        String resto = pkg.substring(PREFIXO.length());
        int ponto = resto.indexOf('.');
        return ponto < 0 ? resto : resto.substring(0, ponto);
    }
}
