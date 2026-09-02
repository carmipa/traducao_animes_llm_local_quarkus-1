package org.traducao.projeto.remuxer.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: transporta as opções exclusivas do Remuxer.
 *
 * <p>INVARIANTES DO DOMÍNIO: pasta de vídeo é obrigatória; offset, destino e
 * política de faixas são validados pelo endpoint. O campo {@code saida} carrega
 * a pasta das LEGENDAS traduzidas — nome herdado do contrato público e mantido
 * de propósito; quem grava o MKV final é {@code pastaDestino}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes recebem fallback seguro ou
 * geram HTTP 400 antes de entrar na fila. {@code pastaDestino} vazio mantém o
 * padrão histórico {@code <pasta de vídeos>/mkv_final_ptbr}.
 */
public record RemuxRequest(String entrada, String saida, Long syncOffsetMs,
                           Boolean preservarLegendasOriginais, String pastaDestino) {}
