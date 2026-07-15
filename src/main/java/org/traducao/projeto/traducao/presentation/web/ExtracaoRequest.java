package org.traducao.projeto.traducao.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: transporta os parâmetros da extração de legendas —
 * pasta de vídeos, pasta de saída e o formato-alvo escolhido na interface.
 *
 * <p>INVARIANTES DO DOMÍNIO: os nomes dos campos são contrato JSON público; o
 * formato é validado contra {@code FormatoLegenda} antes do job entrar na fila.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco ou formato inválido faz o
 * endpoint responder HTTP 400 antes de qualquer processamento.
 */
public record ExtracaoRequest(String entrada, String saida, String formato) {}
