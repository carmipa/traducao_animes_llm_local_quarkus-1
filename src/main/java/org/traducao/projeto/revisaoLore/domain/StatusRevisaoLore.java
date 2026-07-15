package org.traducao.projeto.revisaoLore.domain;

/**
 * PROPÓSITO DE NEGÓCIO: distingue o desfecho real de uma execução de revisão de
 * lore, substituindo o antigo "[SUCESSO]" incondicional. Permite ao operador
 * saber, num relance no console/relatório, se o job realmente concluiu, se
 * concluiu deixando pendências, se foi cancelado, se falhou ou se nem havia
 * arquivos para processar.
 *
 * <p>INVARIANTES DO DOMÍNIO: exatamente um status descreve cada execução. Só
 * {@link #FALHOU} pode acompanhar uma exceção propagada; os demais representam
 * retornos normais do use case. {@link #CONCLUIDO} exige zero erros e zero falas
 * pendentes.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: é um enum imutável; não dispara exceções.
 * O rótulo textual nunca é nulo.
 */
public enum StatusRevisaoLore {
    CONCLUIDO("Concluído"),
    CONCLUIDO_COM_PENDENCIAS("Concluído com pendências"),
    FALHOU("Falhou"),
    CANCELADO("Cancelado"),
    SEM_ARQUIVOS("Sem arquivos");

    private final String rotulo;

    /**
     * PROPÓSITO DE NEGÓCIO: associa cada estado técnico a um rótulo humano.
     * <p>INVARIANTES DO DOMÍNIO: todo status possui rótulo não nulo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: construção ocorre apenas pelas
     * constantes declaradas no enum.
     */
    StatusRevisaoLore(String rotulo) {
        this.rotulo = rotulo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece o texto exibido nos banners e relatórios.
     * <p>INVARIANTES DO DOMÍNIO: retorna sempre o rótulo da própria constante.
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca retorna nulo.
     */
    public String rotulo() {
        return rotulo;
    }
}
