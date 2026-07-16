package org.traducao.projeto.apiDadosAnime.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * PROPÓSITO DE NEGÓCIO: configuração HTTP própria da fatia {@code apiDadosAnime} —
 * timeouts do cliente para as APIs públicas de metadados de anime (AniList/Jikan/TMDB).
 * Isola a fatia da stack de LLM da Tradução Local ({@code tradutor.llm}), preservando
 * exatamente os valores efetivos herdados (subfase E4a).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Defaults efetivos: connect {@code 5s}, read {@code 180s}.</li>
 *   <li>Sem dependência de {@code LlmProperties} nem de qualquer tipo de {@code traducao}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Valores nulos no binding são ignorados pelos setters, mantendo os defaults {@code 5s/180s}.
 */
@ConfigurationProperties(prefix = "api-dados-anime.http")
public class ApiDadosAnimeHttpProperties {

    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(180);

    public ApiDadosAnimeHttpProperties() {
    }

    public ApiDadosAnimeHttpProperties(Duration connectTimeout, Duration readTimeout) {
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        this.readTimeout = readTimeout == null ? Duration.ofSeconds(180) : readTimeout;
    }

    public Duration connectTimeout() { return connectTimeout; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { if (connectTimeout != null) this.connectTimeout = connectTimeout; }

    public Duration readTimeout() { return readTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { if (readTimeout != null) this.readTimeout = readTimeout; }
}
