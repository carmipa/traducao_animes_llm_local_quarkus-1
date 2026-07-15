package org.traducao.projeto.renomearArquivos.presentation.web;

/**
 * PROPÓSITO DE NEGÓCIO: transporta pasta, nome base e temporada escolhidos no
 * painel da opção 13.
 *
 * <p>INVARIANTES DO DOMÍNIO: validação efetiva permanece no backend; temporada
 * nula permite inferência pelo nome e compatibilidade com clientes antigos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campos ausentes são recusados ou recebem
 * fallback seguro pelo caso de uso, nunca usados diretamente em movimentação.
 */
public record RenomearArquivosRequest(
    String caminhoOrigem,
    String nomePadrao,
    Integer temporada
) {}
