package org.traducao.projeto.core.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: transporta os parâmetros comuns das operações do
 * pipeline (análise, tradução, correção e revisão) enviados pela SPA — pastas de
 * entrada/saída, contexto de lore selecionado e opções de sincronismo/revisão.
 *
 * <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público
 * consumido pelo front-end; caminhos são normalizados e o contexto é validado
 * pelos endpoints antes de qualquer job entrar na fila compartilhada.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes chegam como {@code null} e
 * cada endpoint decide o fallback seguro ou responde HTTP 400 antes de enfileirar.
 */
public record OperacaoRequest(String entrada, String saida, String contextoId, Long syncOffsetMs,
                              Boolean permitirRetraducao, String modoReferencia, String caminhoCache) {}
