package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;
import org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaSrt;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaSrt;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * PROPÓSITO DE NEGÓCIO: orquestra a tradução de uma legenda, reaproveitando o
 * cache, traduzindo somente pendências e publicando uma saída PT-BR recuperável.
 *
 * <p>INVARIANTES DO DOMÍNIO: correções válidas do cache não são reenviadas ao
 * LLM; tags e estrutura temporal são preservadas; saída parcial não substitui a
 * final sem liberação explícita e backup obrigatório.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: respostas inválidas permanecem pendentes,
 * falhas de IO viram {@link ArquivoLegendaException} e uma substituição liberada
 * é abortada se a versão anterior não puder ser copiada para backup.
 */
@Service
public class ProcessarArquivoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarArquivoUseCase.class);

    private final LeitorLegendaAss leitor;
    private final EscritorLegendaAss escritor;
    private final LeitorLegendaSrt leitorSrt;
    private final EscritorLegendaSrt escritorSrt;
    private final MascaradorTags mascarador;
    private final CacheTraducaoService cacheService;
    private final ProcessarEpisodioUseCase processarEpisodioUseCase;
    private final ValidadorTraducaoService validador;
    private final DetectorTraducaoIdenticaService detectorIdentica;
    private final TradutorProperties propriedades;
    private final LlmProperties llmPropriedades;
    private final ConsoleUILogger uiLogger;
    private final PastasExecucao pastasExecucao;
    private final TelemetriaTraducaoPort telemetriaTraducao;
    private final ProtecaoLegendaAssService protecaoAss;
    private final GerenciadorContexto gerenciadorContexto;
    private final ResolvedorSaidaLegenda resolvedorSaida;
    private final ResolvedorCacheTraducao resolvedorCache;
    private final PoliticaBackupTraducao politicaBackup;
    private final SeletorEventosTraduziveis seletorEventos;

    public ProcessarArquivoUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        LeitorLegendaSrt leitorSrt,
        EscritorLegendaSrt escritorSrt,
        MascaradorTags mascarador,
        CacheTraducaoService cacheService,
        ProcessarEpisodioUseCase processarEpisodioUseCase,
        ValidadorTraducaoService validador,
        DetectorTraducaoIdenticaService detectorIdentica,
        TradutorProperties propriedades,
        LlmProperties llmPropriedades,
        ConsoleUILogger uiLogger,
        PastasExecucao pastasExecucao,
        TelemetriaTraducaoPort telemetriaTraducao,
        ProtecaoLegendaAssService protecaoAss,
        GerenciadorContexto gerenciadorContexto,
        ResolvedorSaidaLegenda resolvedorSaida,
        ResolvedorCacheTraducao resolvedorCache,
        PoliticaBackupTraducao politicaBackup,
        SeletorEventosTraduziveis seletorEventos
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.leitorSrt = leitorSrt;
        this.escritorSrt = escritorSrt;
        this.mascarador = mascarador;
        this.cacheService = cacheService;
        this.processarEpisodioUseCase = processarEpisodioUseCase;
        this.validador = validador;
        this.detectorIdentica = detectorIdentica;
        this.propriedades = propriedades;
        this.llmPropriedades = llmPropriedades;
        this.uiLogger = uiLogger;
        this.pastasExecucao = pastasExecucao;
        this.telemetriaTraducao = telemetriaTraducao;
        this.protecaoAss = protecaoAss;
        this.gerenciadorContexto = gerenciadorContexto;
        this.resolvedorSaida = resolvedorSaida;
        this.resolvedorCache = resolvedorCache;
        this.politicaBackup = politicaBackup;
        this.seletorEventos = seletorEventos;
    }

    public Path processar(Path arquivoEntrada) throws InterruptedException, ExecutionException {
        return processar(arquivoEntrada, false).arquivoSaida();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: Traduz um arquivo de legenda. Quando a entrada aparenta
     * já estar em PT-BR, a retradução é BLOQUEADA por padrão — evita traduzir de
     * novo uma legenda já traduzida e sobrescrever trabalho bom.
     *
     * <p>INVARIANTES DO DOMÍNIO: só reprocessa uma entrada que parece traduzida se
     * {@code permitirRetraducao} for explicitamente verdadeiro (confirmação do
     * usuário); com essa liberação, uma saída final existente só é substituída após
     * backup obrigatório, inclusive quando a nova execução ainda ficar parcial.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada aparentemente já traduzida sem
     * confirmação → lança {@link ArquivoLegendaException} (o lote registra o
     * arquivo como falha e segue para o próximo); falha ao criar backup aborta a
     * substituição e preserva o arquivo final anterior.
     */
    public ResultadoTraducaoArquivo processar(Path arquivoEntrada, boolean permitirRetraducao) throws InterruptedException, ExecutionException {
        long inicioMs = System.currentTimeMillis();
        boolean ehSrt = ehSrt(arquivoEntrada);
        log.info("Lendo arquivo de legenda: {}", arquivoEntrada);
        DocumentoLegenda documento = ehSrt ? leitorSrt.ler(arquivoEntrada) : leitor.ler(arquivoEntrada);

        Path arquivoCache = resolvedorCache.resolverArquivoCache(arquivoEntrada);
        ProvenienciaCache proveniencia = resolvedorCache.provenienciaAtual();
        if (permitirRetraducao && Files.exists(arquivoCache)) {
            politicaBackup.arquivarCacheAntesDaRetraducao(arquivoCache);
        }
        // Congela o prompt de sistema no início do arquivo: se o contexto global
        // mudar (troca de lore) enquanto este episódio traduz, o prompt já capturado
        // continua valendo até o fim — a mesma origem carimbada na proveniência.
        String promptCongelado = gerenciadorContexto.obterPromptAtivo();
        CacheTraducaoService.ResultadoCarga carga = cacheService.carregar(arquivoCache, proveniencia);
        Map<String, String> cacheExistente = carga.mapa();

        // Avisos de falas que ficaram sem tradução confiável (tags corrompidas,
        // resíduo detectado na revalidação final). Alimenta o campo
        // errosOcorridos da telemetria para o painel refletir o que exige
        // revisão manual.
        List<String> avisos = new ArrayList<>();
        if (carga.invalidadas() > 0) {
            String aviso = carga.invalidadas()
                + " entrada(s) do cache anterior invalidadas por mudança de lore/modelo (serão retraduzidas com o lore atual).";
            log.warn(aviso);
            uiLogger.log("[ INFO ] " + aviso);
            avisos.add(aviso);
        }
        if (protecaoAss.caminhoPareceTraduzido(arquivoEntrada)) {
            if (!permitirRetraducao) {
                String msg = "Entrada parece já traduzida (PT-BR): " + arquivoEntrada
                    + ". Retradução BLOQUEADA por padrão — confirme explicitamente para reprocessar.";
                log.warn(msg);
                uiLogger.log("[ BLOQUEADO ] " + msg);
                throw new EntradaJaTraduzidaException(msg);
            }
            String aviso = "Entrada parece já traduzida; reprocessando por confirmação explícita: " + arquivoEntrada;
            log.warn(aviso);
            uiLogger.log("[ WARN ] " + aviso);
            avisos.add(aviso);
        }

        Map<String, Long> frequenciaTextoLimpo = seletorEventos.calcularFrequenciaTextoLimpo(documento);
        List<EventoLegenda> eventosTraduziveis = documento.eventos().stream()
            .filter(evento -> seletorEventos.isTraduzivel(evento, frequenciaTextoLimpo))
            .toList();
        log.info("{} fala(s) traduzível(eis) encontrada(s) em {}", eventosTraduziveis.size(), arquivoEntrada.getFileName());

        LinkedHashSet<String> textosTraduziveisDistintos = new LinkedHashSet<>();
        eventosTraduziveis.forEach(evento -> textosTraduziveisDistintos.add(evento.texto()));

        Map<String, String> cacheReaproveitavel = new HashMap<>();
        LinkedHashSet<String> textosPendentes = new LinkedHashSet<>();
        int cacheSuspeito = 0;
        for (String textoOriginal : textosTraduziveisDistintos) {
            String cacheado = cacheExistente.get(textoOriginal);
            if (cacheado != null && isCacheReaproveitavel(textoOriginal, cacheado)) {
                cacheReaproveitavel.put(textoOriginal, cacheado);
            } else {
                if (cacheado != null) {
                    cacheSuspeito++;
                }
                textosPendentes.add(textoOriginal);
            }
        }
        log.info("{} fala(s) distinta(s) reaproveitada(s) do cache, {} suspeita(s), {} pendente(s) de tradução",
            cacheReaproveitavel.size(), cacheSuspeito, textosPendentes.size());
        uiLogger.registrarFalasCache(cacheReaproveitavel.size());

        Map<String, String> traducoesNovas;
        try {
            traducoesNovas = traduzirPendentes(textosPendentes, arquivoEntrada.getFileName().toString(), avisos, promptCongelado);
        } catch (TraducaoParcialException e) {
            Map<String, String> traducoesParciais = e.getDicionarioParcial();
            if (traducoesParciais != null && !traducoesParciais.isEmpty()) {
                log.info("Salvando {} traducoes parciais no cache antes de abortar o episodio", traducoesParciais.size());
                Map<String, String> combinadasParciais = new HashMap<>(cacheReaproveitavel);
                combinadasParciais.putAll(traducoesParciais);
                Map<String, String> parciaisValidadas = new HashMap<>();
                for (Map.Entry<String, String> parcial : combinadasParciais.entrySet()) {
                    String motivo = motivoFalhaFinal(parcial.getKey(), parcial.getValue());
                    parciaisValidadas.put(parcial.getKey(), motivo == null ? parcial.getValue() : "");
                    if (motivo != null) {
                        telemetriaTraducao.registrarFallbackMantido();
                    }
                }

                List<EntradaCache> entradasCacheParcial = new ArrayList<>();
                for (EventoLegenda evento : documento.eventos()) {
                    if (seletorEventos.isTraduzivel(evento, frequenciaTextoLimpo)) {
                        String txtFinal = parciaisValidadas.get(evento.texto());
                        if (txtFinal != null) {
                            entradasCacheParcial.add(new EntradaCache(
                                evento.indice(), evento.estilo(), evento.texto(), txtFinal,
                                propriedades.idiomaOriginal(), propriedades.idiomaTraduzido()));
                        }
                    }
                }
                if (!entradasCacheParcial.isEmpty()) {
                    politicaBackup.salvarCacheDaExecucao(
                        arquivoCache, proveniencia, entradasCacheParcial, permitirRetraducao);
                }
            }
            throw e;
        }

        Map<String, String> traducoesCombinadas = new HashMap<>(cacheReaproveitavel);
        traducoesCombinadas.putAll(traducoesNovas);

        Map<String, String> traducoesValidadas = new HashMap<>();
        LinkedHashSet<String> falhasDistintas = new LinkedHashSet<>();
        for (Map.Entry<String, String> traducao : traducoesCombinadas.entrySet()) {
            String original = traducao.getKey();
            String traduzido = traducao.getValue();
            String motivoFalha = motivoFalhaFinal(original, traduzido);
            if (motivoFalha == null) {
                traducoesValidadas.put(original, traduzido);
                continue;
            }

            // Uma falha conhecida nunca volta ao banco como se fosse tradução.
            // O original continua visível somente no artefato PARCIAL para que a
            // sincronia da legenda seja preservada durante a revisão.
            traducoesValidadas.put(original, "");
            falhasDistintas.add(original);
            telemetriaTraducao.registrarFallbackMantido();
            String aviso = "Fala pendente após tentativas do LLM: " + motivoFalha + ". Original: " + original;
            log.warn(aviso);
            uiLogger.log("[ WARN ] " + aviso);
            avisos.add(aviso);
        }

        List<EventoLegenda> eventosFinais = new ArrayList<>(documento.eventos().size());
        List<EntradaCache> entradasCache = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!seletorEventos.isTraduzivel(evento, frequenciaTextoLimpo)) {
                eventosFinais.add(evento);
                continue;
            }
            if (!traducoesValidadas.containsKey(evento.texto())) {
                throw new ArquivoLegendaException(
                    "Falha interna: nenhuma tradução encontrada para a fala do evento " + evento.indice()
                        + " em " + arquivoEntrada);
            }
            String textoValidado = traducoesValidadas.get(evento.texto());
            String textoFinal = textoValidado == null || textoValidado.isBlank()
                ? evento.texto() : textoValidado;
            eventosFinais.add(evento.comTexto(textoFinal));
            entradasCache.add(new EntradaCache(
                evento.indice(), evento.estilo(), evento.texto(), textoValidado,
                propriedades.idiomaOriginal(), propriedades.idiomaTraduzido()));
        }

        DocumentoLegenda documentoFinal = new DocumentoLegenda(
            documento.cabecalho(), eventosFinais, documento.quebraDeLinha(), documento.comBom());

        Path arquivoSaidaFinal = resolvedorSaida.resolverSaidaFinal(arquivoEntrada, pastasExecucao.diretorioSaida());
        Path arquivoSaida = resolvedorSaida.selecionar(
            arquivoSaidaFinal, !falhasDistintas.isEmpty(), permitirRetraducao);
        Path backupSobrescrita = null;
        if (permitirRetraducao && arquivoSaida.equals(arquivoSaidaFinal) && Files.exists(arquivoSaidaFinal)) {
            backupSobrescrita = politicaBackup.criarBackupAntesSobrescrita(arquivoSaidaFinal);
        }
        if (ehSrt) {
            escritorSrt.escrever(arquivoSaida, documentoFinal);
        } else {
            escritor.escrever(arquivoSaida, documentoFinal);
        }
        politicaBackup.salvarCacheDaExecucao(arquivoCache, proveniencia, entradasCache, permitirRetraducao);

        long tempoTotalMs = System.currentTimeMillis() - inicioMs;
        String animeNome = resolvedorCache.animeAPartirDoArquivo(arquivoEntrada);
        String loreNome = gerenciadorContexto.obterNomeContextoAtivo();
        int traducoesNovasValidas = (int) traducoesNovas.entrySet().stream()
            .filter(e -> {
                String validada = traducoesValidadas.get(e.getKey());
                return validada != null && !validada.isBlank();
            })
            .count();
        uiLogger.registrarFalasNovas(traducoesNovasValidas);
        StatusArquivoTraducao status = avisos.isEmpty() && falhasDistintas.isEmpty()
            ? StatusArquivoTraducao.CONCLUIDO : StatusArquivoTraducao.PARCIAL;
        telemetriaTraducao.registrarTraducao(new TelemetriaTraducao(
            arquivoEntrada.getFileName().toString(),
            llmPropriedades.model(),
            eventosTraduziveis.size(),
            traducoesNovasValidas,
            cacheReaproveitavel.size(),
            tempoTotalMs,
            List.copyOf(avisos),
            animeNome,
            resolvedorCache.temporadaAPartirDoNome(animeNome),
            java.time.Instant.now().toString(),
            loreNome,
            status.name()
        ));

        if (status == StatusArquivoTraducao.PARCIAL) {
            if (permitirRetraducao) {
                log.warn("Tradução parcial publicada em {} por liberação explícita: {} fala(s) distinta(s) continuam pendentes; backup anterior em {}.",
                    arquivoSaida, falhasDistintas.size(), backupSobrescrita);
            } else {
                log.warn("Tradução parcial salva em {}: {} fala(s) distinta(s) continuam pendentes; saída final {} não foi sobrescrita.",
                    arquivoSaida, falhasDistintas.size(), arquivoSaidaFinal);
            }
        } else {
            log.info("Arquivo traduzido salvo em {} (cache em {})", arquivoSaida, arquivoCache);
        }
        return new ResultadoTraducaoArquivo(
            arquivoSaida, arquivoEntrada.getFileName().toString(), loreNome,
            eventosTraduziveis.size(), cacheReaproveitavel.size(), traducoesNovasValidas,
            avisos.size(), status);
    }

    private Map<String, String> traduzirPendentes(
            LinkedHashSet<String> textosPendentes, String nomeArquivo, List<String> avisos, String promptCongelado)
            throws InterruptedException, ExecutionException {
        if (textosPendentes.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> tagsPorTexto = new LinkedHashMap<>();
        Map<String, String> textoMascaradoPorOriginal = new LinkedHashMap<>();
        for (String original : textosPendentes) {
            MascaradorTags.Mascarado mascarado = mascarador.mascarar(original);
            tagsPorTexto.put(original, mascarado.tags());
            textoMascaradoPorOriginal.put(original, mascarado.texto());
        }

        List<String> textosPendentesOrdenados = new ArrayList<>(textosPendentes);
        int tamanhoLote = propriedades.tamanhoLote();

        List<List<String>> chunksOriginais = new ArrayList<>();
        List<Lote> lotes = new ArrayList<>();
        for (int i = 0; i < textosPendentesOrdenados.size(); i += tamanhoLote) {
            List<String> chunkOriginais = textosPendentesOrdenados.subList(i, Math.min(i + tamanhoLote, textosPendentesOrdenados.size()));
            List<String> chunkMascarados = chunkOriginais.stream().map(textoMascaradoPorOriginal::get).toList();
            chunksOriginais.add(chunkOriginais);
            lotes.add(new Lote(lotes.size() + 1, chunkMascarados));
        }

        uiLogger.iniciarLotes(lotes.size(), nomeArquivo);
        List<TraducaoLote> resultados;
        try {
            resultados = processarEpisodioUseCase.processarEpisodio(lotes, promptCongelado);
        } catch (TraducaoParcialException e) {
            Map<String, String> traducoesParciais = new HashMap<>();
            if (e.getLotesSalvos() != null) {
                for (TraducaoLote tl : e.getLotesSalvos()) {
                    int k = tl.idLote() - 1; 
                    List<String> chunkOriginais = chunksOriginais.get(k);
                    List<String> traduzidoMascaradoLinhas = tl.linhasTraduzidas();
                    if (traduzidoMascaradoLinhas != null && chunkOriginais.size() == traduzidoMascaradoLinhas.size()) {
                        for (int j = 0; j < chunkOriginais.size(); j++) {
                            String original = chunkOriginais.get(j);
                            String traduzidoMascarado = traduzidoMascaradoLinhas.get(j);
                            traducoesParciais.put(original, desmascararComFallback(original, traduzidoMascarado, tagsPorTexto.get(original), avisos));
                        }
                    }
                }
            }
            throw new TraducaoParcialException(e.getMessage(), traducoesParciais, e.getCause());
        } finally {
            uiLogger.finalizar();
        }

        Map<String, String> traducoesNovas = new HashMap<>();
        for (int k = 0; k < lotes.size(); k++) {
            List<String> chunkOriginais = chunksOriginais.get(k);
            List<String> traduzidoMascaradoLinhas = resultados.get(k).linhasTraduzidas();
            for (int j = 0; j < chunkOriginais.size(); j++) {
                String original = chunkOriginais.get(j);
                String traduzidoMascarado = traduzidoMascaradoLinhas.get(j);
                traducoesNovas.put(original, desmascararComFallback(original, traduzidoMascarado, tagsPorTexto.get(original), avisos));
            }
        }
        return traducoesNovas;
    }

    /**
     * Restaura as tags numa fala traduzida; se o LLM corrompeu/perdeu marcadores
     * [[TAGn]] (alucinação isolada numa única fala), não derruba o lote/episódio
     * inteiro por causa disso: mantém o texto original (sem tradução) só para essa
     * fala e sinaliza para revisão manual no cache.
     */
    private String desmascararComFallback(String original, String traduzidoMascarado, List<String> tags, List<String> avisos) {
        try {
            String traduzido = mascarador.desmascarar(traduzidoMascarado, tags);
            if (protecaoAss.respostaSuspeita(original, traduzido)) {
                telemetriaTraducao.registrarAlucinacaoPrevenida();
                log.warn("LLM contaminou linha ASS pesada — mantendo original. Original: \"{}\" Traduzido: \"{}\"",
                    original, traduzido);
                uiLogger.log("[ WARN ] Linha ASS pesada contaminada pelo LLM — mantida sem tradução (revise manualmente): " + original);
                avisos.add("Linha ASS pesada mantida sem tradução por resposta suspeita do LLM: " + original);
                return original;
            }
            return traduzido;
        } catch (AlucinacaoDetectadaException e) {
            telemetriaTraducao.registrarAlucinacaoPrevenida();
            log.warn("Tags corrompidas pelo LLM nesta fala — mantendo o texto original sem tradução. Motivo: {}. Original: \"{}\"",
                e.getMessage(), original);
            uiLogger.log("[ WARN ] Tags corrompidas pelo LLM — fala mantida sem tradução (revise manualmente): " + original);
            avisos.add("Fala mantida sem tradução (tags corrompidas pelo LLM): " + original);
            return original;
        }
    }

    private boolean isCacheReaproveitavel(String original, String traduzido) {
        if (traduzido == null || traduzido.isBlank()) {
            return false;
        }
        if (!mascarador.preservaEstruturaOriginal(original, traduzido)) {
            log.warn("Cache ignorado porque as tags divergem do original: {}", traduzido);
            return false;
        }
        if (normalizarParaComparacao(original).equals(normalizarParaComparacao(traduzido))) {
            return detectorIdentica.deveManterIdentico(original);
        }
        try {
            validador.validarFala(traduzido);
            return true;
        } catch (AlucinacaoDetectadaException e) {
            log.warn("Cache ignorado porque parece conter fala ainda nao traduzida: {}", traduzido);
            return false;
        }
    }

    private String normalizarParaComparacao(String texto) {
        return texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se o resultado final pode entrar no banco
     * bilíngue como uma tradução reaproveitável ou deve permanecer pendente.
     *
     * <p>INVARIANTES DO DOMÍNIO: fallback idêntico só é aceito para nome, sigla,
     * número ou termo protegido pela lore; vazio e resíduo gringo nunca são
     * contabilizados como tradução concluída.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve uma justificativa legível; uma
     * tradução válida devolve {@code null}.
     */
    private String motivoFalhaFinal(String original, String traduzido) {
        if (traduzido == null || traduzido.isBlank()) {
            return "resposta vazia";
        }
        if (!mascarador.preservaEstruturaOriginal(original, traduzido)) {
            return "tags ASS/SSA ou quebras de linha divergentes do original";
        }
        if (detectorIdentica.pareceNaoTraduzida(original, traduzido)) {
            return "modelo devolveu o texto original sem tradução";
        }
        try {
            validador.validarFala(traduzido);
            return null;
        } catch (AlucinacaoDetectadaException e) {
            return e.getMessage();
        }
    }

    private static boolean ehSrt(Path arquivo) {
        return arquivo.getFileName().toString().toLowerCase().endsWith(".srt");
    }


}
