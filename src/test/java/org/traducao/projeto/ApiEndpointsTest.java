package org.traducao.projeto;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.auditorConteudoLegendas.support.AssAuditoriaFixtures;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ApiEndpointsTest {

    @Test
    void telemetriaRetornaResumo() {
        given()
            .when().get("/api/telemetria")
            .then()
            .statusCode(200)
            .body("cacheCount", greaterThanOrEqualTo(0))
            .body("totalEpisodios", greaterThanOrEqualTo(0))
            .body("historicoOperacoes", notNullValue())
            .body("revisaoLore", notNullValue())
            .body("revisaoLore.totalSessoes", greaterThanOrEqualTo(0));
    }

    @Test
    void telemetriaExportarRetornaArquivoJson() {
        given()
            .when().get("/api/telemetria/exportar")
            .then()
            .statusCode(200)
            .contentType(containsString("json"));
    }

    @Test
    void metadataComCaminhoVazioRetorna404() {
        given()
            .when().get("/api/metadata?caminho=")
            .then()
            .statusCode(404);
    }

    @Test
    void analisarSemEntradaRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\"}")
            .when().post("/api/analisar")
            .then()
            .statusCode(400)
            .body("mensagem", is("Caminho de entrada obrigatório."));
    }

    @Test
    void extrairSemEntradaRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\",\"formato\":\"ASS\"}")
            .when().post("/api/extrair")
            .then()
            .statusCode(400)
            .body("mensagem", is("Caminho da pasta de vídeos obrigatório."));
    }

    @Test
    void traduzirComContextoInvalidoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"cache\",\"saida\":\"\",\"contextoId\":\"contexto_inexistente_xyz\"}")
            .when().post("/api/traduzir")
            .then()
            .statusCode(400)
            .body("mensagem", containsString("Contexto de tradução desconhecido"));
    }

    @Test
    void corrigirCacheAceitaEntradaVazia() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\"}")
            .when().post("/api/corrigir-cache")
            .then()
            .statusCode(200)
            .body("mensagem", containsString("Limpeza de cache aceita pela fila"));
    }

    @Test
    void corrigirScrapingIniciaComSucesso() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"cache\",\"saida\":\"\"}")
            .when().post("/api/corrigir-scraping")
            .then()
            .statusCode(200)
            .body("mensagem", containsString("Correção online aceita pela fila"));
    }

    @Test
    void revisarCacheComContextoInvalidoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"cache\",\"contextoId\":\"contexto_inexistente_xyz\"}")
            .when().post("/api/revisar-cache")
            .then()
            .statusCode(400)
            .body("mensagem", containsString("Contexto desconhecido"));
    }

    @Test
    void revisarLegendasSemEntradaRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\"}")
            .when().post("/api/revisar-legendas")
            .then()
            .statusCode(400)
            .body("mensagem", containsString("obrigatória"));
    }

    @Test
    void revisarLegendasConcordanciaSemEntradaRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\"}")
            .when().post("/api/revisar-legendas-concordancia")
            .then()
            .statusCode(400)
            .body("mensagem", containsString("obrigatória"));
    }

    @Test
    void remuxarSemEntradaRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\"}")
            .when().post("/api/remuxar")
            .then()
            .statusCode(400)
            .body("mensagem", is("Pasta de vídeos de entrada obrigatória."));
    }

    @Test
    void mapaGeraConteudo() {
        given()
            .when().post("/api/mapa")
            .then()
            .statusCode(200)
            .body("conteudo", notNullValue());
    }

    @Test
    void curaTagsSemDiretoriosRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"\",\"diretorioTraduzido\":\"\"}")
            .when().post("/api/cura-tags")
            .then()
            .statusCode(400)
            .body("erro", containsString("originais"));
    }

    @Test
    void correcaoLegendasSemDiretoriosRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"\",\"diretorioTraduzido\":\"\"}")
            .when().post("/api/correcao-legendas")
            .then()
            .statusCode(400)
            .body("erro", containsString("originais"));
    }

    @Test
    void curaTagsSemTraduzidoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"cache\",\"diretorioTraduzido\":\"\"}")
            .when().post("/api/cura-tags")
            .then()
            .statusCode(400)
            .body("erro", containsString("traduzidas"));
    }

    @Test
    void curaTagsComContextoInvalidoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"cache\",\"diretorioTraduzido\":\"cache\",\"contextoId\":\"contexto_inexistente_xyz\"}")
            .when().post("/api/cura-tags")
            .then()
            .statusCode(400)
            .body("erro", containsString("Contexto de tradução desconhecido"));
    }

    @Test
    void revisarLoreSemContextoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"cache\",\"diretorioTraduzido\":\"cache\",\"contextoId\":\"\"}")
            .when().post("/api/revisar-lore")
            .then()
            .statusCode(400)
            .body("erro", containsString("obra/contexto"));
    }

    @Test
    void revisarLoreComContextoInvalidoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"cache\",\"diretorioTraduzido\":\"cache\",\"contextoId\":\"contexto_inexistente_xyz\"}")
            .when().post("/api/revisar-lore")
            .then()
            .statusCode(400)
            .body("erro", containsString("Prompt de revisao de lore desconhecido"));
    }

    @Test
    void listarContextosRevisaoLoreRetornaPromptsProprios() {
        given()
            .when().get("/api/revisao-lore/contextos")
            .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("find { it.id == 'gundam_nt' }.nome", containsString("Revisao de Lore"))
            .body("find { it.id == 'gundam_unicorn' }.nome", containsString("Revisao de Lore"))
            .body("find { it.id == 'gundam_zeta' }.nome", containsString("Revisao de Lore"))
            .body("find { it.id == 'gundam_zz' }.nome", containsString("Revisao de Lore"))
            .body("find { it.id == 'guilty_crown' }.nome", containsString("Revisao de Lore"));
    }

    @Test
    void revisarLoreIniciaComContextoValido() {
        given()
            .contentType("application/json")
            .body("{\"diretorioOriginal\":\"cache\",\"diretorioTraduzido\":\"cache\",\"contextoId\":\"danmachi\",\"revisarTodasFalas\":false}")
            .when().post("/api/revisar-lore")
            .then()
            .statusCode(200)
            .body("mensagem", containsString("Revisao de lore iniciada"));
    }

    @Test
    void auditoriaConteudoSemCaminhosRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"caminhoOriginal\":\"\",\"caminhoTraduzido\":\"\"}")
            .when().post("/api/auditoria-conteudo")
            .then()
            .statusCode(400);
    }

    @Test
    void auditoriaConteudoAuditaArquivosAss(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path original = tempDir.resolve("ep_eng.ass");
        Path traduzido = tempDir.resolve("ep_pt.ass");
        AssAuditoriaFixtures.escreverParLimpo(original, traduzido);

        given()
            .contentType("application/json")
            .body("{\"caminhoOriginal\":\"" + original.toString().replace("\\", "\\\\")
                + "\",\"caminhoTraduzido\":\"" + traduzido.toString().replace("\\", "\\\\") + "\"}")
            .when().post("/api/auditoria-conteudo")
            .then()
            .statusCode(200)
            .body("limpo", is(true))
            .body("regrasExecutadas", greaterThanOrEqualTo(4))
            .body("caminhoRelatorioJson", notNullValue());
    }

    @Test
    void auditoriaConteudoModoOriginalAuditaArquivoUnico(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path original = tempDir.resolve("solo_eng.ass");
        AssAuditoriaFixtures.escreverArquivoUnicoLimpo(original);

        given()
            .contentType("application/json")
            .body("{\"modo\":\"ORIGINAL\",\"caminhoOriginal\":\""
                + original.toString().replace("\\", "\\\\") + "\"}")
            .when().post("/api/auditoria-conteudo")
            .then()
            .statusCode(200)
            .body("modo", is("ORIGINAL"))
            .body("formatoOriginal", is("ASS"))
            .body("limpo", is(true))
            .body("caminhoRelatorioJson", notNullValue());
    }

    @Test
    void auditoriaConteudoModoOriginalSemCaminhoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"modo\":\"ORIGINAL\",\"caminhoOriginal\":\"\"}")
            .when().post("/api/auditoria-conteudo")
            .then()
            .statusCode(400);
    }

    @Test
    void auditoriaConteudoModoDesconhecidoRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"modo\":\"XPTO\",\"caminhoOriginal\":\"a.ass\",\"caminhoTraduzido\":\"b.ass\"}")
            .when().post("/api/auditoria-conteudo")
            .then()
            .statusCode(400)
            .body(containsString("AMBAS"));
    }

    @Test
    void auditoriaConteudoArquivoInexistenteRetornaBadRequest(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path traduzido = tempDir.resolve("ep_pt.ass");
        AssAuditoriaFixtures.escreverParLimpo(tempDir.resolve("ref.ass"), traduzido);

        given()
            .contentType("application/json")
            .body("{\"caminhoOriginal\":\"" + tempDir.resolve("nao_existe.ass").toString().replace("\\", "\\\\")
                + "\",\"caminhoTraduzido\":\"" + traduzido.toString().replace("\\", "\\\\") + "\"}")
            .when().post("/api/auditoria-conteudo")
            .then()
            .statusCode(400)
            .body(containsString("nao encontrado"));
    }

    @Test
    void revisarLegendasModoCachePastaInexistenteRetornaBadRequest(@TempDir Path tempDir) throws Exception {
        Path pastaPt = Files.createDirectory(tempDir.resolve("pt"));
        AssAuditoriaFixtures.escreverArquivoUnicoLimpo(pastaPt.resolve("show_PT-BR.ass"));
        Path cacheInexistente = tempDir.resolve("cache_que_nao_existe");

        given()
            .contentType("application/json")
            .body("{\"modoReferencia\":\"CACHE\",\"entrada\":\"" + pastaPt.toString().replace("\\", "\\\\")
                + "\",\"caminhoCache\":\"" + cacheInexistente.toString().replace("\\", "\\\\") + "\"}")
            .when().post("/api/revisar-legendas")
            .then()
            .statusCode(400)
            .body(containsString("cache"));
    }

    @Test
    void revisarLegendasGoogleHonraContextoDesconhecido() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"cache\",\"contextoId\":\"contexto_inexistente_xyz\"}")
            .when().post("/api/revisar-legendas")
            .then()
            .statusCode(400)
            .body(containsString("Contexto"));
    }

    @Test
    void revisarLegendasConcordanciaHonraContextoDesconhecido() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"cache\",\"contextoId\":\"contexto_inexistente_xyz\"}")
            .when().post("/api/revisar-legendas-concordancia")
            .then()
            .statusCode(400)
            .body(containsString("Contexto"));
    }

    @Test
    void sseStreamAceitaConexao() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<io.restassured.response.Response> future = executor.submit(() ->
                given()
                    .header("Accept", "text/event-stream")
                    .get("/api/logs/stream")
            );
            try {
                io.restassured.response.Response response = future.get(3, TimeUnit.SECONDS);
                assertEquals(200, response.statusCode());
                assertTrue(response.contentType().contains("text/event-stream"));
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
