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
        // o Karaokê Simples (10) e a Correção de Karaoke (12); numeração +1 após a
        // entrada da Revisão de Concordância como item 8 da Qualidade.
        org.junit.jupiter.api.Assertions.assertTrue(
            grupoPreparacao < grupoTraducao && grupoTraducao < grupoQualidade
                && grupoQualidade < grupoKaraoke && grupoKaraoke < grupoFinalizacao,
            "Ordem dos grupos principais do pipeline ficou inconsistente"
        );
        int itemConcordancia = html.indexOf("data-target=\"revisao-concordancia\"");
        org.junit.jupiter.api.Assertions.assertTrue(
            itemConcordancia > grupoQualidade && itemConcordancia < grupoKaraoke,
            "Revisão de Concordância deve ficar no grupo Qualidade"
        );

        // A NUMERAÇÃO É COBRADA COMO INVARIANTE, NÃO COMO RÓTULO LITERAL.
        //
        // Até 05/08/2026 esta guarda congelava a string exata de cada item
        // ("<span>3. Análise de Legenda</span>"), e isso tinha dois furos: quebrava
        // a cada renomeação sem que nada estivesse errado, e não via item NOVO
        // entrando sem número — que é o defeito real, porque foi assim que nasceu
        // o "4b.". Agora a régua é: no grupo de ordem G, o item de posição N
        // começa com "G.N ". Vale para os itens que existem hoje e para os que
        // ainda não foram escritos.
        verificarNumeracaoPorGrupo(html);

        int itemNovoKaraoke = html.indexOf("data-target=\"novo-karaoke\"");
        int itemTraducaoKaraoke = html.indexOf("data-target=\"traducao-karaoke\"");
        int itemCura = html.indexOf("data-target=\"cura\"");
        org.junit.jupiter.api.Assertions.assertTrue(
            itemNovoKaraoke > grupoKaraoke && itemNovoKaraoke < grupoFinalizacao
                && itemTraducaoKaraoke > grupoKaraoke && itemTraducaoKaraoke < grupoFinalizacao
                && itemCura > grupoKaraoke && itemCura < grupoFinalizacao,
            "Karaokê Simples, Tradução de Karaokê e Correção de Karaokê devem ficar no grupo Karaokê"
        );
        // Decisão 2026-08-05 (revoga a de 2026-07-09): o Karaokê Simples é o ÚLTIMO
        // do grupo. Ele apaga a animação KFX e o resultado não se desfaz, então tudo
        // que precisa da letra — traduzir e corrigir — roda ANTES. Na ordem anterior
        // ele era o primeiro (10.), e o número dizia o contrário da regra seguida na
        // prática. Esta asserção existe para a ordem não voltar sozinha.
        org.junit.jupiter.api.Assertions.assertTrue(
            itemTraducaoKaraoke < itemCura && itemCura < itemNovoKaraoke,
            "O Karaokê Simples é DESTRUTIVO e tem de ser o último do grupo Karaokê: "
                + "traduzir (4.1) e corrigir (4.2) vêm antes de simplificar (4.3)"
        );
        org.junit.jupiter.api.Assertions.assertTrue(
            html.contains("data-modulo=\"traducaoKaraoke\""),
            "Shell do módulo Tradução de Karaokê ausente no index"
        );
    }

    /**
     * Grupos do pipeline na ordem em que se executam. O grupo {@code sistema} fica de fora
     * de propósito: Telemetria, Mapa, Documentação e Sobre não são passos, são consulta, e
     * numerá-los daria a entender que existe um "6." a executar.
     */
    private static final java.util.List<String> GRUPOS_PIPELINE =
        java.util.List.of("preparacao", "traducao", "qualidade", "karaoke", "finalizacao");

    /** Casa o item do menu com o seu rótulo: o {@code <span>} SEM atributo é o texto. */
    private static final java.util.regex.Pattern ITEM_COM_ROTULO = java.util.regex.Pattern.compile(
        "data-target=\"([a-z-]+)\"[\\s\\S]*?<span>([^<]+)</span>");

    /**
     * PROPÓSITO DE NEGÓCIO: o número do menu é a ordem de execução que se segue na prática.
     * Aqui ele é cobrado como REGRA — no grupo de ordem G, o item de posição N começa com
     * {@code "G.N "} — e não como uma lista de rótulos congelados.
     *
     * <p>INVARIANTES DO DOMÍNIO: todo item de grupo do pipeline tem número; o número
     * corresponde à posição REAL no DOM, que é a ordem que o usuário lê. Item novo sem
     * número reprova, e foi assim que nasceu o antigo "4b.". Grupo vazio também reprova.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: a mensagem diz o grupo, o alvo, o número esperado
     * e o rótulo encontrado — para o conserto não exigir abrir o HTML.
     */
    private static void verificarNumeracaoPorGrupo(String html) {
        int inicioNav = html.indexOf("<nav class=\"nav-menu\">");
        int fimNav = html.indexOf("</nav>", inicioNav);
        org.junit.jupiter.api.Assertions.assertTrue(inicioNav >= 0 && fimNav > inicioNav,
            "nav-menu não encontrado no index — sem ele nada aqui pode ser verificado");
        String nav = html.substring(inicioNav, fimNav);

        for (int g = 0; g < GRUPOS_PIPELINE.size(); g++) {
            String grupo = GRUPOS_PIPELINE.get(g);
            int ini = nav.indexOf("data-grupo=\"" + grupo + "\"");
            org.junit.jupiter.api.Assertions.assertTrue(ini >= 0, "Grupo ausente: " + grupo);

            int prox = nav.indexOf("data-grupo=\"", ini + 1);
            String bloco = nav.substring(ini, prox < 0 ? nav.length() : prox);

            java.util.regex.Matcher m = ITEM_COM_ROTULO.matcher(bloco);
            int posicao = 0;
            while (m.find()) {
                posicao++;
                String esperado = (g + 1) + "." + posicao + " ";
                String rotulo = m.group(2);
                org.junit.jupiter.api.Assertions.assertTrue(rotulo.startsWith(esperado),
                    "Numeração fora da regra no grupo \"" + grupo + "\": o item \""
                        + m.group(1) + "\" está na posição " + posicao + ", então o rótulo tem de "
                        + "começar com \"" + esperado + "\" — encontrado: \"" + rotulo + "\". "
                        + "Se a ordem mudou de propósito, renumere o grupo inteiro.");
            }
            org.junit.jupiter.api.Assertions.assertTrue(posicao > 0,
                "Grupo do pipeline sem nenhum item: " + grupo);
        }
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
