package org.traducao.projeto.raspagemRevisao.application;

/**
 * PROPÓSITO DE NEGÓCIO: comunica ao painel o desfecho real da Opção 6, separando
 * correções aplicadas de problemas que ainda exigem atenção.
 *
 * <p>INVARIANTES DO DOMÍNIO: pendências nunca produzem status de conclusão
 * integral; contadores negativos são normalizados para zero.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de arquivos gera status
 * {@code SEM_ARQUIVOS}; o record não lança exceções por contagem inválida.
 */
public record ResultadoRevisaoLegendas(
    int arquivosAnalisados,
    int falasCorrigidas,
    int falasComProblema,
    int falasPendentes
) {
    public ResultadoRevisaoLegendas {
        arquivosAnalisados = Math.max(0, arquivosAnalisados);
        falasCorrigidas = Math.max(0, falasCorrigidas);
        falasComProblema = Math.max(0, falasComProblema);
        falasPendentes = Math.max(0, falasPendentes);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece um estado único para cor, banner e telemetria.
     * <p>INVARIANTES DO DOMÍNIO: pendência tem precedência sobre conclusão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: zero arquivos retorna {@code SEM_ARQUIVOS}.
     */
    public String status() {
        if (arquivosAnalisados == 0) return "SEM_ARQUIVOS";
        if (falasPendentes > 0) return "CONCLUIDO_COM_PENDENCIAS";
        return "CONCLUIDO";
    }
}
