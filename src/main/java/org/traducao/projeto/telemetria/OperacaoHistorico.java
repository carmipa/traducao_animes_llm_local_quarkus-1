package org.traducao.projeto.telemetria;

/**
 * Uma linha da tabela de histórico de operações exibida no painel de Telemetria.
 */
public record OperacaoHistorico(
    String nomeOperacao,
    String detalheOperacao,
    String duracaoFormatada,
    Integer taxaSucesso,
    String origem,
    Long duracaoMs,
    /** Instante UTC (ISO-8601) do registro; o navegador converte para hora local. */
    String registradoEm
) {}
