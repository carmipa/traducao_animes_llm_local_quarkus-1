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
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.traducaoKaraoke.domain.DesfechoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.StatusExecucaoKaraoke;
import org.traducao.projeto.traducaoKaraoke.domain.TraducaoKaraokeException;
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
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;

class TraduzirKaraokeUseCaseTest {

    private static final String NOME_ARQUIVO = "Anime Teste - S01E01.ass";
    private static final String CONTEXTO_08TH = "gundam_08ms";
    private static final String PROMPT_08TH = "PROMPT EXCLUSIVO DO 08TH MS TEAM";
    private static final String PROMPT_ZZ = "PROMPT EXCLUSIVO DO GUNDAM ZZ";

    private Path tempDir;
    private Path pastaEntrada;
    private TraduzirKaraokeUseCase useCase;
    private LlmPortFake llmFake;
    private MockPersistencia persistenciaMock;
    private RegistroDaExecucao registroMock;

    private record ContextoTeste(String id, String nome, String prompt) implements ProvedorContexto {
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return prompt; }
    }

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
        /** Guarda o desfecho para os testes de telemetria de falha. */
        final AtomicReference<DesfechoKaraoke> desfechoCapturado = new AtomicReference<>();

        @Override
        public Path salvarManifesto(
            Path origem,
            Path destino,
            List<ResultadoTraducaoKaraoke> resultados,
            long duracaoMs,
            SnapshotContexto contexto,
            ProvenienciaCache proveniencia,
            DesfechoKaraoke desfecho
        ) {
            desfechoCapturado.set(desfecho);
            return null; // não grava manifesto em logs/ nos testes
        }
    }

    /** Servidor de LLM fora do ar: {@code null} é o que o pipeline trata como indisponível. */
    static class LlmForaDoAr extends LlmPortFake {
        @Override
        public StatusLlm verificarDisponibilidade() {
            return null;
        }
    }

    /**
     * Aponta os DOIS consumidores do LLM para o mesmo dublê.
     *
     * <p>Desde 2026-08-19 o LLM tem dois papéis nesta fatia: o use case o consulta para saber se
     * o servidor está no ar e qual o modelo ativo, e {@code TradutorDeLetraKaraoke} o usa para
     * traduzir de fato. Trocar só um dos dois faria o teste medir um mundo e o codigo rodar
     * outro — foi exatamente o que quebrou estes dois testes quando o colaborador foi extraido.
     */
    private void usarLlm(LlmPort llm) {
        useCase.llmPort = llm;
        useCase.tradutorDeLetra.llmPort = llm;
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
        useCase.llmPort = llmFake;
        useCase.gerenciadorContexto = new GerenciadorContexto(List.of(
            new ContextoTeste(CONTEXTO_08TH, "Mobile Suit Gundam: The 08th MS Team", PROMPT_08TH),
            new ContextoTeste("gundam_zz", "Mobile Suit Gundam ZZ", PROMPT_ZZ)));
        useCase.classificador = new ClassificadorLetraKaraokeService(new DetectorEfeitoKaraokeService());

        // O colaborador que leva a linha ao LLM saiu do use case em 2026-08-19. Aqui ele recebe
        // os MESMOS dubles — se recebesse outros, o teste passaria a medir dois mundos.
        TradutorDeLetraKaraoke tradutorDeLetra = new TradutorDeLetraKaraoke();
        tradutorDeLetra.llmPort = llmFake;
        tradutorDeLetra.mascarador = new MascaradorTags();
        tradutorDeLetra.validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());
        tradutorDeLetra.telemetriaService = new MockTelemetria();
        tradutorDeLetra.logStream = new MockLogStream();
        useCase.tradutorDeLetra = tradutorDeLetra;
        // Os colaboradores extraidos em 2026-08-19. Todos recebem os MESMOS dubles do use case —
        // dois mundos de duble fariam o teste medir uma coisa e o codigo rodar outra.
        registroMock = new RegistroDaExecucao();
        registroMock.persistencia = persistenciaMock = new MockPersistencia();
        registroMock.telemetriaService = new MockTelemetria();
        registroMock.logStream = new MockLogStream();
        // O acervo do dataset entra como duble de MEMORIA, nunca nulo: acrescentarAoDataset
        // engole RuntimeException de proposito, entao colaborador ausente faria o caminho do
        // dataset passar inteiro pelo catch e o teste ficaria verde sem exercitar uma linha.
        registroMock.acervoDataset = new AcervoKaraokeCapturado();
        useCase.registro = registroMock;

        CacheDoArquivo cacheDoArquivo = new CacheDoArquivo();
        cacheDoArquivo.cacheService = new CacheTraducaoService(new ObjectMapper());
        cacheDoArquivo.logStream = new MockLogStream();
        cacheDoArquivo.idiomaOriginal = Optional.empty();
        cacheDoArquivo.idiomaTraduzido = Optional.empty();
        cacheDoArquivo.diretorioCache = Optional.of(tempDir.resolve("cache").toString());
        useCase.cacheDoArquivo = cacheDoArquivo;

        // Sem corretor ortografico: o dicionario do sistema nao entra em teste de unidade, e a
        // lista NOMINAL da fatia continua valendo — e o que MontadorEventoFinal garante.
        useCase.montador = new MontadorEventoFinal();

        useCase.logStream = new MockLogStream();
        useCase.telemetriaService = new MockTelemetria();
        useCase.idiomaOriginal = Optional.empty();
        useCase.idiomaTraduzido = Optional.empty();
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

    /**
     * CASO DOENTE da telemetria de falha, 2026-08-14.
     *
     * <p>O registro do manifesto era condicionado a {@code !resultados.isEmpty()} e vinha DEPOIS
     * do laço. Consequência medida na auditoria: LLM fora do ar, destino não criável ou todos os
     * arquivos falhando produziam <b>zero artefato</b> — o pior desfecho possível saía
     * indistinguível de "não havia nada a fazer", e o motivo vivia só numa linha de texto dentro
     * de um log de 78 MB. Se alguém devolver aquela condição ou tirar o {@code finally}, este
     * teste cai.
     */
    @Test
    void abortoPorLlmForaDoArAindaRegistraOManifesto() {
        useCase.llmPort = new LlmForaDoAr();

        assertThrows(TraducaoKaraokeException.class, () -> useCase.aplicar(pastaEntrada, CONTEXTO_08TH),
            "o chamador continua sabendo que deu errado — o rastro é ADICIONAL, não substituto");

        DesfechoKaraoke desfecho = persistenciaMock.desfechoCapturado.get();
        assertNotNull(desfecho, "NENHUM manifesto foi registrado numa execucao abortada, "
            + "e essa e justamente a execucao que mais precisa de rastro");
        assertEquals(StatusExecucaoKaraoke.ABORTADA, desfecho.status());
        assertTrue(desfecho.motivo() != null && desfecho.motivo().contains("LLM"),
            "o motivo tem de dizer o que houve, nao 'erro desconhecido': " + desfecho.motivo());
        assertEquals(DesfechoKaraoke.EstadoDicionario.NAO_CONSULTADO, desfecho.estadoDicionario(),
            "abortou antes de traduzir qualquer linha: o dicionario NAO foi consultado, "
                + "que e diferente de estar ausente");
        assertTrue(desfecho.falhas().isEmpty(),
            "nenhum arquivo chegou a ser processado — lista vazia aqui e informacao, nao omissao");
    }

    /**
     * CONTRA-TESTE do anterior. Sem ele, um {@code status} preso em {@code ABORTADA} passaria
     * despercebido — a guarda tem de saber dizer NÃO e também SIM.
     */
    @Test
    void execucaoNormalRegistraManifestoComoCompleta() {
        useCase.aplicar(pastaEntrada, CONTEXTO_08TH);

        DesfechoKaraoke desfecho = persistenciaMock.desfechoCapturado.get();
        assertNotNull(desfecho, "execucao normal tambem tem de registrar desfecho");
        assertEquals(StatusExecucaoKaraoke.COMPLETA, desfecho.status());
        assertNull(desfecho.motivo(), "execucao completa nao tem motivo de aborto");
        assertTrue(desfecho.falhas().isEmpty(), "nenhum arquivo falhou nesta legenda de teste");
    }

    @Test
    void aplicarTraduzCamadaInglesaEPreservaRomajiEDialogo() throws IOException {
        List<ResultadoTraducaoKaraoke> resultados = useCase.aplicar(pastaEntrada, CONTEXTO_08TH);

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
    void contextoSelecionadoFicaCongeladoMesmoSeGlobalMudarDuranteExecucao() throws IOException {
        escreverLegendaDuasLinhasInglesas(pastaEntrada.resolve(NOME_ARQUIVO));
        List<String> promptsRecebidos = new ArrayList<>();

        usarLlm(new LlmPortFake() {
            @Override
            public TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistemaCongelado) {
                promptsRecebidos.add(promptSistemaCongelado);
                useCase.gerenciadorContexto.definirContextoAtivo("gundam_zz");
                return super.traduzir(lote);
            }
        });
        useCase.gerenciadorContexto.definirContextoAtivo("gundam_zz");

        useCase.aplicar(pastaEntrada, CONTEXTO_08TH);

        assertEquals(List.of(PROMPT_08TH, PROMPT_08TH), promptsRecebidos,
            "todas as chamadas devem usar a lore escolhida no início, nunca o contexto global");
        assertEquals("gundam_zz", useCase.gerenciadorContexto.obterIdContextoAtivo(),
            "o teste precisa provar que o global realmente estava em outra obra");

        Path cache = tempDir.resolve("cache").resolve("karaoke")
            .resolve("Anime Teste - S01E01.cache.json");
        String json = Files.readString(cache, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"contextoId\" : \"gundam_08ms\""));
        assertTrue(json.contains(ProvenienciaCache.hashDe(PROMPT_08TH)));
        assertFalse(json.contains(ProvenienciaCache.hashDe(PROMPT_ZZ)));
    }

    @Test
    void reexecucaoReaproveitaCacheSemChamarLlmDeNovo() {
        useCase.aplicar(pastaEntrada, CONTEXTO_08TH);
        assertEquals(1, llmFake.chamadasTraduzir);

        List<ResultadoTraducaoKaraoke> segunda = useCase.aplicar(pastaEntrada, CONTEXTO_08TH);
        assertEquals(1, llmFake.chamadasTraduzir, "segunda execução deve vir 100% do cache");
        assertEquals(1, segunda.getFirst().reaproveitadasCache());
        assertEquals(0, segunda.getFirst().traduzidas());
    }

    @Test
    void cacheLegadoSemLoreEhPreservadoMasNaoReaproveitado() throws IOException {
        Path pastaCache = Files.createDirectories(tempDir.resolve("cache").resolve("karaoke"));
        Path cache = pastaCache.resolve("Anime Teste - S01E01.cache.json");
        Files.writeString(cache, """
            [ {
              "indice" : 2,
              "estilo" : "OP - English",
              "original" : "Even if the world ends tomorrow",
              "traduzido" : "TRADUCAO DE LORE DESCONHECIDA",
              "idiomaOriginal" : "en",
              "idiomaTraduzido" : "pt-br"
            } ]
            """, StandardCharsets.UTF_8);

        useCase.aplicar(pastaEntrada, CONTEXTO_08TH);

        assertEquals(1, llmFake.chamadasTraduzir,
            "cache sem contexto não pode evitar a chamada ao LLM da lore selecionada");
        String novoCache = Files.readString(cache, StandardCharsets.UTF_8);
        assertTrue(novoCache.contains("\"contextoId\" : \"gundam_08ms\""));
        assertFalse(novoCache.contains("TRADUCAO DE LORE DESCONHECIDA"));
        try (var backups = Files.list(pastaCache)) {
            assertEquals(1, backups.filter(p -> p.getFileName().toString()
                .contains(".geracao_anterior_")).count(),
                "o cache sem proveniência deve continuar recuperável como geração anterior");
        }
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
        usarLlm(fake);

        AtomicReference<Throwable> falhaAplicador = new AtomicReference<>();
        Thread aplicador = new Thread(() -> {
            try {
                useCase.aplicar(pastaEntrada, CONTEXTO_08TH);
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
