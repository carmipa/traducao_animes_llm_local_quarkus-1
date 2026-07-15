package org.traducao.projeto.traducao.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Cliente HTTP JSON baseado em {@link HttpClient} do JDK (sem Spring RestClient).
 */
public class JsonHttpClient {

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

    public JsonHttpClient(LlmProperties propriedades, String baseUrl, ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(propriedades.connectTimeout())
            .build();
        this.baseUrl = normalizarBaseUrl(baseUrl);
        this.readTimeout = propriedades.readTimeout();
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

    public String getString(String path) throws IOException, InterruptedException {
        HttpResponse<String> response = enviar(
            HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(readTimeout)
                .header("Accept", "application/json")
                .GET()
                .build()
        );
        return response.body();
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
    // estourava ClassCastException quando a cadeia continha um Error.
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
