package org.traducao.projeto.revisaoLore.domain;

/**
 * Entrada estruturada do log de sessao da revisao de lore (serializavel em JSON).
 */
public record LogEventoRevisaoLore(
    String instante,
    String nivel,
    String mensagem
) {}
