package org.traducao.projeto.revisaoLore.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * PROPÓSITO DE NEGÓCIO: cliente HTTP JSON próprio da Revisão de Lore, baseado no
 * {@link HttpClient} do JDK. Cobre exatamente o necessário para falar com o LLM
 * local: {@code GET} relativo, {@code GET} absoluto (API estendida da LM Studio)
 * e {@code POST} JSON. Duplicação técnica consciente — não depende do cliente
 * HTTP da Tradução Local.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code connect-timeout} e {@code read-timeout} próprios da fatia são
 *       aplicados a cada requisição.</li>
 *   <li>Respostas com status {@code >= 400} viram {@link HttpClientException}
 *       preservando o código HTTP para a política de retry do adapter.</li>
 *   <li>A interrupção da thread é propagada: {@code send} do JDK lança
 *       {@link InterruptedException}, repassada ao chamador sem ser engolida aqui.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Erros de rede/timeout propagam a exceção original de {@link HttpClient}; erros
 * HTTP viram {@link HttpClientException}. Nenhum estado é mantido entre chamadas.
 */
public class RevisaoLoreHttpClient {

    /**
     * PROPÓSITO DE NEGÓCIO: erro HTTP com o código de status preservado, para o
     * adapter decidir se a falha é permanente (não repetir) ou transitória.
     * <p>INVARIANTES DO DOMÍNIO: {@link #statusCode()} reflete o status recebido.
     * <p>COMPORTAMENTO EM CASO DE FALHA: é uma {@link RuntimeException} propagada
     * ao laço de tentativas do adapter.
     */
    public static class HttpClientException extends RuntimeException {
        private final int statusCode;

        public HttpClientException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }

    private final HttpClient httpClient;
    private final String baseUrl;
    private final Duration readTimeout;
    private final ObjectMapper objectMapper;

    public RevisaoLoreHttpClient(Duration connectTimeout, String baseUrl, Duration readTimeout, ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(connectTimeout)
            .build();
        this.baseUrl = normalizarBaseUrl(baseUrl);
        this.readTimeout = readTimeout;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String path, Class<T> responseType) throws IOException, InterruptedException {
        HttpResponse<String> response = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(readTimeout)
                .header("Accept", "application/json")
                .GET()
                .build()
        );
        return objectMapper.readValue(response.body(), responseType);
    }

    public String getAbsolute(String urlCompleta) throws IOException, InterruptedException {
        HttpResponse<String> response = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(urlCompleta))
                .timeout(readTimeout)
                .header("Accept", "application/json")
                .GET()
                .build()
        );
        return response.body();
    }

    public <T, B> T post(String path, B body, Class<T> responseType) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(body);
        HttpResponse<String> response = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(readTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build()
        );
        return objectMapper.readValue(response.body(), responseType);
    }

    // Recebe Throwable (não Exception): o cast cego da causa para Exception
    // estouraria ClassCastException quando a cadeia contivesse um Error.
    public static boolean isErroRedeOuTimeout(Throwable e) {
        if (e == null) {
            return false;
        }
        return e instanceof HttpTimeoutException
            || e instanceof java.net.ConnectException
            || e instanceof java.net.UnknownHostException
            || isErroRedeOuTimeout(e.getCause());
    }

    private HttpResponse<String> enviar(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new HttpClientException(response.statusCode(),
                "HTTP " + response.statusCode() + ": " + response.body());
        }
        return response;
    }

    private static String normalizarBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
