package org.traducao.projeto.traducao.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: transporta as opções exclusivas do Remuxer.
 * INVARIANTES DO DOMÍNIO: pasta de vídeo é obrigatória; offset e política de
 * faixas são validados pelo endpoint.
 * COMPORTAMENTO EM CASO DE FALHA: campos ausentes recebem fallback seguro ou
 * geram HTTP 400 antes de entrar na fila.
 */
public record RemuxRequest(String entrada, String saida, Long syncOffsetMs,
                           Boolean preservarLegendasOriginais) {}
