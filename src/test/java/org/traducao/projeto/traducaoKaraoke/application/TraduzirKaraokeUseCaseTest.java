package org.traducao.projeto.traducaoKaraoke.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.cache.CacheTraducaoService;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.legenda.EscritorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.LeitorLegendaAss;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.infrastructure.TraducaoKaraokePersistencia;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TraduzirKaraokeUseCaseTest {

    private static final String NOME_ARQUIVO = "Anime Teste - S01E01.ass";

    private Path tempDir;
    private Path pastaEntrada;
    private TraduzirKaraokeUseCase useCase;
    private MistralPortFake mistralFake;

    /** Traduções fixas com contador de chamadas para provar reuso de cache e dry-run sem LLM. */
    static class MistralPortFake implements MistralPort {
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

        mistralFake = new MistralPortFake();
        TradutorProperties props = new TradutorProperties();
        props.setDiretorioCache(tempDir.resolve("cache").toString());

        useCase = new TraduzirKaraokeUseCase();
        useCase.leitor = new LeitorLegendaAss();
        useCase.escritor = new EscritorLegendaAss();
        useCase.mascarador = new MascaradorTags();
        useCase.validador = new ValidadorTraducaoService();
        useCase.cacheService = new CacheTraducaoService(new ObjectMapper());
        useCase.mistralPort = mistralFake;
        useCase.gerenciadorContexto = null; // sem CDI: o use case tolera ausência em teste
        useCase.classificador = new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());
        useCase.logStream = new MockLogStream();
        useCase.telemetriaService = new MockTelemetria();
        useCase.persistencia = new MockPersistencia();
        useCase.propriedades = props;
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
        assertEquals(1, mistralFake.chamadasTraduzir);

        List<ResultadoTraducaoKaraoke> segunda = useCase.aplicar(pastaEntrada, null);
        assertEquals(1, mistralFake.chamadasTraduzir, "segunda execução deve vir 100% do cache");
        assertEquals(1, segunda.getFirst().reaproveitadasCache());
        assertEquals(0, segunda.getFirst().traduzidas());
    }

    @Test
    void simularClassificaSemChamarLlmESemGravar() {
        List<ResultadoTraducaoKaraoke> resultados = useCase.simular(pastaEntrada, null);

        assertEquals(0, mistralFake.chamadasTraduzir, "dry-run nunca chama o LLM");
        Path pastaDestino = TraduzirKaraokeUseCase.resolverPastaSaida(pastaEntrada);
        assertFalse(Files.exists(pastaDestino), "dry-run não pode criar a pasta de destino");

        ResultadoTraducaoKaraoke r = resultados.getFirst();
        assertEquals(1, r.preservadasOriginalJapones());
        assertEquals(1, r.paraTraduzir());
        assertEquals(0, r.traduzidas());
    }
}
