package org.traducao.projeto.traducao.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.domain.exceptions.DivergenciaLinhasException;
import org.traducao.projeto.traducao.domain.exceptions.TradutorException;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.domain.exceptions.TraducaoParcialException;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


@Service
public class ProcessarEpisodioUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarEpisodioUseCase.class);
    private static final String MDC_LOTE_ID = "loteId";

    // Quantas tentativas extras (alem da primeira) sao feitas numa fala isolada
    // (lote de tamanho 1) antes de desistir e manter o texto original sem traducao.
    private static final int MAX_TENTATIVAS_LINHA_UNICA = 2;

    // Temperatura por tentativa numa fala isolada: null = a configurada.
    // Repetir a mesma requisicao com a mesma temperatura tende a reproduzir a
    // mesma alucinacao; subir a temperatura muda a amostragem e da chance real
    // de recuperacao antes de desistir da fala.
    private static final Double[] TEMPERATURA_POR_TENTATIVA = {null, 0.5, 0.7};

    private final MistralPort mistralPort;
    private final ValidadorTraducaoService validador;
    private final ConsoleUILogger uiLogger;
    private final TelemetriaTraducaoPort telemetriaTraducao;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe tradução, validação, acompanhamento visual e
     * telemetria sem confundir resposta rejeitada com tradução recuperada.
     *
     * <p>INVARIANTES DO DOMÍNIO: toda saída do modelo passa pelo validador antes
     * de ser devolvida ao processamento do arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a criação do
     * caso de uso pelo contêiner.
     */
    public ProcessarEpisodioUseCase(
        MistralPort mistralPort,
        ValidadorTraducaoService validador,
        ConsoleUILogger uiLogger,
        TelemetriaTraducaoPort telemetriaTraducao
    ) {
        this.mistralPort = mistralPort;
        this.validador = validador;
        this.uiLogger = uiLogger;
        this.telemetriaTraducao = telemetriaTraducao;
    }

    public List<TraducaoLote> processarEpisodio(List<Lote> lotes) throws InterruptedException, ExecutionException {
        return processarEpisodio(lotes, null);
    }

    /**
     * @param promptSistemaCongelado prompt de sistema capturado no início do job;
     *        garante que uma troca de contexto (lore) no estado global não vaze
     *        para o meio do episódio. {@code null} usa o prompt do contexto ativo.
     */
    public List<TraducaoLote> processarEpisodio(List<Lote> lotes, String promptSistemaCongelado)
            throws InterruptedException, ExecutionException {
        if (lotes.isEmpty()) {
            return List.of();
        }

        log.info("Iniciando processamento de {} lote(s) de forma sequencial (preservando LM Studio/GPU)", lotes.size());

        java.util.List<TraducaoLote> resultado = new java.util.ArrayList<>();
        for (Lote lote : lotes) {
            // Parada cooperativa (botão "Parar" da UI interrompe a thread da
            // fila): sai pelo mesmo caminho de tradução parcial, que salva no
            // cache tudo que já foi traduzido antes de encerrar.
            if (Thread.currentThread().isInterrupted()) {
                uiLogger.log("[ STOP ] Tradução interrompida pelo usuário — salvando progresso parcial.");
                throw new TraducaoParcialException(
                    "Tradução interrompida pelo usuário.", resultado, null);
            }
            try {
                TraducaoLote tl = traduzirEValidar(lote, promptSistemaCongelado);
                resultado.add(tl);
            } catch (Exception e) {
                // Aborta e guarda as traduções parciais que passaram!
                throw new TraducaoParcialException(
                    e.getMessage(), 
                    resultado, 
                    e
                );
            }
        }

        log.info("Processamento concluído: {} lote(s) traduzido(s) com sucesso", resultado.size());
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz e valida um lote tolerando alucinações de contagem
     * de linhas e resíduo/preâmbulo — em vez de abortar o episódio por um único lote
     * problemático, a divisão/retry isola o trecho ruim. Só uma falha de comunicação
     * real (HTTP/timeout, esgotadas as tentativas do {@link MistralPort}) aborta.
     *
     * <p>INVARIANTES DO DOMÍNIO: toda saída passa por {@code traduzirComDivisao} antes
     * de ser devolvida como sucesso; o {@code MDC} do lote é sempre limpo no {@code finally}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: uma falha crítica é logada e repropagada. O
     * catch trata tanto {@link TradutorException} quanto
     * {@link org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException}
     * — desde a E8b a alucinação pertence ao peer {@code qualidadeTraducao} e não é mais
     * {@code TradutorException}; o multi-catch preserva a captura defensiva anterior
     * (embora, no fluxo normal, a alucinação seja absorvida antes por divisão/retry/fallback).
     */
    private TraducaoLote traduzirEValidar(Lote lote, String promptSistemaCongelado) {
        MDC.put(MDC_LOTE_ID, String.valueOf(lote.idLote()));
        try {
            List<String> traduzidas = traduzirComDivisao(lote, promptSistemaCongelado);

            log.debug("Lote {} validado com sucesso", lote.idLote());
            uiLogger.log("[ OK ] Lote " + lote.idLote() + " traduzido com sucesso.");
            uiLogger.passoConcluido(1);

            return new TraducaoLote(lote.idLote(), traduzidas, true, null);
        } catch (TradutorException | AlucinacaoDetectadaException e) {
            log.error("Falha crítica no lote {}: {}", lote.idLote(), e.getMessage());
            uiLogger.log("[ FAIL ] ERRO CRÍTICO no Lote " + lote.idLote() + ": " + e.getMessage());
            throw e;
        } finally {
            MDC.remove(MDC_LOTE_ID);
        }
    }

    /**
     * Tenta traduzir o lote de uma vez; se o LLM devolver a contagem errada de
     * linhas ou uma fala com resíduo/preâmbulo, divide o lote pela metade e
     * tenta cada metade recursivamente, isolando o trecho problemático em vez
     * de descartar o lote inteiro (que pode ter 20+ falas, das quais só 1
     * costuma ser a culpada).
     */
    private List<String> traduzirComDivisao(Lote lote, String promptSistemaCongelado) {
        if (lote.linhasOriginais().size() <= 1) {
            return traduzirLinhaUnicaComFallback(lote, promptSistemaCongelado);
        }

        try {
            return traduzirERevalidarBruto(lote, null, promptSistemaCongelado);
        } catch (DivergenciaLinhasException | AlucinacaoDetectadaException e) {
            int total = lote.linhasOriginais().size();
            int meio = total / 2;
            log.warn("Lote {} (tamanho {}) falhou na validação ({}). Dividindo em 2 partes e tentando novamente...",
                lote.idLote(), total, e.getMessage());
            uiLogger.log("[ WARN ] Lote " + lote.idLote() + " dividido após falha de validação: " + e.getMessage());

            Lote primeiraMetade = new Lote(lote.idLote(), lote.linhasOriginais().subList(0, meio));
            Lote segundaMetade = new Lote(lote.idLote(), lote.linhasOriginais().subList(meio, total));

            List<String> traduzidas = new ArrayList<>(traduzirComDivisao(primeiraMetade, promptSistemaCongelado));
            traduzidas.addAll(traduzirComDivisao(segundaMetade, promptSistemaCongelado));
            return traduzidas;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recupera uma fala isolada com novas amostragens antes
     * de declará-la pendente, preservando o restante do episódio.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada resposta rejeitada e cada recuperação são
     * contabilizadas separadamente; o fallback original não representa sucesso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: após esgotar as tentativas, devolve o
     * original como marcador transitório; {@link ProcessarArquivoUseCase} o
     * converte em tradução vazia no cache e saída explicitamente parcial.
     */
    private List<String> traduzirLinhaUnicaComFallback(Lote lote, String promptSistemaCongelado) {
        if (lote.linhasOriginais().isEmpty()) {
            return List.of();
        }

        RuntimeException ultimaFalha = null;
        boolean houveRespostaRejeitada = false;
        for (int tentativa = 1; tentativa <= 1 + MAX_TENTATIVAS_LINHA_UNICA; tentativa++) {
            try {
                Double temperatura = TEMPERATURA_POR_TENTATIVA[
                    Math.min(tentativa - 1, TEMPERATURA_POR_TENTATIVA.length - 1)];
                List<String> traducao = traduzirERevalidarBruto(lote, temperatura, promptSistemaCongelado);
                if (houveRespostaRejeitada) {
                    telemetriaTraducao.registrarFalhaTraducaoRecuperada();
                }
                return traducao;
            } catch (DivergenciaLinhasException | AlucinacaoDetectadaException e) {
                ultimaFalha = e;
                houveRespostaRejeitada = true;
                telemetriaTraducao.registrarRespostaTraducaoRejeitada();
            }
        }

        String original = lote.linhasOriginais().getFirst();
        log.warn("Lote {}: fala não pôde ser traduzida com confiança após tentativas extras ({}). " +
                "Mantendo o texto original sem tradução: \"{}\"",
            lote.idLote(), ultimaFalha != null ? ultimaFalha.getMessage() : "motivo desconhecido", original);
        uiLogger.log("[ WARN ] Fala mantida sem tradução no Lote " + lote.idLote()
            + " (revise manualmente no cache): " + original);
        return List.of(original);
    }

    private List<String> traduzirERevalidarBruto(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
        TraducaoLote resultado = mistralPort.traduzir(lote, temperaturaOverride, promptSistemaCongelado);

        if (!resultado.sucesso() || resultado.linhasTraduzidas() == null) {
            throw new TradutorException("Lote " + lote.idLote() + " falhou na comunicação: " + resultado.mensagemErro());
        }

        if (resultado.linhasTraduzidas().size() != lote.linhasOriginais().size()) {
            throw new DivergenciaLinhasException(
                "Lote " + lote.idLote() + " retornou " + resultado.linhasTraduzidas().size()
                    + " linha(s), esperado " + lote.linhasOriginais().size()
                    + ". Provável alucinação do LLM fundindo ou quebrando falas, o que desalinharia a legenda.");
        }

        for (String fala : resultado.linhasTraduzidas()) {
            validador.validarFala(fala);
        }

        return resultado.linhasTraduzidas();
    }
}
