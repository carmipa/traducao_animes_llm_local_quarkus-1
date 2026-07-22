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
 *
 * <p>Cobre também a propriedade TRANSACIONAL do caminho ativo (hotfix de 2026-07-22):
 * invalidar por proveniência é decisão de memória e copia a geração anterior para o lado,
 * mas nunca esvazia o caminho ativo — só {@code salvar} troca o conteúdo ativo, de uma vez.
 * Antes do hotfix o arquivo era MOVIDO na invalidação, e uma interrupção durante a
 * retradução deixava o episódio sem cache nenhum.
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
        assertTrue(Files.exists(f),
            "o cache ativo NÃO pode sair do lugar: a invalidação é em memória, e o arquivo só "
                + "será substituído quando a nova geração estiver inteira");
        assertTrue(existeArquivoContendo(".geracao_"), "geração anterior deve ser preservada");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: uma execução interrompida entre "invalidar por proveniência" e
     * "gravar a nova geração" NÃO pode deixar o episódio sem cache. Enquanto a nova geração
     * não existe, o cache ativo continua sendo o antigo, íntegro e reaproveitável pela
     * proveniência que o gerou — o operador não perde horas de LLM por um Ctrl-C.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code carregar} com proveniência divergente é uma decisão
     * de MEMÓRIA (mapa vazio + contagem de invalidadas) e não pode ter efeito destrutivo no
     * disco. O conteúdo do caminho ativo permanece byte a byte o mesmo até {@code salvar}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se este teste falhar, voltou a existir uma janela em
     * que o episódio fica sem cache — o defeito que apagou o S00E02 do 08th MS Team em
     * 2026-07-22, recuperado na mão a partir de {@code backups/traducao-cache}.
     */
    @Test
    void interrupcaoAposInvalidarPorProvenienciaPreservaOCacheAtivoIntacto() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi"), ent("Bye", "Tchau")));
        String conteudoAntes = Files.readString(f);

        // Lore mudou: invalida em memória. A execução MORRE aqui (nenhum salvar depois).
        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h2"));
        assertTrue(r.mapa().isEmpty());
        assertEquals(2, r.invalidadas());

        assertTrue(Files.exists(f), "o cache ativo não pode ter sumido com a execução abortada");
        assertEquals(conteudoAntes, Files.readString(f),
            "o cache ativo deve estar byte a byte igual: nada foi gravado depois da invalidação");

        // E ele continua servindo para quem tem a proveniência original — nada se perdeu.
        CacheTraducaoService.ResultadoCarga reaberto = svc.carregar(f, prov("h1"));
        assertEquals(2, reaberto.mapa().size(), "as duas falas traduzidas continuam recuperáveis");
        assertEquals("Oi", reaberto.mapa().get("Hi"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: concluída a retradução, a nova geração ocupa o caminho ativo de
     * uma vez só — é o único momento em que o conteúdo ativo troca.
     *
     * <p>INVARIANTES DO DOMÍNIO: depois de {@code salvar}, o ativo tem exatamente a geração
     * nova e a cópia datada da anterior continua ao lado para auditoria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: cache ativo com conteúdo velho após a gravação, ou
     * cópia histórica ausente.
     */
    @Test
    void novaGeracaoSubstituiOAtivoSomenteAoSalvarEPreservaACopiaAnterior() throws IOException {
        Path f = dir.resolve("ep.cache.json");
        svc.salvar(f, prov("h1"), List.of(ent("Hi", "Oi")));

        svc.carregar(f, prov("h2"));                       // invalida por lore nova
        svc.salvar(f, prov("h2"), List.of(ent("Hi", "Olá"))); // retradução termina

        CacheTraducaoService.ResultadoCarga r = svc.carregar(f, prov("h2"));
        assertEquals("Olá", r.mapa().get("Hi"), "o ativo passa a ser a geração nova");
        assertTrue(existeArquivoContendo(".geracao_"), "a geração anterior segue auditável ao lado");
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
        assertTrue(Files.exists(f), "o cache ativo permanece; a cópia da geração fica ao lado");
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
        assertTrue(Files.exists(f), "o cache ativo permanece; a cópia da geração fica ao lado");
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
        assertTrue(Files.exists(f), "o cache ativo permanece; a cópia da geração fica ao lado");
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
