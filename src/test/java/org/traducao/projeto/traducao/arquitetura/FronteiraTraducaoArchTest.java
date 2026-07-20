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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 *   <li><b>Baseline dupla (histórico D0)</b>: a auditoria por import textual
 *       encontrou <b>15</b> arestas funcionais; o bytecode revelava <b>17</b> — as
 *       2 extras eram usos por FQN no corpo ({@code TraducaoController → LlmTelemetria}
 *       e {@code TelemetriaController → TelemetriaDatasetService}).</li>
 *   <li><b>Após D-Ext</b>: eliminadas as 2 arestas {@code RestClientConfig →
 *       ExtratorVideoPort/ExtratorStrategy} (producers movidos para
 *       {@code legendasExtracao.ExtracaoBeansConfig}).</li>
 *   <li><b>Após D-Lore</b>: eliminada a aresta {@code LlmClientAdapter →
 *       PromptRevisaoLore} (Revisão de Lore com stack LLM própria).</li>
 *   <li><b>Após D-Tel-4</b>: eliminadas as 5 arestas vivas de bytecode do grupo
 *       D-Tel (ProcessarArquivoUseCase/ProcessarEpisodioUseCase/TraducaoController →
 *       telemetria), pois esses consumidores passaram a usar a telemetria própria
 *       ({@code TelemetriaTraducaoPort}); o bytecode caiu para <b>9</b> arestas
 *       funcionais — somente os três controllers bloqueados para a C2.</li>
 *   <li><b>Após a FASE C2</b>: os três controllers (CorrecaoCacheController,
 *       RevisaoLegendasController, TelemetriaController) foram movidos para suas
 *       fatias proprietárias e o kernel técnico de apresentação (PipelineWebSupport,
 *       OperacaoRequest, RespostaPadrao, AnsiCores, LogStreamService) foi extraído
 *       para {@code core.presentation}. A Tradução Local passa a ter <b>ZERO</b>
 *       arestas funcionais de saída; a exceção ALLOW-TELEMETRIA-C2 foi eliminada.</li>
 *   <li>O conjunto real de arestas funcionais de saída da Tradução Local deve ser
 *       <b>exatamente</b> {@link #ARESTAS_FUNCIONAIS_ESPERADAS} (VAZIO após a C2).</li>
 *   <li><b>Regra dedicada revisaoLore</b>: nenhuma classe de {@code ..revisaoLore..}
 *       depende de {@code LlmPort}, {@code StatusLlm}, {@code LlmProperties},
 *       {@code JsonHttpClient}, {@code RecordsLlm}, {@code LlmClientAdapter}
 *       ou {@code GerenciadorContexto}.</li>
 *   <li><b>Regra blindada telemetria</b>: nenhuma classe de {@code ..traducao..}
 *       depende de {@code ..telemetria..} (ZERO — exceção C2 eliminada).</li>
 *   <li><b>Independência do core</b>: nenhuma classe de {@code ..core..} depende de
 *       qualquer fatia funcional; o core só pode usar JDK, bibliotecas técnicas e o
 *       próprio {@code core}. Nenhuma allowlist {@code core → fatia} é permitida.</li>
 *   <li><b>Após D-Config</b>: a fronteira {@code config ⇄ traducao} é <b>zero nos
 *       dois sentidos</b>. As exceções nominais ALLOW-CONFIG-CLI
 *       ({@code TradutorCLI → config.ExecucaoCli}) e ALLOW-STARTUP-CLI
 *       ({@code ModoExecucaoStartup → TradutorCLI}) foram eliminadas juntas: a
 *       {@code TradutorCLI} não implementa mais {@code ExecucaoCli} e o modo TRADUZIR
 *       ganhou bootstrap próprio ({@code traducao.presentation.bootstrap.TraducaoStartup}),
 *       de modo que {@code config} não conhece nenhuma classe de {@code traducao}.</li>
 *   <li>{@code core} é congelado <b>por tipo</b>: só os doze tipos de
 *       {@link #CORE_TIPOS_CONGELADOS} podem ser usados (5 técnicos + 5 do kernel Web
 *       extraído na C2 + 1 da UI de console migrada na E2 + 1 cliente HTTP da E4a);
 *       nenhum décimo-terceiro tipo de core entra.</li>
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
    private static final String FATIA_LEGENDA = "legenda";
    private static final String FATIA_CACHETRADUCAO = "cachetraducao";
    private static final String FATIA_CONTEXTO = "contexto";
    private static final String FATIA_QUALIDADETRADUCAO = "qualidadeTraducao";
    private static final String FATIA_LLM = "llm";

    // Origens (produção, em traducao) ainda referenciadas pelos testes.
    private static final String PROCESSAR_ARQUIVO = RAIZ + ".traducao.application.ProcessarArquivoUseCase";
    private static final String LLM_ADAPTER = RAIZ + ".traducao.infrastructure.adapters.LlmClientAdapter";

    // FASE C2 CONCLUÍDA: os três controllers bloqueados (CorrecaoCacheController,
    // RevisaoLegendasController, TelemetriaController) saíram de traducao para suas
    // fatias proprietárias (traducaoCorrige / raspagemRevisao / telemetria .presentation.web).
    // Com isso, as 9 arestas funcionais de saída foram ELIMINADAS e a allowlist ficou VAZIA.

    // Pacote da fatia Revisão de Lore e os tipos que a stack LLM própria dela NÃO pode
    // importar (D-Lore). Após a E8d, o contrato do LLM e seus records vivem no peer
    // {@code llm.domain}; a proibição acompanha os novos FQNs para continuar impedindo que
    // revisaoLore volte a acoplar-se à stack LLM compartilhada (porta, status, properties,
    // DTOs, adapter concreto e o gerenciador de contexto).
    private static final String PKG_REVISAO_LORE = RAIZ + ".revisaoLore";
    private static final Set<String> REVISAO_LORE_PROIBIDOS = Set.of(
        RAIZ + ".llm.domain.LlmPort",
        RAIZ + ".llm.domain.StatusLlm",
        RAIZ + ".traducao.infrastructure.config.LlmProperties",
        RAIZ + ".traducao.infrastructure.dtos.RecordsLlm",
        LLM_ADAPTER,
        RAIZ + ".contexto.infrastructure.GerenciadorContexto"
    );

    // Pacote do módulo de telemetria. Após a FASE C2 (TelemetriaController movido para
    // telemetria.presentation.web), NENHUMA classe de traducao depende de telemetria —
    // a exceção nominal ALLOW-TELEMETRIA-C2 foi eliminada.
    private static final String PKG_TELEMETRIA = RAIZ + ".telemetria";

    // D-Config CONCLUÍDA: as exceções nominais ALLOW-CONFIG-CLI (traducao → config)
    // e ALLOW-STARTUP-CLI (config → traducao) foram eliminadas juntas. A fronteira
    // config ⇄ traducao é agora ZERO nos dois sentidos (ver testes dedicados abaixo).

    /**
     * Baseline exata após a FASE C2: a Tradução Local possui <b>ZERO</b> arestas
     * funcionais de saída para outras fatias. Os três controllers bloqueados foram
     * movidos para suas fatias proprietárias; não há mais nenhuma dependência de
     * saída de {@code traducao..} para código de outra fatia funcional.
     */
    private static final Set<String> ARESTAS_FUNCIONAIS_ESPERADAS = Set.of();

    // Fatia (primeiro segmento após org.traducao.projeto.) do pacote core.
    private static final String FATIA_CORE_NOME = "core";

    /**
     * Superfície técnica de core congelada POR TIPO. Após a FASE C2, o kernel técnico
     * de apresentação foi extraído para {@code core.presentation}: os cinco tipos
     * originais somam-se às cinco classes-kernel Web movidas (PipelineWebSupport,
     * OperacaoRequest, RespostaPadrao, AnsiCores, LogStreamService), ao ConsoleEntrada
     * migrado na E2 (core.presentation.ui) e ao JsonHttpClient neutralizado na E4a
     * (core.infrastructure.http) — exatamente DOZE tipos congelados; nenhum
     * décimo-terceiro tipo de core pode entrar.
     */
    private static final Set<String> CORE_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".core.util.ArquivoAtomicoUtil",
        RAIZ + ".core.io.DiretorioBaseKronos",
        RAIZ + ".core.execucao.FilaExecucaoPipeline",
        RAIZ + ".core.util.DuracaoUtil",
        RAIZ + ".core.exception.BasePipelineException",
        RAIZ + ".core.presentation.web.PipelineWebSupport",
        RAIZ + ".core.presentation.web.OperacaoRequest",
        RAIZ + ".core.presentation.web.RespostaPadrao",
        RAIZ + ".core.presentation.web.LogStreamService",
        RAIZ + ".core.presentation.ui.AnsiCores",
        RAIZ + ".core.presentation.ui.ConsoleEntrada",
        RAIZ + ".core.infrastructure.http.JsonHttpClient"
    );

    /**
     * Superfície do módulo peer {@code legenda} congelada POR TIPO. A Tradução Local só
     * pode depender nominalmente destes tipos; qualquer tipo extra reprova até autorização
     * explícita (sem flexibilização genérica do pacote legenda).
     * <ul>
     *   <li>E3c: {@code PoliticaEstiloMusical} (política de estilo musical).</li>
     *   <li>E5a: {@code DocumentoLegenda} e {@code EventoLegenda} (modelo puro de legenda,
     *       movido de {@code traducao.domain.legenda}).</li>
     *   <li>E5b: {@code ExcecaoLegenda} (raiz das falhas do módulo) e
     *       {@code ArquivoLegendaException} (falha de I/O de legenda), movidos de
     *       {@code traducao.domain.exceptions}. {@code EntradaJaTraduzidaException}
     *       permanece em {@code traducao} e NÃO entra neste conjunto.</li>
     *   <li>E5c: {@code LeitorLegendaAss/Srt} e {@code EscritorLegendaAss/Srt} (I/O de
     *       legenda), movidos de {@code traducao.infrastructure.legenda} para
     *       {@code legenda.infrastructure}. {@code MascaradorTags} saiu de traducao na E8b
     *       para o peer {@code qualidadeTraducao} (ver {@link #QUALIDADE_TRADUCAO_TIPOS_CONGELADOS}).</li>
     *   <li>E8a: {@code DetectorEfeitoKaraokeService} (regra única música/karaokê),
     *       movido de {@code traducao.application} para {@code legenda.application};
     *       consumido pela Tradução Local via {@code ProcessarArquivoUseCase}.</li>
     * </ul>
     */
    private static final Set<String> LEGENDA_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".legenda.domain.PoliticaEstiloMusical",
        RAIZ + ".legenda.domain.DocumentoLegenda",
        RAIZ + ".legenda.domain.EventoLegenda",
        RAIZ + ".legenda.domain.ExcecaoLegenda",
        RAIZ + ".legenda.domain.ArquivoLegendaException",
        RAIZ + ".legenda.infrastructure.LeitorLegendaAss",
        RAIZ + ".legenda.infrastructure.LeitorLegendaSrt",
        RAIZ + ".legenda.infrastructure.EscritorLegendaAss",
        RAIZ + ".legenda.infrastructure.EscritorLegendaSrt",
        RAIZ + ".legenda.application.DetectorEfeitoKaraokeService"
    );

    /**
     * Superfície do peer {@code cachetraducao} (E6) congelada POR TIPO, SEPARADA de
     * {@link #LEGENDA_TIPOS_CONGELADOS}. A Tradução Local só pode depender nominalmente
     * destes tipos do peer de cache; qualquer tipo extra reprova até autorização explícita
     * (sem liberação genérica do pacote {@code cachetraducao}). Contém apenas os tipos que
     * a fatia {@code traducao} consome de fato (via {@code ProcessarArquivoUseCase});
     * {@code CacheDocumento} e {@code CacheManutencaoService} não entram por não serem
     * consumidos diretamente pela fatia {@code traducao}.
     */
    private static final Set<String> CACHE_TRADUCAO_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".cachetraducao.infrastructure.CacheTraducaoService",
        RAIZ + ".cachetraducao.domain.EntradaCache",
        RAIZ + ".cachetraducao.domain.ProvenienciaCache"
    );

    /**
     * Superfície do peer {@code contexto} congelada POR TIPO (medida na E7b), SEPARADA dos
     * demais peers. A Tradução Local só pode depender nominalmente destes tipos; qualquer
     * tipo extra reprova até autorização explícita (sem liberação genérica do pacote
     * {@code contexto}). É EXATA: {@link #contextoCongeladoPorTipo()} exige igualdade
     * {@code tiposContextoUsados == CONTEXTO_TIPOS_CONGELADOS}, reprovando tanto tipo novo
     * inesperado quanto entrada obsoleta sobrando na allowlist. Conteúdo medido pós-E7b:
     * <ul>
     *   <li>{@code GerenciadorContexto} (infrastructure): consumido por classes de
     *       {@code traducao} (ProcessarArquivoUseCase, LlmClientAdapter, controllers e,
     *       desde a E8c.1, {@code LoreAtivaContextoAdapter}) — migrou do {@code traducao} para
     *       o peer na E7b. Na E8c.1 o {@code DetectorTraducaoIdenticaService} deixou de
     *       consumi-lo diretamente (saiu para {@code qualidadeTraducao} e passou a depender da
     *       porta {@code LoreAtivaPort}); o adapter assumiu essa dependência, então o tipo
     *       {@code GerenciadorContexto} permanece consumido por traducao e no congelamento.</li>
     *   <li>{@code ProvedorContexto}: {@code PipelineController.getProvedores()}
     *       (lambda {@code p -> new ContextoResponse(p.getId()...)}).</li>
     *   <li>{@code RegrasConcordanciaPtBr}: {@code LlmClientAdapter} (bloco de tradução
     *       e prompt de revisão de concordância).</li>
     * </ul>
     * SAÍRAM na E7b (deixaram de ser consumidos diretamente por {@code traducao}, pois só o
     * manager os usava): {@code ContextoPrompt} (obterLoreAtiva) e
     * {@code ContextoNaoEncontradoException} (lançada pelo manager). As 56 lores, as três
     * agregadoras Macross, {@code ExcecaoContexto} e {@code ContextoBeansConfig} NÃO entram.
     */
    private static final Set<String> CONTEXTO_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".contexto.infrastructure.GerenciadorContexto",
        RAIZ + ".contexto.domain.ProvedorContexto",
        RAIZ + ".contexto.domain.RegrasConcordanciaPtBr"
    );

    /**
     * PROPÓSITO DE NEGÓCIO: superfície do peer {@code qualidadeTraducao} (E8b/E8c/E8c.1)
     * congelada POR TIPO com igualdade EXATA, SEPARADA dos demais peers. A Tradução Local só
     * pode depender nominalmente destes tipos; qualquer tipo extra reprova até autorização
     * explícita (sem liberação genérica do pacote {@code qualidadeTraducao}).
     *
     * <p>INVARIANTES DO DOMÍNIO: contém apenas os tipos que a fatia {@code traducao}
     * consome de fato (medido): {@code MascaradorTags} e {@code AlucinacaoDetectadaException}
     * desde a E8b; {@code ValidadorTraducaoService} (via {@code ProcessarArquivoUseCase} e
     * {@code ProcessarEpisodioUseCase}) e {@code ProtecaoLegendaAssService} (via
     * {@code ProcessarArquivoUseCase}) desde a E8c; {@code DetectorTraducaoIdenticaService}
     * (via {@code ProcessarArquivoUseCase}) e {@code LoreAtivaPort} (implementada pelo
     * {@code LoreAtivaContextoAdapter} em {@code traducao.infrastructure}) desde a E8c.1.
     * {@code ExcecaoQualidadeTraducao} NÃO entra por ser base interna da exceção, sem consumo
     * direto medido. O nested {@code MascaradorTags$Mascarado} é normalizado ao proprietário
     * {@code MascaradorTags}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #qualidadeTraducaoCongeladoPorTipo()} exige
     * igualdade {@code tiposUsados == QUALIDADE_TRADUCAO_TIPOS_CONGELADOS}, reprovando tanto
     * tipo novo inesperado quanto entrada obsoleta na allowlist.
     */
    private static final Set<String> QUALIDADE_TRADUCAO_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".qualidadeTraducao.application.DetectorTraducaoIdenticaService",
        RAIZ + ".qualidadeTraducao.application.MascaradorTags",
        // NormalizadorAcentosComuns movido de traducao.application para o peer: fecha o arco
        // proibido raspagemRevisao.RevisorPtOnlyService -> traducao (INBOUND voltou a 0).
        RAIZ + ".qualidadeTraducao.application.NormalizadorAcentosComuns",
        RAIZ + ".qualidadeTraducao.application.ProtecaoLegendaAssService",
        RAIZ + ".qualidadeTraducao.application.ValidadorTraducaoService",
        RAIZ + ".qualidadeTraducao.domain.AlucinacaoDetectadaException",
        RAIZ + ".qualidadeTraducao.domain.LoreAtivaPort"
    );

    /**
     * PROPÓSITO DE NEGÓCIO: superfície do peer de topo {@code llm} (E8d) congelada POR TIPO
     * com igualdade EXATA, SEPARADA dos demais peers. A Tradução Local só pode depender
     * nominalmente destes tipos; qualquer tipo extra reprova até autorização explícita (sem
     * liberação genérica do pacote {@code llm}).
     *
     * <p>INVARIANTES DO DOMÍNIO: contém apenas os tipos que a fatia {@code traducao} consome
     * de fato (medido) — o contrato {@code LlmPort} e seus records {@code Lote},
     * {@code TraducaoLote} e {@code StatusLlm} — via {@code LlmClientAdapter} (ponto de
     * composição), use cases e presentation. O adapter permanece em
     * {@code traducao.infrastructure}; por isso traducao consome o peer {@code llm}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #llmCongeladoPorTipo()} exige igualdade
     * {@code tiposUsados == LLM_TIPOS_CONGELADOS}, reprovando tanto tipo novo inesperado
     * quanto entrada obsoleta na allowlist.
     */
    private static final Set<String> LLM_TIPOS_CONGELADOS = Set.of(
        RAIZ + ".llm.domain.LlmPort",
        RAIZ + ".llm.domain.Lote",
        RAIZ + ".llm.domain.StatusLlm",
        RAIZ + ".llm.domain.TraducaoLote"
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
    @DisplayName("Saídas funcionais da Tradução Local == ZERO arestas (FASE C2 concluída)")
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
                    || fatia.equals(FATIA_CONFIG) || fatia.equals(FATIA_LEGENDA)
                    || fatia.equals(FATIA_CACHETRADUCAO) || fatia.equals(FATIA_CONTEXTO)
                    || fatia.equals(FATIA_QUALIDADETRADUCAO) || fatia.equals(FATIA_LLM)) {
                    continue; // core, config, legenda, cachetraducao, contexto e qualidadeTraducao (peers) e interno tratados em testes próprios
                }
                reais.add(aresta(origem, destino));
            }
        }

        Set<String> inesperadas = new TreeSet<>(reais);
        inesperadas.removeAll(ARESTAS_FUNCIONAIS_ESPERADAS);
        Set<String> ausentes = new TreeSet<>(ARESTAS_FUNCIONAIS_ESPERADAS);
        ausentes.removeAll(reais);

        assertTrue(inesperadas.isEmpty() && ausentes.isEmpty(),
            () -> "Após a FASE C2, a Tradução Local deve ter ZERO arestas funcionais de saída.\n"
                + "Arestas INESPERADAS (bumerangue/saída remanescente): " + inesperadas + "\n"
                + "Arestas ESPERADAS AUSENTES: " + ausentes);
    }

    @Test
    @DisplayName("Saída da Tradução Local para config == ZERO arestas (D-Config concluída)")
    void saidaParaConfigEhZero() {
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
        assertTrue(reais.isEmpty(),
            () -> "Após D-Config nenhuma classe de traducao pode depender de config "
                + "(ALLOW-CONFIG-CLI eliminada). Encontrado: " + reais);
    }

    @Test
    @DisplayName("config não depende de traducao (regra reversa exata: ZERO arestas, D-Config concluída)")
    void configNaoDependeDeTraducao() {
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
        assertTrue(reais.isEmpty(),
            () -> "Após D-Config a fatia config não pode depender de traducao "
                + "(ALLOW-STARTUP-CLI eliminada; TRADUZIR tem bootstrap próprio). Encontrado: " + reais);
    }

    @Test
    @DisplayName("core é congelado por tipo: somente os doze tipos homologados (5 técnicos + 5 kernel Web da C2 + 1 UI de console da E2 + 1 cliente HTTP da E4a)")
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
            () -> "Tipo de core fora dos doze homologados:\n" + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("legenda é congelado por tipo: Tradução Local só usa os tipos homologados do módulo (E3c/E5a/E5b + E5c: Leitor/Escritor Ass/Srt)")
    void legendaCongeladoPorTipo() {
        List<String> violacoes = new ArrayList<>();
        Set<String> tiposLegendaUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (FATIA_LEGENDA.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    tiposLegendaUsados.add(destino);
                    if (!LEGENDA_TIPOS_CONGELADOS.contains(destino)) {
                        violacoes.add(origem + " -> " + destino);
                    }
                }
            }
        }
        assertFalse(tiposLegendaUsados.isEmpty(),
            "Esperado uso do módulo legenda pela Tradução Local (E3c: PoliticaEstiloMusical)");
        assertTrue(violacoes.isEmpty(),
            () -> "Tipo de legenda fora dos homologados (LEGENDA_TIPOS_CONGELADOS):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("cachetraducao é congelado por tipo: Tradução Local só usa os tipos homologados do peer (E6)")
    void cacheTraducaoCongeladoPorTipo() {
        List<String> violacoes = new ArrayList<>();
        Set<String> tiposCacheUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (FATIA_CACHETRADUCAO.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    tiposCacheUsados.add(destino);
                    if (!CACHE_TRADUCAO_TIPOS_CONGELADOS.contains(destino)) {
                        violacoes.add(origem + " -> " + destino);
                    }
                }
            }
        }
        assertFalse(tiposCacheUsados.isEmpty(),
            "Esperado uso do peer cachetraducao pela Tradução Local (E6: CacheTraducaoService/EntradaCache/ProvenienciaCache)");
        assertTrue(violacoes.isEmpty(),
            () -> "Tipo de cachetraducao fora dos homologados (CACHE_TRADUCAO_TIPOS_CONGELADOS):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("contexto é congelado por tipo com igualdade EXATA: Tradução Local usa exatamente os tipos homologados do peer (E7b)")
    void contextoCongeladoPorTipo() {
        List<String> violacoes = new ArrayList<>();
        Set<String> tiposContextoUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (FATIA_CONTEXTO.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    tiposContextoUsados.add(destino);
                    if (!CONTEXTO_TIPOS_CONGELADOS.contains(destino)) {
                        violacoes.add(origem + " -> " + destino);
                    }
                }
            }
        }
        assertFalse(tiposContextoUsados.isEmpty(),
            "Esperado uso do peer contexto pela Tradução Local (E7b: GerenciadorContexto/ProvedorContexto/RegrasConcordanciaPtBr)");
        assertTrue(violacoes.isEmpty(),
            () -> "Tipo de contexto fora dos homologados (CONTEXTO_TIPOS_CONGELADOS):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
        // Igualdade EXATA (E7b): cobre tipos inesperados (violacoes) e entradas obsoletas na allowlist.
        assertEquals(new TreeSet<>(CONTEXTO_TIPOS_CONGELADOS), tiposContextoUsados,
            "CONTEXTO_TIPOS_CONGELADOS deve ser EXATAMENTE igual ao conjunto de tipos de contexto consumidos por traducao");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela por tipo, com igualdade EXATA, a superfície do peer
     * {@code qualidadeTraducao} consumida pela Tradução Local (E8b) — garante que a
     * extração de {@code MascaradorTags} e {@code AlucinacaoDetectadaException} não abriu
     * dependência não homologada nem deixou entrada obsoleta na allowlist.
     *
     * <p>INVARIANTES DO DOMÍNIO: os tipos de {@code qualidadeTraducao} consumidos por
     * {@code traducao} são normalizados à classe de topo (nested {@code MascaradorTags$Mascarado}
     * vira {@code MascaradorTags}) e devem ser EXATAMENTE
     * {@link #QUALIDADE_TRADUCAO_TIPOS_CONGELADOS}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: um tipo fora do conjunto (violação) ou o conjunto
     * medido diferente do congelado (tipo novo ou entrada obsoleta) reprova o teste.
     */
    @Test
    @DisplayName("qualidadeTraducao é congelado por tipo com igualdade EXATA: MascaradorTags, AlucinacaoDetectadaException (E8b), ValidadorTraducaoService, ProtecaoLegendaAssService (E8c), DetectorTraducaoIdenticaService e LoreAtivaPort (E8c.1)")
    void qualidadeTraducaoCongeladoPorTipo() {
        List<String> violacoes = new ArrayList<>();
        Set<String> tiposQualidadeUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (FATIA_QUALIDADETRADUCAO.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    tiposQualidadeUsados.add(destino);
                    if (!QUALIDADE_TRADUCAO_TIPOS_CONGELADOS.contains(destino)) {
                        violacoes.add(origem + " -> " + destino);
                    }
                }
            }
        }
        assertFalse(tiposQualidadeUsados.isEmpty(),
            "Esperado uso do peer qualidadeTraducao pela Tradução Local (E8b: MascaradorTags/AlucinacaoDetectadaException)");
        assertTrue(violacoes.isEmpty(),
            () -> "Tipo de qualidadeTraducao fora dos homologados (QUALIDADE_TRADUCAO_TIPOS_CONGELADOS):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
        assertEquals(new TreeSet<>(QUALIDADE_TRADUCAO_TIPOS_CONGELADOS), tiposQualidadeUsados,
            "QUALIDADE_TRADUCAO_TIPOS_CONGELADOS deve ser EXATAMENTE igual ao conjunto de tipos consumidos por traducao");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que a Tradução Local consome do peer {@code llm} EXATAMENTE
     * os tipos homologados em {@link #LLM_TIPOS_CONGELADOS} — o contrato do LLM extraído na
     * E8d — sem liberação genérica do pacote.
     *
     * <p>INVARIANTES DO DOMÍNIO: varre toda a fatia {@code traducao} (inclusive
     * infrastructure), normaliza classes aninhadas ao top-level proprietário e mede os tipos
     * de {@code llm} realmente consumidos. O adapter {@code LlmClientAdapter}, em
     * {@code traducao.infrastructure}, é o principal consumidor (ponto de composição).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: reprova por tipo inesperado, por entrada obsoleta ou
     * por qualquer divergência da igualdade EXATA {@code tiposUsados == LLM_TIPOS_CONGELADOS}.
     */
    @Test
    @DisplayName("llm é congelado por tipo com igualdade EXATA: Tradução Local usa exatamente LlmPort, Lote, TraducaoLote e StatusLlm (E8d)")
    void llmCongeladoPorTipo() {
        List<String> violacoes = new ArrayList<>();
        Set<String> tiposLlmUsados = new TreeSet<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (FATIA_LLM.equals(fatiaDe(dependencia.getTargetClass().getPackageName()))) {
                    tiposLlmUsados.add(destino);
                    if (!LLM_TIPOS_CONGELADOS.contains(destino)) {
                        violacoes.add(origem + " -> " + destino);
                    }
                }
            }
        }
        assertFalse(tiposLlmUsados.isEmpty(),
            "Esperado uso do peer llm pela Tradução Local (E8d: LlmPort/Lote/TraducaoLote/StatusLlm via LlmClientAdapter)");
        assertTrue(violacoes.isEmpty(),
            () -> "Tipo de llm fora dos homologados (LLM_TIPOS_CONGELADOS):\n"
                + String.join("\n", new TreeSet<>(violacoes)));
        assertEquals(new TreeSet<>(LLM_TIPOS_CONGELADOS), tiposLlmUsados,
            "LLM_TIPOS_CONGELADOS deve ser EXATAMENTE igual ao conjunto de tipos de llm consumidos por traducao");
    }

    @Test
    @DisplayName("revisaoLore não depende da stack LLM/contexto da Tradução Local (D-Lore)")
    void revisaoLoreNaoDependeDaStackLlmDeTraducao() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            boolean ehRevisaoLore = pkg.equals(PKG_REVISAO_LORE) || pkg.startsWith(PKG_REVISAO_LORE + ".");
            if (!ehRevisaoLore) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                if (REVISAO_LORE_PROIBIDOS.contains(destino)) {
                    violacoes.add(origem + " -> " + destino);
                }
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "revisaoLore não pode importar a stack LLM/contexto da Tradução Local "
                + "(LlmPort/StatusLlm/LlmProperties/JsonHttpClient/RecordsLlm/LlmClientAdapter/"
                + "GerenciadorContexto). As demais entradas ficam para a FASE E. Violações:\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("traducao não depende de telemetria (ZERO arestas — exceção C2 eliminada)")
    void traducaoNaoDependeDeTelemetria() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            if (!ehDaTraducao(classe)) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String destino = topo(dependencia.getTargetClass().getName());
                String pkg = dependencia.getTargetClass().getPackageName();
                boolean ehTelemetria = pkg.equals(PKG_TELEMETRIA) || pkg.startsWith(PKG_TELEMETRIA + ".");
                if (!ehTelemetria) {
                    continue;
                }
                violacoes.add(aresta(origem, destino));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "Nenhuma classe de traducao pode depender de telemetria. Após a FASE C2 o "
                + "TelemetriaController saiu para telemetria.presentation.web e a exceção "
                + "ALLOW-TELEMETRIA-C2 foi eliminada. Violações:\n"
                + String.join("\n", new TreeSet<>(violacoes)));
    }

    @Test
    @DisplayName("core NÃO depende de nenhuma fatia funcional (independência do kernel — regra permanente)")
    void coreNaoDependeDeFatiaFuncional() {
        List<String> violacoes = new ArrayList<>();
        for (JavaClass classe : classesProducao) {
            String pkg = classe.getPackageName();
            boolean ehDoCore = pkg.equals(RAIZ + ".core") || pkg.startsWith(RAIZ + ".core.");
            if (!ehDoCore) {
                continue;
            }
            String origem = topo(classe.getName());
            for (Dependency dependencia : classe.getDirectDependenciesFromSelf()) {
                String pkgDestino = dependencia.getTargetClass().getPackageName();
                String fatia = fatiaDe(pkgDestino);
                // Alvos permitidos: JDK / libs externas (fatia == null) e o próprio core.
                if (fatia == null || fatia.equals(FATIA_CORE_NOME)) {
                    continue;
                }
                violacoes.add(aresta(origem, topo(dependencia.getTargetClass().getName())));
            }
        }
        assertTrue(violacoes.isEmpty(),
            () -> "O core deve depender SOMENTE de JDK, bibliotecas técnicas e do próprio "
                + "org.traducao.projeto.core. Nenhuma allowlist core → fatia funcional é permitida, "
                + "nem temporária. Violações:\n" + String.join("\n", new TreeSet<>(violacoes)));
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
