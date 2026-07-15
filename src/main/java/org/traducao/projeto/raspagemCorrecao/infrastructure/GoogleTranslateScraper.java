package org.traducao.projeto.raspagemCorrecao.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduz texto via API pública do Google Translate, preservando tags ASS
 * mascaradas e quebras {@code \N}.
 * <p>
 * O retorno é tipado ({@link ResultadoRaspagem}): cada desfecho — sucesso, sem
 * alteração, falha transitória, resposta inválida ou tag corrompida — vira um
 * {@link StatusRaspagem} explícito, em vez de o chamador ter que adivinhar a
 * partir de "o texto voltou igual". O transporte HTTP fica isolado em
 * {@link #executarGet(String)} para poder ser substituído em testes.
 */
@Component
public class GoogleTranslateScraper {

    private static final Logger log = LoggerFactory.getLogger(GoogleTranslateScraper.class);

    // Retry curado: só transitórios (FALHA_TRANSITORIA) são repetidos, no
    // máximo uma vez. Erro estrutural (RESPOSTA_INVALIDA/TAG_CORROMPIDA) morre
    // na primeira, sem gastar rede à toa nem arriscar intensificar bloqueio.
    private static final int MAX_TENTATIVAS = 2;
    private static final long BACKOFF_BASE_MS = 400;
    private static final long JITTER_MAX_MS = 200;

    // Marcador [Tn]/[B] (com mutilações comuns de espaçamento/parênteses) que
    // sobrou depois da restauração das tags — sinal de resposta corrompida.
    private static final Pattern PADRAO_MARCADOR_RESIDUAL =
        Pattern.compile("(?i)[\\[(]\\s*[tb]\\s*\\d*\\s*[\\])]");

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public GoogleTranslateScraper(ObjectMapper mapper) {
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public ResultadoRaspagem traduzir(String textoOriginal) {
        for (int n = 1; ; n++) {
            Tentativa tentativa = tentarTraduzir(textoOriginal);
            boolean transitoria = tentativa.resultado().status() == StatusRaspagem.FALHA_TRANSITORIA;
            if (!transitoria || n >= MAX_TENTATIVAS) {
                return tentativa.resultado();
            }
            long espera = tentativa.esperaSugeridaMs() > 0 ? tentativa.esperaSugeridaMs() : backoffComJitter();
            log.info("Falha transitória do Google Translate; nova tentativa em {} ms ({}/{}).",
                espera, n + 1, MAX_TENTATIVAS);
            dormir(espera);
        }
    }

    /** Uma única tentativa: mascara tags, chama o transporte e mapeia o desfecho. */
    private Tentativa tentarTraduzir(String textoOriginal) {
        List<String> tags = new ArrayList<>();
        Pattern patternTags = Pattern.compile("\\{[^}]+\\}");
        Matcher matcher = patternTags.matcher(textoOriginal);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            sb.append(textoOriginal, lastEnd, matcher.start());
            tags.add(matcher.group());
            sb.append(" [T").append(tags.size() - 1).append("] ");
            lastEnd = matcher.end();
        }
        sb.append(textoOriginal, lastEnd, textoOriginal.length());
        String textoMascarado = sb.toString();

        boolean temQuebra = textoMascarado.contains("\\N");
        if (temQuebra) {
            textoMascarado = textoMascarado.replace("\\N", " [B] ");
        }

        textoMascarado = textoMascarado.replaceAll("\\s+", " ").strip();

        if (textoMascarado.isEmpty()) {
            return semEspera(ResultadoRaspagem.semAlteracao(textoOriginal));
        }

        RespostaHttp resposta;
        try {
            String query = URLEncoder.encode(textoMascarado, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=pt&dt=t&q="
                + query;
            resposta = executarGet(url);
        } catch (Exception e) {
            log.error("Erro na comunicação com a API do Google Translate: {}", e.getMessage());
            return semEspera(ResultadoRaspagem.falhaTransitoria(textoOriginal));
        }

        if (resposta.statusCode() != 200) {
            log.warn("Erro HTTP na chamada do Google Translate: {}", resposta.statusCode());
            if (ehTransitorio(resposta.statusCode())) {
                // Honra Retry-After (quando presente) na próxima tentativa.
                return new Tentativa(ResultadoRaspagem.falhaTransitoria(textoOriginal), resposta.retryAfterMs());
            }
            return semEspera(ResultadoRaspagem.respostaInvalida(textoOriginal));
        }

        String traduzido;
        try {
            JsonNode root = mapper.readTree(resposta.corpo());
            JsonNode segments = root.get(0);
            StringBuilder resultadoTraduzido = new StringBuilder();
            if (segments != null && segments.isArray()) {
                for (JsonNode segment : segments) {
                    JsonNode text = segment.get(0);
                    if (text != null && !text.isNull()) {
                        resultadoTraduzido.append(text.asText());
                    }
                }
            }
            traduzido = resultadoTraduzido.toString();
        } catch (Exception e) {
            log.warn("Resposta do Google Translate em formato inesperado ({}); mantendo texto original.", e.getMessage());
            return semEspera(ResultadoRaspagem.respostaInvalida(textoOriginal));
        }

        if (traduzido.isBlank()) {
            log.warn("Resposta do Google Translate sem segmentos traduzíveis; mantendo texto original.");
            return semEspera(ResultadoRaspagem.respostaInvalida(textoOriginal));
        }

        if (temQuebra) {
            traduzido = traduzido.replaceAll("(?i)\\s*\\[b\\]\\s*", "\\\\N");
        }

        for (int i = 0; i < tags.size(); i++) {
            String pattern = "(?i)\\s*\\[t" + i + "\\]\\s*";
            traduzido = traduzido.replaceAll(pattern, Matcher.quoteReplacement(tags.get(i)));
        }

        traduzido = traduzido.replace("\\ N", "\\N").replace("\\ n", "\\N");

        // O Google às vezes mutila os marcadores ("[ T0 ]", "(T0)", "[b ]"...): o
        // replace acima não casa e sobraria marcador visível/tag ASS perdida.
        // TAG_CORROMPIDA (o chamador mantém o original) não deve ser retentada.
        if (PADRAO_MARCADOR_RESIDUAL.matcher(traduzido).find()) {
            log.warn("Google Translate mutilou marcadores de tag/quebra; mantendo texto original: {}", traduzido);
            return semEspera(ResultadoRaspagem.tagCorrompida(textoOriginal));
        }
        for (String tag : tags) {
            if (!traduzido.contains(tag)) {
                log.warn("Google Translate perdeu a tag ASS {}; mantendo texto original.", tag);
                return semEspera(ResultadoRaspagem.tagCorrompida(textoOriginal));
            }
        }

        if (traduzido.equals(textoOriginal)) {
            return semEspera(ResultadoRaspagem.semAlteracao(textoOriginal));
        }
        return semEspera(ResultadoRaspagem.sucesso(traduzido));
    }

    private static Tentativa semEspera(ResultadoRaspagem resultado) {
        return new Tentativa(resultado, 0);
    }

    /** Resultado de uma tentativa + espera sugerida (ex.: Retry-After) para o retry. */
    private record Tentativa(ResultadoRaspagem resultado, long esperaSugeridaMs) {}

    /**
     * Transporte HTTP cru (status + corpo). Isolado num método {@code protected}
     * para os testes substituírem o transporte sem rede — separando a política de
     * desfecho/preservação de tags da chamada de rede propriamente dita.
     */
    protected RespostaHttp executarGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<String> response = httpClient.send(
            request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        long retryAfterMs = parseRetryAfter(response.headers().firstValue("Retry-After").orElse(null));
        return new RespostaHttp(response.statusCode(), response.body(), retryAfterMs);
    }

    /** Resposta HTTP mínima (status, corpo e Retry-After em ms) usada como seam de transporte. */
    protected record RespostaHttp(int statusCode, String corpo, long retryAfterMs) {}

    /** Espera do backoff — {@code protected} para os testes anularem sem dormir de verdade. */
    protected void dormir(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static long backoffComJitter() {
        return BACKOFF_BASE_MS + ThreadLocalRandom.current().nextLong(JITTER_MAX_MS);
    }

    /** Retry-After no formato de segundos (o formato de data HTTP é ignorado). Retorna ms; 0 se ausente. */
    private static long parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(header.trim()) * 1000L;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 408/429 e 5xx são transitórios (vale retry); os demais não. */
    private static boolean ehTransitorio(int statusCode) {
        return statusCode == 408 || statusCode == 429
            || statusCode == 500 || statusCode == 502 || statusCode == 503 || statusCode == 504;
    }
}
