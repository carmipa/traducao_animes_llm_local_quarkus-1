package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.CategoriaConteudo;
import org.traducao.projeto.traducao.domain.CausaRaizPendencia;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.ResumoPendencia;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;
import org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaSrt;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaSrt;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final CacheTraducaoService cacheService;
    private final TradutorProperties propriedades;
    private final ConsoleUILogger uiLogger;
    private final PastasExecucao pastasExecucao;
    private final TelemetriaTraducaoPort telemetriaTraducao;
    private final ProtecaoLegendaAssService protecaoAss;
    private final GerenciadorContexto gerenciadorContexto;
    private final ResolvedorSaidaLegenda resolvedorSaida;
    private final ResolvedorCacheTraducao resolvedorCache;
    private final PoliticaBackupTraducao politicaBackup;
    private final SeletorEventosTraduziveis seletorEventos;
    private final AvaliadorTraducaoCache avaliadorCache;
    private final TradutorLotesService tradutorLotes;
    private final MontadorTelemetriaTraducao montadorTelemetria;
    private final ClassificadorPendenciaTelemetria classificadorPendencia;
    private final RecuperarPendenciaGoogleService recuperarPendenciaGoogle;

    // Prefixo EXATO do aviso emitido por TradutorLotesService.desmascararComFallback
    // quando o LLM corrompe os marcadores [[TAGn]]. Usado só para o KPI: identifica
    // que a causa-raiz da pendência é MARCADORES_CORROMPIDOS (e não o eco que ela
    // gera em seguida), sem alterar o caminho quente de tradução.
    private static final String PREFIXO_TAGS_CORROMPIDAS =
        "Fala mantida sem tradução (tags corrompidas pelo LLM): ";

    public ProcessarArquivoUseCase(
        LeitorLegendaAss leitor,
        EscritorLegendaAss escritor,
        LeitorLegendaSrt leitorSrt,
        EscritorLegendaSrt escritorSrt,
        CacheTraducaoService cacheService,
        TradutorProperties propriedades,
        ConsoleUILogger uiLogger,
        PastasExecucao pastasExecucao,
        TelemetriaTraducaoPort telemetriaTraducao,
        ProtecaoLegendaAssService protecaoAss,
        GerenciadorContexto gerenciadorContexto,
        ResolvedorSaidaLegenda resolvedorSaida,
        ResolvedorCacheTraducao resolvedorCache,
        PoliticaBackupTraducao politicaBackup,
        SeletorEventosTraduziveis seletorEventos,
        AvaliadorTraducaoCache avaliadorCache,
        TradutorLotesService tradutorLotes,
        MontadorTelemetriaTraducao montadorTelemetria,
        ClassificadorPendenciaTelemetria classificadorPendencia,
        RecuperarPendenciaGoogleService recuperarPendenciaGoogle
    ) {
        this.leitor = leitor;
        this.escritor = escritor;
        this.leitorSrt = leitorSrt;
        this.escritorSrt = escritorSrt;
        this.cacheService = cacheService;
        this.propriedades = propriedades;
        this.uiLogger = uiLogger;
        this.pastasExecucao = pastasExecucao;
        this.telemetriaTraducao = telemetriaTraducao;
        this.protecaoAss = protecaoAss;
        this.gerenciadorContexto = gerenciadorContexto;
        this.resolvedorSaida = resolvedorSaida;
        this.resolvedorCache = resolvedorCache;
        this.politicaBackup = politicaBackup;
        this.seletorEventos = seletorEventos;
        this.avaliadorCache = avaliadorCache;
        this.tradutorLotes = tradutorLotes;
        this.montadorTelemetria = montadorTelemetria;
        this.classificadorPendencia = classificadorPendencia;
        this.recuperarPendenciaGoogle = recuperarPendenciaGoogle;
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
            if (cacheado != null && avaliadorCache.isCacheReaproveitavel(textoOriginal, cacheado)) {
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

        // Estilo por texto (reusado no dedup e no KPI de pendência).
        Map<String, String> estiloPorTexto = new HashMap<>();
        for (EventoLegenda evento : documento.eventos()) {
            if (evento.temTexto()) {
                estiloPorTexto.putIfAbsent(evento.texto(), evento.estilo());
            }
        }
        // Dedup de camadas musicais (Incremento 2): o mesmo verso de OP/ED aparece
        // em muitas camadas KFX/clip com tags diferentes mas o MESMO texto mascarado.
        // Marca as falas NÃO-diálogo para o LLM traduzir cada mascarado uma vez e a
        // tradução ser reaplicada a cada camada com suas próprias tags. Diálogo fica
        // de fora (comportamento intacto). Romaji já nem chega aqui (é preservado).
        Set<String> textosDeduplicaveis = new HashSet<>();
        for (String pendente : textosPendentes) {
            if (classificadorPendencia.categoria(estiloPorTexto.get(pendente), pendente) != CategoriaConteudo.DIALOGO) {
                textosDeduplicaveis.add(pendente);
            }
        }

        Map<String, String> traducoesNovas;
        try {
            traducoesNovas = tradutorLotes.traduzirPendentes(textosPendentes, textosDeduplicaveis, arquivoEntrada.getFileName().toString(), avisos, promptCongelado);
        } catch (TraducaoParcialException e) {
            Map<String, String> traducoesParciais = e.getDicionarioParcial();
            if (traducoesParciais != null && !traducoesParciais.isEmpty()) {
                log.info("Salvando {} traducoes parciais no cache antes de abortar o episodio", traducoesParciais.size());
                Map<String, String> combinadasParciais = new HashMap<>(cacheReaproveitavel);
                combinadasParciais.putAll(traducoesParciais);
                Map<String, String> parciaisValidadas = new HashMap<>();
                for (Map.Entry<String, String> parcial : combinadasParciais.entrySet()) {
                    String motivo = avaliadorCache.motivoFalhaFinal(parcial.getKey(), parcial.getValue());
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

        // KPI de pendência (schemaVersion 1.1): usa o estiloPorTexto já construído
        // acima para o balde de conteúdo, e o conjunto de falas cuja causa-raiz é
        // marcador corrompido (extraído dos avisos já emitidos pela via de
        // desmascaramento). Precedência: MARCADORES_CORROMPIDOS vence o ECO.
        Set<String> falasComTagCorrompida = new HashSet<>();
        for (String aviso : avisos) {
            if (aviso.startsWith(PREFIXO_TAGS_CORROMPIDAS)) {
                falasComTagCorrompida.add(aviso.substring(PREFIXO_TAGS_CORROMPIDAS.length()));
            }
        }
        // 1ª passada: separa o que passou na validação canônica do que ficou pendente.
        // Ainda NÃO emite aviso/KPI: o fallback online (opt-in) pode recuperar algumas
        // pendências antes de a lista final de pendentes ser consolidada.
        Map<String, String> traducoesValidadas = new HashMap<>();
        LinkedHashSet<String> falhasDistintas = new LinkedHashSet<>();
        Map<String, String> motivoPorFalha = new HashMap<>();
        for (Map.Entry<String, String> traducao : traducoesCombinadas.entrySet()) {
            String original = traducao.getKey();
            String traduzido = traducao.getValue();
            String motivoFalha = avaliadorCache.motivoFalhaFinal(original, traduzido);
            if (motivoFalha == null) {
                traducoesValidadas.put(original, traduzido);
                continue;
            }
            // Uma falha conhecida nunca volta ao banco como se fosse tradução.
            traducoesValidadas.put(original, "");
            falhasDistintas.add(original);
            motivoPorFalha.put(original, motivoFalha);
        }

        // Fallback online de último recurso (opt-in, desligado por padrão): só as falas
        // de DIÁLOGO pendentes desta execução vão ao provedor externo; a resposta só é
        // aceita se passar pela MESMA validação canônica do LLM. Rede falha/recusa =
        // fala continua pendente. Quando desligado, recuperar() devolve mapa vazio.
        if (!falhasDistintas.isEmpty()) {
            LinkedHashSet<String> dialogosPendentes = new LinkedHashSet<>();
            for (String pendente : falhasDistintas) {
                if (classificadorPendencia.categoria(estiloPorTexto.get(pendente), pendente) == CategoriaConteudo.DIALOGO) {
                    dialogosPendentes.add(pendente);
                }
            }
            Map<String, String> candidatas = recuperarPendenciaGoogle.recuperar(dialogosPendentes);
            for (Map.Entry<String, String> candidata : candidatas.entrySet()) {
                String original = candidata.getKey();
                String google = candidata.getValue();
                if (avaliadorCache.motivoFalhaFinal(original, google) != null) {
                    continue; // não passou na validação canônica: mantém pendente
                }
                traducoesValidadas.put(original, google);
                falhasDistintas.remove(original);
                motivoPorFalha.remove(original);
                telemetriaTraducao.registrarFalhaTraducaoRecuperada();
                String recuperada = "Fala recuperada via fallback Google (último recurso): " + original;
                log.info(recuperada);
                uiLogger.log("[ OK ] " + recuperada);
            }
        }

        // 2ª passada: consolida avisos e KPI só das falas que SOBRARAM pendentes.
        List<ResumoPendencia> pendenciasUnitarias = new ArrayList<>();
        for (String original : falhasDistintas) {
            telemetriaTraducao.registrarFallbackMantido();
            String motivoFalha = motivoPorFalha.get(original);
            String aviso = "Fala pendente após tentativas do LLM: " + motivoFalha + ". Original: " + original;
            log.warn(aviso);
            uiLogger.log("[ WARN ] " + aviso);
            avisos.add(aviso);

            CausaRaizPendencia causa = falasComTagCorrompida.contains(original)
                ? CausaRaizPendencia.MARCADORES_CORROMPIDOS
                : classificadorPendencia.causaDeMotivoFinal(motivoFalha);
            CategoriaConteudo categoria = classificadorPendencia.categoria(estiloPorTexto.get(original), original);
            pendenciasUnitarias.add(new ResumoPendencia(categoria.name(), causa.name(), 1));
        }
        List<ResumoPendencia> pendenciasPorCausa = classificadorPendencia.consolidar(pendenciasUnitarias);

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
        telemetriaTraducao.registrarTraducao(montadorTelemetria.montar(
            arquivoEntrada, eventosTraduziveis.size(), traducoesNovasValidas,
            cacheReaproveitavel.size(), tempoTotalMs, avisos, animeNome, loreNome, status,
            pendenciasPorCausa));

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



    private static boolean ehSrt(Path arquivo) {
        return arquivo.getFileName().toString().toLowerCase().endsWith(".srt");
    }


}
