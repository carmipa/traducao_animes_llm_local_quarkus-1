package org.traducao.projeto;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class ApiControllerTest {

    @Test
    void statusRetornaOnline() {
        given()
            .when().get("/api/status")
            .then()
            .statusCode(200)
            .body("mensagem", is("online"));
    }

    @Test
    void contextosRetornaListaNaoVazia() {
        given()
            .when().get("/api/contextos")
            .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    void traduzirSemEntradaRetornaBadRequest() {
        given()
            .contentType("application/json")
            .body("{\"entrada\":\"\",\"saida\":\"\"}")
            .when().post("/api/traduzir")
            .then()
            .statusCode(400)
            .body("mensagem", is("Pasta de legendas de entrada obrigatória."));
    }
}
