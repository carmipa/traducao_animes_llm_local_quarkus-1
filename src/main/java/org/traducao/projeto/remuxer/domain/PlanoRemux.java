package org.traducao.projeto.remuxer.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: representa o pareamento auditável entre vídeos e
 * legendas antes de qualquer chamada ao mkvmerge.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada legenda participa de no máximo uma tarefa;
 * cada destino é único; ambiguidades e ausências nunca viram pareamentos
 * silenciosos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: problemas de conteúdo são devolvidos como
 * avisos e contadores; falhas de leitura da pasta lançam {@link RemuxerException}.
 */
public record PlanoRemux(
    List<RemuxTarefa> tarefas,
    int videosDetectados,
    int legendasDetectadas,
    int videosSemLegenda,
    int pareamentosAmbiguos,
    List<String> avisos
) {
    /**
     * PROPÓSITO DE NEGÓCIO: congela tarefas e avisos para impedir alteração do
     * plano durante a execução do lote.
     *
     * <p>INVARIANTES DO DOMÍNIO: listas públicas nunca são nulas nem mutáveis.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula é normalizada para vazia.
     */
    public PlanoRemux {
        tarefas = tarefas == null ? List.of() : List.copyOf(tarefas);
        avisos = avisos == null ? List.of() : List.copyOf(avisos);
    }
}
