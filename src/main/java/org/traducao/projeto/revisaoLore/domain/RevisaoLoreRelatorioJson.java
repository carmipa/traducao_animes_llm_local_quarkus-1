package org.traducao.projeto.revisaoLore.domain;

import org.traducao.projeto.telemetria.OperacaoTelemetria;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: persiste o dataset completo da revisão de lore com
 * contexto, métricas, erros e eventos granulares.
 * <p>INVARIANTES DO DOMÍNIO: todos os blocos pertencem à mesma sessão e o
 * status resume os contadores persistidos.
 * <p>COMPORTAMENTO EM CASO DE FALHA: é imutável; a infraestrutura decide como
 * registrar impossibilidade de escrita.
 */
public record RevisaoLoreRelatorioJson(
    String tipo,
    OperacaoTelemetria operacao,
    ContextoObra contexto,
    PastasOperacao pastas,
    String modo,
    MetricasRevisaoLore metricas,
    List<String> erros,
    List<LogEventoRevisaoLore> eventos
) {
    public record ContextoObra(String id, String nome) {}

    public record PastasOperacao(String originalEn, String traduzidaPtBr) {}

    /**
     * PROPÓSITO DE NEGÓCIO: estrutura as métricas usadas para diagnóstico e
     * evolução futura do revisor.
     * <p>INVARIANTES DO DOMÍNIO: pendentes correspondem a sem-resposta,
     * descartadas e encaminhadas à Opção 6; concluído exige zero pendências e erros.
     * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável, sem efeitos colaterais.
     */
    public record MetricasRevisaoLore(
        StatusRevisaoLore status,
        long duracaoMs,
        String duracaoFormatada,
        int arquivosAnalisados,
        int arquivosAlterados,
        int falasAuditadas,
        int falasSinalizadas,
        int falasCorrigidas,
        int falasSemAlteracao,
        int falasSemResposta,
        int falasDescartadas,
        int falasEncaminhadasOpcao6,
        int falasPendentes,
        int totalErros
    ) {}
}
