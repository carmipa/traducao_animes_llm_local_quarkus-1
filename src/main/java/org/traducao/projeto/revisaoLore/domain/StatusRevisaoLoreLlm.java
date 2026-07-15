package org.traducao.projeto.revisaoLore.domain;

/**
 * PROPÓSITO DE NEGÓCIO: representa o resultado da verificação de disponibilidade
 * do servidor LLM local usado exclusivamente pela Revisão de Lore, antes de
 * iniciar uma sessão. Permite abortar cedo, com mensagem clara, quando o modelo
 * não está carregado — em vez de descobrir isso só no meio da revisão.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code modeloCarregado == true} implica {@code servidorOnline == true}.</li>
 *   <li>{@code mensagem} descreve o estado de forma legível ao operador.</li>
 *   <li>Tipo próprio da fatia Revisão de Lore — não reutiliza o status da Tradução Local.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Servidor inacessível é representado por {@code servidorOnline == false} e
 * {@code modeloCarregado == false}, com a causa técnica na {@code mensagem}.
 */
public record StatusRevisaoLoreLlm(boolean servidorOnline, boolean modeloCarregado, String mensagem) {
}
