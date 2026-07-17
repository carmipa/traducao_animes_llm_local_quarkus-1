package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.correcaoLegendas.application.SanitizadorTagsService;
import org.traducao.projeto.raspagemCorrecao.infrastructure.GoogleTranslateScraper;
import org.traducao.projeto.raspagemCorrecao.infrastructure.ResultadoRaspagem;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.raspagemRevisao.domain.exceptions.RaspagemRevisaoException;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class RevisarLegendasUseCase {

    public enum ModoRevisaoLegendas {
        GOOGLE,
        LLM_CONCORDANCIA
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fonte de referência que protege o sentido durante a
     * revisão do PT — a legenda EN + cache (AMBOS, comportamento histórico) ou
     * exclusivamente o cache (CACHE), com vínculo seguro por entrada.
     * <p>INVARIANTES DO DOMÍNIO: em CACHE não se usa {@code .ass} EN irmão nem
     * fallback por texto não validado; só entradas de cache que casam com
     * segurança (índice + estilo + proveniência + texto) viram referência.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada de cache que não casa marca a
     * fala como SEM_REFERÊNCIA_SEGURA e nunca é usada em silêncio.
     */
    public enum ModoReferenciaRevisao {
        AMBOS,
        CACHE
    }

    private static final Set<String> EXTENSOES = Set.of(".ass", ".ssa");
    private static final long PAUSA_GOOGLE_MS = 400;
    private static final int LIMIAR_ABSOLUTO_RETRADUCAO_EM_MASSA = 20;
    private static final int DIVISOR_PROPORCAO_RETRADUCAO_EM_MASSA = 10;
    private static final Pattern CODIGO_EPISODIO = Pattern.compile("(?i)(S\\d{1,2}E\\d{1,3})");
    private static final Pattern SUFIXO_PTBR_TRACK = Pattern.compile("(?i)_PT-?BR(_Track\\d+)?$");
    private static final DateTimeFormatter TS_BACKUP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final GoogleTranslateScraper googleScraper;
    private final AuditorProblemasLegendaService auditor;
    private final ValidadorTraducaoService validador;
    private final LeitorCacheReferenciaService leitorCache;
    private final SincronizadorLegendaCacheService sincronizadorCache;
    private final MistralPort mistralPort;
    private final MascaradorTags mascaradorTags;
    private final GerenciadorContexto gerenciadorContexto;
    private final TelemetriaService telemetriaService;
    private final SanitizadorTagsService sanitizadorTags;
    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;
    private final ProtetorTermosLoreService protetorLore;
    private final CorretorDeterministicoConcordanciaService corretorDeterministico;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe a revisão final de legendas com leitura de
     * cache versionado, validação linguística, proteção ASS e persistência segura.
     *
     * <p>INVARIANTES DO DOMÍNIO: todas as dependências usam o mesmo catálogo de
     * contexto e o cache é aberto pela porta canônica de manutenção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência obrigatória ausente impede a
     * construção do serviço pelo contêiner de injeção.
     */
    public RevisarLegendasUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        GoogleTranslateScraper googleScraper,
        AuditorProblemasLegendaService auditor,
        ValidadorTraducaoService validador,
        LeitorCacheReferenciaService leitorCache,
        SincronizadorLegendaCacheService sincronizadorCache,
        MistralPort mistralPort,
        MascaradorTags mascaradorTags,
        GerenciadorContexto gerenciadorContexto,
        TelemetriaService telemetriaService,
        SanitizadorTagsService sanitizadorTags,
        PoliticaEstiloMusical politicaEstiloMusical,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss,
        ProtetorTermosLoreService protetorLore,
        CorretorDeterministicoConcordanciaService corretorDeterministico
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.googleScraper = googleScraper;
        this.auditor = auditor;
        this.validador = validador;
        this.leitorCache = leitorCache;
        this.sincronizadorCache = sincronizadorCache;
        this.mistralPort = mistralPort;
        this.mascaradorTags = mascaradorTags;
        this.gerenciadorContexto = gerenciadorContexto;
        this.telemetriaService = telemetriaService;
        this.sanitizadorTags = sanitizadorTags;
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
        this.protetorLore = protetorLore;
        this.corretorDeterministico = corretorDeterministico;
    }

    /**
     * Valida a pasta informada antes de iniciar a revisão (API/CLI).
     * Retorna mensagem de erro quando a pasta não contém legendas .ass/.ssa.
     */
    public Optional<String> validarPastaEntrada(Path pasta) {
        if (pasta == null || pasta.toString().isBlank()) {
            return Optional.of("Pasta com legendas traduzidas em português (.ass) é obrigatória.");
        }
        if (!Files.isDirectory(pasta)) {
            return Optional.of("Pasta não encontrada: " + pasta.toAbsolutePath());
        }

        try (Stream<Path> stream = Files.list(pasta)) {
            List<Path> arquivos = stream.filter(Files::isRegularFile).toList();
            if (arquivos.stream().anyMatch(this::temExtensaoSuportada)) {
                return Optional.empty();
            }

            long cacheJson = arquivos.stream()
                .filter(p -> p.getFileName().toString().endsWith(".cache.json"))
                .count();
            String abs = pasta.toAbsolutePath().toString().replace('\\', '/').toLowerCase();
            boolean pareceCache = cacheJson > 0
                || abs.contains("/cache/")
                || abs.endsWith("/cache");

            if (pareceCache) {
                return Optional.of(
                    "Esta pasta parece ser de CACHE ("
                        + cacheJson + " arquivo(s) .cache.json, nenhum .ass/.ssa). "
                        + "Informe a pasta com os arquivos de legenda traduzidos (.ass), por exemplo: "
                        + "E:\\animes\\DANMACHI\\temporada_5\\legendas_extraidas_ass.");
            }
            return Optional.of(
                "Nenhum arquivo .ass/.ssa encontrado em: " + pasta.toAbsolutePath());
        } catch (IOException e) {
            return Optional.of("Erro ao ler pasta: " + e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o contrato histórico da revisão Google e
     * delega ao fluxo completo com sincronização prévia do cache corrigido.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente arquivos PT-BR suportados entram no
     * lote; a fila respeita interrupção e toda sobrescrita cria backup.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inválida devolve resultado vazio;
     * falha de listagem lança exceção de domínio sem alterar legendas.
     *
     * @param pastaLegendasPt pasta com arquivos .ass/.ssa já traduzidos
     * @param pastaLegendasEn pasta opcional com originais em inglês
     * @param pastaCache pasta de cache; padrão {@code cache}
     * @param pastaSaida pasta opcional de saída; padrão sobrescreve PT com backup
     */
    public ResultadoRevisaoLegendas executar(
        Path pastaLegendasPt,
        Path pastaLegendasEn,
        Path pastaCache,
        Path pastaSaida
    ) {
        return executar(pastaLegendasPt, pastaLegendasEn, pastaCache, pastaSaida,
            ModoRevisaoLegendas.GOOGLE, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a revisão em lote no modo Google ou LLM,
     * incluindo a sincronização prévia das correções confirmadas no cache.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente arquivos PT-BR suportados entram no
     * lote; o modo Google não corrige concordância reservada à lore local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inválida devolve resultado vazio;
     * falha de listagem lança exceção de domínio sem alterar legendas.
     */
    public ResultadoRevisaoLegendas executar(
        Path pastaLegendasPt,
        Path pastaLegendasEn,
        Path pastaCache,
        Path pastaSaida,
        ModoRevisaoLegendas modo,
        String contextoId
    ) {
        return executar(pastaLegendasPt, pastaLegendasEn, pastaCache, pastaSaida,
            modo, contextoId, ModoReferenciaRevisao.AMBOS);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a revisão em lote escolhendo a fonte de
     * referência — EN + cache (AMBOS) ou somente o cache com vínculo seguro (CACHE).
     *
     * <p>INVARIANTES DO DOMÍNIO: em CACHE, cada fala só recebe referência de uma
     * entrada de cache que casa com segurança; as demais viram SEM_REFERÊNCIA_SEGURA
     * e são revisadas sem proteção semântica, nunca comparadas em silêncio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta inválida devolve resultado vazio;
     * falha de listagem lança exceção de domínio sem alterar legendas.
     */
    public ResultadoRevisaoLegendas executar(
        Path pastaLegendasPt,
        Path pastaLegendasEn,
        Path pastaCache,
        Path pastaSaida,
        ModoRevisaoLegendas modo,
        String contextoId,
        ModoReferenciaRevisao referencia
    ) {
        long inicioMs = System.currentTimeMillis();
        if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
            out("Iniciando revisão de concordância PT-BR (LLM) em legendas: "
                + pastaLegendasPt.toAbsolutePath());
        } else {
            out("Iniciando revisão de legendas traduzidas em: " + pastaLegendasPt.toAbsolutePath());
        }

        if (!Files.isDirectory(pastaLegendasPt)) {
            out(AnsiCores.RED + "Erro: pasta de legendas traduzidas inválida." + AnsiCores.RESET);
            return new ResultadoRevisaoLegendas(0, 0, 0, 0);
        }

        Path pastaEn = pastaLegendasEn != null ? pastaLegendasEn : pastaLegendasPt;
        Path cacheDir = pastaCache != null ? pastaCache : DiretorioBaseKronos.resolver("cache");
        Path saidaDir = pastaSaida != null ? pastaSaida : pastaLegendasPt;
        Path pastaBackup = DiretorioBaseKronos.resolver("backups", "revisao-legendas",
            "revisao_" + LocalDateTime.now().format(TS_BACKUP)).toAbsolutePath().normalize();

        int[] arquivosProcessados = {0};
        int[] falasCorrigidas = {0};
        int[] falasComProblema = {0};
        int[] falasAuditadas = {0};
        int[] falasSemOriginal = {0};
        int[] falasPendentes = {0};
        int[] falasSemReferenciaSegura = {0};
        List<DetalheRevisao> detalhesRevisao = new ArrayList<>();

        try (Stream<Path> stream = Files.list(pastaLegendasPt)) {
            List<Path> arquivos = stream
                .filter(Files::isRegularFile)
                .filter(this::temExtensaoSuportada)
                .filter(this::eLegendaTraduzida)
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();

            if (arquivos.isEmpty()) {
                Optional<String> erro = validarPastaEntrada(pastaLegendasPt);
                out(AnsiCores.YELLOW + erro.orElse("Nenhum arquivo .ass/.ssa traduzido encontrado na pasta.")
                    + AnsiCores.RESET);
                registrarTelemetria(pastaLegendasPt, inicioMs, 0, 0, 0, 0, 0, 0, modo,
                    detalhesRevisao);
                return new ResultadoRevisaoLegendas(0, 0, 0, 0);
            }

            out("Originais EN: .ass em " + pastaEn.toAbsolutePath()
                + " (se existir) + cache/ em " + cacheDir.toAbsolutePath());

            for (Path arquivoPt : arquivos) {
                // Parada cooperativa (botão "Parar" da UI): arquivos já
                // revisados ficaram salvos; os restantes não são tocados.
                if (Thread.currentThread().isInterrupted()) {
                    out(AnsiCores.YELLOW + "Revisão interrompida pelo usuário — "
                        + "arquivos restantes não foram processados." + AnsiCores.RESET);
                    break;
                }
                processarArquivo(
                    arquivoPt, pastaEn, cacheDir, saidaDir, pastaBackup, modo, referencia,
                    arquivosProcessados, falasCorrigidas, falasComProblema,
                    falasAuditadas, falasSemOriginal, falasPendentes,
                    falasSemReferenciaSegura, contextoId, detalhesRevisao);
            }
        } catch (IOException e) {
            out(AnsiCores.RED + "Erro ao listar legendas: " + e.getMessage() + AnsiCores.RESET);
            throw new RaspagemRevisaoException("Falha ao listar legendas em: " + pastaLegendasPt, e);
        }

        out("Arquivos analisados: " + arquivosProcessados[0]);
        out("Falas auditadas: " + falasAuditadas[0]);
        if (referencia == ModoReferenciaRevisao.CACHE) {
            out("Falas sem referência segura no cache: " + falasSemReferenciaSegura[0]);
        }
        out("Falas sem original EN (ignoradas): " + falasSemOriginal[0]);
        out("Falas com problemas detectados: " + falasComProblema[0]);
        out("Falas ainda pendentes: " + falasPendentes[0]);
        if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
            out("Falas corrigidas via LLM e salvas: " + falasCorrigidas[0]);
        } else {
            out("Falas corrigidas via Google e salvas: " + falasCorrigidas[0]);
        }
        registrarTelemetria(pastaLegendasPt, inicioMs, arquivosProcessados[0], falasComProblema[0],
            falasCorrigidas[0], falasAuditadas[0], falasSemOriginal[0], falasPendentes[0], modo,
            detalhesRevisao);
        return new ResultadoRevisaoLegendas(
            arquivosProcessados[0], falasCorrigidas[0], falasComProblema[0], falasPendentes[0]);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ativa para cada legenda a lore registrada no cache
     * que a originou, impedindo revisão de uma obra com o contexto de outra.
     *
     * <p>INVARIANTES DO DOMÍNIO: proveniência versionada sempre vence a seleção
     * manual; seleção da interface é fallback apenas para cache legado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: contexto inexistente interrompe o
     * arquivo antes de qualquer chamada externa ou sobrescrita da legenda.
     */
    ContextoRevisao ativarContextoDoArquivo(
        ProvenienciaCache proveniencia,
        String contextoFallback,
        Path cachePath
    ) {
        String contextoProveniencia = proveniencia != null ? proveniencia.contextoId() : null;
        String contextoEfetivo = contextoProveniencia != null && !contextoProveniencia.isBlank()
            ? contextoProveniencia : contextoFallback;

        if (contextoProveniencia != null && !contextoProveniencia.isBlank()
            && contextoFallback != null && !contextoFallback.isBlank()
            && !contextoProveniencia.equals(contextoFallback)) {
            out(AnsiCores.YELLOW + "  [CONTEXTO] Seleção manual \"" + contextoFallback
                + "\" ignorada: a proveniência do cache exige \"" + contextoProveniencia + "\"."
                + AnsiCores.RESET);
        }
        if (contextoEfetivo == null || contextoEfetivo.isBlank()) {
            contextoEfetivo = gerenciadorContexto.obterIdContextoAtivo();
            out(AnsiCores.YELLOW + "  [CONTEXTO] Cache legado sem proveniência e sem seleção; "
                + "usando contexto ativo \"" + contextoEfetivo + "\"." + AnsiCores.RESET);
        }
        if (!gerenciadorContexto.existeContexto(contextoEfetivo)) {
            throw new RaspagemRevisaoException(
                "Contexto \"" + contextoEfetivo + "\" do cache não existe no projeto: " + cachePath);
        }

        gerenciadorContexto.definirContextoAtivo(contextoEfetivo);
        out(AnsiCores.CYAN + "  Contexto ativo: " + gerenciadorContexto.obterNomeContextoAtivo()
            + " (fonte: " + (contextoProveniencia != null ? "proveniência do cache" : "seleção/fallback")
            + ")" + AnsiCores.RESET);
        return new ContextoRevisao(
            contextoEfetivo,
            gerenciadorContexto.obterLoreAtiva(),
            gerenciadorContexto.termosProtegidosAtivos());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encerra a revisão persistindo métricas agregadas e a
     * explicação por ocorrência para auditoria e evolução do dataset.
     *
     * <p>INVARIANTES DO DOMÍNIO: os totais do relatório correspondem ao resultado
     * devolvido pela operação e detalhes nunca substituem a telemetria canônica.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista de detalhes vazia ainda produz um
     * relatório válido com os totais disponíveis.
     */
    private void registrarTelemetria(
        Path pastaLegendasPt,
        long inicioMs,
        int arquivos,
        int problemas,
        int corrigidas,
        int auditadas,
        int semOriginal,
        int pendentes,
        ModoRevisaoLegendas modo,
        List<DetalheRevisao> detalhes
    ) {
        long duracaoMs = System.currentTimeMillis() - inicioMs;
        boolean llm = modo == ModoRevisaoLegendas.LLM_CONCORDANCIA;
        String nomeOperacao = llm
            ? "Revisão Concordância (.ass LLM)"
            : "Revisão Legendas (.ass Google)";
        OperacaoTelemetria operacao = TelemetriaService.criarOperacao(
            nomeOperacao,
            pastaLegendasPt.toAbsolutePath().toString(),
            duracaoMs,
            arquivos,
            problemas,
            corrigidas
        );
        String relatorio = llm ? """
            REVISÃO DE CONCORDÂNCIA PT-BR (.ass via LLM)
            ============================================
            Pasta: %s
            Duração: %s
            Arquivos analisados: %d
            Falas auditadas: %d
            Falas sem original EN (ignoradas): %d
            Problemas detectados: %d
            Falas corrigidas via LLM: %d
            Falas pendentes: %d
            """.formatted(
            pastaLegendasPt.toAbsolutePath(),
            formatarDuracaoMs(duracaoMs),
            arquivos,
            auditadas,
            semOriginal,
            problemas,
            corrigidas,
            pendentes
        ) : """
            REVISÃO DE LEGENDAS (.ass)
            ==========================
            Pasta: %s
            Duração: %s
            Arquivos analisados: %d
            Falas auditadas: %d
            Falas sem original EN (ignoradas): %d
            Problemas detectados: %d
            Falas corrigidas via Google: %d
            Falas pendentes: %d
            """.formatted(
            pastaLegendasPt.toAbsolutePath(),
            formatarDuracaoMs(duracaoMs),
            arquivos,
            auditadas,
            semOriginal,
            problemas,
            corrigidas,
            pendentes
        );
        relatorio += formatarDetalhesRelatorio(detalhes);
        telemetriaService.finalizarOperacao(
            operacao, pastaLegendasPt,
            llm ? "revisao_concordancia_legendas" : "revisao_legendas",
            relatorio);
        out("Relatório salvo em: " + TelemetriaService.resolverPastaRelatorios(pastaLegendasPt));
    }

    private String formatarDuracaoMs(long ms) {
        long segundos = ms / 1000;
        return segundos >= 60 ? (segundos / 60) + "min " + (segundos % 60) + "s" : segundos + "s";
    }

    private void out(String mensagem) {
        System.out.println(mensagem);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sincroniza uma legenda com o cache corrigido mais
     * recente e aplica a revisão linguística correspondente ao modo selecionado.
     *
     * <p>INVARIANTES DO DOMÍNIO: cache vazio nunca apaga fala; a proveniência
     * define a lore; cache mais antigo nunca sobrescreve revisão posterior;
     * qualquer gravação cria backup e preserva tempos, estilos e estrutura ASS.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceções de leitura/escrita interrompem
     * o arquivo atual sem produzir uma substituição parcial.
     */
    private void processarArquivo(
        Path arquivoPt,
        Path pastaLegendasEn,
        Path cacheDir,
        Path saidaDir,
        Path pastaBackup,
        ModoRevisaoLegendas modo,
        ModoReferenciaRevisao referencia,
        int[] totalArquivos,
        int[] totalCorrigidas,
        int[] totalProblemas,
        int[] totalAuditadas,
        int[] totalSemOriginal,
        int[] totalPendentes,
        int[] totalSemReferenciaSegura,
        String contextoFallback,
        List<DetalheRevisao> detalhesRevisao
    ) {
        totalArquivos[0]++;
        out("\nAnalisando legenda: " + arquivoPt.getFileName());

        DocumentoLegenda documentoPt = leitor.ler(arquivoPt);

        Path cachePath = resolverArquivoCache(arquivoPt, cacheDir);
        LeitorCacheReferenciaService.DocumentoReferencia cache = carregarDocumentoCache(cachePath);
        List<EntradaCache> entradasCache = cache.entradas();

        // Modo Cache: sem cache correspondente não há referência possível. O arquivo
        // fica BLOQUEADO/PENDENTE em vez de ser "revisado" com zero referência segura.
        if (referencia == ModoReferenciaRevisao.CACHE && entradasCache.isEmpty()) {
            out(AnsiCores.RED + "  [BLOQUEADO] Modo Cache: nenhum cache correspondente encontrado para "
                + arquivoPt.getFileName() + " em " + cacheDir.toAbsolutePath()
                + " (esperado algo como " + cachePath.getFileName() + "). Arquivo não revisado."
                + AnsiCores.RESET);
            totalProblemas[0]++;
            totalPendentes[0]++;
            return;
        }

        ContextoRevisao contexto = ativarContextoDoArquivo(cache.proveniencia(), contextoFallback, cachePath);

        Path arquivoEn;
        Map<Integer, String> originaisPorIndice;
        Map<String, String> originalPorTraduzido;
        Set<Integer> indicesSemReferenciaSegura;
        if (referencia == ModoReferenciaRevisao.CACHE) {
            // Modo "Cache": referência vem SÓ do cache, com vínculo seguro por entrada.
            // Sem .ass EN irmão e sem fallback por texto não validado.
            arquivoEn = null;
            ReferenciaCacheSegura referenciaCache = montarReferenciaCacheSegura(
                documentoPt, entradasCache, cache.proveniencia());
            originaisPorIndice = referenciaCache.originaisPorIndice();
            indicesSemReferenciaSegura = referenciaCache.semReferenciaSegura();
            originalPorTraduzido = Map.of();
        } else {
            // Modo "Ambos": .ass EN + cache preenchendo lacunas (comportamento histórico).
            arquivoEn = resolverArquivoOriginal(arquivoPt, pastaLegendasEn);
            originaisPorIndice = carregarOriginaisDeLegenda(arquivoEn);
            originalPorTraduzido = indexarOriginalPorTraduzido(entradasCache);
            for (EntradaCache entrada : entradasCache) {
                if (entrada.original() != null && !entrada.original().isBlank()) {
                    originaisPorIndice.putIfAbsent(entrada.indice(), entrada.original());
                }
            }
            indicesSemReferenciaSegura = Set.of();
        }

        if (!entradasCache.isEmpty()) {
            out("  Cache carregado: " + cachePath.getFileName()
                + " (" + entradasCache.size() + " entradas EN)");
        } else {
            out(AnsiCores.YELLOW + "  Aviso: cache EN não encontrado. Procurado em: "
                + cacheDir.toAbsolutePath()
                + " (esperado algo como " + cachePath.getFileName() + ")"
                + AnsiCores.RESET);
        }
        if (arquivoEn != null && Files.isRegularFile(arquivoEn) && !originaisPorIndice.isEmpty()) {
            out("  Legenda .ass EN: " + arquivoEn.getFileName());
        }
        if (referencia == ModoReferenciaRevisao.CACHE) {
            long dialogosAuditaveis = documentoPt.eventos().stream()
                .filter(e -> e.isDialogo() && e.texto() != null && !e.texto().isBlank()
                    && !deveIgnorarAuditoria(e, e.texto()))
                .count();
            // Cache resolvido (por código de episódio, p.ex.) mas que não casa com
            // NENHUMA fala com segurança = cache de outra obra/episódio ou estale.
            // Bloqueia em vez de "concluir com sucesso" sem nenhuma referência segura.
            if (originaisPorIndice.isEmpty() && dialogosAuditaveis > 0) {
                out(AnsiCores.RED + "  [BLOQUEADO] Modo Cache: o cache resolvido (" + cachePath.getFileName()
                    + ") não corresponde com segurança a nenhuma das " + dialogosAuditaveis
                    + " fala(s) de " + arquivoPt.getFileName()
                    + " (índice/estilo/proveniência/texto divergem). Arquivo não revisado." + AnsiCores.RESET);
                totalProblemas[0]++;
                totalPendentes[0]++;
                return;
            }
            if (!indicesSemReferenciaSegura.isEmpty()) {
                totalSemReferenciaSegura[0] += indicesSemReferenciaSegura.size();
                // Fala sem vínculo seguro é PENDÊNCIA: no modo Cache não há "conclusão
                // com sucesso" enquanto restar fala sem referência segura.
                totalPendentes[0] += indicesSemReferenciaSegura.size();
                out(AnsiCores.YELLOW + "  [SEM_REFERÊNCIA_SEGURA] " + indicesSemReferenciaSegura.size()
                    + " fala(s) sem vínculo seguro no cache (índice/estilo/proveniência/texto não conferem); "
                    + "marcadas como pendentes, nunca comparadas em silêncio. Índices: "
                    + indicesSemReferenciaSegura + AnsiCores.RESET);
            }
        }

        List<EventoLegenda> eventosAtualizados = new ArrayList<>();
        Map<String, String> cacheRevisaoMasc = new HashMap<>();
        Set<String> revisoesSemAlteracao = new LinkedHashSet<>();
        int corrigidasNesteArquivo = 0;
        boolean sincronizarCache = cacheMaisNovoQueLegenda(cachePath, arquivoPt);
        Set<Integer> indicesCanonicosProtegidos = localizarIndicesCanonicosProtegidos(
            documentoPt, entradasCache, contexto);
        // Modo Cache: a sincronização só pode escrever índices com vínculo seguro
        // (as chaves de originaisPorIndice). AMBOS mantém null = comportamento histórico.
        Set<Integer> indicesPermitidosSync = referencia == ModoReferenciaRevisao.CACHE
            ? originaisPorIndice.keySet() : null;
        SincronizadorLegendaCacheService.Resultado sincronizacao = sincronizadorCache.sincronizar(
            documentoPt, entradasCache, sincronizarCache, indicesCanonicosProtegidos, indicesPermitidosSync);
        documentoPt = sincronizacao.documento();
        int sincronizadasNesteArquivo = sincronizacao.total();
        int problemasNesteArquivo = 0;
        int falasAuditadas = 0;
        int falasSemOriginal = 0;
        boolean modificado = sincronizadasNesteArquivo > 0;
        if (sincronizarCache) {
            out(AnsiCores.CYAN + "  Cache corrigido é mais novo que a legenda; "
                + "sincronizando traduções antes da revisão." + AnsiCores.RESET);
            for (Integer indice : sincronizacao.indicesSincronizados()) {
                out("  [CACHE] Evento " + indice + " sincronizado com a correção da Opção 5.");
            }
        }
        if (!sincronizarCache && !sincronizacao.indicesRecuperadosDoOriginal().isEmpty()) {
            out(AnsiCores.YELLOW + "  [RECUPERAÇÃO] O ASS continha "
                + sincronizacao.indicesRecuperadosDoOriginal().size()
                + " fala(s) que haviam voltado exatamente ao inglês. "
                + "As traduções persistentes do cache foram restauradas mesmo com cache mais antigo."
                + AnsiCores.RESET);
            for (Integer indice : sincronizacao.indicesRecuperadosDoOriginal()) {
                out("  [CACHE/RECUPERADO] Evento " + indice + " restaurado do banco de tradução.");
            }
        }

        DiagnosticoRetraducao diagnosticoRetraducao = diagnosticarRetraducaoEmMassa(
            documentoPt, originaisPorIndice, contexto);
        if (diagnosticoRetraducao.deveBloquear()) {
            if (sincronizadasNesteArquivo > 0) {
                Path destino = saidaDir.resolve(arquivoPt.getFileName());
                Path backup = criarBackupSeSobrescrever(arquivoPt, destino, pastaBackup);
                escritor.escrever(destino, documentoPt);
                out(AnsiCores.GREEN + "  [RECUPERADO] Traduções disponíveis no cache foram salvas antes do bloqueio."
                    + AnsiCores.RESET);
                if (backup != null) out(AnsiCores.CYAN + "  Backup anterior: " + backup + AnsiCores.RESET);
            }
            totalAuditadas[0] += diagnosticoRetraducao.falasAuditaveis();
            totalProblemas[0] += diagnosticoRetraducao.falasNaoTraduzidas();
            totalPendentes[0] += diagnosticoRetraducao.falasNaoTraduzidas();
            out(AnsiCores.RED + "  [BLOQUEADO] A entrada ainda possui "
                + diagnosticoRetraducao.falasNaoTraduzidas() + " de "
                + diagnosticoRetraducao.falasAuditaveis()
                + " falas auditáveis iguais ao inglês. A Opção 6 revisa traduções; "
                + "não fará retradução em massa. Regenere pela Opção 4 ou corrija o cache."
                + AnsiCores.RESET);
            detalhesRevisao.add(new DetalheRevisao(
                arquivoPt.getFileName().toString(), -1, "ARQUIVO", "BLOQUEADO_RETRADUCAO_EM_MASSA",
                List.of("Falas iguais ao original acima do limite seguro"),
                diagnosticoRetraducao.falasNaoTraduzidas() + " de "
                    + diagnosticoRetraducao.falasAuditaveis() + " falas auditáveis",
                null, null, null));
            return;
        }

        boolean interrompido = false;
        for (EventoLegenda evento : documentoPt.eventos()) {
            // Parada cooperativa no meio do arquivo: as falas restantes entram
            // sem alteração e o que já foi corrigido é gravado normalmente.
            if (interrompido || Thread.currentThread().isInterrupted()) {
                if (!interrompido) {
                    out("  " + AnsiCores.YELLOW
                        + "[STOP] Interrompido pelo usuário — falas restantes mantidas como estão."
                        + AnsiCores.RESET);
                    interrompido = true;
                }
                eventosAtualizados.add(evento);
                continue;
            }
            if (!evento.isDialogo() || evento.texto() == null) {
                eventosAtualizados.add(evento);
                continue;
            }

            if (evento.texto().isBlank()) {
                eventosAtualizados.add(evento);
                continue;
            }

            if (deveIgnorarAuditoria(evento, evento.texto())) {
                eventosAtualizados.add(evento);
                continue;
            }

            // Localiza o original EN ANTES da correção de karaokê: a busca por
            // texto traduzido usa o texto como está no cache (pré-correção), e o
            // original serve de referência para preservar comentários {...}
            // legítimos em vez de escapá-los como alucinação.
            String textoNormalizado = evento.texto();
            String originalEn = originaisPorIndice.get(evento.indice());
            if (originalEn == null || originalEn.isBlank()) {
                originalEn = originalPorTraduzido.get(normalizarTexto(textoNormalizado));
            }
            boolean temOriginalEn = originalEn != null && !originalEn.isBlank();

            String textoCorrigidoKaraoke = sanitizadorTags.escaparChavesInvalidas(textoNormalizado, originalEn);
            if (!textoNormalizado.equals(textoCorrigidoKaraoke)) {
                evento = evento.comTexto(textoCorrigidoKaraoke);
                modificado = true;
                corrigidasNesteArquivo++;
                out("  -> Karaoke corrigido na linha " + evento.indice() + ":");
                out("     De : " + textoNormalizado);
                out("     Para: " + textoCorrigidoKaraoke);
            }

            String traducaoAtual = evento.texto();
            if (!temOriginalEn) {
                falasSemOriginal++;
                totalSemOriginal[0]++;
                if (modo != ModoRevisaoLegendas.LLM_CONCORDANCIA) {
                    eventosAtualizados.add(evento);
                    continue;
                }
            }

            falasAuditadas++;
            totalAuditadas[0]++;

            if (temOriginalEn
                && normalizarTexto(originalEn).equals(normalizarTexto(traducaoAtual))
                && protetorLore.contemSomenteTermosCanonicos(
                    originalEn, contexto.lore(), contexto.termosProtegidos())) {
                out("  [LORE] Evento " + evento.indice()
                    + " contém somente nome/termo canônico; mantido sem chamar IA.");
                eventosAtualizados.add(evento);
                continue;
            }

            ResultadoDeteccaoConcordancia auditoria = auditor.auditar(originalEn, traducaoAtual);
            if (!auditoria.suspeito()) {
                eventosAtualizados.add(evento);
                continue;
            }

            problemasNesteArquivo++;
            totalProblemas[0]++;

            out("  -> Linha " + evento.indice() + " [" + evento.estilo() + "]:");
            out("     EN: " + AnsiCores.YELLOW + originalEn + AnsiCores.RESET);
            out("     PT: " + AnsiCores.YELLOW + traducaoAtual + AnsiCores.RESET);
            auditoria.motivos().forEach(m ->
                out("     " + AnsiCores.DIM + "• " + m + AnsiCores.RESET));

            String textoMascOriginal = temOriginalEn
                ? mascaradorTags.mascarar(originalEn).texto()
                : null;
            Optional<String> correcaoDeterministica = corretorDeterministico.corrigir(
                originalEn, traducaoAtual);
            if (correcaoDeterministica.isPresent()
                && correcaoEhSegura(
                    originalEn, traducaoAtual, correcaoDeterministica.get(), auditoria, contexto)) {
                String corrigida = correcaoDeterministica.get();
                out("     PT corrigido por regra segura: " + AnsiCores.GREEN + corrigida + AnsiCores.RESET);
                detalhesRevisao.add(new DetalheRevisao(
                    arquivoPt.getFileName().toString(), evento.indice(), evento.estilo(),
                    "CORRIGIDA_REGRA_SEGURA", auditoria.motivos(),
                    "Contradição objetiva corrigida localmente, sem chamar LLM ou Google.",
                    originalEn, traducaoAtual, corrigida));
                eventosAtualizados.add(evento.comTexto(corrigida));
                corrigidasNesteArquivo++;
                modificado = true;
                if (textoMascOriginal != null) {
                    cacheRevisaoMasc.put(textoMascOriginal, mascaradorTags.mascarar(corrigida).texto());
                }
                continue;
            }
            if (textoMascOriginal != null && revisoesSemAlteracao.contains(textoMascOriginal)) {
                totalPendentes[0]++;
                eventosAtualizados.add(evento);
                continue;
            }
            if (textoMascOriginal != null && cacheRevisaoMasc.containsKey(textoMascOriginal)) {
                String respostaMascCorrigida = cacheRevisaoMasc.get(textoMascOriginal);
                MascaradorTags.Mascarado mascTraducaoAtual = mascaradorTags.mascarar(traducaoAtual);
                String novaTraducaoCache;
                try {
                    novaTraducaoCache = mascaradorTags.desmascarar(respostaMascCorrigida, mascTraducaoAtual.tags());
                } catch (AlucinacaoDetectadaException e) {
                    out("  " + AnsiCores.YELLOW
                        + "Cache local ignorado na linha " + evento.indice()
                        + ": marcadores de tags incompatíveis com a tradução atual."
                        + AnsiCores.RESET);
                    totalPendentes[0]++;
                    eventosAtualizados.add(evento);
                    continue;
                }

                if (novaTraducaoCache.equals(traducaoAtual)
                    || !correcaoEhSegura(
                        originalEn, traducaoAtual, novaTraducaoCache, auditoria, contexto)) {
                    revisoesSemAlteracao.add(textoMascOriginal);
                    totalPendentes[0]++;
                    eventosAtualizados.add(evento);
                    continue;
                }

                out("  -> Linha " + evento.indice() + " [" + evento.estilo() + "] (Reutilizando correção do cache local):");
                out("     EN: " + AnsiCores.YELLOW + originalEn + AnsiCores.RESET);
                out("     PT: " + AnsiCores.YELLOW + traducaoAtual + AnsiCores.RESET);
                out("     PT corrigido: " + AnsiCores.GREEN + novaTraducaoCache + AnsiCores.RESET);

                eventosAtualizados.add(evento.comTexto(novaTraducaoCache));
                corrigidasNesteArquivo++;
                modificado = true;
                continue;
            }

            String novaTraducao;
            if (modo == ModoRevisaoLegendas.LLM_CONCORDANCIA) {
                TentativaRevisaoLegenda tentativa = tentarRevisarConcordancia(
                    originalEn, traducaoAtual, auditoria.motivos(), contexto);
                if (tentativa.revisado().isEmpty()) {
                    out("     " + AnsiCores.RED
                        + "Revisão não aplicada: " + tentativa.detalhe()
                        + AnsiCores.RESET);
                    detalhesRevisao.add(new DetalheRevisao(
                        arquivoPt.getFileName().toString(), evento.indice(), evento.estilo(),
                        tentativa.codigo(), auditoria.motivos(), tentativa.detalhe(),
                        originalEn, traducaoAtual, tentativa.proposta()));
                    totalPendentes[0]++;
                    eventosAtualizados.add(evento);
                    continue;
                }
                novaTraducao = tentativa.revisado().get();
                if (novaTraducao.equals(traducaoAtual)) {
                    out("     " + AnsiCores.DIM + "LLM manteve o texto original." + AnsiCores.RESET);
                    detalhesRevisao.add(new DetalheRevisao(
                        arquivoPt.getFileName().toString(), evento.indice(), evento.estilo(),
                        "LLM_SEM_ALTERACAO", auditoria.motivos(),
                        "O modelo respondeu, mas manteve a tradução atual.",
                        originalEn, traducaoAtual, novaTraducao));
                    registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                    totalPendentes[0]++;
                    eventosAtualizados.add(evento);
                    continue;
                }
            } else {
                if (!exigeRetraducao(auditoria)) {
                    out("     " + AnsiCores.DIM
                        + "Google não acionado: problema reservado à revisão LLM." + AnsiCores.RESET);
                    registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                    totalPendentes[0]++;
                    eventosAtualizados.add(evento);
                    continue;
                }
                ProtetorTermosLoreService.TextoProtegido originalProtegido = protetorLore.mascarar(
                    originalEn, contexto.lore(), contexto.termosProtegidos());
                ResultadoRaspagem resultadoGoogle = googleScraper.traduzir(originalProtegido.textoMascarado());
                pausaGoogle();

                String restauradaGoogle = resultadoGoogle.sucesso()
                    ? protetorLore.restaurar(resultadoGoogle.texto(), originalProtegido)
                    : null;
                if (!resultadoGoogle.sucesso() || restauradaGoogle == null
                    || restauradaGoogle.equals(traducaoAtual)) {
                    out("     " + AnsiCores.DIM + "Google sem alteração aplicável ("
                        + resultadoGoogle.status() + "); mantido." + AnsiCores.RESET);
                    registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                    totalPendentes[0]++;
                    eventosAtualizados.add(evento);
                    continue;
                }
                novaTraducao = restauradaGoogle;
            }

            if (!correcaoEhSegura(originalEn, traducaoAtual, novaTraducao, auditoria, contexto)) {
                String motivo = modo == ModoRevisaoLegendas.LLM_CONCORDANCIA
                    ? "Correção descartada: resposta LLM inválida ou sem melhoria."
                    : "Correção descartada: resposta Google inválida ou sem melhoria.";
                out("     " + AnsiCores.YELLOW + motivo + AnsiCores.RESET);
                detalhesRevisao.add(new DetalheRevisao(
                    arquivoPt.getFileName().toString(), evento.indice(), evento.estilo(),
                    modo == ModoRevisaoLegendas.LLM_CONCORDANCIA
                        ? "LLM_REJEITADO_SEM_MELHORIA" : "GOOGLE_REJEITADO_SEM_MELHORIA",
                    auditoria.motivos(), motivo, originalEn, traducaoAtual, novaTraducao));
                registrarSemAlteracao(textoMascOriginal, revisoesSemAlteracao);
                totalPendentes[0]++;
                eventosAtualizados.add(evento);
                continue;
            }

            out("     PT corrigido: " + AnsiCores.GREEN + novaTraducao + AnsiCores.RESET);
            detalhesRevisao.add(new DetalheRevisao(
                arquivoPt.getFileName().toString(), evento.indice(), evento.estilo(),
                modo == ModoRevisaoLegendas.LLM_CONCORDANCIA ? "CORRIGIDA_LLM" : "CORRIGIDA_GOOGLE",
                auditoria.motivos(), "Correção validada e persistida.",
                originalEn, traducaoAtual, novaTraducao));
            eventosAtualizados.add(evento.comTexto(novaTraducao));
            corrigidasNesteArquivo++;
            modificado = true;

            MascaradorTags.Mascarado mascNova = mascaradorTags.mascarar(novaTraducao);
            if (textoMascOriginal != null) {
                cacheRevisaoMasc.put(textoMascOriginal, mascNova.texto());
            }
        }

        if (modificado) {
            DocumentoLegenda revisado = new DocumentoLegenda(
                documentoPt.cabecalho(),
                eventosAtualizados,
                documentoPt.quebraDeLinha(),
                documentoPt.comBom()
            );
            Path destino = saidaDir.resolve(arquivoPt.getFileName());
            Path backup = criarBackupSeSobrescrever(arquivoPt, destino, pastaBackup);
            escritor.escrever(destino, revisado);
            totalCorrigidas[0] += corrigidasNesteArquivo;
            out(AnsiCores.GREEN + "  [OK] sincronizadas=" + sincronizadasNesteArquivo
                + ", revisadas=" + corrigidasNesteArquivo
                + ". Salvo em: " + destino.getFileName() + AnsiCores.RESET);
            if (backup != null) {
                out(AnsiCores.CYAN + "  Backup anterior: " + backup + AnsiCores.RESET);
            }
        } else if (problemasNesteArquivo > 0) {
            out(AnsiCores.YELLOW + "  Problemas encontrados, mas nenhuma correção aplicada."
                + AnsiCores.RESET);
        } else if (falasAuditadas == 0 && falasSemOriginal > 0) {
            out(AnsiCores.YELLOW + "  -> Nenhuma fala auditada ("
                + falasSemOriginal + " ignoradas por falta de original EN)." + AnsiCores.RESET);
        } else {
            out("  -> Nenhum problema detectado neste arquivo ("
                + falasAuditadas + " falas auditadas).");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma resposta externa pode substituir a
     * fala atual sem introduzir alucinação ou piorar a auditoria.
     *
     * <p>INVARIANTES DO DOMÍNIO: texto vazio, alteração de termo canônico,
     * suspeita estrutural, problema novo e resultado sem redução de problemas
     * são sempre rejeitados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação que lança exceção retorna
     * {@code false} e mantém a legenda anterior.
     */
    private boolean correcaoEhSegura(
        String original,
        String traducaoAtual,
        String candidata,
        ResultadoDeteccaoConcordancia auditoriaAnterior,
        ContextoRevisao contexto
    ) {
        if (candidata == null || candidata.isBlank() || candidata.equals(traducaoAtual)) return false;
        List<String> termosAlterados = protetorLore.termosCanonicosAlterados(
            original, candidata, contexto.lore(), contexto.termosProtegidos());
        if (!termosAlterados.isEmpty()) {
            out("     " + AnsiCores.YELLOW
                + "[LORE] Correção rejeitada: alteraria termo(s) canônico(s): "
                + String.join(", ", termosAlterados) + AnsiCores.RESET);
            return false;
        }
        try {
            validador.validarFala(candidata);
            if (protecaoAss.respostaSuspeita(original, candidata)) return false;
        } catch (AlucinacaoDetectadaException e) {
            return false;
        }
        ResultadoDeteccaoConcordancia posterior = auditor.auditar(original, candidata);
        boolean introduziuProblemaNovo = posterior.motivos().stream()
            .anyMatch(motivo -> !auditoriaAnterior.motivos().contains(motivo));
        if (introduziuProblemaNovo) {
            out("     " + AnsiCores.YELLOW
                + "Correção rejeitada: a proposta introduziu um problema diferente do original."
                + AnsiCores.RESET);
            return false;
        }
        return !posterior.suspeito()
            || posterior.motivos().size() < auditoriaAnterior.motivos().size();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restringe o Google a falhas objetivas de tradução,
     * deixando concordância e estilo para o LLM local com lore.
     *
     * <p>INVARIANTES DO DOMÍNIO: gênero, pronome e tratamento isolados nunca
     * provocam retradução completa pelo Google.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: motivo desconhecido retorna falso e a
     * fala é preservada para inspeção segura.
     */
    private boolean exigeRetraducao(ResultadoDeteccaoConcordancia auditoria) {
        return auditoria.motivos().stream().anyMatch(motivo ->
            motivo.contains("Resíduo gringo")
                || motivo.contains("Fala não traduzida")
                || motivo.contains("Idioma incorreto")
                || motivo.contains("Preâmbulo")
                || motivo.contains("Marcador de erro de tradução"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra que uma origem já foi analisada e não teve
     * correção aplicável sem usar o próprio inglês como sentinela textual.
     *
     * <p>INVARIANTES DO DOMÍNIO: chave nula — caso de fala sem original — nunca
     * entra no conjunto compartilhado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: chave ausente não produz efeito.
     */
    private void registrarSemAlteracao(String chave, Set<String> revisoesSemAlteracao) {
        if (chave != null && !chave.isBlank()) revisoesSemAlteracao.add(chave);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ativa a ponte 5→6 somente quando a manutenção do
     * cache ocorreu depois da geração da legenda.
     *
     * <p>INVARIANTES DO DOMÍNIO: arquivo inexistente ou empate de data não
     * autoriza sobrescrita da legenda.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de metadados desativa a
     * sincronização e preserva o ASS atual.
     */
    private boolean cacheMaisNovoQueLegenda(Path cache, Path legenda) {
        if (!Files.isRegularFile(cache) || !Files.isRegularFile(legenda)) return false;
        try {
            return Files.getLastModifiedTime(cache).compareTo(Files.getLastModifiedTime(legenda)) > 0;
        } catch (IOException e) {
            out(AnsiCores.YELLOW + "  Aviso: não foi possível comparar cache e legenda; "
                + "sincronização automática desativada." + AnsiCores.RESET);
            return false;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica falas válidas que coincidem com o inglês
     * apenas porque são formadas exclusivamente por nomes ou termos canônicos.
     * <p>INVARIANTES DO DOMÍNIO: exige igualdade exata com o original do cache,
     * evento dialogado e confirmação pelo protetor da lore ativa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas ausentes retornam conjunto
     * vazio; nenhuma fala ambígua recebe proteção automática.
     */
    private Set<Integer> localizarIndicesCanonicosProtegidos(
        DocumentoLegenda documento,
        List<EntradaCache> entradas,
        ContextoRevisao contexto
    ) {
        if (documento == null || entradas == null || entradas.isEmpty() || contexto == null) {
            return Set.of();
        }
        Map<Integer, EntradaCache> porIndice = new HashMap<>();
        for (EntradaCache entrada : entradas) {
            porIndice.putIfAbsent(entrada.indice(), entrada);
        }
        Set<Integer> protegidos = new LinkedHashSet<>();
        for (EventoLegenda evento : documento.eventos()) {
            EntradaCache entrada = porIndice.get(evento.indice());
            if (!evento.isDialogo() || entrada == null || entrada.original() == null
                || !entrada.original().equals(evento.texto())) {
                continue;
            }
            if (protetorLore.contemSomenteTermosCanonicos(
                entrada.original(), contexto.lore(), contexto.termosProtegidos())) {
                protegidos.add(evento.indice());
            }
        }
        return Set.copyOf(protegidos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta quando uma legenda apresentada à etapa de
     * revisão é, na realidade, um artefato não traduzido ou parcialmente
     * restaurado do inglês, evitando usar o revisor como tradutor em massa.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente diálogos auditáveis com original EN e
     * igualdade textual efetiva entram na contagem; nomes protegidos pela lore
     * não são falsamente classificados como falha; o bloqueio exige ao menos
     * vinte ocorrências e dez por cento das falas auditáveis.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: documento sem base comparável produz
     * diagnóstico vazio e não bloqueia o fluxo.
     */
    private DiagnosticoRetraducao diagnosticarRetraducaoEmMassa(
        DocumentoLegenda documento,
        Map<Integer, String> originaisPorIndice,
        ContextoRevisao contexto
    ) {
        if (documento == null || originaisPorIndice == null || originaisPorIndice.isEmpty()) {
            return new DiagnosticoRetraducao(0, 0, false);
        }
        int auditaveis = 0;
        int naoTraduzidas = 0;
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || evento.texto() == null || evento.texto().isBlank()
                || deveIgnorarAuditoria(evento, evento.texto())) {
                continue;
            }
            String original = originaisPorIndice.get(evento.indice());
            if (original == null || original.isBlank()) continue;
            auditaveis++;
            if (!normalizarTexto(original).equals(normalizarTexto(evento.texto()))) continue;
            if (protetorLore.contemSomenteTermosCanonicos(
                original, contexto.lore(), contexto.termosProtegidos())) {
                continue;
            }
            ResultadoDeteccaoConcordancia resultado = auditor.auditar(original, evento.texto());
            if (resultado.motivos().stream().anyMatch(m -> m.contains("Fala não traduzida"))) {
                naoTraduzidas++;
            }
        }
        boolean bloquear = excedeLimiarRetraducaoEmMassa(auditaveis, naoTraduzidas);
        return new DiagnosticoRetraducao(auditaveis, naoTraduzidas, bloquear);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica o limite operacional que impede a etapa de
     * revisão de assumir silenciosamente o trabalho da Tradução Local.
     * <p>INVARIANTES DO DOMÍNIO: exige simultaneamente vinte falas e dez por
     * cento do material auditável; valores negativos nunca autorizam bloqueio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: contagens inválidas retornam falso.
     */
    static boolean excedeLimiarRetraducaoEmMassa(int falasAuditaveis, int falasNaoTraduzidas) {
        if (falasAuditaveis <= 0 || falasNaoTraduzidas < 0) return false;
        return falasNaoTraduzidas >= LIMIAR_ABSOLUTO_RETRADUCAO_EM_MASSA
            && falasNaoTraduzidas * DIVISOR_PROPORCAO_RETRADUCAO_EM_MASSA >= falasAuditaveis;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transporta as métricas usadas para separar revisão
     * linguística de uma retradução acidental do arquivo inteiro.
     * <p>INVARIANTES DO DOMÍNIO: contadores representam a mesma fotografia do
     * documento após a recuperação do cache.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record é imutável e não altera arquivos.
     */
    private record DiagnosticoRetraducao(
        int falasAuditaveis,
        int falasNaoTraduzidas,
        boolean deveBloquear
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a legenda anterior antes de a Opção 6
     * sobrescrever o arquivo de trabalho.
     *
     * <p>INVARIANTES DO DOMÍNIO: backup só é necessário quando origem e destino
     * são o mesmo arquivo; a primeira fotografia da sessão nunca é substituída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança exceção de domínio e bloqueia a
     * escrita da nova legenda.
     */
    Path criarBackupSeSobrescrever(Path origem, Path destino, Path pastaBackup) {
        Path origemAbs = origem.toAbsolutePath().normalize();
        Path destinoAbs = destino.toAbsolutePath().normalize();
        if (!origemAbs.equals(destinoAbs)) return null;
        Path backup = pastaBackup.resolve(origem.getFileName()).normalize();
        if (!backup.startsWith(pastaBackup)) {
            throw new RaspagemRevisaoException("Caminho de backup inválido para: " + origem);
        }
        try {
            Files.createDirectories(backup.getParent());
            if (Files.notExists(backup)) {
                Files.copy(origemAbs, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
            return backup;
        } catch (IOException e) {
            throw new RaspagemRevisaoException("Falha ao criar backup da legenda: " + origem, e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: exclui da revisão linguística elementos estruturais,
     * desenhos, estilos ignorados e karaokê que não representam diálogo PT-BR.
     *
     * <p>INVARIANTES DO DOMÍNIO: conteúdo vetorial ASS e efeitos protegidos nunca
     * são enviados ao Google ou ao LLM; música traduzível continua auditável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo sem texto visível é preservado
     * e tratado como ignorável, evitando alteração estrutural.
     */
    private boolean deveIgnorarAuditoria(EventoLegenda evento, String texto) {
        if (!mascaradorTags.contemTextoTraduzivel(texto)) {
            return true;
        }
        if (evento.estilo() != null
            && politicaEstiloMusical.estiloIgnorado(evento.estilo())
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (detectorKaraoke.eEfeitoKaraoke(texto)
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return true;
        }
        if (protecaoAss.deveIgnorarIntervencaoIa(evento.estilo(), texto)) {
            return true;
        }
        String estilo = evento.estilo() != null ? evento.estilo().toLowerCase() : "";
        if (estilo.contains("sign")) {
            return true;
        }
        String visivel = extrairTextoVisivel(texto);
        return estilo.contains("romaji") && visivel.equalsIgnoreCase("you");
    }

    private String extrairTextoVisivel(String texto) {
        return texto.replaceAll("\\{[^}]*\\}", "").replace("\\N", " ").trim();
    }

    private String normalizarTexto(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    private String normalizarEstilo(String estilo) {
        return estilo == null ? "" : estilo.trim().toLowerCase();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: no modo "Cache", monta a referência EN vindo somente
     * do cache, aceitando uma entrada como referência de uma fala apenas quando o
     * vínculo é seguro; o resto fica marcado como SEM_REFERÊNCIA_SEGURA.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma entrada só vira referência se houver
     * proveniência válida no cache e ela casar com a fala em índice, estilo e
     * texto traduzido (normalizado). Placas/karaokê ({@link #deveIgnorarAuditoria})
     * não exigem referência e não são marcadas. Falas sem qualquer entrada no
     * índice não são "inseguras" — apenas ficam sem referência.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: proveniência ausente (cache legado) torna
     * toda fala SEM_REFERÊNCIA_SEGURA, protegendo contra vínculo às cegas.
     */
    ReferenciaCacheSegura montarReferenciaCacheSegura(
        DocumentoLegenda documentoPt,
        List<EntradaCache> entradasCache,
        ProvenienciaCache proveniencia
    ) {
        Map<Integer, String> originaisPorIndice = new HashMap<>();
        Set<Integer> semReferenciaSegura = new LinkedHashSet<>();

        Map<Integer, EntradaCache> cachePorIndice = new HashMap<>();
        for (EntradaCache entrada : entradasCache) {
            cachePorIndice.putIfAbsent(entrada.indice(), entrada);
        }
        boolean provenienciaOk = proveniencia != null
            && proveniencia.contextoId() != null && !proveniencia.contextoId().isBlank();

        for (EventoLegenda evento : documentoPt.eventos()) {
            if (!evento.isDialogo() || evento.texto() == null || evento.texto().isBlank()) {
                continue;
            }
            if (deveIgnorarAuditoria(evento, evento.texto())) {
                continue;
            }
            EntradaCache entrada = cachePorIndice.get(evento.indice());
            if (entrada == null) {
                continue; // sem entrada no índice: fala apenas não referenciada, não "insegura"
            }
            boolean estiloOk = normalizarEstilo(entrada.estilo()).equals(normalizarEstilo(evento.estilo()));
            boolean originalOk = entrada.original() != null && !entrada.original().isBlank();
            // Vínculo seguro coerente com a sincronização: a fala PT atual precisa
            // corresponder à tradução do cache (já correta) OU ao original inglês do
            // cache (regrediu ao EN e será restaurada pela sincronização). Qualquer
            // outro texto indica índice deslocado / outro episódio → inseguro.
            String ptNormalizado = normalizarTexto(evento.texto());
            boolean textoOk = (entrada.traduzido() != null
                    && ptNormalizado.equals(normalizarTexto(entrada.traduzido())))
                || (originalOk && ptNormalizado.equals(normalizarTexto(entrada.original())));
            if (provenienciaOk && estiloOk && textoOk && originalOk) {
                originaisPorIndice.put(evento.indice(), entrada.original());
            } else {
                semReferenciaSegura.add(evento.indice());
            }
        }
        return new ReferenciaCacheSegura(originaisPorIndice, semReferenciaSegura);
    }

    record ReferenciaCacheSegura(
        Map<Integer, String> originaisPorIndice,
        Set<Integer> semReferenciaSegura
    ) {}

    /**
     * PROPÓSITO DE NEGÓCIO: localiza deterministicamente o cache correspondente
     * à legenda PT-BR mesmo quando a raiz contém subpastas por obra.
     *
     * <p>INVARIANTES DO DOMÍNIO: candidatos diretos têm prioridade; busca
     * recursiva é ordenada e nunca seleciona arquivo fora da raiz informada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de varredura devolve o caminho
     * esperado, que será tratado como cache ausente pelo leitor.
     */
    private Path resolverArquivoCache(Path arquivoPt, Path cacheDir) {
        String baseLegenda = nomeBaseLegenda(arquivoPt);
        String baseMidia = normalizarBaseLegenda(baseLegenda);
        String codigoEpisodio = extrairCodigoEpisodio(baseLegenda);

        for (String candidato : candidatosNomeCache(baseLegenda, baseMidia)) {
            Path direto = cacheDir.resolve(candidato);
            if (Files.isRegularFile(direto)) {
                return direto;
            }
        }

        if (Files.isDirectory(cacheDir)) {
            try (Stream<Path> stream = Files.walk(cacheDir)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> correspondeCache(p.getFileName().toString(), baseMidia, codigoEpisodio))
                    .sorted()
                    .findFirst()
                    .orElse(cacheDir.resolve(baseMidia + "_ENG.cache.json"));
            } catch (IOException e) {
                return cacheDir.resolve(baseMidia + "_ENG.cache.json");
            }
        }

        return cacheDir.resolve(baseMidia + "_ENG.cache.json");
    }

    private List<String> candidatosNomeCache(String baseLegenda, String baseMidia) {
        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(baseMidia + "_ENG.cache.json");
        candidatos.add(baseMidia + ".cache.json");
        candidatos.add(baseLegenda + ".cache.json");
        candidatos.add(baseLegenda + "_ENG.cache.json");
        return List.copyOf(candidatos);
    }

    private boolean correspondeCache(String nomeArquivo, String baseMidia, String codigoEpisodio) {
        if (!nomeArquivo.toLowerCase().endsWith(".cache.json")) {
            return false;
        }
        String stem = nomeArquivo.substring(0, nomeArquivo.length() - ".cache.json".length());
        if (normalizarBaseLegenda(stem).equalsIgnoreCase(baseMidia)) {
            return true;
        }
        return codigoEpisodio != null
            && nomeArquivo.toUpperCase().contains(codigoEpisodio)
            && nomeArquivo.toUpperCase().contains("_ENG");
    }

    private String nomeBaseLegenda(Path arquivoPt) {
        String nome = arquivoPt.getFileName().toString();
        String ext = extensaoLegenda(nome);
        return nome.substring(0, nome.length() - ext.length());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece à revisão as referências EN/PT e a
     * proveniência produzidas pelas etapas 4 e 5.
     *
     * <p>INVARIANTES DO DOMÍNIO: entradas e contexto pertencem ao mesmo documento
     * e a leitura nunca modifica o banco de cache.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: registra aviso e devolve lista vazia,
     * permitindo usar uma legenda inglesa externa como fallback.
     */
    private LeitorCacheReferenciaService.DocumentoReferencia carregarDocumentoCache(Path cachePath) {
        if (!Files.isRegularFile(cachePath)) {
            return new LeitorCacheReferenciaService.DocumentoReferencia(List.of(), null);
        }
        try {
            return leitorCache.carregarDocumento(cachePath);
        } catch (IOException e) {
            out(AnsiCores.YELLOW + "  Aviso: não foi possível ler cache "
                + cachePath.getFileName() + ": " + e.getMessage() + AnsiCores.RESET);
            return new LeitorCacheReferenciaService.DocumentoReferencia(List.of(), null);
        }
    }

    private Map<String, String> indexarOriginalPorTraduzido(List<EntradaCache> entradas) {
        Map<String, String> mapa = new HashMap<>();
        for (EntradaCache entrada : entradas) {
            if (entrada.traduzido() == null || entrada.traduzido().isBlank()) {
                continue;
            }
            if (entrada.original() == null || entrada.original().isBlank()) {
                continue;
            }
            mapa.putIfAbsent(normalizarTexto(entrada.traduzido()), entrada.original());
        }
        return mapa;
    }

    private Map<Integer, String> carregarOriginaisDeLegenda(Path arquivoEn) {
        Map<Integer, String> mapa = new HashMap<>();
        if (!Files.isRegularFile(arquivoEn)) {
            return mapa;
        }

        DocumentoLegenda docEn = leitor.ler(arquivoEn);
        for (EventoLegenda evento : docEn.eventos()) {
            if (evento.isDialogo() && evento.texto() != null && !evento.texto().isBlank()) {
                mapa.put(evento.indice(), evento.texto());
            }
        }
        return mapa;
    }

    private Path resolverArquivoOriginal(Path arquivoPt, Path pastaLegendasEn) {
        String nome = arquivoPt.getFileName().toString();
        String ext = extensaoLegenda(nome);
        String baseSemPt = normalizarBaseLegenda(nome.substring(0, nome.length() - ext.length()));
        String codigoEpisodio = extrairCodigoEpisodio(baseSemPt);

        Set<String> candidatos = new LinkedHashSet<>();
        candidatos.add(baseSemPt + ext);
        candidatos.add(baseSemPt + "_ENG" + ext);
        candidatos.add(baseSemPt + "_Eng" + ext);
        for (int track = 1; track <= 9; track++) {
            candidatos.add(baseSemPt + "_Track" + track + ext);
        }

        Matcher ptbrTrack = SUFIXO_PTBR_TRACK.matcher(nome.substring(0, nome.length() - ext.length()));
        if (ptbrTrack.find()) {
            String baseMidia = nome.substring(0, ptbrTrack.start());
            candidatos.add(baseMidia + "_Track2" + ext);
            candidatos.add(baseMidia + "_Track1" + ext);
            candidatos.add(baseMidia + ext);
        }

        for (String candidato : candidatos) {
            Path path = pastaLegendasEn.resolve(candidato);
            if (Files.isRegularFile(path) && !path.equals(arquivoPt) && !eLegendaTraduzida(path)) {
                return path;
            }
        }

        if (codigoEpisodio != null && Files.isDirectory(pastaLegendasEn)) {
            try (Stream<Path> stream = Files.list(pastaLegendasEn)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(this::temExtensaoSuportada)
                    .filter(p -> !p.equals(arquivoPt))
                    .filter(p -> !eLegendaTraduzida(p))
                    .filter(p -> p.getFileName().toString().toUpperCase().contains(codigoEpisodio))
                    .min(Comparator.comparingInt(p -> preferenciaArquivoOriginal(p.getFileName().toString())))
                    .orElse(pastaLegendasEn.resolve(baseSemPt + ext));
            } catch (IOException e) {
                return pastaLegendasEn.resolve(baseSemPt + ext);
            }
        }

        return pastaLegendasEn.resolve(baseSemPt + ext);
    }

    private int preferenciaArquivoOriginal(String nome) {
        String n = nome.toLowerCase();
        if (n.contains("_track2")) {
            return 0;
        }
        if (n.contains("_eng")) {
            return 1;
        }
        if (n.contains("_track1")) {
            return 2;
        }
        return 10;
    }

    private boolean eLegendaTraduzida(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return nome.contains("_ptbr") || nome.contains("_pt-br");
    }

    private String normalizarBaseLegenda(String base) {
        return base
            .replaceFirst("(?i)_PT-?BR(_Track\\d+)?$", "")
            .replaceFirst("(?i)_Track\\d+$", "")
            .replaceFirst("(?i)_ENG$", "");
    }

    private String extensaoLegenda(String nome) {
        return nome.toLowerCase().endsWith(".ssa") ? ".ssa" : ".ass";
    }

    private String extrairCodigoEpisodio(String nome) {
        Matcher matcher = CODIGO_EPISODIO.matcher(nome);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: solicita ao LLM uma revisão pontual sem permitir que
     * nomes e termos oficiais definidos pela lore sejam traduzidos.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags ASS e termos canônicos são mascarados antes
     * da chamada e precisam ser restaurados integralmente antes da validação.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resposta vazia, marcador perdido ou
     * proposta estruturalmente inválida devolve diagnóstico explícito, sem
     * confundir rejeição de conteúdo com indisponibilidade do servidor.
     */
    private TentativaRevisaoLegenda tentarRevisarConcordancia(
        String original,
        String traduzido,
        List<String> motivos,
        ContextoRevisao contexto
    ) {
        String textoOriginal = original != null ? original : "";
        ProtetorTermosLoreService.TextoProtegido originalProtegido = protetorLore.mascarar(
            textoOriginal, contexto.lore(), contexto.termosProtegidos());
        ProtetorTermosLoreService.TextoProtegido traducaoProtegida = protetorLore.mascarar(
            traduzido, contexto.lore(), contexto.termosProtegidos());
        MascaradorTags.Mascarado mascOriginal = mascaradorTags.mascarar(originalProtegido.textoMascarado());
        MascaradorTags.Mascarado mascTraduzido = mascaradorTags.mascarar(traducaoProtegida.textoMascarado());

        boolean precisaRetraducaoCompleta = motivos.stream().anyMatch(
            m -> m.contains("Resíduo gringo") || m.contains("não traduzida"));
        Optional<String> resposta;

        if (precisaRetraducaoCompleta) {
            resposta = mistralPort.corrigirTraducao(
                mascOriginal.texto(),
                mascTraduzido.texto(),
                String.join(", ", motivos)
            );
        } else {
            resposta = mistralPort.revisarConcordancia(
                mascOriginal.texto(),
                mascTraduzido.texto(),
                motivos
            );
        }

        if (resposta.isEmpty()) {
            return TentativaRevisaoLegenda.pendente(
                "LLM_SEM_CONTEUDO_UTILIZAVEL",
                "O servidor não devolveu choices/content utilizável; consulte o log técnico para HTTP ou timeout.",
                null);
        }

        String proposta = resposta.get();
        try {
            String desmascarado = mascaradorTags.desmascarar(proposta, mascTraduzido.tags());
            String restaurado = protetorLore.restaurar(desmascarado, traducaoProtegida);
            if (restaurado == null) {
                return TentativaRevisaoLegenda.pendente(
                    "LLM_MARCADOR_LORE_INCOMPATIVEL",
                    "A proposta perdeu ou alterou marcador protegido pela lore.", proposta);
            }
            validador.validarFala(restaurado);
            if (protecaoAss.respostaSuspeita(original, restaurado)) {
                return TentativaRevisaoLegenda.pendente(
                    "LLM_ESTRUTURA_ASS_SUSPEITA",
                    "A proposta alterou a estrutura protegida da legenda ASS.", proposta);
            }
            return TentativaRevisaoLegenda.sucesso(restaurado, proposta);
        } catch (AlucinacaoDetectadaException e) {
            return TentativaRevisaoLegenda.pendente(
                "LLM_VALIDACAO_REJEITADA", mensagemFalha(e), proposta);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma exceções de validação em mensagens curtas
     * para o console e para o relatório operacional da revisão.
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca expõe stack trace e nunca devolve texto
     * vazio; a proposta completa continua registrada separadamente no detalhe.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: quando a exceção não possui mensagem,
     * usa o nome da classe como diagnóstico mínimo.
     */
    private String mensagemFalha(Exception erro) {
        return erro.getMessage() == null || erro.getMessage().isBlank()
            ? erro.getClass().getSimpleName() : erro.getMessage();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta ao relatório final a trilha auditável de
     * cada correção, rejeição ou bloqueio ocorrido durante a Opção 6.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada item identifica arquivo, evento, resultado,
     * problemas detectados e proposta do modelo; quebras internas são escapadas
     * para que uma ocorrência permaneça legível em um único bloco.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula ou vazia gera seção explícita
     * sem detalhes, sem impedir a persistência da telemetria resumida.
     */
    private String formatarDetalhesRelatorio(List<DetalheRevisao> detalhes) {
        StringBuilder texto = new StringBuilder("\nDETALHES POR OCORRÊNCIA\n=======================\n");
        if (detalhes == null || detalhes.isEmpty()) {
            return texto.append("Nenhuma ocorrência detalhada registrada.\n").toString();
        }
        for (DetalheRevisao detalhe : detalhes) {
            texto.append("\nArquivo: ").append(detalhe.arquivo()).append('\n')
                .append("Evento: ").append(detalhe.evento()).append(" | Estilo: ")
                .append(resumirCampo(detalhe.estilo())).append('\n')
                .append("Resultado: ").append(detalhe.resultado()).append('\n')
                .append("Problemas: ").append(String.join(" | ", detalhe.problemas())).append('\n')
                .append("Diagnóstico: ").append(resumirCampo(detalhe.diagnostico())).append('\n')
                .append("EN: ").append(resumirCampo(detalhe.original())).append('\n')
                .append("PT anterior: ").append(resumirCampo(detalhe.antes())).append('\n')
                .append("Proposta: ").append(resumirCampo(detalhe.depois())).append('\n');
        }
        return texto.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém textos de legenda legíveis dentro do relatório
     * operacional sem perder as quebras ASS relevantes.
     *
     * <p>INVARIANTES DO DOMÍNIO: não altera o conteúdo persistido e limita apenas
     * a representação diagnóstica a 500 caracteres.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor ausente é representado por hífen.
     */
    private String resumirCampo(String valor) {
        if (valor == null || valor.isBlank()) return "—";
        String limpo = valor.replace("\r", "").replace("\n", " ↵ ").strip();
        return limpo.length() <= 500 ? limpo : limpo.substring(0, 497) + "...";
    }

    private void pausaGoogle() {
        try {
            Thread.sleep(PAUSA_GOOGLE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean temExtensaoSuportada(Path arquivo) {
        String nome = arquivo.getFileName().toString().toLowerCase();
        return EXTENSOES.stream().anyMatch(nome::endsWith);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o resultado técnico de uma chamada de
     * revisão para que console e relatório expliquem por que a fala foi mantida.
     *
     * <p>INVARIANTES DO DOMÍNIO: sucesso sempre contém texto revisado; pendência
     * sempre contém código e diagnóstico, podendo conservar a proposta rejeitada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fábricas normalizam valores ausentes e
     * nunca permitem que a indisponibilidade seja inferida sem evidência.
     */
    private record TentativaRevisaoLegenda(
        Optional<String> revisado,
        String codigo,
        String detalhe,
        String proposta
    ) {
        static TentativaRevisaoLegenda sucesso(String revisado, String proposta) {
            return new TentativaRevisaoLegenda(
                Optional.of(revisado), "LLM_RESPOSTA_VALIDADA", "Resposta validada.", proposta);
        }

        static TentativaRevisaoLegenda pendente(String codigo, String detalhe, String proposta) {
            return new TentativaRevisaoLegenda(
                Optional.empty(), codigo, detalhe == null ? "Falha não detalhada." : detalhe, proposta);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra a evidência por fala usada no relatório da
     * revisão e no futuro dataset de melhoria dos detectores.
     *
     * <p>INVARIANTES DO DOMÍNIO: problemas é imutável e cada registro pertence a
     * um arquivo/evento ou ao bloqueio global do arquivo indicado por evento -1.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista de problemas ausente é normalizada
     * para lista vazia e o record não executa I/O.
     */
    private record DetalheRevisao(
        String arquivo,
        int evento,
        String estilo,
        String resultado,
        List<String> problemas,
        String diagnostico,
        String original,
        String antes,
        String depois
    ) {
        private DetalheRevisao {
            problemas = problemas == null ? List.of() : List.copyOf(problemas);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém a identidade e o glossário operacional da
     * obra ativos durante a revisão de um arquivo.
     * <p>INVARIANTES DO DOMÍNIO: lore nunca é nula e termos pertencem ao contexto.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável não executa I/O.
     */
    record ContextoRevisao(String id, String lore, Set<String> termosProtegidos) {}
}
