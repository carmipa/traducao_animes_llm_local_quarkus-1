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

    /**
     * PROPÓSITO DE NEGÓCIO: a regravação não pode perder nem alterar nada do schema legado.
     *
     * <h2>Duas coisas diferentes, separadas em 12/08/2026</h2>
     * A versão anterior exigia igualdade estrutural TOTAL, o que confundia dois eventos de
     * gravidade oposta:
     * <ul>
     *   <li><b>campo legado sumiu ou mudou de valor</b> — quebra os caches no disco dos usuários,
     *       e continua reprovando aqui, campo a campo;</li>
     *   <li><b>apareceu campo novo VAZIO</b> — evolução aditiva, que a leitura antiga ignora e a
     *       comparação de proveniência não consulta.</li>
     * </ul>
     * A distinção nasceu do campo {@code modeloHerdado} (reuso entre modelos): serializado por um
     * mapper cru ele aparece como {@code null}. O {@code CacheTraducaoService} o omite via mixin, e
     * é por isso que o ARQUIVO real continua idêntico ao legado — mas este teste caracteriza o
     * tipo com um mapper qualquer, e tem de tolerar o aditivo sem deixar de pegar o destrutivo.
     *
     * <p><b>Campo novo com VALOR preenchido continua reprovando</b>: seria schema novo de fato,
     * não evolução compatível.
     */
    @Test
    @DisplayName("regravação preserva todo campo legado; campo novo só é tolerado se vier vazio")
    void regravacaoMantemSchema() throws Exception {
        JsonNode original = mapper.readTree(lerFixture());
        CacheDocumento doc = mapper.readValue(lerFixture(), CacheDocumento.class);

        byte[] regravado = mapper.writeValueAsBytes(doc);
        JsonNode releitura = mapper.readTree(regravado);

        assertTrue(releitura.has("proveniencia") && releitura.has("entradas"),
            "envelope versionado deve preservar 'proveniencia' e 'entradas'");
        conferirCompatibilidade(original, releitura, "$");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: percorre as duas árvores exigindo que TUDO do legado sobreviva
     * idêntico, e que o que for novo esteja vazio.
     *
     * <p>INVARIANTES DO DOMÍNIO: recursivo em objetos e arrays; folhas comparadas por igualdade
     * exata. Um campo do legado ausente na releitura reprova; um campo novo NÃO nulo reprova.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a mensagem traz o caminho JSON exato do desvio.
     */
    private static void conferirCompatibilidade(JsonNode legado, JsonNode atual, String caminho) {
        if (legado.isObject()) {
            assertTrue(atual.isObject(), caminho + ": deixou de ser objeto");
            legado.fieldNames().forEachRemaining(campo -> {
                assertTrue(atual.has(campo),
                    caminho + "." + campo + ": campo do schema legado SUMIU da regravação — "
                        + "isto quebra os caches já gravados no disco");
                conferirCompatibilidade(legado.get(campo), atual.get(campo), caminho + "." + campo);
            });
            atual.fieldNames().forEachRemaining(campo -> {
                if (!legado.has(campo)) {
                    assertTrue(atual.get(campo).isNull(),
                        caminho + "." + campo + ": campo NOVO com valor \"" + atual.get(campo)
                            + "\". Evolução aditiva só é compatível enquanto o campo vem vazio; "
                            + "com valor, é schema novo e exige decisão explícita");
                }
            });
            return;
        }
        if (legado.isArray()) {
            assertEquals(legado.size(), atual.size(), caminho + ": tamanho do array mudou");
            for (int i = 0; i < legado.size(); i++) {
                conferirCompatibilidade(legado.get(i), atual.get(i), caminho + "[" + i + "]");
            }
            return;
        }
        assertEquals(legado, atual, caminho + ": valor do schema legado mudou");
    }

    /**
     * CONTRAPROVA da tolerância acima (regra 9): o instrumento precisa ter sido visto REPROVANDO.
     * Um campo novo com VALOR preenchido não é evolução aditiva, e tem de derrubar a catraca.
     */
    @Test
    @DisplayName("contraprova: campo novo COM valor reprova, e campo legado ausente também")
    void aToleranciaNaoAceitaSchemaNovoDeVerdade() throws Exception {
        JsonNode legado = mapper.readTree(lerFixture());

        com.fasterxml.jackson.databind.node.ObjectNode comCampoNovo = legado.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) comCampoNovo.get("proveniencia"))
            .put("modeloHerdado", "mistral-nemo");
        assertTrue(erroAoConferir(legado, comCampoNovo).contains("campo NOVO com valor"),
            "campo novo PREENCHIDO tem de reprovar — senão a tolerância vira porta aberta");

        com.fasterxml.jackson.databind.node.ObjectNode semCampoLegado = legado.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) semCampoLegado.get("proveniencia"))
            .remove("modeloLlm");
        assertTrue(erroAoConferir(legado, semCampoLegado).contains("SUMIU"),
            "campo legado removido tem de reprovar — é o dano que esta catraca existe para pegar");
    }

    private static String erroAoConferir(JsonNode legado, JsonNode atual) {
        try {
            conferirCompatibilidade(legado, atual, "$");
            return "";
        } catch (AssertionError e) {
            return e.getMessage() == null ? "" : e.getMessage();
        }
    }
}
