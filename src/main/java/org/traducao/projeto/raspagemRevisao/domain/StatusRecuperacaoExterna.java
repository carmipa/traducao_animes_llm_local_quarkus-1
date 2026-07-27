package org.traducao.projeto.raspagemRevisao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: desfecho de uma tentativa de recuperar uma fala num tradutor de máquina
 * externo, no vocabulário DESTA fatia. É a cópia consciente do enum equivalente de
 * {@code raspagemCorrecao} — o contrato do projeto proíbe uma fatia importar tipo de outra, e a
 * FASE 2 já resolveu o mesmo problema assim quando {@code correcaoLegendas} passou a ter o próprio
 * resumo de telemetria em vez de importar o da fatia vizinha.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>As constantes têm os MESMOS nomes das da fatia de origem, e isso é deliberado: a revisão
 *       imprime {@code status()} direto no painel do operador. Nome diferente aqui mudaria a saída
 *       vista pelo usuário sem mudar comportamento nenhum — divergência pura.</li>
 *   <li>Enum puro de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de causa; não lança. Todo status diferente de {@link #SUCESSO} implica manter a
 * tradução atual da legenda.
 */
public enum StatusRecuperacaoExterna {
    /** Tradução válida e diferente do original. */
    SUCESSO,
    /** O provedor devolveu texto idêntico ao original — nada a corrigir. */
    SEM_ALTERACAO,
    /** HTTP transitório, timeout ou falha de rede. */
    FALHA_TRANSITORIA,
    /** HTTP não transitório, resposta em formato inesperado ou sem segmentos traduzíveis. */
    RESPOSTA_INVALIDA,
    /** Marcador de tag/quebra mutilado ou tag ASS perdida na volta da tradução. */
    TAG_CORROMPIDA
}
