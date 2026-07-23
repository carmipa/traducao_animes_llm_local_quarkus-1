package org.traducao.projeto.traducao.infrastructure.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.traducao.domain.NormalizadorNomeEpisodio;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducaoDocumento;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROPÓSITO DE NEGÓCIO: única escritora do arquivo canônico próprio da telemetria
 * da Tradução Local ({@code logs/telemetria_traducao.json}). Projeta, por episódio,
 * o estado final consolidado das traduções e mantém os quatro contadores da fatia,
 * isolando a Tradução Local do módulo de telemetria (o painel apenas lê este arquivo).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Deduplicação por nome de episódio normalizado ({@link NormalizadorNomeEpisodio});
 *       o registro mais recente substitui o anterior — nunca append-only.</li>
 *   <li>Os quatro contadores iniciam em zero e acumulam SOMENTE eventos da Tradução
 *       Local após a adoção deste arquivo; jamais copiam valores do legado.</li>
 *   <li>Cada mutação persiste o documento inteiro (registros + contadores) como uma
 *       ÚNICA alteração lógica, via escrita atômica (temporário no mesmo diretório +
 *       movimentação segura).</li>
 *   <li>Sincronização de escopo JVM: as mutações são {@code synchronized}. Não há
 *       coordenação entre processos — assume-se uma única instância escrevendo o arquivo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Um arquivo existente ilegível NÃO é destruído silenciosamente: é preservado com sufixo
 * único {@code .corrompido_<timestamp>_<seq>} (sem sobrescrever evidência anterior) antes de
 * recomeçar o estado. O carregamento tolera elemento {@code null} no array de registros e
 * captura também erros de runtime, para nunca reprovar o {@code @PostConstruct} e impedir o
 * boot. Falha de I/O ao persistir é registrada, mantendo o estado em memória coerente.
 */
@Component
public class TelemetriaTraducaoAdapter implements TelemetriaTraducaoPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaTraducaoAdapter.class);

    // 1.1: acrescenta pendenciasPorCausa (KPI estruturado) a cada registro; aditivo e
    // retrocompatível — arquivos 1.0 sao lidos com o campo ausente como null.
    private static final String SCHEMA_VERSION = "1.1";
    private static final String NOME_ARQUIVO = "telemetria_traducao.json";
    /** Histórico append-only: uma linha JSON por EXECUÇÃO (o canônico guarda só a última). */
    static final String NOME_ARQUIVO_HISTORICO = "telemetria_execucoes.jsonl";
    /** Teto LOCAL de execuções guardadas. O acervo completo vive no repositório do dataset. */
    private static final int LIMITE_LOCAL_EXECUCOES = 20_000;
    /** Folga antes de podar, para não reescrever o arquivo a cada episódio traduzido. */
    private static final int FOLGA_PODA = 1_000;
    private static final String SUBPASTA = "logs";
    // Carimbo único (timestamp + sequência JVM) para NÃO sobrescrever evidência forense de
    // corrupções sucessivas; a sequência garante unicidade mesmo dentro do mesmo milissegundo.
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final AtomicInteger SEQ_CORROMPIDO = new AtomicInteger(0);

    private final ObjectMapper objectMapper;
    private final Map<String, TelemetriaTraducao> banco = new LinkedHashMap<>();
    private final AtomicInteger alucinacoesPrevenidas = new AtomicInteger(0);
    private final AtomicInteger respostasTraducaoRejeitadas = new AtomicInteger(0);
    private final AtomicInteger falhasTraducaoRecuperadas = new AtomicInteger(0);
    private final AtomicInteger fallbacksTraducaoMantidos = new AtomicInteger(0);

    public TelemetriaTraducaoAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public synchronized void carregar() {
        Path arquivo = arquivo();
        if (!Files.exists(arquivo)) {
            return;
        }
        try {
            TelemetriaTraducaoDocumento doc = objectMapper.readValue(arquivo.toFile(), TelemetriaTraducaoDocumento.class);
            if (doc == null) {
                return;
            }
            if (doc.registros() != null) {
                for (TelemetriaTraducao t : doc.registros()) {
                    if (t == null) {
                        continue; // elemento null no array (edição manual / escrita parcial): ignora
                    }
                    banco.put(NormalizadorNomeEpisodio.normalizar(t.nomeEpisodio()), t);
                }
            }
            alucinacoesPrevenidas.set(doc.alucinacoesPrevenidas());
            respostasTraducaoRejeitadas.set(doc.respostasTraducaoRejeitadas());
            falhasTraducaoRecuperadas.set(doc.falhasTraducaoRecuperadas());
            fallbacksTraducaoMantidos.set(doc.fallbacksTraducaoMantidos());
            log.info("Telemetria da Traducao Local carregada: {} episodio(s) de {}.", banco.size(), arquivo);
        } catch (IOException | RuntimeException e) {
            // Qualquer falha de leitura/processamento (I/O, JSON, dado inesperado) preserva o
            // arquivo em vez de reprovar o @PostConstruct e impedir o boot da aplicação.
            preservarCorrompido(arquivo, e);
        }
    }

    @Override
    public synchronized void registrarTraducao(TelemetriaTraducao telemetria) {
        if (telemetria == null) {
            return;
        }
        banco.put(NormalizadorNomeEpisodio.normalizar(telemetria.nomeEpisodio()), telemetria);
        persistir();
        acrescentarAoHistorico(telemetria);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva CADA execução de tradução como uma linha própria, para que o
     * dataset de pesquisa possa comparar o mesmo episódio ao longo do tempo ("esta mudança
     * melhorou?"). O arquivo canônico {@code telemetria_traducao.json} é, por construção, uma
     * FOTO: {@code banco} é um mapa chaveado pelo nome do episódio, então retraduzir apaga a
     * medição anterior. Em 2026-07-23 isso foi confirmado nos dados reais — 155 registros para
     * 155 episódios distintos, zero repetições, e as medições do 08th de 2026-07-22 já haviam
     * sumido ao serem retraduzidas.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>APPEND puro: uma linha JSON por execução, jamais reescrita ou editada.</li>
     *   <li>Teto LOCAL de {@value #LIMITE_LOCAL_EXECUCOES} linhas — a máquina do operador não
     *       pode crescer sem fim. Ao estourar, as MAIS ANTIGAS saem daqui; elas continuam no
     *       repositório do dataset, que só cresce.</li>
     *   <li>A poda roda com folga ({@value #FOLGA_PODA} linhas além do teto) para não reescrever
     *       o arquivo a cada episódio.</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * NUNCA propaga: telemetria histórica é observabilidade, não pode derrubar uma tradução que
     * já terminou. Falha de I/O é registrada em WARN e a execução segue.
     */
    private void acrescentarAoHistorico(TelemetriaTraducao telemetria) {
        try {
            Path pasta = pasta();
            Files.createDirectories(pasta);
            Path historico = pasta.resolve(NOME_ARQUIVO_HISTORICO);
            String linha = objectMapper.writeValueAsString(telemetria);
            Files.writeString(historico, linha + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            podarHistoricoSeNecessario(historico);
        } catch (IOException | RuntimeException e) {
            log.warn("Falha ao acrescentar execucao ao historico de telemetria: {}", e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mantém o histórico local dentro de um teto sem jamais perder dado já
     * publicado — o corte local é de ARMAZENAMENTO, não de acervo.
     *
     * <p>INVARIANTES DO DOMÍNIO: descarta apenas as linhas mais ANTIGAS e preserva a ordem
     * cronológica de escrita; a reescrita é atômica (temporário + substituição), então uma queda
     * no meio da poda não trunca o histórico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException} para o chamador, que apenas
     * loga — o arquivo anterior permanece íntegro porque a troca só ocorre no fim.
     */
    private void podarHistoricoSeNecessario(Path historico) throws IOException {
        List<String> linhas = Files.readAllLines(historico, StandardCharsets.UTF_8);
        if (linhas.size() <= LIMITE_LOCAL_EXECUCOES + FOLGA_PODA) {
            return;
        }
        List<String> mantidas = linhas.subList(linhas.size() - LIMITE_LOCAL_EXECUCOES, linhas.size());
        Path temporario = historico.resolveSibling(NOME_ARQUIVO_HISTORICO + ".tmp");
        Files.write(temporario, mantidas, StandardCharsets.UTF_8);
        ArquivoAtomicoUtil.substituirAtomico(temporario, historico);
        log.info("Historico local de telemetria podado para as {} execucoes mais recentes "
            + "(as anteriores permanecem no repositorio do dataset).", LIMITE_LOCAL_EXECUCOES);
    }

    @Override
    public synchronized void registrarAlucinacaoPrevenida() {
        alucinacoesPrevenidas.incrementAndGet();
        persistir();
    }

    @Override
    public synchronized void registrarRespostaTraducaoRejeitada() {
        respostasTraducaoRejeitadas.incrementAndGet();
        persistir();
    }

    @Override
    public synchronized void registrarFalhaTraducaoRecuperada() {
        falhasTraducaoRecuperadas.incrementAndGet();
        persistir();
    }

    @Override
    public synchronized void registrarFallbackMantido() {
        fallbacksTraducaoMantidos.incrementAndGet();
        persistir();
    }

    private void persistir() {
        try {
            Path pasta = pasta();
            Files.createDirectories(pasta);
            Path arquivo = pasta.resolve(NOME_ARQUIVO);
            TelemetriaTraducaoDocumento doc = new TelemetriaTraducaoDocumento(
                SCHEMA_VERSION,
                new ArrayList<>(banco.values()),
                alucinacoesPrevenidas.get(),
                respostasTraducaoRejeitadas.get(),
                falhasTraducaoRecuperadas.get(),
                fallbacksTraducaoMantidos.get());
            Path temporario = pasta.resolve(NOME_ARQUIVO + ".tmp");
            objectMapper.writeValue(temporario.toFile(), doc);
            ArquivoAtomicoUtil.substituirAtomico(temporario, arquivo);
        } catch (IOException e) {
            log.error("Falha ao persistir a telemetria da Traducao Local: {}", e.getMessage(), e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva o arquivo de telemetria ilegível como evidência forense
     * antes de recomeçar o estado — sem apagar nem sobrescrever a evidência de corrupções
     * anteriores, para que cada falha possa ser investigada.
     *
     * <p>INVARIANTES DO DOMÍNIO: o nome preservado é único por corrupção (carimbo de tempo +
     * sequência JVM); NUNCA sobrescreve um {@code .corrompido_*} já existente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se nem mover for possível, o original é mantido
     * intacto (não sobrescrito) e o estado inicia vazio; ambas as causas são logadas.
     */
    private void preservarCorrompido(Path arquivo, Exception causa) {
        String carimbo = LocalDateTime.now().format(TS) + "_" + SEQ_CORROMPIDO.incrementAndGet();
        Path corrompido = arquivo.resolveSibling(NOME_ARQUIVO + ".corrompido_" + carimbo);
        try {
            Files.move(arquivo, corrompido);
            log.error("Telemetria da Traducao Local ilegivel; preservada como {} e reiniciado estado vazio. Causa: {}",
                corrompido, causa.getMessage());
        } catch (IOException e) {
            log.error("Telemetria da Traducao Local ilegivel e NAO foi possivel preserva-la ({}). "
                + "Estado iniciado vazio; o arquivo original NAO sera sobrescrito ate nova escrita bem-sucedida. Causa: {}",
                e.getMessage(), causa.getMessage());
        }
    }

    private static Path pasta() {
        return DiretorioBaseKronos.resolver(SUBPASTA);
    }

    private static Path arquivo() {
        return pasta().resolve(NOME_ARQUIVO);
    }
}
