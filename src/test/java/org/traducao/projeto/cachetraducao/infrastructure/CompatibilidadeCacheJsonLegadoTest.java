package org.traducao.projeto.cachetraducao.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.cachetraducao.domain.EntradaCache;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: gate de compatibilidade retroativa da E6. Prova que um
 * arquivo {@code .cache.json} produzido ANTES da extração do peer {@code cachetraducao}
 * continua legível pelos tipos pós-move, sem depender de nenhum FQN antigo — garantindo
 * que a migração de pacote NÃO quebra os caches já persistidos no disco dos usuários.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A fixture é textual e estável ({@code src/test/resources/cachetraducao/legado.cache.json}),
 *       NÃO gerada pelas classes pós-move — caracteriza o schema histórico.</li>
 *   <li>Desserialização por campos (sem tipagem polimórfica): o JSON não carrega
 *       {@code @class}/discriminador, logo o nome do pacote é irrelevante para a leitura.</li>
 *   <li>Regravação mantém o mesmo schema (chaves/valores), comparado estruturalmente
 *       (não por igualdade textual de espaços/ordem).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer campo ausente/divergente ou schema alterado na regravação reprova o teste —
 * sinal de que a E6 quebrou a compatibilidade do cache.
 */
@DisplayName("E6: compatibilidade do .cache.json legado com os tipos pós-move")
class CompatibilidadeCacheJsonLegadoTest {

    private static final String FIXTURE = "/cachetraducao/legado.cache.json";

    private final ObjectMapper mapper = new ObjectMapper();

    private byte[] lerFixture() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(FIXTURE)) {
            assertNotNull(in, "Fixture legada não encontrada: " + FIXTURE);
            return in.readAllBytes();
        }
    }

    @Test
    @DisplayName("lê a fixture legada com CacheDocumento pós-move e valida todos os campos")
    void leFixtureLegadaComTiposPosMove() throws Exception {
        CacheDocumento doc = mapper.readValue(lerFixture(), CacheDocumento.class);

        assertNotNull(doc);
        ProvenienciaCache p = doc.proveniencia();
        assertNotNull(p, "proveniência deve ser lida");
        assertEquals(1, p.schemaVersion(), "versão do schema");
        assertEquals("gundam-08th-ms-team", p.contextoId(), "lore/contextoId");
        assertEquals("9f2c1a7b4e6d8f0a2c4e6b8d0f1a3c5e7b9d1f3a5c7e9b1d3f5a7c9e1b3d5f70", p.contextoHash(), "hash");
        assertEquals("mistral-small", p.modeloLlm(), "modelo");
        assertEquals("en", p.idiomaOrigem(), "idioma de origem");
        assertEquals("pt-br", p.idiomaDestino(), "idioma de destino");

        assertEquals(2, doc.entradas().size(), "número de entradas");
        EntradaCache e0 = doc.entradas().get(0);
        assertEquals(0, e0.indice(), "índice");
        assertEquals("Default", e0.estilo(), "estilo");
        assertEquals("Federation forces, retreat!", e0.original(), "texto original");
        assertEquals("Forças da Federação, recuar!", e0.traduzido(), "texto traduzido");
        assertEquals("en", e0.idiomaOriginal(), "idioma original da entrada");
        assertEquals("pt-br", e0.idiomaTraduzido(), "idioma traduzido da entrada");
    }

    @Test
    @DisplayName("regravação mantém o schema esperado (comparação estrutural, não textual)")
    void regravacaoMantemSchema() throws Exception {
        JsonNode original = mapper.readTree(lerFixture());
        CacheDocumento doc = mapper.readValue(lerFixture(), CacheDocumento.class);

        // Regrava a partir dos tipos pós-move e relê como árvore. Usa NON_NULL, EXATAMENTE como
        // o CacheTraducaoService grava em produção (escritorCache), para que o campo aditivo e
        // nulável assinaturaContexto (null no fluxo legado/desligado) NÃO apareça — mantendo o
        // cache legado estruturalmente idêntico.
        ObjectMapper escritor = mapper.copy()
            .setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
        byte[] regravado = escritor.writeValueAsBytes(doc);
        JsonNode releitura = mapper.readTree(regravado);

        // Comparação estrutural: as árvores JSON devem ser equivalentes em chaves/valores,
        // independentemente de espaços em branco ou ordem de propriedades.
        assertEquals(original, releitura, "o schema regravado deve ser estruturalmente idêntico ao legado");
        assertTrue(releitura.has("proveniencia") && releitura.has("entradas"),
            "envelope versionado deve preservar 'proveniencia' e 'entradas'");
    }
}
