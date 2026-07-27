package org.traducao.projeto.raspagemRevisao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.correcaoLegendas.application.SanitizadorTagsService;
import org.traducao.projeto.raspagemCorrecao.application.ProtetorTermosLoreService;
import org.traducao.projeto.raspagemRevisao.domain.ContextoRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DetalheRevisao;
import org.traducao.projeto.raspagemRevisao.domain.DiagnosticoRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.FrescorCache;
import org.traducao.projeto.raspagemRevisao.domain.ModoReferenciaRevisao;
import org.traducao.projeto.raspagemRevisao.domain.PreparacaoReferencia;
import org.traducao.projeto.raspagemRevisao.domain.ModoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.domain.PoliticaRetraducao;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoRecuperacaoExterna;
import org.traducao.projeto.raspagemRevisao.domain.ports.RecuperacaoExternaRevisaoPort;
import org.traducao.projeto.raspagemRevisao.domain.exceptions.RaspagemRevisaoException;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class RevisarLegendasUseCase {

    private static final long PAUSA_GOOGLE_MS = 400;
    private static final DateTimeFormatter TS_BACKUP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final PersistenciaLegendaRevisada persistencia;
    private final RecuperacaoExternaRevisaoPort recuperacaoExterna;
    private final AuditorProblemasLegendaService auditor;
    private final ValidadorTraducaoService validador;
    private final LlmPort llmPort;
    private final MascaradorTags mascaradorTags;
    private final RelatorioRevisaoService relatorio;
    private final SanitizadorTagsService sanitizadorTags;
    private final ProtecaoLegendaAssService protecaoAss;
    private final ProtetorTermosLoreService protetorLore;
    private final CorretorDeterministicoConcordanciaService corretorDeterministico;
    private final ResolvedorArtefatosRevisao resolvedorArtefatos;
    private final SincronizacaoPreviaRevisao sincronizacaoPrevia;
    private final FiltroAuditoriaLinha filtroAuditoria;
    private final DetectorRetraducaoEmMassaService detectorRetraducaoEmMassa;
    private final PreparadorReferenciaRevisao preparador;

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
        PersistenciaLegendaRevisada persistencia,
        RecuperacaoExternaRevisaoPort recuperacaoExterna,
        AuditorProblemasLegendaService auditor,
        ValidadorTraducaoService validador,
        LlmPort llmPort,
        MascaradorTags mascaradorTags,
        RelatorioRevisaoService relatorio,
        SanitizadorTagsService sanitizadorTags,
        ProtecaoLegendaAssService protecaoAss,
        ProtetorTermosLoreService protetorLore,
        CorretorDeterministicoConcordanciaService corretorDeterministico,
        ResolvedorArtefatosRevisao resolvedorArtefatos,
        SincronizacaoPreviaRevisao sincronizacaoPrevia,
        FiltroAuditoriaLinha filtroAuditoria,
        DetectorRetraducaoEmMassaService detectorRetraducaoEmMassa,
        PreparadorReferenciaRevisao preparador
    ) {
        this.persistencia = persistencia;
        this.recuperacaoExterna = recuperacaoExterna;
        this.auditor = auditor;
        this.validador = validador;
        this.llmPort = llmPort;
        this.mascaradorTags = mascaradorTags;
        this.relatorio = relatorio;
        this.sanitizadorTags = sanitizadorTags;
        this.protecaoAss = protecaoAss;
        this.protetorLore = protetorLore;
        this.corretorDeterministico = corretorDeterministico;
        this.resolvedorArtefatos = resolvedorArtefatos;
        this.sincronizacaoPrevia = sincronizacaoPrevia;
        this.filtroAuditoria = filtroAuditoria;
        this.detectorRetraducaoEmMassa = detectorRetraducaoEmMassa;
        this.preparador = preparador;
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
            if (arquivos.stream().anyMatch(resolvedorArtefatos::temExtensaoSuportada)) {
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
                .filter(resolvedorArtefatos::temExtensaoSuportada)
                .filter(resolvedorArtefatos::eLegendaTraduzida)
                .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                .toList();

            if (arquivos.isEmpty()) {
                Optional<String> erro = validarPastaEntrada(pastaLegendasPt);
                out(AnsiCores.YELLOW + erro.orElse("Nenhum arquivo .ass/.ssa traduzido encontrado na pasta.")
                    + AnsiCores.RESET);
                out("Relatório salvo em: " + relatorio.registrar(
                    pastaLegendasPt, System.currentTimeMillis() - inicioMs,
                    0, 0, 0, 0, 0, 0, modo, detalhesRevisao));
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
        out("Relatório salvo em: " + relatorio.registrar(
            pastaLegendasPt, System.currentTimeMillis() - inicioMs,
            arquivosProcessados[0], falasComProblema[0], falasCorrigidas[0], falasAuditadas[0],
            falasSemOriginal[0], falasPendentes[0], modo, detalhesRevisao));
        return new ResultadoRevisaoLegendas(
            arquivosProcessados[0], falasCorrigidas[0], falasComProblema[0], falasPendentes[0]);
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

        PreparacaoReferencia preparacao = preparador.preparar(
            arquivoPt, pastaLegendasEn, cacheDir, referencia, contextoFallback);
        if (preparacao instanceof PreparacaoReferencia.Bloqueada bloqueada) {
            totalProblemas[0] += bloqueada.problemas();
            totalPendentes[0] += bloqueada.pendentes();
            return;
        }
        PreparacaoReferencia.Pronta pronta = (PreparacaoReferencia.Pronta) preparacao;
        DocumentoLegenda documentoPt = pronta.documento();
        Path cachePath = pronta.cachePath();
        List<EntradaCache> entradasCache = pronta.entradasCache();
        ContextoRevisao contexto = pronta.contexto();
        Path arquivoEn = pronta.arquivoEn();
        Map<Integer, String> originaisPorIndice = pronta.originaisPorIndice();
        Map<String, String> originalPorTraduzido = pronta.originalPorTraduzido();
        Set<Integer> indicesSemReferenciaSegura = pronta.indicesSemReferenciaSegura();
        // Fala sem vinculo seguro e PENDENCIA: no modo Cache nao ha "conclusao com
        // sucesso" enquanto restar fala sem referencia segura. Em AMBOS o conjunto e
        // vazio, entao a conta e a mesma sem ramo por modo.
        totalSemReferenciaSegura[0] += indicesSemReferenciaSegura.size();
        totalPendentes[0] += indicesSemReferenciaSegura.size();

        SessaoRevisaoArquivo sessao = new SessaoRevisaoArquivo();
        SincronizacaoPreviaRevisao.Resultado sincronizacao = sincronizacaoPrevia.sincronizar(
            documentoPt, entradasCache, cachePath, arquivoPt, contexto, referencia, originaisPorIndice);
        if (sincronizacao.frescor() == FrescorCache.INDETERMINADO) {
            out(AnsiCores.YELLOW + "  Aviso: não foi possível comparar cache e legenda; "
                + "sincronização automática desativada." + AnsiCores.RESET);
        }
        boolean sincronizarCache = sincronizacao.sincronizou();
        documentoPt = sincronizacao.documento();
        int sincronizadasNesteArquivo = sincronizacao.total();
        if (sincronizadasNesteArquivo > 0) {
            sessao.marcarModificado();
        }
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

        DiagnosticoRetraducao diagnosticoRetraducao = detectorRetraducaoEmMassa.diagnosticar(
            documentoPt, originaisPorIndice, contexto);
        if (diagnosticoRetraducao.deveBloquear()) {
            if (sincronizadasNesteArquivo > 0) {
                PersistenciaLegendaRevisada.Gravacao gravacao = persistencia.gravar(
                    documentoPt, arquivoPt, saidaDir, pastaBackup);
                out(AnsiCores.GREEN + "  [RECUPERADO] Traduções disponíveis no cache foram salvas antes do bloqueio."
                    + AnsiCores.RESET);
                if (gravacao.backup() != null) {
                    out(AnsiCores.CYAN + "  Backup anterior: " + gravacao.backup() + AnsiCores.RESET);
                }
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
                sessao.manter(evento);
                continue;
            }
            if (!evento.isDialogo() || evento.texto() == null) {
                sessao.manter(evento);
                continue;
            }

            if (evento.texto().isBlank()) {
                sessao.manter(evento);
                continue;
            }

            if (filtroAuditoria.deveIgnorar(evento, evento.texto())) {
                sessao.manter(evento);
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
            // GUARDA DE INTEGRIDADE: uma correção NUNCA pode apagar uma fala. Se o
            // saneamento de tags deixou o texto VISÍVEL vazio numa linha que tinha
            // conteúdo, a "correção" destruiria a legenda — mantém-se o original.
            // Sem esta guarda, um bloco contínuo de diálogo (Guilty Crown ep4, 13:58–20:52)
            // foi esvaziado: as linhas ficaram com tempo e estilo, mas texto em branco.
            boolean esvaziaria = saneamentoEsvaziariaFala(textoNormalizado, textoCorrigidoKaraoke);
            if (!textoNormalizado.equals(textoCorrigidoKaraoke) && !esvaziaria) {
                evento = evento.comTexto(textoCorrigidoKaraoke);
                sessao.contarCorrecaoJaAplicada();
                out("  -> Karaoke corrigido na linha " + evento.indice() + ":");
                out("     De : " + textoNormalizado);
                out("     Para: " + textoCorrigidoKaraoke);
            } else if (esvaziaria) {
                out("  [GUARDA] Linha " + evento.indice()
                    + " preservada: o saneamento de tags esvaziaria a fala \"" + textoNormalizado + "\".");
            }

            String traducaoAtual = evento.texto();
            if (!temOriginalEn) {
                sessao.contarSemOriginal();
                if (modo != ModoRevisaoLegendas.LLM_CONCORDANCIA) {
                    sessao.manter(evento);
                    continue;
                }
            }

            sessao.contarAuditada();

            if (temOriginalEn
                && normalizarTexto(originalEn).equals(normalizarTexto(traducaoAtual))
                && protetorLore.contemSomenteTermosCanonicos(
                    originalEn, contexto.lore(), contexto.termosProtegidos())) {
                out("  [LORE] Evento " + evento.indice()
                    + " contém somente nome/termo canônico; mantido sem chamar IA.");
                sessao.manter(evento);
                continue;
            }

            ResultadoDeteccaoConcordancia auditoria = auditor.auditar(originalEn, traducaoAtual);
            if (!auditoria.suspeito()) {
                sessao.manter(evento);
                continue;
            }

            sessao.contarProblema();

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
                sessao.corrigir(evento, corrigida);
                sessao.registrarCorrecao(textoMascOriginal, mascaradorTags.mascarar(corrigida).texto());
                continue;
            }
            if (sessao.jaSabidoSemAlteracao(textoMascOriginal)) {
                sessao.pendente(evento);
                continue;
            }
            String respostaMascCorrigida = sessao.correcaoConhecida(textoMascOriginal);
            if (respostaMascCorrigida != null) {
                MascaradorTags.Mascarado mascTraducaoAtual = mascaradorTags.mascarar(traducaoAtual);
                String novaTraducaoCache;
                try {
                    novaTraducaoCache = mascaradorTags.desmascarar(respostaMascCorrigida, mascTraducaoAtual.tags());
                } catch (AlucinacaoDetectadaException e) {
                    out("  " + AnsiCores.YELLOW
                        + "Cache local ignorado na linha " + evento.indice()
                        + ": marcadores de tags incompatíveis com a tradução atual."
                        + AnsiCores.RESET);
                    sessao.pendente(evento);
                    continue;
                }

                if (novaTraducaoCache.equals(traducaoAtual)
                    || !correcaoEhSegura(
                        originalEn, traducaoAtual, novaTraducaoCache, auditoria, contexto)) {
                    sessao.registrarSemAlteracao(textoMascOriginal);
                    sessao.pendente(evento);
                    continue;
                }

                out("  -> Linha " + evento.indice() + " [" + evento.estilo() + "] (Reutilizando correção do cache local):");
                out("     EN: " + AnsiCores.YELLOW + originalEn + AnsiCores.RESET);
                out("     PT: " + AnsiCores.YELLOW + traducaoAtual + AnsiCores.RESET);
                out("     PT corrigido: " + AnsiCores.GREEN + novaTraducaoCache + AnsiCores.RESET);

                sessao.corrigir(evento, novaTraducaoCache);
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
                    sessao.pendente(evento);
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
                    sessao.registrarSemAlteracao(textoMascOriginal);
                    sessao.pendente(evento);
                    continue;
                }
            } else {
                if (!PoliticaRetraducao.exigeRetraducaoPeloGoogle(auditoria.motivos())) {
                    out("     " + AnsiCores.DIM
                        + "Google não acionado: problema reservado à revisão LLM." + AnsiCores.RESET);
                    sessao.registrarSemAlteracao(textoMascOriginal);
                    sessao.pendente(evento);
                    continue;
                }
                ProtetorTermosLoreService.TextoProtegido originalProtegido = protetorLore.mascarar(
                    originalEn, contexto.lore(), contexto.termosProtegidos());
                ResultadoRecuperacaoExterna resultadoGoogle =
                    recuperacaoExterna.traduzir(originalProtegido.textoMascarado());
                pausaGoogle();

                String restauradaGoogle = resultadoGoogle.sucesso()
                    ? protetorLore.restaurar(resultadoGoogle.texto(), originalProtegido)
                    : null;
                if (!resultadoGoogle.sucesso() || restauradaGoogle == null
                    || restauradaGoogle.equals(traducaoAtual)) {
                    out("     " + AnsiCores.DIM + "Google sem alteração aplicável ("
                        + resultadoGoogle.status() + "); mantido." + AnsiCores.RESET);
                    sessao.registrarSemAlteracao(textoMascOriginal);
                    sessao.pendente(evento);
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
                sessao.registrarSemAlteracao(textoMascOriginal);
                sessao.pendente(evento);
                continue;
            }

            out("     PT corrigido: " + AnsiCores.GREEN + novaTraducao + AnsiCores.RESET);
            detalhesRevisao.add(new DetalheRevisao(
                arquivoPt.getFileName().toString(), evento.indice(), evento.estilo(),
                modo == ModoRevisaoLegendas.LLM_CONCORDANCIA ? "CORRIGIDA_LLM" : "CORRIGIDA_GOOGLE",
                auditoria.motivos(), "Correção validada e persistida.",
                originalEn, traducaoAtual, novaTraducao));
            sessao.corrigir(evento, novaTraducao);

            sessao.registrarCorrecao(textoMascOriginal,
                mascaradorTags.mascarar(novaTraducao).texto());
        }

        // Os totais do LOTE sobem de uma vez, no fim do arquivo. Antes eram sete `int[]`
        // incrementados de dentro do laco, em quinze lugares diferentes.
        totalAuditadas[0] += sessao.auditadas();
        totalProblemas[0] += sessao.problemas();
        totalSemOriginal[0] += sessao.semOriginal();
        totalPendentes[0] += sessao.pendentes();

        if (sessao.modificado()) {
            DocumentoLegenda revisado = new DocumentoLegenda(
                documentoPt.cabecalho(),
                sessao.eventos(),
                documentoPt.quebraDeLinha(),
                documentoPt.comBom()
            );
            PersistenciaLegendaRevisada.Gravacao gravacao = persistencia.gravar(
                revisado, arquivoPt, saidaDir, pastaBackup);
            totalCorrigidas[0] += sessao.corrigidas();
            out(AnsiCores.GREEN + "  [OK] sincronizadas=" + sincronizadasNesteArquivo
                + ", revisadas=" + sessao.corrigidas()
                + ". Salvo em: " + gravacao.destino().getFileName() + AnsiCores.RESET);
            if (gravacao.backup() != null) {
                out(AnsiCores.CYAN + "  Backup anterior: " + gravacao.backup() + AnsiCores.RESET);
            }
        } else if (sessao.problemas() > 0) {
            out(AnsiCores.YELLOW + "  Problemas encontrados, mas nenhuma correção aplicada."
                + AnsiCores.RESET);
        } else if (sessao.auditadas() == 0 && sessao.semOriginal() > 0) {
            out(AnsiCores.YELLOW + "  -> Nenhuma fala auditada ("
                + sessao.semOriginal() + " ignoradas por falta de original EN)." + AnsiCores.RESET);
        } else {
            out("  -> Nenhum problema detectado neste arquivo ("
                + sessao.auditadas() + " falas auditadas).");
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
     * PROPÓSITO DE NEGÓCIO: guarda de integridade das ferramentas que corrigem a legenda PT-BR
     * direto no arquivo (sem inglês nem cache). Uma correção JAMAIS pode transformar uma fala
     * com texto em uma linha vazia — o resultado é uma legenda que some da tela mantendo tempo e
     * estilo. Foi o que apagou um bloco contínuo de diálogo do Guilty Crown ep4 (13:58–20:52).
     *
     * <p>INVARIANTES DO DOMÍNIO: só acusa quando o ORIGINAL tinha texto visível e o CORRIGIDO
     * ficou sem nenhum — reordenar, reescrever ou trocar tags continua permitido; apagar, não.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulos degradam para string vazia; nunca lança. Original
     * já vazio devolve {@code false} (não há fala a proteger).
     *
     * @return {@code true} se aplicar {@code corrigido} apagaria uma fala que tinha conteúdo
     */
    boolean saneamentoEsvaziariaFala(String original, String corrigido) {
        return !protecaoAss.textoVisivel(original).isBlank()
            && protecaoAss.textoVisivel(corrigido).isBlank();
    }

    private String normalizarTexto(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    private String normalizarEstilo(String estilo) {
        return estilo == null ? "" : estilo.trim().toLowerCase();
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

        boolean precisaRetraducaoCompleta = PoliticaRetraducao.exigeRetraducaoCompletaPeloLlm(motivos);
        Optional<String> resposta;

        if (precisaRetraducaoCompleta) {
            resposta = llmPort.corrigirTraducao(
                mascOriginal.texto(),
                mascTraduzido.texto(),
                String.join(", ", motivos)
            );
        } else {
            resposta = llmPort.revisarConcordancia(
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
                "LLM_VALIDACAO_REJEITADA", RelatorioRevisaoService.mensagemFalha(e), proposta);
        }
    }

    private void pausaGoogle() {
        try {
            Thread.sleep(PAUSA_GOOGLE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
}
