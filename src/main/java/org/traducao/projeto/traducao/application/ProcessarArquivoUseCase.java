package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.fallback.ResultadoRecuperacao;
import org.traducao.projeto.traducao.domain.CategoriaConteudo;
import org.traducao.projeto.traducao.domain.CausaRaizPendencia;
import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.ResumoPendencia;
import org.traducao.projeto.traducao.domain.FalaNaoTraduzida;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService.ProtecaoCamadas;
import org.traducao.projeto.legenda.domain.ArquivoLegendaException;
import org.traducao.projeto.legenda.domain.CarimboCabecalhoLegenda;
import org.traducao.projeto.traducao.domain.exceptions.EntradaJaTraduzidaException;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaSrt;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaSrt;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;
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
    private final ResolvedorSaidaLegenda resolvedorSaida;
    private final ResolvedorCacheTraducao resolvedorCache;
    private final PoliticaBackupTraducao politicaBackup;
    private final SeletorEventosTraduziveis seletorEventos;
    private final AvaliadorTraducaoCache avaliadorCache;
    private final TradutorLotesService tradutorLotes;
    private final MontadorTelemetriaTraducao montadorTelemetria;
    private final ClassificadorPendenciaTelemetria classificadorPendencia;
    private final RecuperarPendenciaFallbackService recuperarPendenciaGoogle;
    private final EnforcadorTermosLore enforcadorTermosLore;
    private final EnforcadorGlossarioFala enforcadorGlossarioFala;
    private final DetectorIdiomaFonteService detectorIdiomaFonte;
    private final NormalizadorAspasService normalizadorAspas;
    private final NormalizadorAcentosComuns normalizadorAcentos;
    private final org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda corretorOrtografico;
    private final NormalizadorCartaoDataService normalizadorCartaoData;
    private final GuardaContextoObraTraducao guardaContextoObra;
    private final ContextoCongeladoDaExecucao contextoCongelado;
    private final org.traducao.projeto.qualidadeTraducao.application.nomeProprio
        .DetectorNomeProprioTraduzido detectorNomeProprio;

    // Prefixo EXATO do aviso emitido por TradutorLotesService.desmascararComFallback
    // quando o LLM corrompe os marcadores [[TAGn]]. Usado só para o KPI: identifica
    // que a causa-raiz da pendência é MARCADORES_CORROMPIDOS (e não o eco que ela
    // gera em seguida), sem alterar o caminho quente de tradução.
    private static final String PREFIXO_TAGS_CORROMPIDAS =
        "Fala mantida sem tradução (tags corrompidas pelo LLM): ";

    /**
     * PROPÓSITO DE NEGÓCIO: extrai {@code inicio,fim} do prefixo do evento, que é a chave
     * ESTÁVEL para casar uma fala do dataset com a linha do {@code .ass} publicado.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto muda ao traduzir, o instante não — casar por índice
     * mente quando o arquivo sai reordenado, e foi assim que uma auditoria acusou corrupção
     * inexistente no Guilty Crown. O prefixo tem a forma
     * {@code "Dialogue: 0,0:00:01.00,0:00:02.00,Estilo,,0,0,0,,"}: os campos 1 e 2 são os tempos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: prefixo nulo ou com menos de três campos devolve
     * string vazia — o registro perde a chave de casamento mas não derruba a tradução.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: condensa os nomes perdidos numa linha legível de log.
     *
     * <p>INVARIANTES DO DOMÍNIO: no máximo oito nomes distintos; o excedente vira contagem. Log de
     * episódio com centenas de nomes não é lido por ninguém, e o detalhe completo já vai para o
     * relatório.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa nulo ou vazio devolve string vazia.
     */
    private static String resumirNomesPerdidos(Map<String, List<String>> perdidos) {
        if (perdidos == null || perdidos.isEmpty()) {
            return "";
        }
        java.util.LinkedHashSet<String> distintos = new java.util.LinkedHashSet<>();
        perdidos.values().forEach(distintos::addAll);
        String amostra = distintos.stream().limit(8).collect(java.util.stream.Collectors.joining(", "));
        return distintos.size() > 8 ? amostra + " (+" + (distintos.size() - 8) + ")" : amostra;
    }

    private static String instanteDe(EventoLegenda evento) {
        if (evento == null || evento.prefixo() == null) {
            return "";
        }
        String[] campos = evento.prefixo().split(",");
        return campos.length >= 3 ? campos[1] + "," + campos[2] : "";
    }

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
        ResolvedorSaidaLegenda resolvedorSaida,
        ResolvedorCacheTraducao resolvedorCache,
        PoliticaBackupTraducao politicaBackup,
        SeletorEventosTraduziveis seletorEventos,
        AvaliadorTraducaoCache avaliadorCache,
        TradutorLotesService tradutorLotes,
        MontadorTelemetriaTraducao montadorTelemetria,
        ClassificadorPendenciaTelemetria classificadorPendencia,
        RecuperarPendenciaFallbackService recuperarPendenciaGoogle,
        EnforcadorTermosLore enforcadorTermosLore,
        EnforcadorGlossarioFala enforcadorGlossarioFala,
        DetectorIdiomaFonteService detectorIdiomaFonte,
        NormalizadorAspasService normalizadorAspas,
        NormalizadorAcentosComuns normalizadorAcentos,
        org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda corretorOrtografico,
        NormalizadorCartaoDataService normalizadorCartaoData,
        GuardaContextoObraTraducao guardaContextoObra,
        ContextoCongeladoDaExecucao contextoCongelado,
        org.traducao.projeto.qualidadeTraducao.application.nomeProprio
            .DetectorNomeProprioTraduzido detectorNomeProprio
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
        this.resolvedorSaida = resolvedorSaida;
        this.resolvedorCache = resolvedorCache;
        this.politicaBackup = politicaBackup;
        this.seletorEventos = seletorEventos;
        this.avaliadorCache = avaliadorCache;
        this.tradutorLotes = tradutorLotes;
        this.montadorTelemetria = montadorTelemetria;
        this.classificadorPendencia = classificadorPendencia;
        this.recuperarPendenciaGoogle = recuperarPendenciaGoogle;
        this.enforcadorTermosLore = enforcadorTermosLore;
        this.enforcadorGlossarioFala = enforcadorGlossarioFala;
        this.detectorIdiomaFonte = detectorIdiomaFonte;
        this.normalizadorAspas = normalizadorAspas;
        this.normalizadorAcentos = normalizadorAcentos;
        this.corretorOrtografico = corretorOrtografico;
        this.normalizadorCartaoData = normalizadorCartaoData;
        this.guardaContextoObra = guardaContextoObra;
        this.contextoCongelado = contextoCongelado;
        this.detectorNomeProprio = detectorNomeProprio;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: Traduz um arquivo de legenda sob o contexto que o JOB escolheu.
     * Quando a entrada aparenta já estar em PT-BR, a retradução é BLOQUEADA por padrão —
     * evita traduzir de novo uma legenda já traduzida e sobrescrever trabalho bom.
     *
     * <p><b>Invariantes do domínio</b>
     * <ul>
     *   <li>O contexto chega por PARÂMETRO e é o mesmo objeto para todos os arquivos do lote:
     *       quem resolve é o chamador, UMA vez, a partir do id explicitamente pedido. Este
     *       caso de uso não conhece mais o {@code GerenciadorContexto} — antes ele consultava
     *       o contexto ATIVO GLOBAL a cada arquivo, e uma troca de obra no meio do lote fazia
     *       os arquivos seguintes saírem com outra lore, prompt e proveniência, sem sinal.</li>
     *   <li>A guarda obra×contexto roda POR ARQUIVO (pastas mistas: obras diferentes sob a
     *       mesma entrada), sempre contra ESTE mesmo snapshot — é a revalidação que bloqueia
     *       o arquivo alheio sem reabrir a porta para outra lore entrar no lugar.</li>
     *   <li>Só reprocessa uma entrada que parece traduzida se {@code permitirRetraducao} for
     *       explicitamente verdadeiro (confirmação do usuário); com essa liberação, uma saída
     *       final existente só é substituída após backup obrigatório, inclusive quando a nova
     *       execução ainda ficar parcial.</li>
     * </ul>
     *
     * <p><b>Comportamento em caso de falha</b>
     * Entrada aparentemente já traduzida sem confirmação → lança
     * {@link EntradaJaTraduzidaException} (o lote registra o arquivo como falha e segue para
     * o próximo); obra do caminho divergente do contexto do job → lança
     * {@code ObraDivergenteDoContextoException} antes de qualquer LLM ou escrita; falha ao
     * criar backup aborta a substituição e preserva o arquivo final anterior. Contexto nulo é
     * um erro de programação do chamador e falha rápido, em vez de degradar silenciosamente
     * para o contexto global — degradar aqui reintroduziria o bug que este parâmetro elimina.
     *
     * @param arquivoEntrada legenda a traduzir
     * @param permitirRetraducao liberação explícita para reprocessar entrada já traduzida
     * @param contextoDoJob fotografia imutável do contexto escolhido para o job inteiro
     */
    public ResultadoTraducaoArquivo processar(
        Path arquivoEntrada, boolean permitirRetraducao, SnapshotContexto contextoDoJob
    ) throws InterruptedException, ExecutionException {
        return processar(arquivoEntrada, permitirRetraducao, contextoDoJob, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesma tradução, com a possibilidade de DISPENSAR o portão de obra.
     * Existe só para a tradução sem lore: quando o operador escolhe traduzir cru uma pasta cuja
     * obra JÁ tem lore declarada, a guarda bloquearia — e bloquear é o certo por padrão. A
     * dispensa é a caixa de escape marcada conscientemente na tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: a dispensa não é silenciosa — registra {@code [ AVISO ]} em log
     * e console com a obra e o contexto ativo, para que a decisão apareça no relatório da
     * execução. Nenhuma outra blindagem é afetada: tags, alucinação, numérico e eco continuam.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: idêntico ao da sobrecarga de três argumentos.
     *
     * @param ignorarLoreExistente dispensa o portão de compatibilidade obra × contexto
     */
    public ResultadoTraducaoArquivo processar(
        Path arquivoEntrada, boolean permitirRetraducao, SnapshotContexto contextoDoJob,
        boolean ignorarLoreExistente
    ) throws InterruptedException, ExecutionException {
        long inicioMs = System.currentTimeMillis();
        if (contextoDoJob == null) {
            throw new IllegalArgumentException(
                "Contexto do job obrigatório: a tradução de " + arquivoEntrada
                    + " não pode escolher lore por conta própria.");
        }
        // Portão determinístico ANTES de ler a legenda, de chamar o LLM e de escrever cache:
        // arquivo cuja obra é reconhecida por outro contexto é BLOQUEADO aqui. Roda por
        // arquivo (a pasta de entrada pode misturar obras) contra o MESMO snapshot do job.
        if (ignorarLoreExistente) {
            String aviso = "Portão de obra DISPENSADO por escolha explícita (tradução sem lore): "
                + arquivoEntrada + " — contexto ativo \"" + contextoDoJob.id()
                + "\". A terminologia da obra, se existir, NÃO será aplicada.";
            log.warn(aviso);
            uiLogger.log("[ AVISO ] " + aviso);
        } else {
            guardaContextoObra.verificar(arquivoEntrada, contextoDoJob);
        }
        // Ponte para o consumidor legado que ainda lê a lore por ThreadLocal (ver
        // ContextoCongeladoDaExecucao): a validação de cada fala atravessa a porta
        // LoreAtivaPort, que não recebe o snapshot por parâmetro.
        contextoCongelado.definir(contextoDoJob);
        try {
            return traduzirArquivo(arquivoEntrada, permitirRetraducao, inicioMs, contextoDoJob);
        } finally {
            // Sempre: um snapshot vazado sobreviveria à thread da fila única e contaminaria
            // o próximo job com a lore do arquivo anterior.
            contextoCongelado.limpar();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: executa a tradução do arquivo já autorizada pela guarda e sob um
     * contexto congelado, do cache à publicação da legenda PT-BR.
     *
     * <p>INVARIANTES DO DOMÍNIO: toda leitura de lore desta execução vem do parâmetro
     * {@code contexto} — prompt do LLM, proveniência do cache, mapa de terminologia e nome
     * da lore na telemetria. Nenhuma consulta ao contexto global acontece daqui para baixo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mantém o contrato de erro do fluxo original —
     * entrada aparentemente já traduzida sem confirmação lança
     * {@link EntradaJaTraduzidaException}; cancelamento no meio propaga
     * {@link TraducaoParcialException} com o progresso persistido; falha de I/O vira
     * {@link ArquivoLegendaException}.
     */
    private ResultadoTraducaoArquivo traduzirArquivo(
        Path arquivoEntrada, boolean permitirRetraducao, long inicioMs, SnapshotContexto contexto
    ) throws InterruptedException, ExecutionException {
        boolean ehSrt = ehSrt(arquivoEntrada);
        log.info("Lendo arquivo de legenda: {}", arquivoEntrada);
        DocumentoLegenda documento = ehSrt ? leitorSrt.ler(arquivoEntrada) : leitor.ler(arquivoEntrada);

        Path arquivoCache = resolvedorCache.resolverArquivoCache(arquivoEntrada);
        ProvenienciaCache proveniencia = resolvedorCache.provenienciaDe(contexto);
        boolean cacheAnteriorJaPreservado = false;
        if (permitirRetraducao && Files.exists(arquivoCache)) {
            politicaBackup.arquivarCacheAntesDaRetraducao(arquivoCache);
            cacheAnteriorJaPreservado = true;
        }
        // Prompt do snapshot congelado no início do arquivo: se o contexto global mudar
        // (troca de lore) enquanto este episódio traduz, o prompt já capturado continua
        // valendo até o fim — a mesma origem carimbada na proveniência acima.
        String promptCongelado = contexto.promptSistema();
        // Retradução liberada: a geração anterior é descartada EM MEMÓRIA. Antes, quem
        // produzia o mapa vazio era o arquivo ter sido apagado do disco no passo acima —
        // e era isso que deixava o episódio sem cache nenhum se a execução caísse no meio.
        CacheTraducaoService.ResultadoCarga carga = permitirRetraducao
            ? CacheTraducaoService.ResultadoCarga.vazio()
            : cacheService.carregar(arquivoCache, proveniencia, propriedades.reusoEntreModelos());
        Map<String, String> cacheExistente = carga.mapa();

        // Herdou de outro modelo? Então o carimbo tem de dizer isso. Gravar a proveniência SEM a
        // herança faria o arquivo afirmar que o modelo atual produziu falas que ele apenas
        // reaproveitou — e a proveniência é o que sustenta comparar modelos. O reuso é autorizado;
        // a mentira sobre ele, não.
        if (carga.herdouDeOutroModelo()) {
            proveniencia = proveniencia.herdandoDe(carga.modeloHerdado());
        }

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
        // Pre-passe do peer legenda: quais linhas sao a camada ORIGINAL do karaoke (romaji). E uma
        // propriedade do DOCUMENTO — duas linhas no mesmo tempo —, por isso calculada uma vez aqui,
        // no mesmo molde da frequencia de texto. Com a flag desligada devolve protecao vazia e a
        // decisao do que traduzir fica identica a de antes.
        ProtecaoCamadas protecaoCamadas = propriedades.protecaoRomajiPareamento()
            ? seletorEventos.calcularProtecaoCamadas(documento)
            : ProtecaoCamadas.VAZIA;
        log.info("[ PROTECAO-ROMAJI ] pareamento de camadas {} — {} par(es), {} indeciso(s), {} linha(s) preservada(s)",
            propriedades.protecaoRomajiPareamento() ? "LIGADO" : "DESLIGADO",
            protecaoCamadas.paresEncontrados(), protecaoCamadas.paresIndecisos(),
            protecaoCamadas.indicesPreservados().size());
        List<EventoLegenda> eventosTraduziveis = documento.eventos().stream()
            .filter(evento -> seletorEventos.isTraduzivel(evento, frequenciaTextoLimpo, protecaoCamadas))
            .toList();
        log.info("{} fala(s) traduzível(eis) encontrada(s) em {}", eventosTraduziveis.size(), arquivoEntrada.getFileName());

        LinkedHashSet<String> textosTraduziveisDistintos = new LinkedHashSet<>();
        eventosTraduziveis.forEach(evento -> textosTraduziveisDistintos.add(evento.texto()));

        Map<String, String> cacheReaproveitavel = new HashMap<>();
        // Falas cuja FONTE já está no idioma-alvo (fonte contaminada/meio-traduzida): não vão
        // ao LLM (evita eco/recusa) e são mantidas como estão. Mapa original->original.
        Map<String, String> jaNoIdiomaAlvo = new HashMap<>();
        LinkedHashSet<String> textosPendentes = new LinkedHashSet<>();
        int cacheSuspeito = 0;
        for (String textoOriginal : textosTraduziveisDistintos) {
            String cacheado = cacheExistente.get(textoOriginal);
            if (cacheado != null && avaliadorCache.isCacheReaproveitavel(textoOriginal, cacheado)) {
                cacheReaproveitavel.put(textoOriginal, cacheado);
            } else if (detectorIdiomaFonte.jaNoIdiomaAlvo(textoOriginal, propriedades.idiomaTraduzido())) {
                jaNoIdiomaAlvo.put(textoOriginal, textoOriginal);
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
        if (!jaNoIdiomaAlvo.isEmpty()) {
            // Mensagem INFORMATIVA (não entra em 'avisos', que sinaliza pendência e marcaria o
            // episódio como PARCIAL): fala já no alvo é sucesso, não problema.
            String info = jaNoIdiomaAlvo.size() + " fala(s) já no idioma-alvo ("
                + propriedades.idiomaTraduzido() + ") na fonte — mantida(s) sem retradução (não enviadas ao LLM).";
            log.info(info);
            uiLogger.log("[ INFO ] " + info);
        }

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
        // Inventário de quebra isolável: onde o \N é quebra VISUAL, tirá-lo antes do
        // mascaramento evita o marcador no meio da frase — que é o que o LLM mais erra, porque
        // o português reordena as palavras e o marcador se perde. Diálogo já fazia isso; música
        // latina e letreiro entram agora. Medição que motivou (Break Blade filme 1, 2026-07-23):
        // 6 das 34 falas viraram pendência por marcador corrompido, TODAS verso de música e
        // cartela com tag no meio do texto. KFX e romaji preservado ficam FORA de propósito: ali
        // a quebra acompanha o tempo do efeito, não é decoração visual.
        Set<String> textosDeduplicaveis = new HashSet<>();
        Set<String> textosComQuebraIsolavel = new HashSet<>();
        for (String pendente : textosPendentes) {
            CategoriaConteudo categoria = classificadorPendencia.categoria(estiloPorTexto.get(pendente), pendente);
            if (categoria != CategoriaConteudo.DIALOGO) {
                textosDeduplicaveis.add(pendente);
            }
            if (categoria == CategoriaConteudo.DIALOGO
                || categoria == CategoriaConteudo.MUSICA_LATINA
                || categoria == CategoriaConteudo.LETREIRO) {
                textosComQuebraIsolavel.add(pendente);
            }
        }

        Map<String, String> traducoesNovas;
        try {
            traducoesNovas = tradutorLotes.traduzirPendentes(textosPendentes, textosDeduplicaveis,
                textosComQuebraIsolavel, arquivoEntrada.getFileName().toString(), avisos, promptCongelado);
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
                    if (seletorEventos.isTraduzivel(evento, frequenciaTextoLimpo, protecaoCamadas)) {
                        String txtFinal = parciaisValidadas.get(evento.texto());
                        if (txtFinal != null) {
                            entradasCacheParcial.add(new EntradaCache(
                                evento.indice(), evento.estilo(), evento.texto(), txtFinal,
                                propriedades.idiomaOriginal(), propriedades.idiomaTraduzido()));
                        }
                    }
                }
                if (!entradasCacheParcial.isEmpty()) {
                    // Sem backup duplicado: a retradução já preservou esta mesma geração
                    // ANTES de começar (o arquivo ativo permanece intacto até aqui).
                    politicaBackup.salvarCacheDaExecucao(
                        arquivoCache, proveniencia, entradasCacheParcial,
                        permitirRetraducao && !cacheAnteriorJaPreservado);
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
        // Itera na ordem ESTÁVEL do documento (textosTraduziveisDistintos é LinkedHashSet), não
        // na ordem de um HashMap — assim os avisos, a fila do fallback Google e o KPI de
        // pendência ficam determinísticos entre execuções.
        for (String original : textosTraduziveisDistintos) {
            if (jaNoIdiomaAlvo.containsKey(original)) {
                continue; // fonte já no idioma-alvo: passthrough, fora da validação/pendência.
            }
            String traduzido = traducoesCombinadas.get(original);
            String motivoFalha = avaliadorCache.motivoFalhaFinal(original, traduzido);
            if (motivoFalha == null) {
                traducoesValidadas.put(original, traduzido);
                continue;
            }
            // ANTES de descartar: troca de entidade é a única falha que o sistema sabe CONSERTAR,
            // porque o portão já identificou o termo certo e o errado. A tradução costuma estar
            // impecável no resto — medido no Zeta em 2026-07-29, 22 das 59 falas descartadas eram
            // isto, incluindo a cena da morte da Four Murasame saindo em branco. O reparo passa
            // pelo MESMO portão; se não passar, a fala segue pendente como antes.
            String reparado = avaliadorCache.repararSePossivel(original, traduzido);
            if (reparado != null) {
                traducoesValidadas.put(original, reparado);
                String aviso = "Troca de entidade DESFEITA: \"" + traduzido + "\" -> \"" + reparado
                    + "\". Original: " + original;
                log.info(aviso);
                uiLogger.log("[REPARADA] " + aviso);
                continue;
            }
            // Segundo resgate: a fala é um NOME e o modelo a devolveu certa, só sem o itálico
            // que perdeu junto com o marcador. Reprovar aqui republica a fala EM INGLÊS — medido
            // no 08th em 2026-07-31: 8 das 16 falas com itálico descartado terminaram assim na
            // tela ("{\i1}Eledore!", "{\i1}Karen!", "{\i1}Sanders...").  Restaurar devolve o
            // ORIGINAL intacto: texto certo E itálico preservado.
            String identicaRestaurada = avaliadorCache.restaurarIdenticaSemItalico(original, traduzido);
            if (identicaRestaurada != null) {
                traducoesValidadas.put(original, identicaRestaurada);
                String aviso = "Itálico restaurado em fala-nome: o modelo perdeu a tag mas não mudou"
                    + " o texto. Publicado o original. Original: " + original;
                log.info(aviso);
                uiLogger.log("[RESTAURADA] " + aviso);
                continue;
            }
            // Uma falha conhecida nunca volta ao banco como se fosse tradução.
            traducoesValidadas.put(original, "");
            falhasDistintas.add(original);
            motivoPorFalha.put(original, motivoFalha);
        }
        // Falas já no idioma-alvo entram direto como estão; passam pela mesma consolidação de
        // saída/cache adiante e ainda recebem o reforço determinístico de termos de lore.
        traducoesValidadas.putAll(jaNoIdiomaAlvo);

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
            if (recuperarPendenciaGoogle.ativo() && !dialogosPendentes.isEmpty()) {
                uiLogger.log("[FALLBACK] LLM não resolveu " + dialogosPendentes.size()
                    + " fala(s) de diálogo — enviando ao tradutor de máquina ("
                    + recuperarPendenciaGoogle.provedorAtivo() + ")...");
            }
            ResultadoRecuperacao recuperacao = recuperarPendenciaGoogle.recuperar(dialogosPendentes);
            int reprovadasNaValidacao = 0;
            for (Map.Entry<String, String> candidata : recuperacao.recuperadas().entrySet()) {
                String original = candidata.getKey();
                String traducaoMaquina = candidata.getValue();
                if (avaliadorCache.motivoFalhaFinal(original, traducaoMaquina) != null) {
                    // Passou na guarda, mas a validação canônica (a mesma do LLM) reprovou:
                    // continua pendente. Contabilizado à parte para não inflar "recuperadas".
                    reprovadasNaValidacao++;
                    continue;
                }
                traducoesValidadas.put(original, traducaoMaquina);
                falhasDistintas.remove(original);
                motivoPorFalha.remove(original);
                telemetriaTraducao.registrarFalhaTraducaoRecuperada();
                String recuperada = "Fala recuperada via fallback (último recurso): " + original;
                log.info(recuperada);
                uiLogger.log("[ OK ] " + recuperada);
            }
            // Relatório por CAUSA: sem ele, "recuperou N de M" não diz se o resto caiu por
            // provedor fora do ar, marcador corrompido ou guarda de lore — e não há como
            // decidir se vale trocar de provedor, ajustar a guarda ou subir um container.
            if (!dialogosPendentes.isEmpty() && recuperarPendenciaGoogle.ativo()) {
                String resumo = recuperacao.resumoPorCausa()
                    + (reprovadasNaValidacao > 0 ? ", REPROVADA_VALIDACAO=" + reprovadasNaValidacao : "");
                log.info("Fallback por causa: {}", resumo);
                uiLogger.log("[FALLBACK] Desfecho por causa: " + resumo);
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

        // Reforço determinístico de terminologia da lore ativa (ex.: "Legião" -> "Legion",
        // "Coveiro" -> "Undertaker"): restaura a grafia oficial nas traduções válidas SEM
        // depender do modelo. Só age quando o original contém o termo canônico; mapa vazio
        // (outra obra) é no-op. Não altera pendências (falas em branco são ignoradas).
        Map<String, String> correcoesLore = contexto.correcoesTerminologia();
        if (!correcoesLore.isEmpty()) {
            for (Map.Entry<String, String> traducao : traducoesValidadas.entrySet()) {
                String traduzido = traducao.getValue();
                if (traduzido != null && !traduzido.isBlank()) {
                    traducao.setValue(enforcadorTermosLore.reforcar(traducao.getKey(), traduzido, correcoesLore));
                }
            }
        }

        // Normalização determinística final das traduções válidas (roda SEMPRE, independe de
        // lore; falas em branco/pendentes são ignoradas):
        //  (1) aspas: remove as que ENVOLVEM a fala inteira quando o EN não as tinha (o LLM às
        //      vezes cita um diálogo que a fonte não citava);
        //  (2) acentos: repõe acento nas formas que sem ele nunca são palavra válida (infancia,
        //      nao, ...), sem tocar homógrafos;
        //  (3) glossário de fala completa: fixa a forma canônica das falas que são INTEIRAMENTE
        //      um termo conhecido ("Roger." -> "Entendido."). Roda por ÚLTIMO porque decide a
        //      partir do ORIGINAL e reescreve a fala inteira — é o único ponto capaz de corrigir
        //      tradução alucinada ("Shiro: Roger!") e erro semântico ("Rogério."), que passam
        //      limpos por todas as outras guardas justamente por não serem iguais ao original.
        for (Map.Entry<String, String> traducao : traducoesValidadas.entrySet()) {
            String traduzido = traducao.getValue();
            if (traduzido != null && !traduzido.isBlank()) {
                String normalizado = normalizadorAspas.normalizar(traducao.getKey(), traduzido);
                normalizado = normalizadorAcentos.normalizar(normalizado);
                // DEPOIS do normalizador determinístico, e não antes: o que a lista e a regra de
                // terminação já resolvem não chega a custar consulta ao dicionário. Sobra o que
                // nenhuma regra alcança — fatidico, minimo, aereo, psicicas, medidos no Unicorn em
                // 13/08/2026, quando o dicionário manual resolvia ZERO das 119 faltas do Zeta.
                normalizado = corretorOrtografico.corrigir(normalizado);
                normalizado = normalizadorCartaoData.normalizar(traducao.getKey(), normalizado);
                normalizado = enforcadorGlossarioFala.reforcar(traducao.getKey(), normalizado);
                traducao.setValue(normalizado);
            }
        }

        // DIAGNÓSTICO de nome próprio traduzido. Só mede: não reescreve nenhuma fala, porque a
        // versão anterior desta ideia — "toda palavra capitalizada tem de sobreviver" — foi
        // removida do projeto por gerar 323 falso positivo em 560 pendências (57,7%). O que mudou
        // foi passar a existir dicionário de quatro idiomas para separar Sky, que não é palavra de
        // idioma nenhum, de Never, que é inglês comum capitalizado.
        //
        // Vale sobretudo SEM LORE: ali correcoesTerminologia() é vazio por definição e nada além
        // da instrução em prosa impede o modelo de traduzir nome de personagem. Roda também com
        // lore para os dois números serem comparáveis. Uma consulta ao dicionário por EXECUÇÃO.
        //
        // O número é PISO, não teto: nome próprio que também é palavra comum escapa. Medido em
        // 13/08/2026 no Memories — "Heinz", 17 falas, passa ileso porque en_US e de_DE conhecem a
        // palavra. Pegá-lo exigiria acusar toda capitalizada, que é o falso positivo de 57,7%.
        var nomesProprios = detectorNomeProprio.verificarLote(traducoesValidadas);
        if (!nomesProprios.verificado()) {
            uiLogger.log("[ INFO ] Nomes próprios: NÃO VERIFICADO (dicionário indisponível) — "
                + "ausência de aviso aqui não significa que nenhum nome foi traduzido.");
        } else if (nomesProprios.temPerda()) {
            String aviso = nomesProprios.totalPerdidos() + " nome(s) próprio(s) do original ausente(s) "
                + "da tradução em " + nomesProprios.falasAfetadas() + " fala(s), de "
                + nomesProprios.candidatasExaminadas() + " candidata(s) examinada(s): "
                + resumirNomesPerdidos(nomesProprios.perdidos());
            log.info(aviso);
            uiLogger.log("[ INFO ] " + aviso);
        }

        List<EventoLegenda> eventosFinais = new ArrayList<>(documento.eventos().size());
        List<EntradaCache> entradasCache = new ArrayList<>();
        // Dataset das falas que saem IGUAIS a origem, com o motivo de cada uma. Sem ele, "por
        // que esta fala esta em ingles?" so tem resposta recalculando por fora as regras que o
        // pipeline aplicou por dentro — e foi assim que a auditoria de 07/08/2026 classificou
        // 18.431 falas de letra de musica como residuo de traducao.
        List<FalaNaoTraduzida> naoTraduzidas = new ArrayList<>();
        for (EventoLegenda evento : documento.eventos()) {
            String instante = instanteDe(evento);
            if (!seletorEventos.isTraduzivel(evento, frequenciaTextoLimpo, protecaoCamadas)) {
                eventosFinais.add(evento);
                naoTraduzidas.add(new FalaNaoTraduzida(evento.indice(), instante, evento.estilo(),
                    FalaNaoTraduzida.Motivo.PRESERVADA_POR_REGRA,
                    "seletor: musica, karaoke, romaji protegido ou estilo em estilos-ignorados"));
                continue;
            }
            if (!traducoesValidadas.containsKey(evento.texto())) {
                throw new ArquivoLegendaException(
                    "Falha interna: nenhuma tradução encontrada para a fala do evento " + evento.indice()
                        + " em " + arquivoEntrada);
            }
            String textoValidado = traducoesValidadas.get(evento.texto());
            boolean semTraducaoUtil = textoValidado == null || textoValidado.isBlank();
            String textoFinal = semTraducaoUtil ? evento.texto() : textoValidado;
            if (semTraducaoUtil) {
                naoTraduzidas.add(new FalaNaoTraduzida(evento.indice(), instante, evento.estilo(),
                    FalaNaoTraduzida.Motivo.PENDENTE,
                    "sem traducao aproveitavel — marcador corrompido, eco ou estrutura divergente; "
                        + "a causa-raiz por fala esta na telemetria do episodio"));
            } else if (textoFinal != null && textoFinal.equals(evento.texto())) {
                naoTraduzidas.add(new FalaNaoTraduzida(evento.indice(), instante, evento.estilo(),
                    FalaNaoTraduzida.Motivo.TRADUCAO_IGUAL_AO_ORIGINAL,
                    "o LLM devolveu texto identico — legitimo em nome proprio e fala de uma palavra"));
            }
            eventosFinais.add(evento.comTexto(textoFinal));
            entradasCache.add(new EntradaCache(
                evento.indice(), evento.estilo(), evento.texto(), textoValidado,
                propriedades.idiomaOriginal(), propriedades.idiomaTraduzido()));
        }

        // Carimbo de proveniencia NO PROPRIO ARQUIVO. Sem ele, "por que esta fala ficou em
        // ingles?" e "cade as 138 falas que sumiram?" so tem resposta recalculando por fora as
        // regras que o pipeline ja aplicou por dentro — e recalcular por fora foi a causa das
        // CINCO conclusoes erradas da auditoria de 07/08/2026, entre elas 18.431 falas
        // classificadas como residuo de traducao que eram letra de musica.
        //
        // So ASS: o SRT nao tem cabecalho para comentario, e inventar um quebraria o formato.
        String cabecalhoFinal = documento.cabecalho();
        if (!ehSrt) {
            int naOrigem = documento.eventos().size();
            int traduziveis = entradasCache.size();
            List<String> carimbo = new ArrayList<>();
            carimbo.add("traduziu: " + naOrigem + " fala(s) na origem, " + traduziveis
                + " traduzivel(is), " + (naOrigem - traduziveis)
                + " preservada(s) por regra do pipeline (musica, karaoke, estilo ignorado)");
            carimbo.add("lore: " + contexto.nomeExibicao() + " (" + contexto.id() + ")");
            if (!falhasDistintas.isEmpty()) {
                carimbo.add("pendentes: " + falhasDistintas.size()
                    + " fala(s) mantida(s) no original — arquivo publicado como parcial");
            }
            cabecalhoFinal = CarimboCabecalhoLegenda.aplicar(cabecalhoFinal, carimbo);
        }

        DocumentoLegenda documentoFinal = new DocumentoLegenda(
            cabecalhoFinal, eventosFinais, documento.quebraDeLinha(), documento.comBom());

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
        // Substituição atômica da geração: só AQUI o cache ativo deixa de ser o anterior.
        // preservarAnterior fica falso quando a retradução já copiou esta mesma geração no
        // início — do contrário o mesmo arquivo iria duas vezes para backups/traducao-cache.
        // Depois de a legenda estar em disco: o dataset e ACESSORIO e a porta engole a propria
        // falha, entao perder o registro nunca custa o arquivo que acabou de ser publicado.
        telemetriaTraducao.registrarFalasNaoTraduzidas(
            arquivoSaida, resolvedorCache.animeAPartirDoArquivo(arquivoEntrada), naoTraduzidas);

        politicaBackup.salvarCacheDaExecucao(arquivoCache, proveniencia, entradasCache,
            permitirRetraducao && !cacheAnteriorJaPreservado);

        long tempoTotalMs = System.currentTimeMillis() - inicioMs;
        String animeNome = resolvedorCache.animeAPartirDoArquivo(arquivoEntrada);
        String loreNome = contexto.nomeExibicao();
        int traducoesNovasValidas = (int) traducoesNovas.entrySet().stream()
            .filter(e -> {
                String validada = traducoesValidadas.get(e.getKey());
                return validada != null && !validada.isBlank();
            })
            .count();
        uiLogger.registrarFalasNovas(traducoesNovasValidas);
        // O status reflete PENDÊNCIA FINAL, não aviso. `avisos` também carrega mensagens
        // meramente informativas — cache invalidado por mudança de lore e reprocessamento
        // confirmado pelo operador — que não deixam nenhuma fala por traduzir. Enquanto o
        // status dependia delas, um episódio 100% traduzido saía PARCIAL: no run de Guilty
        // Crown foram SETE episódios marcados assim sem ter uma única pendência, o que torna
        // o indicador inútil justamente para quem precisa achar o que ainda falta.
        // `falhasDistintas` já é a lista consolidada DEPOIS da recuperação pelo fallback.
        StatusArquivoTraducao status = falhasDistintas.isEmpty()
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
        // Mesmos valores que já iam para a telemetria (linha do registrarTraducao acima):
        // o relatório de console passa a receber tempo e causa das pendências pelo retorno,
        // em vez de reler logs/telemetria_traducao.json.
        return new ResultadoTraducaoArquivo(
            arquivoSaida, arquivoEntrada.getFileName().toString(), loreNome,
            eventosTraduziveis.size(), cacheReaproveitavel.size(), traducoesNovasValidas,
            avisos.size(), status, tempoTotalMs, falhasDistintas.size(), pendenciasPorCausa);
    }



    private static boolean ehSrt(Path arquivo) {
        return arquivo.getFileName().toString().toLowerCase().endsWith(".srt");
    }


}

