package org.traducao.projeto.revisaoLore.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: entrega ao controller o desfecho completo de uma
 * revisão de lore para banner, logs e decisões operacionais.
 * <p>INVARIANTES DO DOMÍNIO: status e contadores descrevem a mesma sessão;
 * pendentes incluem respostas ausentes, propostas descartadas e falas que
 * precisam voltar à revisão linguística da Opção 6.
 * <p>COMPORTAMENTO EM CASO DE FALHA: o record é imutável; falhas totais são
 * comunicadas por exceção antes de sua criação.
 */
public record ResultadoRevisaoLore(
    StatusRevisaoLore status,
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
    int totalErros,
    List<String> erros,
    String caminhoRelatorioJson
) {
    public boolean teveErros() {
        return totalErros > 0;
    }
}
