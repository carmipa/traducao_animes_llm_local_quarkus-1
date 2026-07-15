package org.traducao.projeto.traducaoCorrige.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.traducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.traducao.application.ValidadorTraducaoService;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.traducao.infrastructure.cache.CacheManutencaoService;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: testa o fluxo completo de limpeza sobre a pasta cache,
 * incluindo proveniência, lore, backup, auditoria e formato versionado.
 *
 * <p>INVARIANTES DO DOMÍNIO: fallback inglês é invalidado, termo de lore é
 * preservado e cache legado sem seleção não sofre alteração destrutiva.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o resultado acusa falha e o arquivo
 * original permanece byte a byte igual.
 */
class LimparCacheUseCaseTest {

    @TempDir
    Path temp;

    private final ObjectMapper mapper = new ObjectMapper();
    private AuditoriaStub auditoria;
    private LimparCacheUseCase useCase;

    @BeforeEach
    void preparar() {
        GerenciadorContexto contexto = new GerenciadorContexto(List.of(new ContextoTeste()));
        ClassificadorEntradaCacheService classificador = new ClassificadorEntradaCacheService(
            new DetectorTraducaoIdenticaService(contexto), new ValidadorTraducaoService(),
            new TradutorProperties(), new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        auditoria = new AuditoriaStub(mapper);
        useCase = new LimparCacheUseCase(
            new CacheServiceTeste(mapper, temp.resolve("backups")), classificador,
            new ContextoManutencaoCacheService(contexto), auditoria, new TelemetriaStub());
    }

    @Test
    void limpaVersionadoPreservandoLoreEProveniencia() throws Exception {
        Path cache = temp.resolve("cache");
        Path arquivo = cache.resolve("anime/ep.cache.json");
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"danmachi","contextoHash":"abc","modeloLlm":"gemma","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[
               {"indice":1,"estilo":"Default","original":"Good Morning","traduzido":"Good Morning"},
               {"indice":2,"estilo":"Default","original":"Bell Cranel","traduzido":"Bell Cranel"},
               {"indice":3,"estilo":"Default","original":"Help!","traduzido":""}
             ]}
            """);

        ResultadoManutencaoCache resultado = useCase.executar(cache, null);
        var salvo = mapper.readTree(arquivo.toFile());

        assertEquals("", salvo.path("entradas").get(0).path("traduzido").asText());
        assertEquals("Bell Cranel", salvo.path("entradas").get(1).path("traduzido").asText());
        assertEquals("danmachi", salvo.path("proveniencia").path("contextoId").asText());
        assertEquals(2, resultado.itensDetectados());
        assertEquals(1, resultado.itensCorrigidos());
        assertEquals("CONCLUIDO", resultado.status());
        assertEquals(2, auditoria.entradas.size());
        assertTrue(Files.walk(temp.resolve("backups")).anyMatch(Files::isRegularFile));
    }

    @Test
    void cacheLegadoSemContextoFalhaSemModificar() throws Exception {
        Path cache = temp.resolve("cache-legado");
        Path arquivo = cache.resolve("ep.cache.json");
        Files.createDirectories(cache);
        String original = "[{\"indice\":1,\"estilo\":\"Default\",\"original\":\"Run!\",\"traduzido\":\"Run!\"}]";
        Files.writeString(arquivo, original);

        ResultadoManutencaoCache resultado = useCase.executar(cache, null);

        assertEquals("CONCLUIDO_COM_FALHAS", resultado.status());
        assertEquals(1, resultado.falhas());
        assertEquals(original, Files.readString(arquivo));
    }

    /** Cache de teste que redireciona backups para o diretório temporário. */
    private static final class CacheServiceTeste extends CacheManutencaoService {
        private final Path backup;
        CacheServiceTeste(ObjectMapper mapper, Path backup) { super(mapper); this.backup = backup; }
        @Override public Sessao iniciarSessao(Path raizCache, String operacao) {
            return new Sessao(raizCache.toAbsolutePath().normalize(), backup.toAbsolutePath().normalize(), operacao);
        }
    }

    /** Auditoria em memória para não escrever artefatos do teste no projeto. */
    private static final class AuditoriaStub extends CorrecaoCacheAuditoria {
        private final List<EntradaAuditoriaCorrecaoCache> entradas = new ArrayList<>();
        AuditoriaStub(ObjectMapper mapper) { super(mapper); }
        @Override public synchronized void registrar(EntradaAuditoriaCorrecaoCache entrada) { entradas.add(entrada); }
    }

    /** Telemetria sem I/O usada para verificar apenas a regra de negócio. */
    private static final class TelemetriaStub extends TelemetriaService {
        @Override public synchronized void finalizarOperacao(
            OperacaoTelemetria operacao, Path pastaEntrada, String prefixoRelatorio, String conteudoRelatorio) {
            // deliberadamente sem persistência no teste
        }
    }

    /** Lore mínima para provar proteção automática por conteúdo do contexto. */
    private static final class ContextoTeste implements ProvedorContexto {
        private static final String PROMPT = ContextoPrompt.montar("Teste", "Principais nomes: Bell Cranel, Hestia.");
        @Override public String getId() { return "danmachi"; }
        @Override public String getNomeExibicao() { return "Teste"; }
        @Override public String obterPromptSistema() { return PROMPT; }
    }
}
