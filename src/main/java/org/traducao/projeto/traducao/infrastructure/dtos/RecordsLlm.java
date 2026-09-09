package org.traducao.projeto.traducao.infrastructure.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

public class RecordsLlm {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mensagem(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatRequest(String model, List<Mensagem> messages, double temperature, int max_tokens) {}

    /**
     * Uma escolha devolvida pelo LLM. Além da mensagem, carrega o {@code finish_reason} — o
     * motivo pelo qual a geração parou. Sem ele, o adaptador não tinha como distinguir uma
     * tradução COMPLETA de uma cortada no teto de tokens, e declarava sucesso nas duas
     * (defeito medido em 2026-09-09 com resposta controlada). Chega nulo quando o servidor
     * não informa o campo, e nulo significa "não sei", nunca "terminou bem".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Mensagem message, String finish_reason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespostaLlm(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModeloDisponivel(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaModelos(List<ModeloDisponivel> data) {}

    /**
     * Shape da API estendida da LM Studio ({@code /api/v0/models}, fora do
     * prefixo {@code /v1}), que — diferente do endpoint OpenAI-compatible
     * {@code /v1/models} — informa o campo {@code state} ("loaded" /
     * "not-loaded"), permitindo saber com certeza qual modelo está de fato
     * carregado em memória.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModeloDisponivelV0(String id, String state) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaModelosV0(List<ModeloDisponivelV0> data) {}
}
