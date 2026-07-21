package org.traducao.projeto.cachetraducao.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a evolução ADITIVA do {@link EntradaCache} com o campo
 * {@code assinaturaContexto} (subfase 6c-i da correção de gênero por contexto de cena):
 * o construtor legado de seis argumentos continua criando entradas sem assinatura, e um
 * cache ANTIGO (JSON sem o campo) é lido com {@code assinaturaContexto == null} — sem bump
 * de schema e sem perder compatibilidade.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Construtor de 6 args ⇒ {@code assinaturaContexto == null}.</li>
 *   <li>JSON legado (6 campos) ⇒ desserializa com {@code assinaturaContexto == null}.</li>
 *   <li>Round-trip preserva a assinatura quando presente.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer quebra de compatibilidade retroativa (falha ao ler o JSON legado, ou perda da
 * assinatura no round-trip) reprova.
 */
class EntradaCacheTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("construtor de 6 args cria entrada sem assinatura contextual (null)")
    void construtorLegadoSemAssinatura() {
        EntradaCache e = new EntradaCache(1, "Default", "Hi", "Oi", "en", "pt-br");
        assertNull(e.assinaturaContexto());
        assertEquals("Oi", e.traduzido());
    }

    @Test
    @DisplayName("construtor de 7 args carrega a assinatura contextual")
    void construtorContextualCarregaAssinatura() {
        EntradaCache e = new EntradaCache(1, "Default", "Hi", "Oi", "en", "pt-br", "abc123");
        assertEquals("abc123", e.assinaturaContexto());
    }

    @Test
    @DisplayName("round-trip Jackson preserva a assinatura quando presente")
    void roundTripComAssinatura() throws Exception {
        EntradaCache original = new EntradaCache(7, "Default", "Thank you.", "Obrigada.", "en", "pt-br", "sig-xyz");
        String json = mapper.writeValueAsString(original);
        EntradaCache lido = mapper.readValue(json, EntradaCache.class);
        assertEquals(original, lido);
        assertEquals("sig-xyz", lido.assinaturaContexto());
    }

    @Test
    @DisplayName("serializacao NON_NULL (como a producao grava): entrada sem assinatura NAO emite o campo")
    void serializaSemCampoQuandoNull() throws Exception {
        // Espelha o escritorCache do CacheTraducaoService, que grava com NON_NULL para o campo
        // aditivo null nao aparecer e o cache legado permanecer estruturalmente identico.
        ObjectMapper naoNulo = mapper.copy()
            .setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        EntradaCache e = new EntradaCache(1, "Default", "Hi", "Oi", "en", "pt-br");
        String json = naoNulo.writeValueAsString(e);
        assertFalse(json.contains("assinaturaContexto"),
            "com NON_NULL o campo null NAO deve aparecer no JSON");
    }

    @Test
    @DisplayName("cache LEGADO (JSON de 6 campos, sem o novo campo) le com assinatura null")
    void leCacheLegadoSemCampo() throws Exception {
        String jsonLegado = "{\"indice\":3,\"estilo\":\"Default\",\"original\":\"Hi\","
            + "\"traduzido\":\"Oi\",\"idiomaOriginal\":\"en\",\"idiomaTraduzido\":\"pt-br\"}";
        EntradaCache lido = mapper.readValue(jsonLegado, EntradaCache.class);
        assertNull(lido.assinaturaContexto(), "cache legado deve ser lido com assinatura null");
        assertEquals("Oi", lido.traduzido());
        assertEquals(3, lido.indice());
    }
}
