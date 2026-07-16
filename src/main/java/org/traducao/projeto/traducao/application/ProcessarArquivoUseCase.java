package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.domain.exceptions.ArquivoLegendaException;
import org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.infrastructure.cache.CacheTraducaoService;
import org.traducao.projeto.traducao.infrastructure.cache.EntradaCache;
import org.traducao.projeto.traducao.infrastructure.cache.ProvenienciaCache;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaSrt;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaSrt;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

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

    // Um letreiro/título animado quadro a quadro reaparece muitas vezes com o
    // mesmo texto visível (só a tag de efeito muda a cada quadro). Abaixo
    // disso é mais provável ser só uma fala com efeito visual pontual (ex.:
    // duas camadas contorno+preenchimento de uma mesma linha de encerramento).
    private static final int LIMIAR_REPETICAO_LETREIRO = 5;

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
    private final PoliticaEstiloMusical politicaEstiloMusical;
    private final LlmProperties llmPropriedades;
    private final ConsoleUILogger uiLogger;
    private final PastasExecucao pastasExecucao;
    private final TelemetriaTraducaoPort telemetriaTraducao;
    private final DetectorEfeitoKaraokeService detectorKaraoke;
    private final ProtecaoLegendaAssService protecaoAss;
    private final GerenciadorContexto gerenciadorContexto;

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
        PoliticaEstiloMusical politicaEstiloMusical,
        LlmProperties llmPropriedades,
        ConsoleUILogger uiLogger,
        PastasExecucao pastasExecucao,
        TelemetriaTraducaoPort telemetriaTraducao,
        DetectorEfeitoKaraokeService detectorKaraoke,
        ProtecaoLegendaAssService protecaoAss,
        GerenciadorContexto gerenciadorContexto
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
        this.politicaEstiloMusical = politicaEstiloMusical;
        this.llmPropriedades = llmPropriedades;
        this.uiLogger = uiLogger;
        this.pastasExecucao = pastasExecucao;
        this.telemetriaTraducao = telemetriaTraducao;
        this.detectorKaraoke = detectorKaraoke;
        this.protecaoAss = protecaoAss;
        this.gerenciadorContexto = gerenciadorContexto;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: Carimbo de origem da tradução em cache — qual lore,
     * hash do prompt de sistema, modelo e idiomas estão em vigor nesta execução.
     * É o que impede o cache de reusar traduções feitas com um lore diferente.
     *
     * <p>INVARIANTES DO DOMÍNIO: o hash reflete o prompt ativo inteiro; qualquer
     * mudança de lore/regra o altera e invalida o cache antigo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; se não houver contexto ativo,
     * {@code contextoId} vem nulo e o {@code hashDe} do prompt padrão ainda é
     * calculado — a comparação de proveniência trata nulos como divergência.
     */
    private ProvenienciaCache provenienciaAtual() {
        return new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL,
            gerenciadorContexto.obterIdContextoAtivo(),
            ProvenienciaCache.hashDe(gerenciadorContexto.obterPromptAtivo()),
            llmPropriedades.model(),
            propriedades.idiomaOriginal(),
            propriedades.idiomaTraduzido()
        );
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

        Path arquivoCache = resolverArquivoCache(arquivoEntrada);
        ProvenienciaCache proveniencia = provenienciaAtual();
        if (permitirRetraducao && Files.exists(arquivoCache)) {
            Path raizBackupCache = Path.of("backups", "traducao-cache").toAbsolutePath().normalize();
            try {
                Path backupCache = arquivarCacheParaRetraducao(arquivoCache, raizBackupCache);
                log.warn("Retradução liberada: cache anterior removido do uso e preservado em {}", backupCache);
                uiLogger.log("[ CACHE REINICIADO ] Geração anterior preservada em: " + backupCache);
            } catch (IOException e) {
                throw new ArquivoLegendaException(
                    "Falha ao preservar e reiniciar o cache antes da retradução: " + arquivoCache, e);
            }
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

        Map<String, Long> frequenciaTextoLimpo = calcularFrequenciaTextoLimpo(documento);
        List<EventoLegenda> eventosTraduziveis = documento.eventos().stream()
            .filter(evento -> isTraduzivel(evento, frequenciaTextoLimpo))
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
                    if (isTraduzivel(evento, frequenciaTextoLimpo)) {
                        String txtFinal = parciaisValidadas.get(evento.texto());
                        if (txtFinal != null) {
                            entradasCacheParcial.add(new EntradaCache(
                                evento.indice(), evento.estilo(), evento.texto(), txtFinal,
                                propriedades.idiomaOriginal(), propriedades.idiomaTraduzido()));
                        }
                    }
                }
                if (!entradasCacheParcial.isEmpty()) {
                    salvarCacheDaExecucao(
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
            if (!isTraduzivel(evento, frequenciaTextoLimpo)) {
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

        Path arquivoSaidaFinal = resolverArquivoSaida(arquivoEntrada);
        Path arquivoSaida = selecionarArquivoSaida(
            arquivoSaidaFinal, !falhasDistintas.isEmpty(), permitirRetraducao);
        Path backupSobrescrita = null;
        if (permitirRetraducao && arquivoSaida.equals(arquivoSaidaFinal) && Files.exists(arquivoSaidaFinal)) {
            backupSobrescrita = criarBackupAntesSobrescrita(arquivoSaidaFinal);
        }
        if (ehSrt) {
            escritorSrt.escrever(arquivoSaida, documentoFinal);
        } else {
            escritor.escrever(arquivoSaida, documentoFinal);
        }
        salvarCacheDaExecucao(arquivoCache, proveniencia, entradasCache, permitirRetraducao);

        long tempoTotalMs = System.currentTimeMillis() - inicioMs;
        String animeNome = animeAPartirDoArquivo(arquivoEntrada);
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
            temporadaAPartirDoNome(animeNome),
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

    static boolean respostaAssPesadaSuspeita(String original, String traduzido) {
        return ProtecaoLegendaAssService.respostaAssPesadaSuspeita(original, traduzido);
    }

    private static String extrairTextoVisivelAss(String texto) {
        return ProtecaoLegendaAssService.extrairTextoVisivelAss(texto);
    }

    static boolean deveBloquearAntesDoLlm(String estilo, String texto, long repeticoesTextoVisivel) {
        return ProtecaoLegendaAssService.deveBloquearAntesDoLlm(estilo, texto, repeticoesTextoVisivel);
    }

    static boolean caminhoPareceLegendaTraduzida(Path arquivoEntrada) {
        return ProtecaoLegendaAssService.caminhoPareceLegendaTraduzida(arquivoEntrada);
    }

    /**
     * Conta, por texto "limpo" (sem tags de override ASS), quantas vezes ele
     * aparece entre as falas de diálogo do documento. Um letreiro/título
     * animado quadro a quadro reaproveita o mesmo texto visível dezenas ou
     * milhares de vezes (só a tag de efeito muda); uma fala normal — mesmo
     * com duas camadas de estilo (contorno+preenchimento) ou repetida em
     * momentos diferentes do episódio — nunca chega perto desse volume.
     */
    private Map<String, Long> calcularFrequenciaTextoLimpo(DocumentoLegenda documento) {
        Map<String, Long> frequencia = new HashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (!evento.isDialogo() || !evento.temTexto()) {
                continue;
            }
            String textoLimpo = extrairTextoVisivelAss(evento.texto());
            if (!textoLimpo.isEmpty()) {
                frequencia.merge(textoLimpo, 1L, Long::sum);
            }
        }
        return frequencia;
    }

    private boolean isTraduzivel(EventoLegenda evento, Map<String, Long> frequenciaTextoLimpo) {
        if (!evento.isDialogo() || !evento.temTexto()) {
            return false;
        }
        String texto = evento.texto();
        if (politicaEstiloMusical.estiloIgnorado(evento.estilo())
            && !detectorKaraoke.eKaraokeOuMusicaTraduzivel(evento.estilo(), texto)) {
            return false;
        }

        // Blindagem contra karaokê cru (\k, \kf, \ko). Só as tags de timing:
        // a detecção agressiva de pós-template (eEfeitoKaraoke) pegaria também
        // letreiros/títulos com \t e texto curto, que aqui DEVEM ser traduzidos
        // — o caso karaokê pós-template é coberto pela heurística de letreiro
        // animado logo abaixo (que exige repetição).
        if (detectorKaraoke.devePreservarKaraokeOriginal(evento.estilo(), texto)) {
            return false;
        }

        // 1. Blindagem Contra Lixo Vetorial Absoluto (modo de desenho \p1, \p2, ... do Aegisub)
        if (protecaoAss.temDesenhoVetorial(texto)) {
            return false;
        }

        String textoLimpo = extrairTextoVisivelAss(texto);
        if (textoLimpo.isEmpty()) {
            return false;
        }
        long repeticoes = frequenciaTextoLimpo.getOrDefault(textoLimpo, 1L);
        if (protecaoAss.deveBloquearLinhaAntesDoLlm(evento.estilo(), texto, repeticoes)) {
            log.debug("Bloqueando evento de typesetting de alto risco antes do LLM. Repetido {}x. Estilo: {} Texto: {}",
                repeticoes, evento.estilo(), textoLimpo);
            return false;
        }

        // 2. Blindagem Contra Typesetting Dinâmico (letreiros/títulos animados quadro a quadro):
        // tag de efeito pesada + pouco texto visível + o mesmo texto repetido muitas vezes no
        // arquivo. A repetição é o sinal decisivo: sem ela, uma fala isolada com efeito visual
        // (ex.: a camada de contorno de uma legenda de encerramento) seria descartada por engano.
        boolean temTagDeAnimacao = texto.contains("\\clip") || texto.contains("\\move")
            || texto.contains("\\pos") || texto.contains("\\fad") || texto.contains("\\t(");
        if (temTagDeAnimacao && texto.length() > 40 && textoLimpo.length() * 3 < texto.length()) {
            if (repeticoes >= LIMIAR_REPETICAO_LETREIRO) {
                log.debug("Bloqueando evento suspeito de letreiro animado (repetido {}x). Estilo: {} Texto: {}",
                    repeticoes, evento.estilo(), textoLimpo);
                return false;
            }
        }

        return mascarador.contemTextoTraduzivel(texto);
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

    /**
     * PROPÓSITO DE NEGÓCIO: promove a nova geração validada do cache da obra
     * selecionada sem perder a versão que sustentava a legenda anterior.
     *
     * <p>INVARIANTES DO DOMÍNIO: a liberação explícita exige backup do cache
     * existente antes da substituição; sem liberação permanece a gravação
     * atômica normal; caches de outros episódios ou obras não são acessados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha no backup ou na gravação lança
     * {@link ArquivoLegendaException}; o destino anterior permanece recuperável
     * e a legenda final não prossegue como se o cache tivesse sido atualizado.
     */
    private void salvarCacheDaExecucao(
            Path arquivoCache,
            ProvenienciaCache proveniencia,
            List<EntradaCache> entradas,
            boolean preservarAnterior) {
        if (preservarAnterior && Files.exists(arquivoCache)) {
            Path raizBackup = Path.of("backups", "traducao-cache").toAbsolutePath().normalize();
            try {
                Path backup = copiarParaBackupExclusivo(arquivoCache, raizBackup);
                log.info("Backup do cache anterior criado em {}", backup);
                uiLogger.log("[ BACKUP CACHE ] Geração anterior preservada em: " + backup);
            } catch (IOException e) {
                throw new ArquivoLegendaException(
                    "Falha ao criar backup obrigatório antes de atualizar o cache: " + arquivoCache, e);
            }
        }
        cacheService.salvar(arquivoCache, proveniencia, entradas);
    }

    private static boolean ehSrt(Path arquivo) {
        return arquivo.getFileName().toString().toLowerCase().endsWith(".srt");
    }

    private static String extensaoLegenda(String nome) {
        String lower = nome.toLowerCase();
        if (lower.endsWith(".srt")) {
            return ".srt";
        }
        return lower.endsWith(".ssa") ? ".ssa" : ".ass";
    }

    private Path resolverArquivoSaida(Path entrada) {
        String nome = entrada.getFileName().toString();
        String extensao = extensaoLegenda(nome);
        String base = nome.substring(0, nome.length() - extensao.length());
        String baseSemSufixoIngles = base.replaceFirst("(?i)_ENG$", "");
        return pastasExecucao.diretorioSaida().resolve(baseSemSufixoIngles + "_PT-BR" + extensao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diferencia visualmente uma legenda ainda incompleta
     * do artefato PT-BR final usado no remux.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva pasta e extensão e acrescenta o sufixo
     * {@code .parcial} antes da extensão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho sem extensão reconhecida ainda
     * recebe o sufixo calculado a partir da convenção ASS/SRT do módulo.
     */
    private static Path resolverArquivoParcial(Path arquivoFinal) {
        String nome = arquivoFinal.getFileName().toString();
        String extensao = extensaoLegenda(nome);
        String base = nome.substring(0, nome.length() - extensao.length());
        return arquivoFinal.resolveSibling(base + ".parcial" + extensao);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se uma retomada publica diretamente a saída
     * PT-BR final ou mantém o resultado incompleto isolado como arquivo parcial.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem pendências, sempre publica a saída final;
     * com pendências, só publica a saída final quando a proteção foi liberada
     * explicitamente, mantendo o comportamento seguro como padrão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não acessa disco nem lança exceção;
     * retorna deterministicamente um caminho final ou com sufixo {@code .parcial}.
     */
    static Path selecionarArquivoSaida(Path arquivoFinal, boolean temPendencias, boolean protecaoLiberada) {
        return !temPendencias || protecaoLiberada
            ? arquivoFinal : resolverArquivoParcial(arquivoFinal);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva a versão PT-BR atualmente publicada antes
     * de uma substituição autorizada, permitindo recuperação após uma revisão ruim.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada substituição recebe uma pasta exclusiva em
     * {@code backups/traducao}; o arquivo original é copiado com seus atributos e
     * nunca é alterado antes de o backup terminar com sucesso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lança {@link ArquivoLegendaException} e
     * impede a escrita da nova saída final, mantendo intacta a versão anterior.
     */
    private Path criarBackupAntesSobrescrita(Path arquivoFinal) {
        Path raizBackup = Path.of("backups", "traducao").toAbsolutePath().normalize();
        try {
            Path backup = copiarParaBackupExclusivo(arquivoFinal, raizBackup);
            log.info("Backup da tradução final criado em {}", backup);
            uiLogger.log("[ BACKUP ] Tradução anterior preservada em: " + backup);
            return backup;
        } catch (IOException e) {
            throw new ArquivoLegendaException(
                "Falha ao criar backup obrigatório antes de sobrescrever: " + arquivoFinal, e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: copia uma tradução publicada para uma pasta exclusiva
     * de histórico antes que uma nova versão ocupe o mesmo caminho final.
     *
     * <p>INVARIANTES DO DOMÍNIO: cria um diretório novo por operação, preserva os
     * atributos do arquivo e nunca substitui um backup anterior.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; o chamador
     * converte a falha em bloqueio da sobrescrita.
     */
    static Path copiarParaBackupExclusivo(Path arquivoFinal, Path raizBackup) throws IOException {
        Files.createDirectories(raizBackup);
        Path pastaBackup = Files.createTempDirectory(raizBackup, "sobrescrita_");
        Path backup = pastaBackup.resolve(arquivoFinal.getFileName()).normalize();
        return Files.copy(arquivoFinal, backup, StandardCopyOption.COPY_ATTRIBUTES);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: inicia uma retradução integral do episódio sem
     * depender de heurísticas para decidir se o cache antigo ainda é confiável.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente o arquivo de cache resolvido para o
     * episódio atual é retirado de uso; uma cópia fiel deve existir no diretório
     * de backup antes da remoção; caches de outras obras nunca são tocados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; se a cópia
     * ou a remoção falhar, o processamento é abortado e não começa uma geração
     * nova fingindo que o cache anterior foi reiniciado.
     */
    static Path arquivarCacheParaRetraducao(Path arquivoCache, Path raizBackup) throws IOException {
        Path backup = copiarParaBackupExclusivo(arquivoCache, raizBackup);
        Files.delete(arquivoCache);
        return backup;
    }

    private Path resolverArquivoCache(Path entrada) {
        String nome = entrada.getFileName().toString();
        String extensao = extensaoLegenda(nome);
        String base = nome.substring(0, nome.length() - extensao.length());
        String animeNome = animeAPartirDoArquivo(entrada);
        return pastasExecucao.diretorioCache().resolve(animeNome).resolve(base + ".cache.json");
    }

    /**
     * Deriva o nome do anime a partir da pasta-avó do arquivo de legenda
     * (arquivo dentro de "&lt;AnimeFolder&gt;/legendas_originais/arquivo.ass"),
     * mesma convenção de duas pastas acima já usada por
     * {@code TradutorProperties.resolverDiretorioCache()} para nomear o cache.
     */
    private String animeAPartirDoArquivo(Path arquivoEntrada) {
        Path pastaEntrada = arquivoEntrada.getParent();
        Path pastaAnime = pastaEntrada != null ? pastaEntrada.getParent() : null;
        if (pastaAnime != null && pastaAnime.getFileName() != null) {
            return pastaAnime.getFileName().toString();
        }
        if (pastaEntrada != null && pastaEntrada.getFileName() != null) {
            return pastaEntrada.getFileName().toString();
        }
        return "Desconhecido";
    }

    private static final Pattern PADRAO_TEMPORADA = Pattern.compile("(?i)(?:season|temporada|\\bs)\\s*0*(\\d{1,2})\\b");

    /**
     * Extrai um marcador de temporada (ex.: "Season 04", "S04") do nome da
     * pasta do anime, quando presente. Sem marcador, agrupa como temporada única.
     */
    private String temporadaAPartirDoNome(String animeNome) {
        if (animeNome == null) {
            return "Temporada Única";
        }
        var matcher = PADRAO_TEMPORADA.matcher(animeNome);
        if (matcher.find()) {
            return "Temporada " + Integer.parseInt(matcher.group(1));
        }
        return "Temporada Única";
    }
}
