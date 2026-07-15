package org.traducao.projeto.traducaoCorrige.domain;

/**
 * PROPÓSITO DE NEGÓCIO: resume de forma verificável o resultado de uma operação
 * sobre a pasta de cache para que console, API, relatório e telemetria não
 * anunciem sucesso quando arquivos falharam.
 *
 * <p>INVARIANTES DO DOMÍNIO: contadores nunca são negativos; uma execução com
 * falhas ou pendências não possuem status {@code CONCLUIDO}; cancelamento tem
 * precedência sobre os demais estados.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: record imutável; entradas negativas são
 * normalizadas para zero pelo construtor compacto.
 */
public record ResultadoManutencaoCache(
    int arquivosAnalisados,
    int arquivosAlterados,
    int itensDetectados,
    int itensCorrigidos,
    int itensIgnorados,
    int itensPendentes,
    int falhas,
    boolean cancelado
) {
    /**
     * PROPÓSITO DE NEGÓCIO: normaliza totais antes de expô-los à UI/telemetria.
     * <p>INVARIANTES DO DOMÍNIO: nenhum contador exibido é negativo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: valores negativos viram zero.
     */
    public ResultadoManutencaoCache {
        arquivosAnalisados = Math.max(0, arquivosAnalisados);
        arquivosAlterados = Math.max(0, arquivosAlterados);
        itensDetectados = Math.max(0, itensDetectados);
        itensCorrigidos = Math.max(0, itensCorrigidos);
        itensIgnorados = Math.max(0, itensIgnorados);
        itensPendentes = Math.max(0, itensPendentes);
        falhas = Math.max(0, falhas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece o estado final canônico exibido ao Paulo e
     * persistido na telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: cancelamento precede falha; ausência de arquivo
     * é diferenciada de execução concluída sem correções.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; sempre retorna um status.
     */
    public String status() {
        if (cancelado) return "CANCELADO";
        if (falhas > 0) return "CONCLUIDO_COM_FALHAS";
        if (arquivosAnalisados == 0) return "NENHUM_CACHE_ENCONTRADO";
        if (itensPendentes() > 0) return "CONCLUIDO_COM_PENDENCIAS";
        return "CONCLUIDO";
    }

}
