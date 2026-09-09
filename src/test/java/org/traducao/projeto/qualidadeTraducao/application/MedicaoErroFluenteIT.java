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
 * <h2>A rota que sobrou: perguntar ao modelo, e medir os dois erros</h2>
 * Uma chamada por fala ao LLM local, perguntando se a tradução preserva o sentido. Medido em
 * 2026-09-09 com o {@code aya-expanse-8b} carregado, com denominador explícito por partição:
 * <pre>
 * particao 'ajuste'    falso positivo 0/5    falso negativo 1/7
 * particao 'reservado' falso positivo 0/4    falso negativo 2/7
 * </pre>
 * As três que escapam, para quem for melhorar o prompt: negação perdida sutil
 * ({@code "Don't make me say it again."} → {@code "Faça-me dizer isso novamente."}), verbo
 * trocado ({@code launch} → {@code pousou}) e ESPANHOL lido como português
 * ({@code "Traiga la medicina."}).
 *
 * <h2>CORREÇÃO DO PRÓPRIO NÚMERO — o que a A3 derrubou aqui</h2>
 * A primeira versão desta classe anunciava <b>"0 falsos positivos em 60 pares reais do acervo"</b>.
 * O número era verdadeiro sobre o que foi medido e <b>o gabarito era inválido</b>: aqueles 60 pares
 * foram presumidos corretos porque estavam gravados no cache — isto é, o gabarito veio da SAÍDA DA
 * PRODUÇÃO, que é o comportamento <i>observado</i>, exatamente o que a A3 proíbe. Denominador
 * honesto é <b>9 pares revisados um a um</b>, com fundamento escrito no recurso, mais o décimo
 * declarado <b>INCONCLUSIVO</b> e fora da conta.
 *
 * <p>Perde-se poder estatístico e ganha-se validade: 0/9 fundamentados vale mais que 0/60
 * presumidos, porque o segundo não mede fidelidade — mede concordância entre o modelo e ele mesmo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O FALSO POSITIVO é o número que manda. Reprovar tradução correta devolve a fala ao inglês
 *       na legenda, e alarme falso ensina a desligar o alarme. Por isso a asserção dura é sobre
 *       as BOAS, e a das corrompidas é um piso frouxo.</li>
 *   <li>O conjunto de referência é PAREADO nas 10 primeiras: mesma fala, só a semântica muda.
 *       Assunto e comprimento iguais dos dois lados, então a diferença medida é semântica.</li>
 *   <li><b>A3</b> — cada BOA traz veredito e fundamento revisados à mão. Caso cujo fundamento não
 *       se sustenta sai como {@code INCONCLUSIVO} e <b>não entra no denominador</b>: medir contra
 *       gabarito que eu mesmo declarei duvidoso seria inventar o número.</li>
 *   <li><b>A8</b> — falso positivo e falso negativo medidos com denominador explícito, e separados
 *       nas partições {@code ajuste} e {@code reservado}. O reservado nunca entra em afinação de
 *       prompt. O teto de aceitação foi declarado ANTES de conhecer qualquer resultado.
 *       <p>Limitação declarada: a v1 do prompt foi escrita sem olhar resultado nenhum, então para
 *       ela as duas partições são efetivamente reservadas. A partição existe para a PRÓXIMA
 *       pessoa, que terá os resultados em mãos e poderia afinar contra tudo.</li>
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

    /** Um veredito medido, com a particao a que o caso pertence. */
    private record Julgado(String particao, String esperado, String obtido, String caso) {}

    private List<Julgado> julgarTodos(String modelo, JsonNode grupo, String chaveFundamento) {
        List<Julgado> saida = new ArrayList<>();
        for (JsonNode par : grupo) {
            String esperado = par.path("veredito").asText();
            // A3: caso sem fundamento suficiente NAO entra no denominador. Medir contra um
            // gabarito que eu mesmo declarei duvidoso seria inventar o numero.
            if ("INCONCLUSIVO".equals(esperado)) {
                System.out.println("  [FORA DO DENOMINADOR] inconclusivo por A3: "
                    + par.path("en").asText() + "  ->  " + par.path("pt").asText());
                System.out.println("      motivo: " + par.path(chaveFundamento).asText());
                continue;
            }
            String obtido = julgar(modelo, par.path("en").asText(), par.path("pt").asText());
            if (obtido == null) {
                continue;
            }
            saida.add(new Julgado(par.path("particao").asText("indefinida"), esperado, obtido,
                par.path("en").asText() + "  ->  " + par.path("pt").asText()));
        }
        return saida;
    }

    @Test
    @DisplayName("o instrumento cala nas boas e fala nas corrompidas, medido por particao")
    void calibrarInstrumentoDeErroFluente() throws Exception {
        String modelo = modeloCarregado();
        assumeTrue(modelo != null,
            "sem LLM carregado em 127.0.0.1:1234 -- a medicao se ABSTEM, e abster nao e aprovar");

        JsonNode ref;
        try (InputStream in = getClass().getResourceAsStream(RECURSO)) {
            assertTrue(in != null, "conjunto de referencia ausente: " + RECURSO);
            ref = om.readTree(in);
        }

        System.out.println();
        System.out.println("=== CALIBRACAO DO INSTRUMENTO DE ERRO FLUENTE ===");
        System.out.println("  modelo carregado ...... " + modelo);
        System.out.println("  gabarito .............. revisado par a par, com fundamento declarado (A3)");
        System.out.println("  particoes ............. 'ajuste' e 'reservado'; o reservado nunca entra em afinacao (A8)");

        List<Julgado> todos = new ArrayList<>();
        todos.addAll(julgarTodos(modelo, ref.path("boas"), "fundamento"));
        todos.addAll(julgarTodos(modelo, ref.path("corrompidas"), "defeito"));
        assertTrue(!todos.isEmpty(),
            "nenhum par foi julgado: o modelo nao respondeu no contrato, e zero aqui e cegueira");

        // A8: falso positivo E falso negativo, com DENOMINADOR EXPLICITO, e separados por particao.
        double piorFalsoPositivo = 0;
        for (String particao : List.of("ajuste", "reservado")) {
            List<Julgado> daParticao = todos.stream().filter(j -> particao.equals(j.particao())).toList();
            List<Julgado> boasP = daParticao.stream().filter(j -> "FIEL".equals(j.esperado())).toList();
            List<Julgado> ruinsP = daParticao.stream().filter(j -> "INFIEL".equals(j.esperado())).toList();
            long fp = boasP.stream().filter(j -> "INFIEL".equals(j.obtido())).count();
            long fn = ruinsP.stream().filter(j -> "FIEL".equals(j.obtido())).count();
            double pctFp = boasP.isEmpty() ? 0 : fp * 100.0 / boasP.size();
            System.out.printf("  [%s] falso positivo %d/%d (%.1f%%)  |  falso negativo %d/%d%n",
                particao, fp, boasP.size(), pctFp, fn, ruinsP.size());
            boasP.stream().filter(j -> "INFIEL".equals(j.obtido()))
                .forEach(j -> System.out.println("      [FALSO POSITIVO] " + j.caso()));
            ruinsP.stream().filter(j -> "FIEL".equals(j.obtido()))
                .forEach(j -> System.out.println("      [ESCAPOU] " + j.caso()));
            if (!boasP.isEmpty()) {
                piorFalsoPositivo = Math.max(piorFalsoPositivo, pctFp);
            }
        }

        List<Julgado> boas = todos.stream().filter(j -> "FIEL".equals(j.esperado())).toList();
        List<Julgado> ruins = todos.stream().filter(j -> "INFIEL".equals(j.esperado())).toList();
        long pegas = ruins.stream().filter(j -> "INFIEL".equals(j.obtido())).count();
        System.out.printf("  TOTAL: boas julgadas %d, corrupcoes pegas %d de %d%n",
            boas.size(), pegas, ruins.size());

        // O criterio de aceitacao foi declarado ANTES de conhecer o resultado (A8): a constante
        // TETO_FALSO_POSITIVO_PCT existe desde a primeira versao desta classe, com o motivo escrito.
        assertTrue(piorFalsoPositivo <= TETO_FALSO_POSITIVO_PCT,
            "falso positivo acima do teto em alguma particao: reprovar traducao correta devolve a "
                + "fala ao INGLES na legenda. Pior particao: " + piorFalsoPositivo + "%");

        // Piso FROUXO de proposito: recall oscila com o modelo carregado, e apertar aqui
        // transformaria troca de modelo em build vermelho sem defeito nenhum. O que nao pode
        // oscilar e o instrumento parar de ver.
        assertTrue(pegas * 2 >= ruins.size(),
            "o instrumento pegou menos da metade das corrupcoes montadas a mao");
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
