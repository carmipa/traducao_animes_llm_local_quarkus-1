package org.traducao.projeto.revisaoLore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * PROPÓSITO DE NEGÓCIO: configuração própria do cliente LLM da Revisão de Lore,
 * sob o namespace {@code revisao-lore.llm}, independente do namespace
 * {@code tradutor.llm} da Tradução Local. Os defaults reproduzem o comportamento
 * efetivo atual (mesmos base-url, model "current", max-tokens, timeouts).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Namespace exclusivo {@code revisao-lore.llm}; nunca reutiliza {@code tradutor.llm}.</li>
 *   <li>{@code model} pode ser resolvido em runtime para o modelo efetivamente
 *       carregado (ver {@code verificarDisponibilidade} do adapter), como no fluxo atual.</li>
 *   <li>{@code pausaEntreTentativas} preserva o equivalente operacional de 2s entre
 *       tentativas de revisão.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Valores ausentes ou inválidos caem para os defaults equivalentes ao efetivo
 * atual, garantindo timeouts e modelo estáveis mesmo sem configuração explícita.
 */
@ConfigurationProperties(prefix = "revisao-lore.llm")
public class RevisaoLoreLlmProperties {

    private String baseUrl = "http://127.0.0.1:1234/v1";
    private String model = "current";
    private int maxTokens = 2000;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(180);
    private Duration pausaEntreTentativas = Duration.ofSeconds(2);

    public RevisaoLoreLlmProperties() {
    }

    public String baseUrl() { return baseUrl; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { if (baseUrl != null && !baseUrl.isBlank()) this.baseUrl = baseUrl; }

    public String model() { return model; }
    public String getModel() { return model; }
    public void setModel(String model) { if (model != null && !model.isBlank()) this.model = model; }

    public int maxTokens() { return maxTokens; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { if (maxTokens > 0) this.maxTokens = maxTokens; }

    public Duration connectTimeout() { return connectTimeout; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { if (connectTimeout != null) this.connectTimeout = connectTimeout; }

    public Duration readTimeout() { return readTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { if (readTimeout != null) this.readTimeout = readTimeout; }

    public Duration pausaEntreTentativas() { return pausaEntreTentativas; }
    public Duration getPausaEntreTentativas() { return pausaEntreTentativas; }
    public void setPausaEntreTentativas(Duration pausaEntreTentativas) { if (pausaEntreTentativas != null) this.pausaEntreTentativas = pausaEntreTentativas; }
}
