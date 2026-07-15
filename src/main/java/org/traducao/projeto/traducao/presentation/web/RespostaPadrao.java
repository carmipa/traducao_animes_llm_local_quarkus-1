package org.traducao.projeto.traducao.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: envelope de resposta textual padrão da API web, usado
 * por praticamente todos os endpoints do pipeline para devolver ao navegador uma
 * mensagem legível (aceitação na fila, validação recusada ou heartbeat).
 *
 * <p>INVARIANTES DO DOMÍNIO: o nome do campo {@code mensagem} é contrato JSON
 * público consumido pela SPA; não pode ser renomeado sem quebrar o front-end.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sendo um record imutável, não há falha de
 * construção; {@code mensagem} pode ser vazia, mas nunca deve carregar dados
 * sensíveis, pois é ecoada diretamente na interface.
 */
public record RespostaPadrao(String mensagem) {}
