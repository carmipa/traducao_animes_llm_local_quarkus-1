package org.traducao.projeto.telemetria;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: representa uma fotografia sanitizada e coerente do
 * hardware da máquina que gerou o snapshot público de telemetria.
 *
 * <p>INVARIANTES DO DOMÍNIO: todos os componentes pertencem à mesma máquina e
 * são detectados automaticamente; não inclui usuário, hostname, IP, serial,
 * MAC, caminhos ou identificadores de hardware.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campos indisponíveis ficam nulos e a lista
 * de GPUs fica vazia, sem recorrer a valores manuais de outra máquina.
 */
public record AmbienteExecucaoDataset(
    String fabricante,
    String modeloMaquina,
    String cpu,
    String gpuPrincipal,
    List<String> gpusDetectadas,
    Integer ramTotalGb,
    String sistemaOperacional,
    String arquitetura,
    boolean hardwareColetadoAutomaticamente
) {
    /**
     * PROPÓSITO DE NEGÓCIO: normaliza a coleção de GPUs antes de expor a
     * fotografia imutável do ambiente no dataset.
     *
     * <p>INVARIANTES DO DOMÍNIO: a lista nunca é nula nem mutável e contém apenas
     * os nomes já sanitizados pela coleta local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula é convertida em lista vazia,
     * preservando os demais componentes detectados.
     */
    public AmbienteExecucaoDataset {
        gpusDetectadas = gpusDetectadas == null ? List.of() : List.copyOf(gpusDetectadas);
    }
}
