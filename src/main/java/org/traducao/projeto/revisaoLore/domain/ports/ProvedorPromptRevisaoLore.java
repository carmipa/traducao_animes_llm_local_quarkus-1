package org.traducao.projeto.revisaoLore.domain.ports;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: contrato de contexto próprio da Revisão de Lore (Opção 7) —
 * fornece o prompt de sistema da obra e o mapa de terminologia canônica usado no
 * reforço determinístico. É o sistema de contexto SEPARADO da fatia {@code contexto}
 * da tradução (que a revisão de lore não pode importar), com IDs equivalentes.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@link #getId()} é único no catálogo; o mapa de correções
 * usa chave = forma-ruim em PT e valor = termo canônico a restaurar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: implementações não fazem I/O; o mapa padrão é
 * vazio (nenhum reforço determinístico de terminologia para a obra).
 */
public interface ProvedorPromptRevisaoLore {

    String getId();

    String getNomeExibicao();

    String obterPromptSistema();

    /**
     * PROPÓSITO DE NEGÓCIO: termos de lore que o LLM tende a localizar indevidamente e
     * que a revisão deve restaurar deterministicamente na grafia oficial.
     *
     * <p>INVARIANTES DO DOMÍNIO: chave = forma-ruim em PT (ex.: "Titãs"); valor = canônico
     * (ex.: "Titans"). A restauração só ocorre quando o original EN contém o canônico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: padrão devolve mapa vazio (sem reforço).
     */
    default Map<String, String> correcoesTerminologia() {
        return Map.of();
    }
}
