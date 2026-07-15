package org.traducao.projeto.auditorConteudoLegendas.domain;

import org.traducao.projeto.telemetria.OperacaoTelemetria;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: persiste a auditoria como dataset estruturado para
 * diagnóstico, evolução das regras e reprodução de falhas.
 * <p>INVARIANTES DO DOMÍNIO: nomes, formatos, métricas e anomalias pertencem à
 * mesma execução.
 * <p>COMPORTAMENTO EM CASO DE FALHA: o record é imutável; falhas de gravação
 * são tratadas pela camada de persistência.
 */
public record AuditoriaConteudoRelatorioJson(
    String tipo,
    OperacaoTelemetria operacao,
    String modo,
    String arquivoOriginal,
    String arquivoTraduzido,
    String formatoOriginal,
    String formatoTraduzido,
    boolean limpo,
    int totalAnomalias,
    long duracaoMs,
    List<AnomaliaConteudo> anomalias
) {}
