package org.traducao.projeto.raspagemCorrecao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.raspagemCorrecao.infrastructure.GoogleTranslateScraper;
import org.traducao.projeto.raspagemCorrecao.infrastructure.ResultadoRaspagem;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.ProtecaoLegendaAssService;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.cachetraducao.infrastructure.CacheManutencaoService;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducaoCorrige.application.ClassificadorEntradaCacheService;
import org.traducao.projeto.traducaoCorrige.application.ContextoManutencaoCacheService;
import org.traducao.projeto.traducaoCorrige.domain.EntradaAuditoriaCorrecaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.infrastructure.CorrecaoCacheAuditoria;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova a regressão central do menu — uma entrada vazia
 * produzida pela limpeza precisa ser preenchida pela contingência Google.
 *
 * <p>INVARIANTES DO DOMÍNIO: teste não acessa a internet nem grava telemetria no
 * projeto; cache versionado e proveniência permanecem intactos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer ausência de tradução aplicada ou
 * alteração do envelope falha o teste.
 */
class CorrigirComGoogleUseCaseTest {

    @TempDir
    Path temp;

    @Test
    void preencheEntradaVaziaDeixadaPelaLimpeza() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GerenciadorContexto contexto = new GerenciadorContexto(List.of(new ContextoTeste()));
        ClassificadorEntradaCacheService classificador = new ClassificadorEntradaCacheService(
            new DetectorTraducaoIdenticaService(contexto), new ValidadorTraducaoService(),
            new PoliticaEstiloMusical(List.of("Song JP")), new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        CacheManutencaoService cacheService = new CacheServiceTeste(mapper, temp.resolve("backups"));
        CorrigirComGoogleUseCase useCase = new CorrigirComGoogleUseCase(
            cacheService, classificador, new ContextoManutencaoCacheService(contexto),
            new ProtetorTermosLoreService(), new GoogleStub(mapper),
            new AuditoriaStub(mapper), new TelemetriaStub());

        Path cache = temp.resolve("cache");
        Path arquivo = cache.resolve("ep.cache.json");
        Files.createDirectories(cache);
        Files.writeString(arquivo, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"danmachi","contextoHash":"abc","modeloLlm":"gemma","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[{"indice":1,"estilo":"Default","original":"Help!","traduzido":""}]}
            """);

        ResultadoManutencaoCache resultado = useCase.executar(cache, null);
        var salvo = mapper.readTree(arquivo.toFile());

        assertEquals("Ajude!", salvo.path("entradas").get(0).path("traduzido").asText());
        assertEquals("danmachi", salvo.path("proveniencia").path("contextoId").asText());
        assertEquals(1, resultado.itensCorrigidos());
        assertEquals("CONCLUIDO", resultado.status());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que uma parada durante a correção não apaga o
     * último item já confirmado no disco.
     * <p>INVARIANTES DO DOMÍNIO: primeiro item persiste; segundo permanece vazio.
     * <p>COMPORTAMENTO EM CASO DE FALHA: limpa o sinal de interrupção no finally.
     */
    @Test
    void interrupcaoPreservaCheckpointJaConfirmadoNoDisco() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        GerenciadorContexto contexto = new GerenciadorContexto(List.of(new ContextoTeste()));
        ClassificadorEntradaCacheService classificador = new ClassificadorEntradaCacheService(
            new DetectorTraducaoIdenticaService(contexto), new ValidadorTraducaoService(),
            new PoliticaEstiloMusical(List.of("Song JP")), new DetectorEfeitoKaraokeService(), new ProtecaoLegendaAssService());
        CacheManutencaoService cacheService = new CacheServiceTeste(mapper, temp.resolve("backups-interrupcao"));
        CorrigirComGoogleUseCase useCase = new CorrigirComGoogleUseCase(
            cacheService, classificador, new ContextoManutencaoCacheService(contexto),
            new ProtetorTermosLoreService(), new GoogleInterrompendoStub(mapper),
            new AuditoriaStub(mapper), new TelemetriaStub());
        Path cache = temp.resolve("cache-interrupcao");
        Path arquivo = cache.resolve("ep.cache.json");
        Files.createDirectories(cache);
        Files.writeString(arquivo, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"danmachi","contextoHash":"abc","modeloLlm":"gemma","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "entradas":[{"indice":1,"estilo":"Default","original":"Help!","traduzido":""},
                         {"indice":2,"estilo":"Default","original":"Run!","traduzido":""}]}
            """);

        try {
            ResultadoManutencaoCache resultado = useCase.executar(cache, null);
            var salvo = mapper.readTree(arquivo.toFile());
            assertTrue(resultado.cancelado());
            assertEquals("Ajude!", salvo.path("entradas").get(0).path("traduzido").asText());
            assertEquals("", salvo.path("entradas").get(1).path("traduzido").asText());
        } finally {
            Thread.interrupted();
        }
    }

    private static final class GoogleStub extends GoogleTranslateScraper {
        GoogleStub(ObjectMapper mapper) { super(mapper); }
        @Override public ResultadoRaspagem traduzir(String textoOriginal) { return ResultadoRaspagem.sucesso("Ajude!"); }
    }

    private static final class GoogleInterrompendoStub extends GoogleTranslateScraper {
        /**
         * PROPÓSITO DE NEGÓCIO: cria a contingência falsa sem acesso à rede.
         * <p>INVARIANTES DO DOMÍNIO: usa apenas o mapper do teste.
         * <p>COMPORTAMENTO EM CASO DE FALHA: construção delega ao scraper-base.
         */
        GoogleInterrompendoStub(ObjectMapper mapper) { super(mapper); }
        /**
         * PROPÓSITO DE NEGÓCIO: simula cancelamento logo após uma resposta válida.
         * <p>INVARIANTES DO DOMÍNIO: resposta permite checkpoint antes da parada.
         * <p>COMPORTAMENTO EM CASO DE FALHA: marca a thread como interrompida.
         */
        @Override public ResultadoRaspagem traduzir(String textoOriginal) {
            Thread.currentThread().interrupt();
            return ResultadoRaspagem.sucesso("Ajude!");
        }
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

    private static final class ContextoTeste implements ProvedorContexto {
        private static final String PROMPT = ContextoPrompt.montar("Teste", "Principais nomes: Bell Cranel.");
        @Override public String getId() { return "danmachi"; }
        @Override public String getNomeExibicao() { return "Teste"; }
        @Override public String obterPromptSistema() { return PROMPT; }
    }
}
