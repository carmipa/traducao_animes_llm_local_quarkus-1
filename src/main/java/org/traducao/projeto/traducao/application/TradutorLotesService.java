package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * PROPÓSITO DE NEGÓCIO: traduz as falas pendentes de um episódio — mascara as tags,
 * fatia em lotes, chama o LLM e restaura as tags na resposta —, isolando o coração do
 * fluxo de tradução da orquestração de {@link ProcessarArquivoUseCase} (FASE F, R6).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>As tags de formatação são mascaradas antes do LLM e restauradas depois; o
 *       modelo nunca vê a sintaxe de estilo.</li>
 *   <li>Os lotes respeitam {@code tradutorProperties.tamanhoLote()} e preservam a ordem
 *       das falas para o mapeamento original↔traduzido.</li>
 *   <li>Uma alucinação isolada numa fala (tags corrompidas ou linha ASS pesada
 *       contaminada) NÃO derruba o lote/episódio: aquela fala mantém o texto original e é
 *       sinalizada para revisão, sem interromper as demais.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Um cancelamento cooperativo no meio do episódio propaga {@link TraducaoParcialException}
 * com as traduções já concluídas (desmascaradas), para que o chamador persista o progresso
 * parcial; a barra de progresso é sempre finalizada.
 */
@Service
public class TradutorLotesService {

    private static final Logger log = LoggerFactory.getLogger(TradutorLotesService.class);

    private final MascaradorTags mascarador;
    private final TradutorProperties propriedades;
    private final ConsoleUILogger uiLogger;
    private final ProcessarEpisodioUseCase processarEpisodioUseCase;
    private final ProtecaoLegendaAssService protecaoAss;
    private final TelemetriaTraducaoPort telemetriaTraducao;
    private final IsoladorQuebraDialogo isoladorQuebra;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as peças do coração do fluxo — mascaramento, tamanho de lote,
     * progresso da UI, execução do episódio, proteção ASS e telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param mascarador mascara/desmascara as tags ao redor do texto enviado ao LLM
     * @param propriedades fornece o tamanho de lote da fatia
     * @param uiLogger barra de progresso e mensagens do episódio
     * @param processarEpisodioUseCase executa a tradução dos lotes (sequencial, GPU única)
     * @param protecaoAss detecta resposta suspeita em linha ASS pesada
     * @param telemetriaTraducao contabiliza alucinações prevenidas
     * @param isoladorQuebra isola o {@code \N} mid-sentence do diálogo antes do LLM e o reaplica depois
     */
    public TradutorLotesService(
        MascaradorTags mascarador,
        TradutorProperties propriedades,
        ConsoleUILogger uiLogger,
        ProcessarEpisodioUseCase processarEpisodioUseCase,
        ProtecaoLegendaAssService protecaoAss,
        TelemetriaTraducaoPort telemetriaTraducao,
        IsoladorQuebraDialogo isoladorQuebra
    ) {
        this.mascarador = mascarador;
        this.propriedades = propriedades;
        this.uiLogger = uiLogger;
        this.processarEpisodioUseCase = processarEpisodioUseCase;
        this.protecaoAss = protecaoAss;
        this.telemetriaTraducao = telemetriaTraducao;
        this.isoladorQuebra = isoladorQuebra;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz o conjunto de falas ainda não resolvidas pelo cache,
     * devolvendo o mapa original↔traduzido pronto para validação e reconstrução.
     *
     * <p>INVARIANTES DO DOMÍNIO: mascara/desmascara as tags; fatia em lotes de
     * {@code tamanhoLote}; preserva a ordem para casar cada tradução com seu original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto vazio devolve {@code Map.of()}; um
     * cancelamento no meio propaga {@link TraducaoParcialException} com o progresso parcial
     * já desmascarado; a barra de progresso é sempre finalizada.
     */
    public Map<String, String> traduzirPendentes(
            LinkedHashSet<String> textosPendentes, Set<String> textosDeduplicaveis,
            String nomeArquivo, List<String> avisos, String promptCongelado)
            throws InterruptedException, ExecutionException {
        if (textosPendentes.isEmpty()) {
            return Map.of();
        }

        Map<String, List<String>> tagsPorTexto = new LinkedHashMap<>();
        Map<String, String> textoMascaradoPorOriginal = new LinkedHashMap<>();
        Map<String, Integer> quebrasPorOriginal = new LinkedHashMap<>();
        for (String original : textosPendentes) {
            // Diálogo (fora do subconjunto deduplicável): isola o \N mid-sentence ANTES de
            // mascarar, para o LLM traduzir a frase inteira sem marcador no meio; a quebra é
            // reaplicada na tradução em desmascararComFallback. Música/karaokê/KFX (dedupláveis)
            // seguem com o \N mascarado como [[TAGn]] pelo caminho antigo, intactos.
            String textoParaMascarar = original;
            if (!textosDeduplicaveis.contains(original)) {
                IsoladorQuebraDialogo.FalaIsolada isolada = isoladorQuebra.isolar(original);
                if (isolada.quebras() > 0) {
                    textoParaMascarar = isolada.textoSemQuebra();
                    quebrasPorOriginal.put(original, isolada.quebras());
                }
            }
            MascaradorTags.Mascarado mascarado = mascarador.mascarar(textoParaMascarar);
            tagsPorTexto.put(original, mascarado.tags());
            textoMascaradoPorOriginal.put(original, mascarado.texto());
        }

        // Dedup por texto MASCARADO no subconjunto deduplicável (camadas musicais):
        // cada texto mascarado distinto é traduzido UMA vez (o 1º original é o
        // "representante"); as demais camadas reaproveitam a tradução mascarada,
        // desmascarando com as PRÓPRIAS tags. Diálogo (fora do subconjunto) nunca
        // deduplica — comportamento antigo intacto. Como a chave é o mascarado (mesmo
        // texto E mesma estrutura de tags), nenhuma tradução muda.
        Map<String, String> representantePorMascarado = new LinkedHashMap<>();
        List<String> representantes = new ArrayList<>();
        Map<String, String> representanteDeOriginal = new LinkedHashMap<>();
        for (String original : textosPendentes) {
            String rep = original;
            if (textosDeduplicaveis.contains(original)) {
                String masc = textoMascaradoPorOriginal.get(original);
                String existente = representantePorMascarado.putIfAbsent(masc, original);
                rep = existente != null ? existente : original;
            }
            representanteDeOriginal.put(original, rep);
            if (rep.equals(original)) {
                representantes.add(original);
            }
        }

        int tamanhoLote = propriedades.tamanhoLote();
        List<List<String>> chunksRepresentantes = new ArrayList<>();
        List<Lote> lotes = new ArrayList<>();
        for (int i = 0; i < representantes.size(); i += tamanhoLote) {
            List<String> chunkReps = representantes.subList(i, Math.min(i + tamanhoLote, representantes.size()));
            List<String> chunkMascarados = chunkReps.stream().map(textoMascaradoPorOriginal::get).toList();
            chunksRepresentantes.add(chunkReps);
            lotes.add(new Lote(lotes.size() + 1, chunkMascarados));
        }

        uiLogger.iniciarLotes(lotes.size(), nomeArquivo);
        List<TraducaoLote> resultados;
        try {
            resultados = processarEpisodioUseCase.processarEpisodio(lotes, promptCongelado);
        } catch (TraducaoParcialException e) {
            Map<String, String> mascaradoPorRepresentante = new HashMap<>();
            if (e.getLotesSalvos() != null) {
                coletarMascaradoPorRepresentante(e.getLotesSalvos(), chunksRepresentantes, mascaradoPorRepresentante);
            }
            Map<String, String> traducoesParciais = expandirParaCamadas(
                textosPendentes, representanteDeOriginal, mascaradoPorRepresentante, tagsPorTexto, quebrasPorOriginal, avisos);
            throw new TraducaoParcialException(e.getMessage(), traducoesParciais, e.getCause());
        } finally {
            uiLogger.finalizar();
        }

        Map<String, String> mascaradoPorRepresentante = new HashMap<>();
        coletarMascaradoPorRepresentante(resultados, chunksRepresentantes, mascaradoPorRepresentante);
        return expandirParaCamadas(
            textosPendentes, representanteDeOriginal, mascaradoPorRepresentante, tagsPorTexto, quebrasPorOriginal, avisos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: coleta a tradução mascarada de cada representante a partir
     * dos lotes devolvidos pelo episódio (completos ou salvos numa parcial).
     *
     * <p>INVARIANTES DO DOMÍNIO: casa cada lote ao seu chunk de representantes pelo id;
     * ignora lote fora de faixa ou com contagem de linhas divergente (defensivo).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: um lote inconsistente é apenas ignorado — seus
     * representantes ficam sem tradução e as camadas correspondentes serão puladas.
     */
    private void coletarMascaradoPorRepresentante(
            List<TraducaoLote> lotes, List<List<String>> chunksRepresentantes, Map<String, String> destino) {
        for (TraducaoLote tl : lotes) {
            int k = tl.idLote() - 1;
            if (k < 0 || k >= chunksRepresentantes.size()) {
                continue;
            }
            List<String> chunkReps = chunksRepresentantes.get(k);
            List<String> linhas = tl.linhasTraduzidas();
            if (linhas == null || linhas.size() != chunkReps.size()) {
                continue;
            }
            for (int j = 0; j < chunkReps.size(); j++) {
                destino.put(chunkReps.get(j), linhas.get(j));
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reaplica a tradução mascarada do representante a TODAS as
     * camadas que compartilham aquele texto mascarado, desmascarando com as tags de cada
     * uma — de forma que um verso musical traduzido uma vez chegue a todas as suas camadas.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada camada é desmascarada com as suas próprias tags
     * ({@code tagsPorTexto}); marcador corrompido cai no fallback por camada (mantém o
     * original só naquela). Camada cujo representante não foi traduzido (parcial) é omitida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #desmascararComFallback} absorve alucinação
     * de tags e resposta suspeita mantendo o original; nada escapa para o laço.
     */
    private Map<String, String> expandirParaCamadas(
            LinkedHashSet<String> textosPendentes, Map<String, String> representanteDeOriginal,
            Map<String, String> mascaradoPorRepresentante, Map<String, List<String>> tagsPorTexto,
            Map<String, Integer> quebrasPorOriginal, List<String> avisos) {
        Map<String, String> traducoes = new HashMap<>();
        for (String original : textosPendentes) {
            String rep = representanteDeOriginal.get(original);
            String traduzidoMascarado = mascaradoPorRepresentante.get(rep);
            if (traduzidoMascarado == null) {
                continue;
            }
            traducoes.put(original, desmascararComFallback(
                original, traduzidoMascarado, tagsPorTexto.get(original),
                quebrasPorOriginal.getOrDefault(original, 0), avisos));
        }
        return traducoes;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura as tags numa fala traduzida; se o LLM corrompeu/perdeu
     * marcadores {@code [[TAGn]]} (alucinação isolada), não derruba o lote/episódio: mantém
     * o texto original (sem tradução) só para essa fala e sinaliza para revisão manual.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags corrompidas ou linha ASS pesada contaminada mantêm o
     * original e registram a alucinação prevenida na telemetria; nenhuma exceção escapa
     * para o laço de lotes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: em {@link AlucinacaoDetectadaException} ou resposta
     * suspeita devolve o texto original (que já contém suas quebras) e acrescenta um aviso;
     * caso contrário devolve a fala traduzida com o {@code \N} isolado reaplicado.
     */
    private String desmascararComFallback(String original, String traduzidoMascarado, List<String> tags,
            int quebrasIsoladas, List<String> avisos) {
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
            return isoladorQuebra.reaplicar(traduzido, quebrasIsoladas);
        } catch (AlucinacaoDetectadaException e) {
            telemetriaTraducao.registrarAlucinacaoPrevenida();
            log.warn("Tags corrompidas pelo LLM nesta fala — mantendo o texto original sem tradução. Motivo: {}. Original: \"{}\"",
                e.getMessage(), original);
            uiLogger.log("[ WARN ] Tags corrompidas pelo LLM — fala mantida sem tradução (revise manualmente): " + original);
            avisos.add("Fala mantida sem tradução (tags corrompidas pelo LLM): " + original);
            return original;
        }
    }
}
