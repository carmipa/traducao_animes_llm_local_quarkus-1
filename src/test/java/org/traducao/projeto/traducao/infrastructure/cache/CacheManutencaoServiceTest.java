package org.traducao.projeto.traducao.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a manutenção da pasta cache preserva formato,
 * proveniência, extensões futuras e uma cópia restaurável antes de salvar.
 *
 * <p>INVARIANTES DO DOMÍNIO: cobre lista legada e documento versionado; nenhuma
 * estrutura inválida é aceita para escrita.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o teste exige {@link IOException} e
 * confirma que o original não foi alterado.
 */
class CacheManutencaoServiceTest {

    @TempDir
    Path temp;

    private final ObjectMapper mapper = new ObjectMapper();
    private final CacheManutencaoService service = new CacheManutencaoService(mapper);

    @Test
    void formatoLegadoEhAlteradoComBackupSemVirarObjeto() throws Exception {
        Path raiz = temp.resolve("cache");
        Path arquivo = raiz.resolve("anime/ep.cache.json");
        Files.createDirectories(arquivo.getParent());
        Files.writeString(arquivo, "[{\"indice\":1,\"original\":\"Hello\",\"traduzido\":\"Hello\"}]");

        CacheManutencaoService.DocumentoEditavel doc = service.carregar(arquivo);
        ((ObjectNode) doc.entradas().get(0)).put("traduzido", "");
        Path backups = temp.resolve("backups");
        Path backup = service.salvarAtomico(doc,
            new CacheManutencaoService.Sessao(raiz.toAbsolutePath(), backups.toAbsolutePath(), "teste"));

        assertTrue(mapper.readTree(arquivo.toFile()).isArray());
        assertEquals("", mapper.readTree(arquivo.toFile()).get(0).path("traduzido").asText());
        assertTrue(Files.exists(backup));
        assertEquals("Hello", mapper.readTree(backup.toFile()).get(0).path("traduzido").asText());
    }

    @Test
    void formatoVersionadoPreservaProvenienciaECampoDesconhecido() throws Exception {
        Path raiz = temp.resolve("cache2");
        Path arquivo = raiz.resolve("ep.cache.json");
        Files.createDirectories(raiz);
        Files.writeString(arquivo, """
            {"proveniencia":{"schemaVersion":1,"contextoId":"danmachi","contextoHash":"abc","modeloLlm":"gemma","idiomaOrigem":"en","idiomaDestino":"pt-br"},
             "campoFuturo":"preservar","entradas":[{"indice":2,"original":"Run!","traduzido":"Run!"}]}
            """);

        CacheManutencaoService.DocumentoEditavel doc = service.carregar(arquivo);
        ((ObjectNode) doc.entradas().get(0)).put("traduzido", "Corra!");
        service.salvarAtomico(doc, new CacheManutencaoService.Sessao(
            raiz.toAbsolutePath(), temp.resolve("backup2").toAbsolutePath(), "teste"));

        var salvo = mapper.readTree(arquivo.toFile());
        assertEquals("danmachi", salvo.path("proveniencia").path("contextoId").asText());
        assertEquals("preservar", salvo.path("campoFuturo").asText());
        assertEquals("Corra!", salvo.path("entradas").get(0).path("traduzido").asText());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante rollback para o estado anterior ao primeiro
     * checkpoint de uma correção longa.
     * <p>INVARIANTES DO DOMÍNIO: checkpoints atualizam o cache, não o backup-base.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo divergente reprova o teste.
     */
    @Test
    void checkpointsNaoSobrescrevemBackupDoInicioDaSessao() throws Exception {
        Path raiz = temp.resolve("cache-checkpoint");
        Path arquivo = raiz.resolve("ep.cache.json");
        Path backups = temp.resolve("backup-checkpoint");
        Files.createDirectories(raiz);
        Files.writeString(arquivo, "[{\"original\":\"Hello\",\"traduzido\":\"\"}]");

        var sessao = new CacheManutencaoService.Sessao(
            raiz.toAbsolutePath(), backups.toAbsolutePath(), "google");
        var doc = service.carregar(arquivo);
        ((ObjectNode) doc.entradas().get(0)).put("traduzido", "Olá");
        Path backup = service.salvarAtomico(doc, sessao);
        ((ObjectNode) doc.entradas().get(0)).put("traduzido", "Olá!");
        service.salvarAtomico(doc, sessao);

        assertEquals("", mapper.readTree(backup.toFile()).get(0).path("traduzido").asText());
        assertEquals("Olá!", mapper.readTree(arquivo.toFile()).get(0).path("traduzido").asText());
    }

    @Test
    void estruturaInvalidaEhRejeitadaSemAlterarOriginal() throws Exception {
        Path arquivo = temp.resolve("invalido.cache.json");
        String original = "{\"semEntradas\":true}";
        Files.writeString(arquivo, original);

        assertThrows(IOException.class, () -> service.carregar(arquivo));
        assertEquals(original, Files.readString(arquivo));
    }

    @Test
    void listagemNaoMisturaCacheEspecificoDeKaraoke() throws Exception {
        Path raiz = temp.resolve("cache-lista");
        Files.createDirectories(raiz.resolve("karaoke"));
        Files.createDirectories(raiz.resolve("anime"));
        Files.writeString(raiz.resolve("karaoke/musica.cache.json"), "[]");
        Files.writeString(raiz.resolve("anime/episodio.cache.json"), "[]");

        var arquivos = service.listarCachesTraducaoBase(raiz);

        assertEquals(1, arquivos.size());
        assertEquals("episodio.cache.json", arquivos.getFirst().getFileName().toString());
    }
}
