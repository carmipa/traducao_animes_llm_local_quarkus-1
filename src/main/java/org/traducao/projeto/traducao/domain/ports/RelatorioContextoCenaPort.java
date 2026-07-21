package org.traducao.projeto.traducao.domain.ports;

import org.traducao.projeto.traducao.domain.contextocena.RegistroExecucaoContextoCena;

/**
 * PROPÓSITO DE NEGÓCIO: porta do relatório A/B da correção de gênero por contexto de cena.
 * Diferente da telemetria canônica (que deduplica por episódio — o registro mais recente vence),
 * este destino é APPEND-ONLY: guarda uma linha por execução para que o braço A (flag desligada)
 * e o braço B (flag ligada) do MESMO episódio coexistam e possam ser comparados. Existe só para
 * o piloto do contexto de cena; fora do experimento (flag {@code relatorio-ab} desligada) nada é
 * escrito.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Append-only: NUNCA deduplica, NUNCA sobrescreve linha anterior — o oposto da telemetria
 *       canônica, por design.</li>
 *   <li>A medição chega pronta da aplicação; o adaptador apenas carimba o envelope de execução
 *       (runId, instante) e persiste.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Observabilidade nunca derruba a tradução: falha de I/O ao gravar é registrada em log e
 * absorvida — a execução do episódio segue normalmente.
 */
public interface RelatorioContextoCenaPort {

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta (append-only) uma linha de medição do experimento A/B.
     * <p>INVARIANTES DO DOMÍNIO: nunca substitui linha anterior; cada chamada gera um novo registro.
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de I/O é logado e absorvido; não propaga ao pipeline.
     */
    void registrar(RegistroExecucaoContextoCena registro);
}
