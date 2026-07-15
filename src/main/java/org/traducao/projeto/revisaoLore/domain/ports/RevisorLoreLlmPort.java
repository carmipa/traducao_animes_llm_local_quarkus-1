package org.traducao.projeto.revisaoLore.domain.ports;

import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;

import java.util.List;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: porta LLM própria da Revisão de Lore. Abstrai a única
 * interação com o modelo local de que a fatia precisa — verificar disponibilidade
 * e revisar terminologia/nomes de lore de uma fala já traduzida —, mantendo a
 * Revisão de Lore independente da stack LLM da Tradução Local.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@link #revisar} preserva integralmente os marcadores estruturais
 *       {@code [[TAGn]]} presentes na tradução; resposta que perca algum marcador
 *       nunca é publicada.</li>
 *   <li>A revisão usa o prompt de sistema de lore fornecido pelo chamador e o
 *       prompt de usuário próprio da fatia — nenhuma responsabilidade de tradução
 *       de lotes ou correção gramatical pertence a esta porta.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link #revisar} devolve {@link Optional#empty()} quando o LLM falha, a resposta
 * é inválida ou nenhuma linha preserva os marcadores — cabendo ao caso de uso
 * manter a tradução atual. {@link #verificarDisponibilidade} nunca lança: reporta
 * indisponibilidade via {@link StatusRevisaoLoreLlm}.
 */
public interface RevisorLoreLlmPort {

    /**
     * PROPÓSITO DE NEGÓCIO: confirma, antes de iniciar a sessão, se o servidor
     * LLM local está online e com um modelo carregado em memória.
     * <p>INVARIANTES DO DOMÍNIO: prefere o modelo configurado quando há vários
     * carregados; nunca escolhe às cegas um modelo não carregado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: servidor inacessível ou sem modelo
     * confirmado devolve status com {@code modeloCarregado == false}.
     */
    StatusRevisaoLoreLlm verificarDisponibilidade();

    /**
     * PROPÓSITO DE NEGÓCIO: revisa nomes próprios, locais, facções e termos de
     * lore na tradução PT-BR, usando a lore do contexto ativo.
     * <p>INVARIANTES DO DOMÍNIO: mantém todos os marcadores {@code [[TAGn]]} da
     * tradução; devolve uma única linha final.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve vazio se o LLM falhar ou a
     * resposta for inválida, sem alterar a legenda.
     */
    Optional<String> revisar(
        String promptSistemaRevisaoLore,
        String originalInglesMascarado,
        String traducaoPtMascarada,
        List<String> problemasDetectados
    );
}
