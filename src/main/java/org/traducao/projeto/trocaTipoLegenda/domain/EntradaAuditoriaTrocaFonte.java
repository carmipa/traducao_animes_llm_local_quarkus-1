package org.traducao.projeto.trocaTipoLegenda.domain;

public record EntradaAuditoriaTrocaFonte(
    String instante,
    String arquivo,
    String estilo,
    String fonteAntiga,
    String fonteNova,
    String pastaBackup,
    String status
) {}
