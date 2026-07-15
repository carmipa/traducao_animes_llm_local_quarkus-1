package org.traducao.projeto.renomearArquivos.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: representa o resultado verificável de uma simulação,
 * aplicação ou reversão de nomes para que a interface exiba o estado real.
 *
 * <p>INVARIANTES DO DOMÍNIO: contadores nunca são negativos; {@code itens}
 * contém somente mapeamentos pertencentes à pasta processada; o status não
 * pode anunciar sucesso quando existem falhas ou pendências.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: operações recusadas antes da execução são
 * respondidas pelo controller como erro HTTP; falhas durante um lote retornam
 * status {@code CONCLUIDO_COM_FALHAS} e preservam o manifesto de reversão.
 */
public record ResultadoRenomeacao(
    String operacao,
    String status,
    int arquivosAnalisados,
    int itensPlanejados,
    int itensConcluidos,
    int conflitos,
    int ignorados,
    int falhas,
    int pendentes,
    String mensagem,
    List<OperacaoRenomeacao.ItemRenomeado> itens
) {
    /**
     * PROPÓSITO DE NEGÓCIO: congela a lista de mapeamentos antes de expor o
     * resultado ao painel e à serialização JSON.
     *
     * <p>INVARIANTES DO DOMÍNIO: a coleção pública nunca é nula nem mutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula é convertida em lista vazia;
     * os demais valores já devem ter sido validados pelo caso de uso.
     */
    public ResultadoRenomeacao {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
