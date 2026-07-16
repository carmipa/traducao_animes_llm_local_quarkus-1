package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o cache versionado por proveniência: reuso só quando lore/modelo batem,
 * invalidação + arquivamento quando divergem, migração do formato antigo e
 * preservação (não sobrescrita) de cache corrompido.
 */
class CacheTraducaoServiceTest {

    @TempDir
    Path dir;

    private final CacheTraducaoService svc = new CacheTraducaoService(new ObjectMapper());

    private static ProvenienciaCache prov(String hash) {
        return new ProvenienciaCache(ProvenienciaCache.SCHEMA_ATUAL, "danmachi", hash, "gemma", "en", "pt-BR");
    }

    private static EntradaCache ent(String original, String traduzido) {
        return new EntradaCache(0, "Default", original, traduzido, "en", "pt-BR");
    }

    private boolean existeArquivoContendo(String fragmento) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.anyMatch(p -> p.getFileName().toString().contains(fragmento));
        }
    }

    /**
     * Escreve um cache no formato OBJETO (com proveniência), controlando o JSON cru
     * da proveniência para exercitar schema 0 explícito e schema ausente — casos que
     * {@code svc.salvar} nunca produz porque sempre grava {@code SCHEMA_ATUAL}.
     */
    private void escreverCacheObjeto(Path f, String provenienciaJson) throws IOException {
        Files.writeString(f, "{\"proveniencia\":" + provenienciaJson
            + ",\"entradas\":[{\"indice\":0,\"estilo\":\"Default\",\"original\":\"Hi\","
            + "\"traduzido\":\"Oi\",\"idiomaOriginal\":\"en\",\"idiomaTraduzido\":\"pt-BR\"}]}");
    }

    @Test
    void salvarECarregarComMesmaProvenienciaReaproveita() {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertEquals(1, r.mapa().size());
        assertEquals("Oi", r.mapa().get("Hi"));
        assertEquals(0, r.invalidadas());
        assertFalse(r.migrado());
    }

    @Test
    void provenienciaDiferenteInvalidaEArquivaSemReutilizar() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h2")); // lore mudou

        assertTrue(r.mapa().isEmpty(), "cache de outro lore não pode ser reutilizado");
        assertEquals(2, r.invalidadas());
        assertFalse(Files.exists(f), "arquivo original deve ter sido arquivado");
        assertTrue(existeArquivoContendo(".geracao_"), "geração anterior deve ser preservada");
    }

    @Test
    void schemaExplicitamenteDiferenteNaoReutilizaEArquiva() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"))); // grava SCHEMA_ATUAL
        // Só o schema difere (demais 5 campos iguais): app subiu de versão de schema.
        ProvenienciaCache schemaFuturo = new ProvenienciaCache(
            ProvenienciaCache.SCHEMA_ATUAL + 1, "danmachi", "h1", "gemma", "en", "pt-BR");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, schemaFuturo);

        assertTrue(r.mapa().isEmpty(), "schema divergente não pode ser reutilizado");
        assertEquals(1, r.invalidadas());
        assertFalse(Files.exists(f), "geração de schema incompatível deve ser arquivada");
        assertTrue(existeArquivoContendo(".geracao_"));
    }

    @Test
    void schemaZeroExplicitoNaoReutilizaEArquiva() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        // Só o schema difere (0 vs SCHEMA_ATUAL); os outros 5 campos batem com prov("h1").
        escreverCacheObjeto(f, "{\"schemaVersion\":0,\"contextoId\":\"danmachi\",\"contextoHash\":\"h1\","
            + "\"modeloLlm\":\"gemma\",\"idiomaOrigem\":\"en\",\"idiomaDestino\":\"pt-BR\"}");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertTrue(r.mapa().isEmpty(), "schema 0 não é normalizado para atual");
        assertEquals(1, r.invalidadas());
        assertFalse(Files.exists(f));
        assertTrue(existeArquivoContendo(".geracao_"));
    }

    @Test
    void provenienciaSemSchemaVersionNaoReutilizaEArquivaSemTratarComoListaLegada() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        // Objeto com proveniência mas SEM o campo schemaVersion → materializa 0 → incompatível.
        escreverCacheObjeto(f, "{\"contextoId\":\"danmachi\",\"contextoHash\":\"h1\","
            + "\"modeloLlm\":\"gemma\",\"idiomaOrigem\":\"en\",\"idiomaDestino\":\"pt-BR\"}");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertTrue(r.mapa().isEmpty(), "objeto sem schema não é reutilizado");
        assertEquals(1, r.invalidadas());
        assertFalse(r.migrado(), "objeto sem schema NÃO é a lista pura legada — são formatos diferentes");
        assertFalse(Files.exists(f));
        assertTrue(existeArquivoContendo(".geracao_"));
    }

    @Test
    void formatoAntigoListaPuraEhMigradoAssumindoCompativel() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        // Formato legado: lista pura de EntradaCache, sem cabeçalho de proveniência.
        new ObjectMapper().writeValue(f.toFile(), List.of(ent("Hi", "Oi")));

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertEquals(1, r.mapa().size());
        assertEquals("Oi", r.mapa().get("Hi"));
        assertEquals(0, r.invalidadas());
        assertTrue(r.migrado());
    }

    @Test
    void cacheCorrompidoEhPreservadoENaoSobrescrito() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        Files.writeString(f, "{ isto nao e json valido ");

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h1"));

        assertTrue(r.mapa().isEmpty());
        assertFalse(Files.exists(f), "corrompido deve ser movido, não lido como vazio");
        assertTrue(existeArquivoContendo(".corrompido_"));
    }

    @Test
    void arquivoInexistenteRetornaVazioSemErro() {
        CacheTraducaoService.ResultadoCarga r = svc.carregar(dir.resolve("nao_existe.cache.json"), prov("h1"));
        assertTrue(r.mapa().isEmpty());
        assertEquals(0, r.invalidadas());
        assertFalse(r.migrado());
    }

    @Test
    void metodoAntigoListaPuraSegueFuncionandoParaOsFluxosNaoVersionados() {
        Path f = dir.resolve("karaoke.cache.json");
        svc.salvar(f, List.of(ent("Hi", "Oi")));
        assertEquals("Oi", svc.carregar(f).get("Hi"));
    }
}
