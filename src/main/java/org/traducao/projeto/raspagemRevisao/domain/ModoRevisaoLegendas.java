package org.traducao.projeto.raspagemRevisao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: qual provedor conduz a revisão de uma legenda já publicada. Não é uma
 * preferência de configuração — é a escolha entre duas ferramentas com forças opostas, e o
 * operador a faz sabendo qual problema está atacando.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O modo escolhido vale para a sessão INTEIRA e aparece no nome da operação e no prefixo do
 *       arquivo de relatório. Sem isso, as duas revisões ficariam indistinguíveis no histórico e
 *       não se saberia qual provedor produziu qual correção.</li>
 *   <li>Enum puro de domínio: só JDK, sem framework e sem I/O. Era aninhado dentro do caso de uso,
 *       o que obrigava o serviço de relatório a importar o caso de uso só para nomear o modo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de escolha; não lança.
 */
public enum ModoRevisaoLegendas {

    /**
     * Tradutor de máquina externo. Rápido e barato, mas NÃO conhece a lore: só recebe falhas
     * objetivas de tradução, nunca questões de concordância, sob pena de devolver nome próprio
     * traduzido.
     */
    GOOGLE,

    /**
     * LLM local com a lore da obra carregada. Trata concordância, gênero e tratamento — o que o
     * Google não pode ver —, ao custo de GPU e de uma fala por vez.
     */
    LLM_CONCORDANCIA
}
