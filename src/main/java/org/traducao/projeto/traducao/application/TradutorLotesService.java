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

    public TradutorLotesService(
        MascaradorTags mascarador,
        TradutorProperties propriedades,
        ConsoleUILogger uiLogger,
        ProcessarEpisodioUseCase processarEpisodioUseCase,
        ProtecaoLegendaAssService protecaoAss,
        TelemetriaTraducaoPort telemetriaTraducao
    ) {
        this.mascarador = mascarador;
        this.propriedades = propriedades;
        this.uiLogger = uiLogger;
        this.processarEpisodioUseCase = processarEpisodioUseCase;
        this.protecaoAss = protecaoAss;
        this.telemetriaTraducao = telemetriaTraducao;
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
     * PROPÓSITO DE NEGÓCIO: restaura as tags numa fala traduzida; se o LLM corrompeu/perdeu
     * marcadores {@code [[TAGn]]} (alucinação isolada), não derruba o lote/episódio: mantém
     * o texto original (sem tradução) só para essa fala e sinaliza para revisão manual.
     *
     * <p>INVARIANTES DO DOMÍNIO: tags corrompidas ou linha ASS pesada contaminada mantêm o
     * original e registram a alucinação prevenida na telemetria; nenhuma exceção escapa
     * para o laço de lotes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: em {@link AlucinacaoDetectadaException} ou resposta
     * suspeita devolve o texto original e acrescenta um aviso; caso contrário devolve a fala
     * traduzida.
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
}
