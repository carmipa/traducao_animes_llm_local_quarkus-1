package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o fluxo LEGADO de cache — o usado pelo karaokê —
 * preserva um JSON ilegível antes de devolver mapa vazio.
 *
 * <p>A assimetria era perigosa: o fluxo versionado sempre copiou o arquivo quebrado
 * para {@code .corrompido_<ts>.json}; o legado apenas logava e devolvia vazio. Como
 * mapa vazio faz o chamador traduzir do zero e GRAVAR por cima, o arquivo ilegível era
 * destruído na mesma execução, sem cópia — perda total de um artefato que representa
 * horas de GPU e que, quebrado, ainda podia ser recuperado à mão.
 *
 * <p>INVARIANTES COBERTAS: a cópia existe; o caminho ativo fica livre para a nova
 * gravação; cache válido continua sendo lido normalmente (a mudança não pode
 * transformar leitura boa em preservação).
 */
class CacheLegadoPreservaCorrompidoTest {

    private final CacheTraducaoService servico = new CacheTraducaoService(new ObjectMapper());

    @Test
    @DisplayName("JSON ilegível é COPIADO para .corrompido antes de virar mapa vazio")
    void preservaAntesDeDevolverVazio(@TempDir Path dir) throws IOException {
        Path cache = dir.resolve("ep01.cache.json");
        Files.writeString(cache, "isto nao e json { ] :", StandardCharsets.UTF_8);

        Map<String, String> mapa = servico.carregar(cache);

        assertTrue(mapa.isEmpty(), "cache ilegível deve resultar em mapa vazio");
        try (var arquivos = Files.list(dir)) {
            boolean temCopia = arquivos.anyMatch(p -> p.getFileName().toString().contains("corrompido"));
            assertTrue(temCopia, "o JSON quebrado precisa sobreviver como .corrompido_*; "
                + "sem isso a próxima gravação o destrói");
        }
    }

    @Test
    @DisplayName("o caminho ativo fica livre — a próxima gravação não colide com o quebrado")
    void caminhoAtivoLiberado(@TempDir Path dir) throws IOException {
        Path cache = dir.resolve("ep02.cache.json");
        Files.writeString(cache, "{{{ quebrado", StandardCharsets.UTF_8);

        servico.carregar(cache);

        assertFalse(Files.exists(cache),
            "o arquivo quebrado deve sair do caminho ativo, não ser sobrescrito depois");
    }

    @Test
    @DisplayName("cache válido continua sendo lido — a mudança não afeta o caminho feliz")
    void cacheValidoNaoEhPreservado(@TempDir Path dir) throws IOException {
        Path cache = dir.resolve("ep03.cache.json");
        Files.writeString(cache,
            "[{\"indice\":0,\"estilo\":\"Default\",\"original\":\"Hello\",\"traduzido\":\"Olá\","
                + "\"idiomaOriginal\":\"en\",\"idiomaTraduzido\":\"pt-br\"}]",
            StandardCharsets.UTF_8);

        Map<String, String> mapa = servico.carregar(cache);

        assertEquals(1, mapa.size());
        assertEquals("Olá", mapa.get("Hello"));
        assertTrue(Files.exists(cache), "cache bom não sai do lugar");
        try (var arquivos = Files.list(dir)) {
            assertFalse(arquivos.anyMatch(p -> p.getFileName().toString().contains("corrompido")),
                "leitura bem-sucedida não pode gerar cópia de corrompido");
        }
    }
}
