package org.traducao.projeto.trocaTipoLegenda.domain;

public record ResultadoTrocaFonte(
    int totalAnalisados,
    int totalAlterados,
    int totalSubstituicoes,
    String dataHora,
    String pastaBackup,
    String caminhoRelatorioJson
) {}
