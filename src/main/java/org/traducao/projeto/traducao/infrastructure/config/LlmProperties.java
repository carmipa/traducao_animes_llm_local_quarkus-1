package org.traducao.projeto.traducao.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tradutor.llm")
public class LlmProperties {
    private String baseUrl = "http://127.0.0.1:1234/v1";
    private String model = "mistralai/mistral-nemo-instruct-2407";
    private double temperature = 0.3;
    private int maxTokens = 2000;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(90);

    public LlmProperties() {
    }

    public LlmProperties(String baseUrl, String model, double temperature, int maxTokens, Duration connectTimeout, Duration readTimeout) {
        this.baseUrl = (baseUrl == null || baseUrl.isBlank()) ? "http://127.0.0.1:1234/v1" : baseUrl;
        this.model = (model == null || model.isBlank()) ? "mistralai/mistral-nemo-instruct-2407" : model;
        this.temperature = temperature <= 0 ? 0.3 : temperature;
        this.maxTokens = maxTokens <= 0 ? 2000 : maxTokens;
        this.connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        this.readTimeout = readTimeout == null ? Duration.ofSeconds(90) : readTimeout;
    }

    public String baseUrl() { return baseUrl; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { if (baseUrl != null && !baseUrl.isBlank()) this.baseUrl = baseUrl; }

    public String model() { return model; }
    public String getModel() { return model; }
    public void setModel(String model) { if (model != null && !model.isBlank()) this.model = model; }

    /**
     * PROPÓSITO DE NEGÓCIO: id do modelo que recebe a SEGUNDA OPINIÃO — a fala que o modelo
     * principal não conseguiu traduzir depois de todas as retentativas. Ele nunca traduz o
     * episódio.
     *
     * <p>INVARIANTES DO DOMÍNIO: vazio é o padrão e significa DESLIGADO — sem configuração,
     * o pipeline se comporta exatamente como antes. Falha fechada: nada de adotar um segundo
     * modelo por conta própria só porque ele está carregado no servidor.
     *
     * <p>Ao contrário de {@link #model}, aqui o id é EXPLÍCITO e não {@code "current"}: o
     * ponto é justamente pedir um modelo diferente do que está atendendo a tradução. O custo
     * está declarado em docs/ref-configuracao.md — o LM Studio mantém os dois residentes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: string vazia; nunca nulo.
     */
    private String modeloRecuperacao = "";

    public String modeloRecuperacao() { return modeloRecuperacao; }
    public String getModeloRecuperacao() { return modeloRecuperacao; }
    public void setModeloRecuperacao(String modeloRecuperacao) {
        // Aceita branco DE PROPÓSITO, ao contrário dos demais setters: branco é o estado
        // "desligado", e um setter que o ignorasse tornaria impossível desligar por config.
        this.modeloRecuperacao = modeloRecuperacao == null ? "" : modeloRecuperacao.trim();
    }

    public double temperature() { return temperature; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { if (temperature > 0) this.temperature = temperature; }

    public int maxTokens() { return maxTokens; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { if (maxTokens > 0) this.maxTokens = maxTokens; }

    public Duration connectTimeout() { return connectTimeout; }
    public Duration getConnectTimeout() { return connectTimeout; }
    public void setConnectTimeout(Duration connectTimeout) { if (connectTimeout != null) this.connectTimeout = connectTimeout; }

    public Duration readTimeout() { return readTimeout; }
    public Duration getReadTimeout() { return readTimeout; }
    public void setReadTimeout(Duration readTimeout) { if (readTimeout != null) this.readTimeout = readTimeout; }
}
