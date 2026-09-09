package org.traducao.projeto.qualidadeTraducao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PROPÓSITO DE NEGÓCIO: calibra um instrumento para o ERRO FLUENTE — a tradução gramaticalmente
 * impecável que INVERTE o sentido do original. É a única classe de defeito do projeto para a qual
 * todos os instrumentos existentes dão ZERO: não há eco, não há resíduo em inglês, o idioma está
 * certo, o número sobreviveu, a estrutura ASS está íntegra e o dicionário aprova cada palavra.
 *
 * <h2>Por que isto é MEDIÇÃO e não guarda</h2>
 * A auditoria de terceiro de 2026-09-09 foi explícita: usar detecção semântica como triagem
 * CALIBRADA antes de torná-la bloqueante. Esta classe faz a calibração e devolve os dois números
 * que decidem se vale ligar. Ela não bloqueia nada.
 *
 * <h2>O que já foi medido e REPROVADO antes desta rota</h2>
 * A primeira ideia foi contar negação: inglês com {@code not}/{@code never}/{@code no} cuja
 * tradução não traz {@code não}/{@code nunca}/{@code nenhum}. Medido no acervo em 2026-09-09:
 * <b>201 suspeitas em 4.231 falas (4,8%), e as inspecionadas eram TODAS tradução correta</b> —
 * o português nega no léxico ({@code not fair} → {@code injusto}, {@code No way!} →
 * {@code Impossível!}, {@code Not at all} → {@code De modo algum}). Regra descartada com número.
 *
 * <h2>A rota que sobrou: perguntar ao modelo, e medir o falso positivo</h2>
 * Uma chamada por fala ao LLM local, perguntando se a tradução preserva o sentido. Medido em
 * 2026-09-09 com o {@code aya-expanse-8b} carregado:
 * <pre>
 * 60 pares REAIS do acervo ....... 0 falsos positivos  (limite superior ~5% a 95% de confianca)
 * 14 corrupcoes montadas a mao ... 11 pegas, 3 escapadas
 * </pre>
 * As três que escapam, para quem for melhorar o prompt: negação perdida sutil
 * ({@code "Don't make me say it again."} → {@code "Faça-me dizer isso novamente."}), verbo
 * trocado ({@code launch} → {@code pousou}) e ESPANHOL lido como português
 * ({@code "Traiga la medicina."}).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O FALSO POSITIVO é o número que manda. Reprovar tradução correta devolve a fala ao inglês
 *       na legenda, e alarme falso ensina a desligar o alarme. Por isso a asserção dura é sobre
 *       as BOAS, e a das corrompidas é um piso frouxo.</li>
 *   <li>O conjunto de referência é PAREADO nas 10 primeiras: mesma fala, só a semântica muda.
 *       Assunto e comprimento iguais dos dois lados, então a diferença medida é semântica.</li>
 *   <li>Sem LLM carregado a medição se ABSTÉM em vez de falhar — "não consegui medir" não pode
 *       virar "medi e passou".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falso positivo acima do teto reprova, com a fala colada no diagnóstico. LLM ausente abstém.
 */
@DisplayName("Erro fluente: calibração do instrumento contra conjunto de referência")
class MedicaoErroFluenteIT {

    private static final String API = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String CATALOGO = "http://127.0.0.1:1234/api/v0/models";
    private static final String RECURSO = "/qualidade/referencia-erro-fluente.json";

    /**
     * Teto de falso positivo. Medido 0 em 60 pares; o teto de 5% é o limite superior estatístico
     * dessa medição, não uma folga escolhida. Subir este número exige medir de novo, não opinar.
     */
    private static final double TETO_FALSO_POSITIVO_PCT = 5.0;

    /** Token de template que o LM Studio às vezes deixa vazar no conteúdo. */
    private static final Pattern TOKEN_DE_CONTROLE = Pattern.compile("<\\|[^|]*\\|>");

    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).build();

    private String modeloCarregado() {
        try {
            HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(CATALOGO)).GET()
                    .timeout(Duration.ofSeconds(10)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            for (JsonNode m : om.readTree(r.body()).path("data")) {
                if ("loaded".equals(m.path("state").asText())) {
                    return m.path("id").asText();
                }
            }
        } catch (Exception fora) {
            return null;
        }
        return null;
    }

    /** FIEL / INFIEL, ou {@code null} quando o modelo respondeu fora do contrato. */
    private String julgar(String modelo, String en, String pt) {
        String prompt = "Voce confere fidelidade de traducao de legenda de anime.\n"
            + "ORIGINAL (ingles): " + en + "\n"
            + "TRADUCAO (portugues): " + pt + "\n\n"
            + "A traducao preserva o SENTIDO do original? Ignore estilo e escolha de palavras.\n"
            + "Responda APENAS uma palavra: FIEL ou INFIEL.";
        try {
            String corpo = om.writeValueAsString(om.createObjectNode()
                .put("model", modelo).put("temperature", 0).put("max_tokens", 6)
                .set("messages", om.createArrayNode().add(om.createObjectNode()
                    .put("role", "user").put("content", prompt))));
            HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(API))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(120)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String texto = TOKEN_DE_CONTROLE
                .matcher(om.readTree(r.body()).at("/choices/0/message/content").asText(""))
                .replaceAll("").trim().toUpperCase(java.util.Locale.ROOT);
            if (texto.startsWith("INFIEL")) {
                return "INFIEL";
            }
            if (texto.startsWith("FIEL")) {
                return "FIEL";
            }
            return null;
        } catch (Exception fora) {
            return null;
        }
    }

    @Test
    @DisplayName("o instrumento cala nas boas e fala nas corrompidas")
    void calibrarInstrumentoDeErroFluente() throws Exception {
        String modelo = modeloCarregado();
        assumeTrue(modelo != null,
            "sem LLM carregado em 127.0.0.1:1234 — a medicao se ABSTEM, e abster nao e aprovar");

        JsonNode ref;
        try (InputStream in = getClass().getResourceAsStream(RECURSO)) {
            assertTrue(in != null, "conjunto de referencia ausente: " + RECURSO);
            ref = om.readTree(in);
        }

        List<String> falsosPositivos = new ArrayList<>();
        int boasJulgadas = 0;
        for (JsonNode par : ref.path("boas")) {
            String v = julgar(modelo, par.path("en").asText(), par.path("pt").asText());
            if (v == null) {
                continue;
            }
            boasJulgadas++;
            if ("INFIEL".equals(v)) {
                falsosPositivos.add(par.path("en").asText() + "  ->  " + par.path("pt").asText());
            }
        }

        List<String> escaparam = new ArrayList<>();
        int corrompidasJulgadas = 0;
        for (JsonNode par : ref.path("corrompidas")) {
            String v = julgar(modelo, par.path("en").asText(), par.path("pt").asText());
            if (v == null) {
                continue;
            }
            corrompidasJulgadas++;
            if ("FIEL".equals(v)) {
                escaparam.add(par.path("en").asText() + "  ->  " + par.path("pt").asText());
            }
        }

        assertTrue(boasJulgadas > 0 && corrompidasJulgadas > 0,
            "nenhum par foi julgado: o modelo nao respondeu no contrato, e zero aqui e cegueira");

        double pctFalsoPositivo = falsosPositivos.size() * 100.0 / boasJulgadas;
        int pegas = corrompidasJulgadas - escaparam.size();

        System.out.println();
        System.out.println("=== CALIBRACAO DO INSTRUMENTO DE ERRO FLUENTE (modelo: " + modelo + ") ===");
        System.out.printf("  BOAS julgadas .............. %d%n", boasJulgadas);
        System.out.printf("  FALSOS POSITIVOS ........... %d (%.1f%%, teto %.1f%%)%n",
            falsosPositivos.size(), pctFalsoPositivo, TETO_FALSO_POSITIVO_PCT);
        System.out.printf("  CORROMPIDAS julgadas ....... %d%n", corrompidasJulgadas);
        System.out.printf("  corrupcoes PEGAS ........... %d de %d%n", pegas, corrompidasJulgadas);
        falsosPositivos.forEach(s -> System.out.println("  [FALSO POSITIVO] " + s));
        escaparam.forEach(s -> System.out.println("  [ESCAPOU] " + s));

        assertTrue(pctFalsoPositivo <= TETO_FALSO_POSITIVO_PCT,
            "falso positivo acima do teto: reprovar traducao correta devolve a fala ao INGLES na "
                + "legenda. Falhas: " + falsosPositivos);

        // Piso FROUXO de proposito: a recall pode oscilar com o modelo carregado, e apertar aqui
        // transformaria troca de modelo em build vermelho sem defeito nenhum. O que nao pode
        // oscilar e o instrumento parar de ver: metade das corrupcoes e o minimo para ele servir.
        assertTrue(pegas * 2 >= corrompidasJulgadas,
            "o instrumento pegou menos da metade das corrupcoes montadas a mao: " + escaparam);
    }

    @Test
    @DisplayName("CASO-CONTROLE: o conjunto de referencia e PAREADO nas 10 primeiras")
    void conjuntoDeReferenciaEhPareado() throws Exception {
        JsonNode ref;
        try (InputStream in = getClass().getResourceAsStream(RECURSO)) {
            assertTrue(in != null, "conjunto de referencia ausente: " + RECURSO);
            ref = om.readTree(in);
        }
        // Sem o pareamento, uma diferenca de ASSUNTO ou de COMPRIMENTO entre os dois grupos
        // explicaria o resultado melhor que a semantica, e a calibracao nao valeria nada.
        for (int i = 0; i < 10; i++) {
            assertEquals(ref.path("boas").get(i).path("en").asText(),
                ref.path("corrompidas").get(i).path("en").asText(),
                "o par " + i + " deixou de ser pareado: o original tem de ser o MESMO nos dois lados");
        }
    }
}
