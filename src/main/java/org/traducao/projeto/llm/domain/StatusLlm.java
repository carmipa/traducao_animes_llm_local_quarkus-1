package org.traducao.projeto.llm.domain;

/**
 * PROPÓSITO DE NEGÓCIO: resultado da checagem de disponibilidade do servidor LLM local
 * (ex.: LM Studio) feita no início da execução, antes de traduzir qualquer episódio — para
 * falhar cedo, com mensagem clara, em vez de descobrir a indisponibilidade após vários
 * timeouts no meio do trabalho.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code servidorOnline} e {@code modeloCarregado} são sinais independentes: o
 *       servidor pode estar de pé sem o modelo configurado carregado.</li>
 *   <li>{@code mensagem} descreve o estado para exibição ao operador.</li>
 *   <li>Record imutável de domínio: só JDK, sem dependência de framework ou fatia.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * É o próprio veículo do estado de falha: servidor offline ou modelo ausente são
 * representados pelos flags e pela {@code mensagem}, não por exceção.
 *
 * @param servidorOnline {@code true} se o servidor LLM respondeu
 * @param modeloCarregado {@code true} se o modelo configurado está carregado em memória
 * @param mensagem descrição do estado para o operador
 */
public record StatusLlm(
    boolean servidorOnline,
    boolean modeloCarregado,
    String mensagem
) {
}
