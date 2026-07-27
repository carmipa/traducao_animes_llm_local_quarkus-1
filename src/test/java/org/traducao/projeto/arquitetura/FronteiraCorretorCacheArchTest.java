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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela o estado ATUAL da área de correção de cache — as quatro fatias
 * {@code traducaoCorrige}, {@code raspagemCorrecao}, {@code raspagemRevisao} e
 * {@code correcaoLegendas} — como rede de segurança da FASE 0 do Plano-Mestre do corretor.
 *
 * <p>Diferente das sete fronteiras já existentes, este teste <b>não afirma que a arquitetura está
 * certa</b>. Ele afirma o contrário: registra a dívida medida e impede que ela cresça em silêncio
 * enquanto a refatoração acontece. É o "teste de caracterização antes de mexer" que o contrato
 * arquitetural exige.
 *
 * <h2>O que a primeira versão não enxergava</h2>
 * A versão de 2026-07-26 lia o grafo SEMPRE pela fatia de ORIGEM, e por isso era cega para tudo
 * que chegava à área de fora: declarava "exatamente 2 ciclos" quando havia <b>5</b> — os três
 * ausentes voltavam por {@code config} — e não via nenhuma das arestas ENTRANDO. Uma catraca que
 * mede menos do que anuncia é pior que catraca nenhuma, porque dá por coberto o que nunca olhou.
 * As cinco checagens de hoje cobrem os dois sentidos e as duas camadas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O inventário de arestas cross-fatia é <b>nominal e exato</b>, por FQN completo. Aresta
 *       nova reprova nomeando o par; aresta que SOME também reprova, porque a dívida encolheu e a
 *       lista tem de registrar o progresso.</li>
 *   <li>Os 5 ciclos que envolvem a área são declarados, detectados sobre o grafo do projeto
 *       INTEIRO — ciclo é propriedade do grafo, não da vizinhança de quem se está olhando.</li>
 *   <li>Quem DEPENDE da área também é congelado, não só de quem a área depende.</li>
 *   <li>{@code application} não alcança {@code infrastructure} — nem o da própria fatia.</li>
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
        // FASE 2, ultima fatia da area: as TRES arestas sairam, inclusive a que nascia no
        // DOMAIN -- a unica das nove que violava duas regras (dominio puro E fatia nao
        // depende de fatia). Agora so o ADAPTADOR alcanca `telemetria`.
        "correcaoLegendas.infrastructure.TelemetriaCorrecaoLegendasAdapter -> telemetria.OperacaoTelemetria",
        "correcaoLegendas.infrastructure.TelemetriaCorrecaoLegendasAdapter -> telemetria.TelemetriaService",
        "raspagemCorrecao.CorretorRaspagemCLI -> config.ExecucaoCli",
        // FASE 2: esta fatia saiu do TelemetriaService direto. A aresta agora nasce do ADAPTADOR,
        // atras da TelemetriaRaspagemCorrecaoPort -- a diferenca entre a divida e a forma prescrita.
        "raspagemCorrecao.infrastructure.TelemetriaRaspagemCorrecaoAdapter -> telemetria.TelemetriaService",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.application.ClassificadorEntradaCacheService",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.application.ContextoManutencaoCacheService",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.domain.ResultadoManutencaoCache",
        "raspagemCorrecao.application.CorrigirComGoogleUseCase -> traducaoCorrige.domain.ports.AuditoriaCorrecaoCachePort",
        "raspagemRevisao.RevisorLegendasCLI -> config.ExecucaoCli",
        // FASE 2: as QUATRO arestas de application desta fatia sairam. A dependencia para
        // `telemetria` agora nasce so do ADAPTADOR, atras da TelemetriaRevisaoPort.
        "raspagemRevisao.infrastructure.TelemetriaRevisaoAdapter -> telemetria.OperacaoTelemetria",
        "raspagemRevisao.infrastructure.TelemetriaRevisaoAdapter -> telemetria.TelemetriaService",
        "raspagemRevisao.RevisorRaspagemCLI -> config.ExecucaoCli",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.application.ClassificadorEntradaCacheService",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.application.ContextoManutencaoCacheService",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.domain.ResultadoManutencaoCache",
        "raspagemRevisao.application.RevisarCacheUseCase -> traducaoCorrige.domain.ports.AuditoriaCorrecaoCachePort",
        "raspagemRevisao.application.RevisarLegendasUseCase -> correcaoLegendas.application.SanitizadorTagsService",
        // Aresta ADICIONADA conscientemente em 2026-07-27 para FECHAR um furo de segurança, não por
        // conveniência: este caso de uso resolvia a lore pelo carimbo do cache sem passar pela
        // guarda obra×contexto, então um cache de Gundam 0083 carimbado "guilty_crown" era revisado
        // sob a lore errada — e ela ficava ativa para o arquivo seguinte. A alternativa era copiar
        // a política do veredicto para cá; duas cópias de uma guarda divergem, e foi assim que o
        // reforço de terminologia acabou com duas implementações desiguais. É a MESMA aresta que
        // RevisarCacheUseCase já tinha para o mesmo tipo, não um acoplamento de espécie nova.
        "raspagemRevisao.application.RevisarLegendasUseCase -> traducaoCorrige.application.ContextoManutencaoCacheService",
        "raspagemRevisao.application.RevisarLegendasUseCase -> raspagemCorrecao.application.ProtetorTermosLoreService",
        // FASE 2, recuperação externa: as duas arestas que este caso de uso tinha para o
        // `infrastructure` da fatia vizinha SUMIRAM — ele agora conhece só a própria
        // RecuperacaoExternaRevisaoPort. As três abaixo são o resíduo, e é preciso ler o que
        // mudou e o que NÃO mudou:
        //   MUDOU: a origem saiu de `application` e foi para `infrastructure`, e o destino saiu
        //   de uma CLASSE CONCRETA de infraestrutura e foi para o `domain.ports` publicado da
        //   vizinha. A violação de CAMADA acabou; nenhuma aplicação da área alcança mais
        //   infraestrutura alheia.
        //   NÃO MUDOU: continua sendo fatia→fatia, que o contrato proíbe. A alternativa era uma
        //   TERCEIRA cópia das 239 linhas do scraper — as duas que já existem (o scraper e o
        //   GoogleFallbackAdapter da fatia gold) já divergiram em produção, e o uso aqui é
        //   idêntico ao da vizinha, sem nenhuma divergência semântica que justificasse a cópia.
        //   Decidir "dono ou peer" é escolha estrutural com plano próprio; até lá, dívida
        //   declarada. Catraca verde em infra cruzada NÃO significa que isto fechou.
        "raspagemRevisao.infrastructure.GoogleRecuperacaoExternaAdapter -> raspagemCorrecao.domain.ResultadoRaspagem",
        "raspagemRevisao.infrastructure.GoogleRecuperacaoExternaAdapter -> raspagemCorrecao.domain.StatusRaspagem",
        "raspagemRevisao.infrastructure.GoogleRecuperacaoExternaAdapter -> raspagemCorrecao.domain.ports.RecuperacaoExternaPort",
        "traducaoCorrige.CorretorCacheCLI -> config.ExecucaoCli",
        // ADAPTADOR, não acoplamento novo de aplicação: é a FASE 2 do Plano-Mestre aterrissando
        // no código novo. A aresta para `telemetria` sai de INFRASTRUCTURE, atrás da
        // TelemetriaCorrecaoPort, e não da camada de aplicação — que é exatamente a diferença
        // entre a dívida das linhas acima e a forma que o contrato prescreve. O reforço de
        // terminologia tinha nascido SEM telemetria para não abrir aresta; o preço foi uma
        // operação que reescreve o acervo e não aparecia em telemetria, log nem relatório.
        "traducaoCorrige.infrastructure.TelemetriaCorrecaoAdapter -> telemetria.TelemetriaService",
        "traducaoCorrige.presentation.web.CorrecaoCacheController -> raspagemCorrecao.application.CorrigirComGoogleUseCase",
        "traducaoCorrige.presentation.web.CorrecaoCacheController -> raspagemRevisao.application.RevisarCacheUseCase");

    /**
     * Os dois CICLOS conhecidos, em forma canônica (par ordenado alfabeticamente). A fase C2 do
     * projeto quebrou o {@code config ⇄ traducao} e registrou isso como conquista; estes nunca
     * foram quebrados. A FASE 3 do Plano-Mestre existe para eliminá-los.
     */
    private static final Set<String> CICLOS_CONGELADOS = Set.of(
        // Os dois ciclos INTERNOS à área, alvo direto da FASE 3.
        "raspagemCorrecao <-> traducaoCorrige",
        "raspagemRevisao <-> traducaoCorrige",
        // Os três com `config`, que a primeira versão desta catraca NÃO ENXERGAVA: ela só
        // enumerava arestas cuja ORIGEM estava na área, e a perna de volta destes nasce fora
        // (`config.ModoExecucaoStartup` -> os CLIs da área). O teste afirmava "exatamente 2"
        // e havia 5 — uma catraca que mede menos do que anuncia é pior que catraca nenhuma,
        // porque dá por coberto o que não olhou. Mesma forma do `config ⇄ traducao` que a
        // fase C2 quebrou; estes seguem abertos.
        "config <-> raspagemCorrecao",
        "config <-> raspagemRevisao",
        "config <-> traducaoCorrige");

    /**
     * Arestas ENTRANDO na área — origem fora, destino dentro. A primeira versão desta catraca
     * era cega a elas pelo mesmo motivo dos ciclos: {@code arestasVivas()} filtra pela fatia de
     * ORIGEM. Sem esta lista, qualquer fatia nova podia passar a depender do corretor de cache
     * sem que o congelamento acusasse — e o propósito declarado dele é justamente impedir que o
     * acoplamento cresça em silêncio.
     */
    private static final Set<String> ARESTAS_ENTRANDO_CONGELADAS = Set.of(
        "config.ModoExecucaoStartup -> raspagemCorrecao.CorretorRaspagemCLI",
        "config.ModoExecucaoStartup -> raspagemRevisao.RevisorLegendasCLI",
        "config.ModoExecucaoStartup -> raspagemRevisao.RevisorRaspagemCLI",
        "config.ModoExecucaoStartup -> traducaoCorrige.CorretorCacheCLI");

    /**
     * Acessos de uma fatia ao {@code infrastructure} de OUTRA. Violam a regra de camada do
     * contrato — {@code application} depende de {@code domain.ports}, nunca de
     * {@code infrastructure} — e são o alvo direto da FASE 2 (portas).
     *
     * <p><b>VAZIA desde 2026-07-27 — a FASE 2 fechou esta lista.</b> Eram 4: a
     * {@code RecuperacaoExternaPort} eliminou 2 e a {@code AuditoriaCorrecaoCachePort} os outros 2.
     * Nenhuma camada de aplicação da área alcança mais a infraestrutura de outra fatia.
     *
     * <p><b>O que este zero NÃO significa.</b> Esta lista mede VIOLAÇÃO DE CAMADA, não
     * independência entre fatias. As duas raspagens continuam dependendo de {@code traducaoCorrige}
     * (agora pelo {@code domain.ports} e pelos tipos de domínio, não pela infraestrutura), e o
     * adaptador da revisão continua consumindo o {@code domain.ports} de {@code raspagemCorrecao} —
     * tudo declarado em {@link #ARESTAS_CONGELADAS}, com os motivos escritos lá. Os dois ciclos
     * internos da área seguem vivos e são a FASE 3. Ler este zero como "as fatias se soltaram"
     * seria o mesmo erro que a primeira versão desta catraca cometeu ao anunciar 2 ciclos havendo 5.
     */
    private static final Set<String> INFRA_CRUZADA_CONGELADA = Set.of();

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
     *
     * <p><b>VAZIA desde 2026-07-27 — a FASE 2 fechou esta lista.</b> Eram 5: a
     * {@code RecuperacaoExternaPort} eliminou 3, a {@code AuditoriaCorrecaoCachePort} 1 e a
     * {@code RelatorioCorrecaoLegendasPort} o último. A lista vazia deixa de registrar dívida e
     * passa a AFIRMAR a regra: nas quatro fatias da área, {@code application} não alcança
     * {@code infrastructure} — nem a da própria fatia. Qualquer caso novo reprova nomeando o tipo.
     */
    private static final Set<String> CAMADA_INTERNA_CONGELADA = Set.of();

    /**
     * Leitura de CONFIGURAÇÃO pela application — que NÃO é a mesma coisa que os casos acima e não
     * é dívida. A receita do projeto manda o {@code @ConfigurationProperties} morar em
     * {@code infrastructure/config}, e a fatia GOLD faz exatamente isto
     * ({@code traducao.application.ProcessarArquivoUseCase} lê {@code TradutorProperties}).
     *
     * <p>A distinção importa: os cinco de {@link #CAMADA_INTERNA_CONGELADA} são application
     * alcançando COMPORTAMENTO em infrastructure — o que a FASE 2 remove com portas. Este é
     * application lendo VALOR declarado. Misturar os dois numa lista só faria o número de dívidas
     * mentir, para cima, e faria a FASE 2 parecer inacabável.
     */
    private static final Set<String> CONFIG_LIDA_PELA_APPLICATION = Set.of(
        "traducaoCorrige.application.ReforcarTerminologiaCacheUseCase -> traducaoCorrige.infrastructure.config.CorrecaoCacheProperties");

    private static JavaClasses classesProducao;

    @BeforeAll
    static void importar() {
        classesProducao = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(RAIZ);
    }

    @Test
    @DisplayName("FASE 0: inventário exato das arestas cross-fatia da área de correção (28, congeladas)")
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
    @DisplayName("FASE 0: exatamente os 5 ciclos que envolvem a área — inclusive os que voltam por fora")
    void ciclosConhecidos() {
        // O grafo é montado sobre TODAS as fatias, não só as da área: um ciclo cuja perna de
        // volta nasce fora (config -> CLI da área) é invisível para quem só enumera a saída.
        Map<String, Set<String>> grafo = grafoEntreFatias();

        Set<String> ciclos = new TreeSet<>();
        for (Map.Entry<String, Set<String>> aresta : grafo.entrySet()) {
            for (String destino : aresta.getValue()) {
                if (!grafo.getOrDefault(destino, Set.of()).contains(aresta.getKey())) {
                    continue;
                }
                String origem = aresta.getKey();
                String a = origem.compareTo(destino) <= 0 ? origem : destino;
                String b = origem.compareTo(destino) <= 0 ? destino : origem;
                if (AREA.contains(a) || AREA.contains(b)) {
                    ciclos.add(a + " <-> " + b);
                }
            }
        }

        assertEquals(CICLOS_CONGELADOS, ciclos,
            "Ciclo NOVO envolvendo a área é regressão; ciclo quebrado é progresso e exige "
                + "atualizar CICLOS_CONGELADOS. Vivos: " + ciclos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela quem DEPENDE da área, não só de quem a área depende. As três
     * outras catracas leem o grafo pela fatia de ORIGEM e por isso eram estruturalmente cegas a
     * este lado — uma fatia nova podia passar a consumir o corretor de cache sem que nada
     * acusasse, exatamente o crescimento silencioso que este arquivo existe para impedir.
     *
     * <p>INVARIANTES DO DOMÍNIO: os 4 casos atuais são o bootstrap dos CLIs
     * ({@code config.ModoExecucaoStartup}) e são a perna de volta dos três ciclos com
     * {@code config}. Aresta nova reprova; aresta que sumir também, para registrar o progresso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: separa regressão de progresso, como as demais.
     */
    @Test
    @DisplayName("FASE 0: arestas ENTRANDO na área — 4 casos, o bootstrap dos CLIs")
    void arestasEntrandoNaAreaCongeladas() {
        Set<String> vivas = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            String origemFatia = fatiaDe(classe.getPackageName());
            if (origemFatia == null || LIVRES.contains(origemFatia) || AREA.contains(origemFatia)) {
                continue;
            }
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destinoFatia = fatiaDe(dependencia.getTargetClass().getPackageName());
                if (destinoFatia == null || !AREA.contains(destinoFatia)) {
                    continue;
                }
                vivas.add(curto(classe.getName()) + " -> " + curto(dependencia.getTargetClass().getName()));
            }
        }

        Set<String> novas = new TreeSet<>(vivas);
        novas.removeAll(ARESTAS_ENTRANDO_CONGELADAS);
        Set<String> sumiram = new TreeSet<>(ARESTAS_ENTRANDO_CONGELADAS);
        sumiram.removeAll(vivas);

        assertTrue(novas.isEmpty() && sumiram.isEmpty(),
            () -> "Quem consome a área de correção de cache também está congelado.\n"
                + (novas.isEmpty() ? "" : "\nNOVAS (regressão — outra fatia passou a depender da área):\n  "
                    + String.join("\n  ", novas))
                + (sumiram.isEmpty() ? "" : "\nSUMIRAM (progresso — atualize a lista):\n  "
                    + String.join("\n  ", sumiram)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: grafo fatia→fatia de TODO o projeto, ignorando {@code core} e os
     * cinco peers. É o insumo da detecção de ciclos, e precisa cobrir o projeto inteiro: ciclo é
     * uma propriedade do grafo, não da vizinhança de quem se está olhando.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; devolve o que encontrou.
     */
    private static Map<String, Set<String>> grafoEntreFatias() {
        Map<String, Set<String>> grafo = new TreeMap<>();
        for (JavaClass classe : classesProducao) {
            String origem = fatiaDe(classe.getPackageName());
            if (origem == null || LIVRES.contains(origem)) {
                continue;
            }
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = fatiaDe(dependencia.getTargetClass().getPackageName());
                if (destino == null || LIVRES.contains(destino) || destino.equals(origem)) {
                    continue;
                }
                grafo.computeIfAbsent(origem, k -> new TreeSet<>()).add(destino);
            }
        }
        return grafo;
    }

    @Test
    @DisplayName("FASE 2 FECHADA: acesso ao infrastructure de OUTRA fatia — ZERO")
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
                + "domain.ports. Eram 4; a RecuperacaoExternaPort eliminou 2. Os 2 que sobraram "
                + "são dívida declarada e a FASE 2 os remove. Caso novo é regressão. Cuidado ao "
                + "ler esta lista zerada: ela mede VIOLAÇÃO DE CAMADA, não independência entre "
                + "fatias — o adaptador da revisão ainda consome o domain.ports da correção.");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha o ponto cego das outras três catracas — elas só enxergam o que
     * ATRAVESSA a fronteira entre fatias, então {@code application} chamando o
     * {@code infrastructure} do MESMO módulo passava livre, embora seja a mesma violação de
     * camada que o contrato proíbe ("application depende de domain.ports, nunca de
     * infrastructure").
     *
     * <p>INVARIANTES DO DOMÍNIO: lista VAZIA — eram 5 casos legados em 2026-07-26 e as portas da
     * FASE 2 eliminaram todos. Caso
     * NOVO é regressão e reprova nomeando o tipo; caso que SUMIR também reprova, porque a FASE 2
     * do Plano-Mestre existe para eliminá-los com portas e o progresso tem de ser registrado.
     * A checagem olha o pacote de origem terminando em {@code .application} e o de destino
     * contendo {@code .infrastructure.} DA MESMA fatia — o caso cross-fatia já é coberto por
     * {@link #acessoCruzadoAInfrastructureCongelado()} e não é duplicado aqui.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista a diferença separando regressão de progresso.
     */
    @Test
    @DisplayName("FASE 2 FECHADA: application -> infrastructure da PRÓPRIA fatia — ZERO")
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
                String aresta = curto(classe.getName()) + " -> "
                    + curto(dependencia.getTargetClass().getName());
                // Configuração é lida, não alcançada: fica na lista própria.
                if (!CONFIG_LIDA_PELA_APPLICATION.contains(aresta)) {
                    vivos.add(aresta);
                }
            }
        }

        Set<String> novos = new TreeSet<>(vivos);
        novos.removeAll(CAMADA_INTERNA_CONGELADA);
        Set<String> sumiram = new TreeSet<>(CAMADA_INTERNA_CONGELADA);
        sumiram.removeAll(vivos);

        assertTrue(novos.isEmpty() && sumiram.isEmpty(),
            () -> "application não pode alcançar infrastructure — nem o da própria fatia. Eram 5; "
                + "a RecuperacaoExternaPort eliminou os 3 de raspagemCorrecao. Os 2 que sobraram "
                + "são dívida declarada que a FASE 2 remove com portas.\n"
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
