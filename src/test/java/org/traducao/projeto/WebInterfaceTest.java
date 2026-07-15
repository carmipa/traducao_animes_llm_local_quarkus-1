package org.traducao.projeto;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class WebInterfaceTest {

    @Test
    void indexHtmlDisponivel() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("KRONOS CORE"));
    }

    @Test
    void cssDisponivel() {
        given()
            .when().get("/css/base.css")
            .then()
            .statusCode(200)
            .contentType(containsString("text/css"))
            .body(containsString("--bg-primary"));
    }

    @Test
    void appJsDisponivel() {
        given()
            .when().get("/js/app.js")
            .then()
            .statusCode(200)
            .contentType(containsString("javascript"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a distribuição web entrega o tradutor
     * Google client-side e as três bandeiras sem depender de classes Java.
     * INVARIANTES DO DOMÍNIO: Brasil, Estados Unidos e Espanha permanecem
     * acessíveis no HTML e o módulo i18n é servido como JavaScript.
     * COMPORTAMENTO EM CASO DE FALHA: recurso ausente ou seletor removido
     * reprova a suíte antes da publicação do JAR.
     */
    @Test
    void internacionalizacaoAutomaticaDisponivel() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("data-idioma=\"pt-BR\""))
            .body(containsString("data-idioma=\"en-US\""))
            .body(containsString("data-idioma=\"es-ES\""))
            .body(containsString("src=\"i18n/flags/br.svg\""))
            .body(containsString("src=\"i18n/flags/us.svg\""))
            .body(containsString("src=\"i18n/flags/es.svg\""));

        given()
            .when().get("/i18n/i18n.js")
            .then()
            .statusCode(200)
            .contentType(containsString("javascript"))
            .body(containsString("translate.google.com/translate_a/element.js"))
            .body(containsString("googtrans"))
            .body(containsString("navigator.languages"))
            .body(containsString("notranslate"))
            .body(containsString("#panel-telemetria"))
            .body(containsString("[id*=\"resultado\"]"));

        given().when().get("/i18n/flags/br.svg").then().statusCode(200);
        given().when().get("/i18n/flags/us.svg").then().statusCode(200);
        given().when().get("/i18n/flags/es.svg").then().statusCode(200);
      }

    @Test
    void logoDisponivel() {
        given()
            .when().get("/img/kronos_logo.svg")
            .then()
            .statusCode(200)
            .contentType(containsString("svg"));
    }

    @Test
    void modulosJsDisponiveis() {
        String[] modulos = {
            "/analise/analise.js",
            "/extracao/extracao.js",
            "/traducao/traducao.js",
            "/correcao/correcao.js",
            "/revisao/revisao.js",
            "/cura/cura.js",
            "/revisaoLore/revisaoLore.js",
            "/auditorConteudoLegendas/auditorConteudoLegendas.js",
            "/remuxer/remuxer.js",
            "/mapa/mapa.js",
            "/telemetria/telemetria.js"
        };
        for (String modulo : modulos) {
            given()
                .when().get(modulo)
                .then()
                .statusCode(200)
                .contentType(containsString("javascript"));
        }
    }

    @Test
    void revisaoLoreHtmlDisponivel() {
        given()
            .when().get("/revisaoLore/revisaoLore.html")
            .then()
            .statusCode(200)
            .contentType(containsString("html"))
            .body(containsString("Revisão de Lore"));
    }

    @Test
    void indexContemRevisaoLore() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("data-modulo=\"revisaoLore\""));
    }

    @Test
    void indexContemAuditorConteudoModulo() {
        given()
            .when().get("/")
            .then()
            .statusCode(200)
            .body(containsString("data-modulo=\"auditorConteudoLegendas\""));
    }

    @Test
    void auditorConteudoHtmlDisponivel() {
        given()
            .when().get("/auditorConteudoLegendas/auditorConteudoLegendas.html")
            .then()
            .statusCode(200)
            .contentType(containsString("html"))
            .body(containsString("Análise de Conteúdo de Legendas"))
            .body(containsString("id=\"btn-exportar-auditor-md\""))
            .body(containsString("Relatório de Anomalias"));
    }

    @Test
    void indexSidebarComEstruturaNavMenuValida() {
        String html = given()
            .when().get("/")
            .then()
            .statusCode(200)
            .extract().asString();

        int abreNavMenu = html.split("<nav class=\"nav-menu\">", -1).length - 1;
        int fechaNav = html.split("</nav>", -1).length - 1;
        org.junit.jupiter.api.Assertions.assertEquals(1, abreNavMenu, "Deve haver exatamente um nav-menu");
        org.junit.jupiter.api.Assertions.assertTrue(fechaNav >= 1, "nav-menu deve fechar corretamente");

        String[] grupos = {"preparacao", "karaoke", "traducao", "qualidade", "finalizacao", "sistema"};
        for (String grupo : grupos) {
            org.junit.jupiter.api.Assertions.assertTrue(
                html.contains("data-grupo=\"" + grupo + "\""),
                "Grupo do menu ausente: " + grupo
            );
            org.junit.jupiter.api.Assertions.assertTrue(
                html.contains("data-grupo=\"" + grupo + "\"") && html.contains("nav-group-itens"),
                "Menu deve usar nav-group-itens nos grupos"
            );
        }

        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("data-target=\"auditor-conteudo\""),
            "Item de menu da auditoria de conteudo ausente"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("data-modulo=\"auditorConteudoLegendas\""),
            "Shell do modulo auditoria de conteudo ausente no index"
        );

        int grupoPreparacao = html.indexOf("data-grupo=\"preparacao\"");
        int grupoTraducao = html.indexOf("data-grupo=\"traducao\"");
        int grupoQualidade = html.indexOf("data-grupo=\"qualidade\"");
        int grupoKaraoke = html.indexOf("data-grupo=\"karaoke\"");
        int grupoFinalizacao = html.indexOf("data-grupo=\"finalizacao\"");
        int itemAuditor = html.indexOf("data-target=\"auditor-conteudo\"");
        // Análise de Legenda (auditoria de conteúdo de legendas) é o item 3 do
        // grupo Preparação (após 1. Análise de Mídia e 2. Extração).
        org.junit.jupiter.api.Assertions.assertTrue(
            itemAuditor > grupoPreparacao && itemAuditor < grupoTraducao,
            "Análise de Legenda deve ficar no grupo Preparação"
        );
        // Decisão 2026-07-08: grupo Karaokê entre Qualidade e Finalização, com
        // o Karaokê Simples (9) e a Correção de Karaoke (10, ex-item 7 da Qualidade).
        org.junit.jupiter.api.Assertions.assertTrue(
            grupoPreparacao < grupoTraducao && grupoTraducao < grupoQualidade
                && grupoQualidade < grupoKaraoke && grupoKaraoke < grupoFinalizacao,
            "Ordem dos grupos principais do pipeline ficou inconsistente"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("<span>3. Análise de Legenda</span>"),
            "Numeração da Análise de Legenda deve refletir o item 3 da Preparação"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("<span>4. Tradução Local</span>")
                && html.contains("<span>5. Correção Cache</span>"),
            "Numeração do grupo Tradução (4 e 5) ausente"
        );
        int itemNovoKaraoke = html.indexOf("data-target=\"novo-karaoke\"");
        int itemTraducaoKaraoke = html.indexOf("data-target=\"traducao-karaoke\"");
        int itemCura = html.indexOf("data-target=\"cura\"");
        org.junit.jupiter.api.Assertions.assertTrue(
            itemNovoKaraoke > grupoKaraoke && itemNovoKaraoke < grupoFinalizacao
                && itemTraducaoKaraoke > grupoKaraoke && itemTraducaoKaraoke < grupoFinalizacao
                && itemCura > grupoKaraoke && itemCura < grupoFinalizacao,
            "Karaokê Simples, Tradução de Karaokê e Correção de Karaoke devem ficar no grupo Karaokê"
        );
        // Decisão 2026-07-09: Tradução de Karaokê é o item 10, logo após o
        // Karaokê Simples (converte KFX → depois traduz a letra), empurrando
        // Correção de Karaoke para 11 e a Finalização para 12/13.
        org.junit.jupiter.api.Assertions.assertTrue(
            itemNovoKaraoke < itemTraducaoKaraoke && itemTraducaoKaraoke < itemCura,
            "Tradução de Karaokê deve ficar entre Karaokê Simples e Correção de Karaoke"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("<span>9. Karaokê Simples</span>")
                && html.contains("<span>10. Tradução de Karaokê</span>")
                && html.contains("<span>11. Correção de Karaoke</span>"),
            "Numeração do grupo Karaokê (9, 10 e 11) ausente"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("<span>12. Remuxer</span>")
                && html.contains("<span>13. Renomear Arquivos</span>"),
            "Numeração da Finalização (12 e 13) ausente"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("data-modulo=\"traducaoKaraoke\""),
            "Shell do módulo Tradução de Karaokê ausente no index"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que a opção 10 entregue o formulário e o console
     * usados para acompanhar a tradução de karaokê em tempo real no navegador.
     *
     * INVARIANTES DO DOMÍNIO: o HTML deve conter o terminal dedicado e o orquestrador
     * deve rotear o canal SSE {@code traducao-karaoke} exclusivamente para ele.
     *
     * COMPORTAMENTO EM CASO DE FALHA: qualquer recurso ausente, resposta HTTP inválida
     * ou contrato de roteamento removido faz o teste falhar antes da publicação.
     */
    @Test
    void traducaoKaraokeHtmlEJsDisponiveis() {
        given()
            .when().get("/traducaoKaraoke/traducaoKaraoke.html")
            .then()
            .statusCode(200)
            .contentType(containsString("html"))
            .body(containsString("Tradução de Karaokê"))
            .body(containsString("id=\"traducao-karaoke-contexto\""))
            .body(containsString("id=\"traducao-karaoke-entrada\""))
            .body(containsString("id=\"console-traducao-karaoke\""));

        given()
            .when().get("/traducaoKaraoke/traducaoKaraoke.js")
            .then()
            .statusCode(200)
            .contentType(containsString("javascript"));

        given()
            .when().get("/js/app.js")
            .then()
            .statusCode(200)
            .contentType(containsString("javascript"))
            .body(containsString("'traducao-karaoke': 'console-traducao-karaoke'"))
            .body(containsString("traducao-karaoke:painel-carregado"));
    }
}
