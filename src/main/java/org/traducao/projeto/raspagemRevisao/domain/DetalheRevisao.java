package org.traducao.projeto.raspagemRevisao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: a trilha auditável de UMA ocorrência da revisão — o que foi corrigido,
 * rejeitado ou bloqueado, com o texto antes e depois. É o que permite, semanas depois, responder
 * "por que esta fala mudou?" sem reprocessar o episódio.
 *
 * <p>Era um {@code record} privado dentro do caso de uso, o que impedia o relatório de existir como
 * responsabilidade separada: quem formata a trilha precisa enxergar o tipo dela.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code problemas} nunca é nulo e nunca é modificável — a lista chega copiada. Uma trilha de
 *       auditoria que o chamador pudesse alterar depois de registrada não seria trilha.</li>
 *   <li>Guarda o texto ANTES e DEPOIS, não só o desfecho: sem os dois, uma correção errada não é
 *       reconstituível.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Campos de texto ausentes são responsabilidade de quem formata, não deste record; ele não lança.
 *
 * @param arquivo nome do arquivo de legenda
 * @param evento índice da fala dentro do arquivo
 * @param estilo estilo ASS da linha
 * @param resultado desfecho da ocorrência (corrigida, rejeitada, bloqueada…)
 * @param problemas motivos apurados pela auditoria; nunca nulo
 * @param diagnostico explicação técnica quando houve falha
 * @param original texto em inglês usado como referência
 * @param antes tradução que existia antes da revisão
 * @param depois proposta do provedor
 */
public record DetalheRevisao(
    String arquivo,
    int evento,
    String estilo,
    String resultado,
    List<String> problemas,
    String diagnostico,
    String original,
    String antes,
    String depois
) {
    public DetalheRevisao {
        problemas = problemas == null ? List.of() : List.copyOf(problemas);
    }
}
