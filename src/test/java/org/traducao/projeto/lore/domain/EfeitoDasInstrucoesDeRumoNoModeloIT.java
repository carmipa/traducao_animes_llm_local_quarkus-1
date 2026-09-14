package org.traducao.projeto.lore.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreYaml;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede o EFEITO das instruções de rumo no modelo local, e não a presença
 * do texto delas no prompt. {@code InstrucaoProaNoPromptTest} e
 * {@code InstrucaoRumoDeRelogioNoPromptTest} conferem que a linha existe — isso é a intenção de
 * quem escreveu. Se o modelo deixar de obedecer, as duas continuam verdes e ninguém sabe.
 *
 * <h2>A lição que obrigou este arquivo a existir</h2>
 * Vem do vault, do binmapper, em 09/09/2026: uma guarda de tela conferia {@code el.hidden} — o
 * ATRIBUTO — enquanto o botão desenhava 157×33 px. <i>Atributo presente não é efeito obtido.</i>
 * Aqui é o mesmo erro com outra roupa: instrução presente no prompt não é instrução obedecida.
 *
 * <h2>Calibração DENTRO da guarda, nos dois sentidos</h2>
 * O braço de controle é o prompt de produção com a linha da instrução REMOVIDA em tempo de
 * execução. Ele precisa <b>reproduzir o defeito</b>; se não reproduzir, esta medição não consegue
 * discriminar e sai como <b>não verificada</b>, jamais como aprovação. É a mesma exigência que o
 * adendo do binmapper cobrou da guarda de UX: plantar o alvo que mente e exigir enxergá-lo.
 *
 * <h2>Premissa ausente é pulo DECLARADO</h2>
 * Sem modelo carregado a medição se abstém dizendo o motivo e o comando que resolve. Vermelho por
 * pré-requisito ausente manda procurar regressão onde não houve, e espera longa ensina a ignorar
 * a suíte.
 *
 * <h2>Os dois casos, e por que estes</h2>
 * Medidos em 14/09/2026 no aya-expanse-8b, temperatura 0.3, e os de separação mais limpa entre
 * os braços:
 * <pre>
 * "Heading 2-8-0. Distance 5,000."   sem instrucao "Titulo: 2-8-0"   com "Direcao: 2-8-0"
 * "Two o'clock! Two enemy vessels"   sem instrucao "Meia-noite!"      com "2 horas!"
 * </pre>
 * Cada um usa a lore da PRÓPRIA obra, como em produção.
 *
 * <h2>As duas instruções não são redundantes — medido</h2>
 * No caso do relógio, 5 repetições por arranjo:
 * <pre>
 * nenhuma instrucao ... defeito 4/5      so relogio .... defeito 0/5
 * so proa ............ defeito 2/5      as duas ....... defeito 0/5
 * </pre>
 * A de proa generaliza em parte, e não substitui a do relógio. É por isso que o braço de
 * controle do caso do relógio remove <b>as duas</b> linhas: com só uma removida ele fica em 2/5,
 * abaixo da maioria, e a guarda se abstém para sempre.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Maioria de 5 repetições, porque o modelo é estocástico. Empate não aprova.</li>
 *   <li>Temperatura 0.3 e {@code max_tokens} 2000: os valores do {@code application.yml}.</li>
 *   <li>Não usa cache: as chamadas são diretas, para o cache não devolver geração anterior.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de asserção com as respostas cruas dos dois braços. Modelo ausente, resposta fora do
 * contrato ou braço de controle que não reproduz o defeito produzem abstenção declarada.
 */
@DisplayName("MEDICAO: as instrucoes de rumo mudam a saida do modelo, nao so o texto do prompt")
class EfeitoDasInstrucoesDeRumoNoModeloIT {

    private static final String API = "http://127.0.0.1:1234/v1/chat/completions";
    private static final String CATALOGO = "http://127.0.0.1:1234/api/v0/models";
    private static final double TEMPERATURA = 0.3;
    private static final int MAX_TOKENS = 2000;
    /**
     * CINCO, e o motivo e medido: com 3 repeticoes o braco de CONTROLE do caso do relogio deixou
     * de reproduzir o defeito numa rodada da suite completa, e a medicao saiu como NAO VERIFICADO.
     * A abstencao estava CORRETA -- guarda que nao discrimina nao pode aprovar --, mas abstencao
     * repetida deixa de ser vocabulario fino e vira paisagem, que e o oposto do que ela serve.
     * Cinco amostras reduzem o vai-e-vem sem mudar o criterio.
     */
    private static final int REPETICOES = 5;
    /** Maioria simples de cinco. Empate nao aprova. */
    private static final int MAIORIA = 3;

    private static final Pattern TOKEN_DE_CONTROLE = Pattern.compile("<\\|[A-Z_]+\\|>");
    private static final Pattern PALAVRA_DE_DOCUMENTO =
        Pattern.compile("(?i)\\b(cabeçalho|cabecalho|título|titulo|headling)\\b");
    private static final Pattern PALAVRA_DE_RUMO =
        Pattern.compile("(?i)\\b(rumo|proa|azimute|direção|direcao)\\b");
    private static final Pattern HORA_DO_DIA = Pattern.compile("(?i)meia[- ]noite|meio[- ]dia");
    /**
     * O valor DOIS sobrevivendo, em algarismo ou por extenso. Aceita {@code dois} porque o modelo
     * erra o gênero ({@code "Dois horas!"}) sem errar o VALOR, e o que esta guarda protege é o
     * valor — concordância é outra régua, e a do projeto é legibilidade, não perfeição.
     */
    private static final Pattern VALOR_DOIS =
        Pattern.compile("(?i)(?<![\\p{L}\\p{N}])(2|dois|duas)(?![\\p{L}\\p{N}])");

    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).build();

    /**
     * PROPÓSITO DE NEGÓCIO: descobre qual modelo está residente, para a medição usar o mesmo que
     * atende a tradução.
     * <p>INVARIANTES DO DOMÍNIO: só considera {@code state == loaded}; modelo listado e não
     * carregado não serve.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} sem lançar — servidor fora do ar é
     * premissa ausente, não defeito do código sob teste.
     */
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

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma fala com o prompt de sistema recebido, reproduzindo o que
     * a produção envia.
     * <p>INVARIANTES DO DOMÍNIO: temperatura e limite de tokens são os do {@code application.yml};
     * o token de template do modelo é removido, como o adaptador de produção faz.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} — resposta fora do contrato não
     * vira veredito.
     */
    private String traduzir(String modelo, String sistema, String fala) {
        try {
            String corpo = om.writeValueAsString(om.createObjectNode()
                .put("model", modelo).put("temperature", TEMPERATURA).put("max_tokens", MAX_TOKENS)
                .set("messages", om.createArrayNode()
                    .add(om.createObjectNode().put("role", "system").put("content", sistema))
                    .add(om.createObjectNode().put("role", "user").put("content", fala))));
            HttpResponse<String> r = http.send(
                HttpRequest.newBuilder(URI.create(API))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(180)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String texto = om.readTree(r.body()).at("/choices/0/message/content").asText("");
            texto = TOKEN_DE_CONTROLE.matcher(texto).replaceAll("").trim();
            return texto.isEmpty() ? null : texto;
        } catch (Exception fora) {
            return null;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o prompt de produção da obra e devolve também a versão SEM a
     * linha da instrução, que é o braço de controle desta medição.
     * <p>INVARIANTES DO DOMÍNIO: a remoção é por trecho exato da linha; trecho ausente aborta a
     * medição, porque comparar o prompt consigo mesmo aprovaria por cegueira.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code null} quando a obra não existe no
     * catálogo ou a linha não é encontrada.
     */
    private String[] comESemAInstrucao(String obraId, String... trechosDasLinhas) {
        ProvedorContexto obra = new CatalogoLoreYaml().obras().stream()
            .filter(o -> obraId.equals(o.getId())).findFirst().orElse(null);
        if (obra == null) {
            return null;
        }
        String completo = obra.obterPromptSistema();
        String controle = completo;
        for (String trecho : trechosDasLinhas) {
            String linha = completo.lines()
                .filter(l -> l.contains(trecho))
                .findFirst().orElse(null);
            if (linha == null) {
                return null;
            }
            controle = controle
                .replace(linha + System.lineSeparator(), "")
                .replace(linha + "\n", "");
        }
        return new String[] {completo, controle};
    }

    private List<String> repetir(String modelo, String sistema, String fala) {
        List<String> saida = new ArrayList<>();
        for (int i = 0; i < REPETICOES; i++) {
            String r = traduzir(modelo, sistema, fala);
            if (r != null) {
                saida.add(r.toLowerCase(Locale.ROOT));
            }
        }
        return saida;
    }

    @Test
    @DisplayName("proa: sem a instrucao o modelo diz 'titulo'; com ela, diz rumo -- e o numero fica")
    void aInstrucaoDeProaMudaOQueOModeloDevolve() {
        String modelo = modeloCarregado();
        assumeTrue(modelo != null,
            "ABSTENCAO declarada: nenhum modelo com state=loaded em 127.0.0.1:1234. "
                + "Abster nao e aprovar. Para medir: carregue o modelo no LM Studio "
                + "(lms load aya-expanse-8b) e rode de novo.");

        String[] prompts = comESemAInstrucao("eight_six", "\"Heading\" seguida de número");
        assumeTrue(prompts != null,
            "ABSTENCAO declarada: obra 'eight_six' ou a linha da instrucao nao foram encontradas "
                + "no catalogo de lore. Sem o braco de controle a medicao nao discrimina.");

        String fala = "Heading 2-8-0. Distance 5,000.";
        List<String> sem = repetir(modelo, prompts[1], fala);
        List<String> com = repetir(modelo, prompts[0], fala);
        assumeTrue(sem.size() == REPETICOES && com.size() == REPETICOES,
            "ABSTENCAO declarada: o modelo nao respondeu as " + REPETICOES
                + " repeticoes dos dois bracos (sem=" + sem.size() + ", com=" + com.size() + ")");

        // Mesma barra assimetrica do caso do relogio: o controle prova ALCANCABILIDADE, a
        // afirmacao exige maioria. Ver o comentario naquele teste para a medicao que a justifica.
        long defeitoSemInstrucao = sem.stream().filter(r -> PALAVRA_DE_DOCUMENTO.matcher(r).find()).count();
        assumeTrue(defeitoSemInstrucao >= 1,
            "NAO VERIFICADO: em " + REPETICOES + " tentativas o braco de CONTROLE nao reproduziu o "
                + "defeito nenhuma vez, entao esta medicao nao discrimina e nao aprova nada. "
                + "Respostas sem a instrucao: " + sem);

        long acertoComInstrucao = com.stream()
            .filter(r -> PALAVRA_DE_RUMO.matcher(r).find() && r.contains("2-8-0"))
            .count();
        assertTrue(acertoComInstrucao >= MAIORIA,
            "a instrucao esta no prompt mas o modelo deixou de obedecer -- e e exatamente isso que "
                + "a catraca de texto nao ve. Com a instrucao: " + com + " | sem ela: " + sem);
    }

    @Test
    @DisplayName("relogio: sem a instrucao 'Two o'clock' vira meia-noite; com ela, o numero sobrevive")
    void aInstrucaoDeRelogioMudaOQueOModeloDevolve() {
        String modelo = modeloCarregado();
        assumeTrue(modelo != null,
            "ABSTENCAO declarada: nenhum modelo com state=loaded em 127.0.0.1:1234. "
                + "Abster nao e aprovar. Para medir: carregue o modelo no LM Studio "
                + "(lms load aya-expanse-8b) e rode de novo.");

        // O CONTROLE REMOVE AS DUAS LINHAS, e a razao foi medida em 14/09/2026 neste mesmo caso,
        // 5 repeticoes por arranjo, prompt vivo do catalogo:
        //
        //   nenhuma instrucao ... defeito 4/5      so relogio .... defeito 0/5
        //   so proa ............ defeito 2/5      as duas ....... defeito 0/5
        //
        // A instrucao de PROA generaliza parcialmente para o relogio. Removendo apenas a linha do
        // relogio, o controle fica em 2/5 — abaixo da maioria — e a guarda se abstinha rodada
        // apos rodada, o que e honesto e inutil. Removendo as duas, o controle reproduz o defeito
        // e a medicao volta a discriminar. O que ela prova passa a ser o que interessa como
        // regressao: o prompt ENTREGUE ainda conserta este caso.
        String[] prompts = comESemAInstrucao(
            "gundam_0083", "horas de relógio", "\"Heading\" seguida de número");
        assumeTrue(prompts != null,
            "ABSTENCAO declarada: obra 'gundam_0083' ou a linha da instrucao nao foram encontradas "
                + "no catalogo de lore. Sem o braco de controle a medicao nao discrimina.");

        String fala = "Two o'clock! Two enemy vessels, retreating!";
        List<String> sem = repetir(modelo, prompts[1], fala);
        List<String> com = repetir(modelo, prompts[0], fala);
        assumeTrue(sem.size() == REPETICOES && com.size() == REPETICOES,
            "ABSTENCAO declarada: o modelo nao respondeu as " + REPETICOES
                + " repeticoes dos dois bracos (sem=" + sem.size() + ", com=" + com.size() + ")");

        // A BARRA DO CONTROLE E ALCANCABILIDADE, NAO FREQUENCIA. O papel dele e provar que o
        // instrumento consegue VER o defeito sem a instrucao; uma reproducao basta para isso.
        // Exigir maioria aqui foi medido e e barra errada: sem nenhuma instrucao este caso oscila
        // entre 2 e 4 em 5, entao a guarda se abstinha em 2 de cada 3 rodadas — honesta e inutil.
        // O lado da AFIRMACAO continua estrito: com a instrucao, zero defeito em 5.
        long defeitoSemInstrucao = sem.stream().filter(r -> HORA_DO_DIA.matcher(r).find()).count();
        assumeTrue(defeitoSemInstrucao >= 1,
            "NAO VERIFICADO: em " + REPETICOES + " tentativas o braco de CONTROLE nao reproduziu o "
                + "defeito nenhuma vez, entao esta medicao nao discrimina e nao aprova nada. "
                + "Respostas sem as instrucoes: " + sem);

        long comDefeito = com.stream().filter(r -> HORA_DO_DIA.matcher(r).find()).count();
        assertTrue(comDefeito == 0,
            "com a instrucao nenhuma resposta pode dizer hora do dia. Com: " + com + " | sem: " + sem);
        long valorSobreviveu = com.stream().filter(r -> VALOR_DOIS.matcher(r).find()).count();
        assertTrue(valorSobreviveu >= MAIORIA,
            "o valor do relogio tem de sobreviver, em algarismo ou por extenso (o modelo escreve "
                + "'Dois horas' as vezes, e o valor esta certo). Com: " + com);
    }
}
