package org.traducao.projeto.raspagemRevisao.domain;

import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: a identidade e o glossário da obra durante a revisão de UM arquivo. É o que
 * impede a revisão de "corrigir" Axis para Eixo ou traduzir o nome de um piloto: sem a lore ativa,
 * um revisor genérico trata termo canônico como erro.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os três campos vêm do MESMO contexto — id, lore e termos protegidos não se misturam entre
 *       obras. Foi um carimbo de proveniência divergente que fez um cache de Gundam 0083 ser
 *       revisado sob a lore de Guilty Crown.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de dados; não lança.
 *
 * @param id identificador do contexto (obra) ativo
 * @param lore texto da lore usado nos prompts e nas guardas
 * @param termosProtegidos termos que nenhum provedor pode traduzir
 */
public record ContextoRevisao(String id, String lore, Set<String> termosProtegidos) {
}
