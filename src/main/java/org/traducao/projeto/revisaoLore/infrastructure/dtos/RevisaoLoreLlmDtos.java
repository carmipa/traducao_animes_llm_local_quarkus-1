package org.traducao.projeto.revisaoLore.infrastructure.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: DTOs próprios da Revisão de Lore para o protocolo
 * OpenAI-compatible do LLM local (chat/completions e catálogo de modelos).
 * Duplicação consciente dos records equivalentes da Tradução Local, para manter
 * a fatia autônoma — nenhuma dependência de {@code RecordsLlm}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Campos desconhecidos são ignorados na desserialização ({@code ignoreUnknown = true}).</li>
 *   <li>{@code ModeloDisponivelV0} carrega o {@code state} da API estendida da LM Studio.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Estruturas ausentes desserializam como {@code null}, tratado pelo adapter como
 * resposta inválida.
 */
public class RevisaoLoreLlmDtos {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Mensagem(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatRequest(String model, List<Mensagem> messages, double temperature, int max_tokens) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Mensagem message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RespostaLlm(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModeloDisponivel(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaModelos(List<ModeloDisponivel> data) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModeloDisponivelV0(String id, String state) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ListaModelosV0(List<ModeloDisponivelV0> data) {}
}
