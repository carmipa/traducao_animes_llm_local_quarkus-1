package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducao.infrastructure.legenda.MascaradorTags;
import org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: comprova que uma raiz cache com várias obras ativa a
 * lore registrada em cada arquivo antes da respectiva revisão LLM.
 *
 * <p>INVARIANTES DO DOMÍNIO: DanMachi e Gundam não compartilham contexto global;
 * ambas as correções passam pelas mesmas validações e gravação segura.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: contexto incorreto, correção descartada ou
 * arquivo não persistido falha explicitamente o teste.
 */
class RevisarCacheUseCaseTest {

    @TempDir
    Path temp;

    /**
     * PROPÓSITO DE NEGÓCIO: comprova que cada obra usa sua própria lore e que
     * o operador acompanha original, tradução e correção evento por evento.
     * <p>INVARIANTES DO DOMÍNIO: progresso, índice e textos aparecem no console;
     * DanMachi e Gundam permanecem isolados por proveniência.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de mensagem dinâmica ou troca
     * de contexto falha explicitamente o teste.
     */
    @Test
    void usaContextoDaProvenienciaParaCadaArquivo() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GerenciadorContexto contexto = new GerenciadorContexto(List.of(
            new ContextoTeste("danmachi", "DanMachi", "Bell Cranel"),
            new ContextoTeste("gundam", "Gundam", "Amuro Ray")));
        MistralStub mistral = new MistralStub(contexto);
        ClassificadorEntradaCacheService classificador = new ClassificadorEntradaCacheService(
            new DetectorTraducaoIdenticaService(contexto), new ValidadorTraducaoService(),
            new PoliticaEstiloMusical(List.of("Song JP")), new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        RevisarCacheUseCase useCase = new RevisarCacheUseCase(
            new CacheServiceTeste(mapper, temp.resolve("backups")), classificador,
            new ContextoManutencaoCacheService(contexto), new DetectorConcordanciaService(), mistral,
            new ValidadorTraducaoService(), new MascaradorTags(), new ProtecaoLegendaAssService(),
            new AuditoriaStub(mapper), new TelemetriaStub());

        Path cache = temp.resolve("cache");
        Files.createDirectories(cache);
        Path danmachi = cache.resolve("01-danmachi.cache.json");
        Path gundam = cache.resolve("02-gundam.cache.json");
        escrever(mapper, danmachi, "danmachi", "She is tired.", "ele está cansado.");
        escrever(mapper, gundam, "gundam", "He is tired.", "ela está cansada.");

        ByteArrayOutputStream console = new ByteArrayOutputStream();
        PrintStream saidaAnterior = System.out;
        ResultadoManutencaoCache resultado;
        try {
            System.setOut(new PrintStream(console, true, StandardCharsets.UTF_8));
            resultado = useCase.executar(cache, null);
        } finally {
            System.setOut(saidaAnterior);
        }

        assertEquals(List.of("danmachi", "gundam"), mistral.contextosUsados);
        assertEquals("ela está cansada.", mapper.readTree(danmachi.toFile()).path("entradas").get(0).path("traduzido").asText());
        assertEquals("ele está cansado.", mapper.readTree(gundam.toFile()).path("entradas").get(0).path("traduzido").asText());
        assertEquals(2, resultado.itensCorrigidos());
        assertEquals("CONCLUIDO", resultado.status());
        String mensagens = console.toString(StandardCharsets.UTF_8);
        assertTrue(mensagens.contains("[REVISANDO 1/1] Evento 1"), mensagens);
        assertTrue(mensagens.contains("Original: She is tired."), mensagens);
        assertTrue(mensagens.contains("Tradução atual: ele está cansado."), mensagens);
        assertTrue(mensagens.contains("[CORRIGIDA 1/1] Evento 1"), mensagens);
        assertTrue(mensagens.contains("Depois: ela está cansada."), mensagens);
        assertTrue(mensagens.contains("pendentes=0"), mensagens);
    }

    private static void escrever(ObjectMapper mapper, Path arquivo, String contexto, String original, String traduzido)
        throws Exception {
        String json = """
            {"proveniencia":{"schemaVersion":1,"contextoId":"%s","contextoHash":"abc","modeloLlm":"gemma","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[{"indice":1,"estilo":"Default","original":%s,"traduzido":%s}]}
            """.formatted(contexto, mapper.writeValueAsString(original), mapper.writeValueAsString(traduzido));
        Files.writeString(arquivo, json);
    }

    private static final class MistralStub implements MistralPort {
        private final GerenciadorContexto contexto;
        private final List<String> contextosUsados = new ArrayList<>();
        MistralStub(GerenciadorContexto contexto) { this.contexto = contexto; }
        @Override public TraducaoLote traduzir(Lote lote) { return null; }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(true, true, "fake carregado"); }
        @Override public Optional<String> revisarConcordancia(String original, String traducao, List<String> problemas) {
            contextosUsados.add(contexto.obterIdContextoAtivo());
            return Optional.of("danmachi".equals(contexto.obterIdContextoAtivo())
                ? "ela está cansada." : "ele está cansado.");
        }
        @Override public Optional<String> corrigirTraducao(String original, String traducao, String motivo) {
            return Optional.empty();
        }
    }

    private static final class ContextoTeste implements ProvedorContexto {
        private final String id;
        private final String nome;
        private final String prompt;
        ContextoTeste(String id, String nome, String termo) {
            this.id = id;
            this.nome = nome;
            this.prompt = ContextoPrompt.montar(nome, "Principais nomes: " + termo + ".");
        }
        @Override public String getId() { return id; }
        @Override public String getNomeExibicao() { return nome; }
        @Override public String obterPromptSistema() { return prompt; }
    }

    private static final class CacheServiceTeste extends CacheManutencaoService {
        private final Path backup;
        CacheServiceTeste(ObjectMapper mapper, Path backup) { super(mapper); this.backup = backup; }
        @Override public Sessao iniciarSessao(Path raizCache, String operacao) {
            return new Sessao(raizCache.toAbsolutePath().normalize(), backup.toAbsolutePath().normalize(), operacao);
        }
    }

    private static final class AuditoriaStub extends CorrecaoCacheAuditoria {
        AuditoriaStub(ObjectMapper mapper) { super(mapper); }
        @Override public synchronized void registrar(EntradaAuditoriaCorrecaoCache entrada) { }
    }

    private static final class TelemetriaStub extends TelemetriaService {
        @Override public synchronized void finalizarOperacao(
            OperacaoTelemetria operacao, Path pastaEntrada, String prefixoRelatorio, String conteudoRelatorio) { }
    }
}
