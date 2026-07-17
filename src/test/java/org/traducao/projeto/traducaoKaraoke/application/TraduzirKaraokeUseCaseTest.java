package org.traducao.projeto.traducaoKaraoke.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.cachetraducao.infrastructure.CacheTraducaoService;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TraducaoKaraokePersistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TraduzirKaraokeUseCaseTest {

    private static final String NOME_ARQUIVO = "Anime Teste - S01E01.ass";

    private Path tempDir;
    private Path pastaEntrada;
    private TraduzirKaraokeUseCase useCase;
    private LlmPortFake llmFake;

    /** Traduções fixas com contador de chamadas para provar reuso de cache e dry-run sem LLM. */
    static class LlmPortFake implements LlmPort {
        int chamadasTraduzir = 0;
        private final Map<String, String> respostas = Map.of(
            "Even if the world ends tomorrow", "Mesmo que o mundo acabe amanhã"
        );

        @Override
        public TraducaoLote traduzir(Lote lote) {
            chamadasTraduzir++;
            String original = lote.linhasOriginais().getFirst();
            String traduzido = respostas.getOrDefault(original, "Tradução simulada");
            return new TraducaoLote(lote.idLote(), List.of(traduzido), true, null);
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return new StatusLlm(true, true, "modelo de teste carregado");
        }

        @Override
        public Optional<String> revisarConcordancia(String original, String traducao, List<String> problemas) {
            return Optional.empty();
        }

        @Override
        public Optional<String> corrigirTraducao(String original, String traducao, String motivo) {
            return Optional.empty();
        }
    }

    static class MockLogStream extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // silencioso nos testes
        }
    }

    static class MockTelemetria extends TelemetriaService {
        @Override
        public void finalizarOperacao(OperacaoTelemetria operacao, Path pastaEntrada, String prefixo, String conteudo) {
            // não persiste telemetria em disco nos testes
        }

        @Override
        public void registrarAlucinacaoPrevenida() {
            // silencioso
        }
    }

    static class MockPersistencia extends TraducaoKaraokePersistencia {
        @Override
        public Path salvarManifesto(Path origem, Path destino, List<ResultadoTraducaoKaraoke> resultados, long duracaoMs) {
            return null; // não grava manifesto em logs/ nos testes
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test_traducao_karaoke");
        pastaEntrada = Files.createDirectories(tempDir.resolve("legendas"));
        escreverLegenda(pastaEntrada.resolve(NOME_ARQUIVO));

        llmFake = new LlmPortFake();

        useCase = new TraduzirKaraokeUseCase();
        useCase.leitor = new LeitorLegendaAss();
        useCase.escritor = new EscritorLegendaAss();
        useCase.mascarador = new MascaradorTags();
        useCase.validador = new ValidadorTraducaoService();
        useCase.cacheService = new CacheTraducaoService(new ObjectMapper());
        useCase.llmPort = llmFake;
        useCase.gerenciadorContexto = null; // sem CDI: o use case tolera ausência em teste
        useCase.classificador = new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());
        useCase.logStream = new MockLogStream();
        useCase.telemetriaService = new MockTelemetria();
        useCase.persistencia = new MockPersistencia();
        useCase.idiomaOriginal = Optional.empty();
        useCase.idiomaTraduzido = Optional.empty();
        useCase.diretorioCache = Optional.of(tempDir.resolve("cache").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var walk = Files.walk(tempDir)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    // melhor esforço na limpeza do temp
                }
            });
        }
    }

    private void escreverLegenda(Path destino) throws IOException {
        String conteudo = String.join("\r\n",
            "[Script Info]",
            "Title: Teste",
            "",
            "[Events]",
            "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
            "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala de diálogo intocável.",
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - Romaji,,0,0,0,,kimi no heart ni fly away",
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - English,,0,0,0,,Even if the world ends tomorrow",
            "");
        Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
    }

    @Test
    void aplicarTraduzCamadaInglesaEPreservaRomajiEDialogo() throws IOException {
        List<ResultadoTraducaoKaraoke> resultados = useCase.aplicar(pastaEntrada, null);

        assertEquals(1, resultados.size());
        ResultadoTraducaoKaraoke r = resultados.getFirst();
        assertEquals(1, r.preservadasOriginalJapones(), "romaji com inglês misturado deve ser preservado");
        assertEquals(1, r.paraTraduzir());
        assertEquals(1, r.traduzidas());
        assertEquals(0, r.mantidasSemTraducao());

        Path destino = TraduzirKaraokeUseCase.resolverPastaSaida(pastaEntrada).resolve(NOME_ARQUIVO);
        assertTrue(Files.exists(destino), "legenda traduzida deve ser gravada na pasta irmã");
        String saida = Files.readString(destino, StandardCharsets.UTF_8);
        assertTrue(saida.contains("kimi no heart ni fly away"), "letra original não pode ser alterada");
        assertTrue(saida.contains("Mesmo que o mundo acabe amanhã"), "camada inglesa deve virar PT-BR");
        assertFalse(saida.contains("Even if the world ends tomorrow"), "inglês da camada de tradução não pode sobrar");
        assertTrue(saida.contains("Fala de diálogo intocável."), "diálogo não pode ser alterado");

        String original = Files.readString(pastaEntrada.resolve(NOME_ARQUIVO), StandardCharsets.UTF_8);
        assertTrue(original.contains("Even if the world ends tomorrow"), "arquivo de entrada deve permanecer intacto");
    }

    @Test
    void reexecucaoReaproveitaCacheSemChamarLlmDeNovo() {
        useCase.aplicar(pastaEntrada, null);
        assertEquals(1, llmFake.chamadasTraduzir);

        List<ResultadoTraducaoKaraoke> segunda = useCase.aplicar(pastaEntrada, null);
        assertEquals(1, llmFake.chamadasTraduzir, "segunda execução deve vir 100% do cache");
        assertEquals(1, segunda.getFirst().reaproveitadasCache());
        assertEquals(0, segunda.getFirst().traduzidas());
    }

    @Test
    void simularClassificaSemChamarLlmESemGravar() {
        List<ResultadoTraducaoKaraoke> resultados = useCase.simular(pastaEntrada, null);

        assertEquals(0, llmFake.chamadasTraduzir, "dry-run nunca chama o LLM");
        Path pastaDestino = TraduzirKaraokeUseCase.resolverPastaSaida(pastaEntrada);
        assertFalse(Files.exists(pastaDestino), "dry-run não pode criar a pasta de destino");

        ResultadoTraducaoKaraoke r = resultados.getFirst();
        assertEquals(1, r.preservadasOriginalJapones());
        assertEquals(1, r.paraTraduzir());
        assertEquals(0, r.traduzidas());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dublê de LLM que bloqueia na PRIMEIRA chamada até ser liberado,
     * registrando os ids de lote observados — permite prender o {@code aplicar} num ponto
     * conhecido para provar o isolamento do contador contra um {@code simular} concorrente.
     *
     * <p>INVARIANTES DO DOMÍNIO: registra todo id de lote recebido (lista sincronizada);
     * sinaliza {@code primeiraChamadaIniciou} e aguarda {@code liberar} apenas na 1ª chamada;
     * as demais respondem imediatamente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: interrompido durante a espera, restaura o flag de
     * interrupção e devolve a tradução simulada, sem travar a thread indefinidamente.
     */
    static class LlmPortBloqueante implements LlmPort {
        final List<Integer> idsObservados = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch primeiraChamadaIniciou = new CountDownLatch(1);
        final CountDownLatch liberar = new CountDownLatch(1);
        private volatile boolean primeira = true;

        @Override
        public TraducaoLote traduzir(Lote lote) {
            idsObservados.add(lote.idLote());
            if (primeira) {
                primeira = false;
                primeiraChamadaIniciou.countDown();
                try {
                    liberar.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return new TraducaoLote(lote.idLote(), List.of("Tradução simulada"), true, null);
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return new StatusLlm(true, true, "modelo de teste carregado");
        }

        @Override
        public Optional<String> revisarConcordancia(String original, String traducao, List<String> problemas) {
            return Optional.empty();
        }

        @Override
        public Optional<String> corrigirTraducao(String original, String traducao, String motivo) {
            return Optional.empty();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que o contador de lotes é isolado por execução — o defeito
     * corrigido na FASE I.3 permitia que um {@code simular} concorrente (fora da fila)
     * resetasse o contador de um {@code aplicar} em curso (na fila), corrompendo os ids de lote.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa a MESMA instância singleton; o LLM bloqueia a 1ª chamada
     * do {@code aplicar} via {@link CountDownLatch} enquanto {@code simular} roda na mesma
     * instância; os ids observados pelo LLM devem ser exatamente {@code [1, 2]} — {@code simular}
     * não perturba o contador nem chama o LLM nem grava saída. Determinístico, sem {@code sleep}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: na implementação antiga (campo de instância
     * {@code sequencialLote}) o {@code simular} zera o contador e o 2º lote reaparece como id 1,
     * reprovando a asserção {@code [1, 2]}; com o contador local atual, passa.
     */
    @Test
    void contadorDeLotesIsoladoEntreSimularEAplicarConcorrentes() throws Exception {
        escreverLegendaDuasLinhasInglesas(pastaEntrada.resolve(NOME_ARQUIVO));
        LlmPortBloqueante fake = new LlmPortBloqueante();
        useCase.llmPort = fake;

        AtomicReference<Throwable> falhaAplicador = new AtomicReference<>();
        Thread aplicador = new Thread(() -> {
            try {
                useCase.aplicar(pastaEntrada, null);
            } catch (Throwable t) {
                falhaAplicador.set(t);
            }
        }, "aplicar-karaoke-teste");
        aplicador.setDaemon(true);
        aplicador.start();

        // O finally SEMPRE libera o aplicador — se qualquer assertiva falhar antes, a thread
        // presa no LLM não pode ficar bloqueada indefinidamente.
        try {
            assertTrue(fake.primeiraChamadaIniciou.await(10, TimeUnit.SECONDS),
                "a primeira chamada ao LLM do aplicar deveria ter iniciado");

            int chamadasAntes = fake.idsObservados.size();
            List<ResultadoTraducaoKaraoke> simulacao = useCase.simular(pastaEntrada, null);
            int chamadasDepois = fake.idsObservados.size();

            assertEquals(1, chamadasAntes, "apenas a 1ª chamada do aplicar deve estar em curso");
            assertEquals(chamadasAntes, chamadasDepois, "simular não pode chamar o LLM");
            assertNull(simulacao.getFirst().arquivoDestino(), "simular não pode gravar saída");
        } finally {
            fake.liberar.countDown();
        }

        aplicador.join(10_000);
        if (aplicador.isAlive()) {
            aplicador.interrupt();
            aplicador.join(5_000);
        }
        assertFalse(aplicador.isAlive(), "aplicar deveria ter concluído (nenhuma thread sobrevive ao teste)");
        assertNull(falhaAplicador.get(), "aplicar não deveria lançar exceção");

        assertEquals(List.of(1, 2), List.copyOf(fake.idsObservados),
            "os ids de lote observados pelo LLM devem ser exatamente [1, 2]; "
            + "simular concorrente não pode reiniciar o contador do aplicar");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escreve uma legenda de teste com DUAS linhas inglesas traduzíveis e
     * distintas, para que o {@code aplicar} gere exatamente dois lotes de LLM (ids 1 e 2) e
     * exponha a corrida do contador.
     *
     * <p>INVARIANTES DO DOMÍNIO: as duas falas usam estilo de camada inglesa e textos
     * diferentes, garantindo duas chamadas distintas ao LLM (sem dedup) na ordem do arquivo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: falha de I/O ao escrever propaga {@link IOException} e
     * interrompe o teste.
     */
    private void escreverLegendaDuasLinhasInglesas(Path destino) throws IOException {
        String conteudo = String.join("\r\n",
            "[Script Info]",
            "Title: Teste",
            "",
            "[Events]",
            "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
            "Dialogue: 0,0:01:00.00,0:01:05.00,OP - English,,0,0,0,,The night sky is calling me",
            "Dialogue: 0,0:01:06.00,0:01:10.00,OP - English,,0,0,0,,Hold my hand and never let go",
            "");
        Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
    }
}
