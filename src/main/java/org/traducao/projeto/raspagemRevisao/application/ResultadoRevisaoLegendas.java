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
    int falasPendentes,
    int arquivosCegos
) {
    public ResultadoRevisaoLegendas {
        arquivosAnalisados = Math.max(0, arquivosAnalisados);
        falasCorrigidas = Math.max(0, falasCorrigidas);
        falasComProblema = Math.max(0, falasComProblema);
        falasPendentes = Math.max(0, falasPendentes);
        arquivosCegos = Math.max(0, arquivosCegos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece um estado único para cor, banner e telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: cegueira tem precedência sobre pendência, e pendência sobre
     * conclusão. A ordem não é estética: <b>pendência é "eu olhei e falta"; cegueira é "eu não
     * olhei"</b>, e não saber é pior que saber que falta.
     *
     * <p><b>Prejuízo medido em 17/08/2026:</b> com a pasta de legendas em inglês apontada para o
     * lugar errado, 4 falas atravessaram a revisão sem serem comparadas com nada e o desfecho saiu
     * {@code CONCLUIDO} — banner verde, {@code [SUCESSO]}, idêntico ao de uma obra realmente
     * limpa. É a regra 12: "nada a processar" e "cego" não podem produzir o mesmo sinal. O risco
     * era concreto: a ordem dos dois campos de pasta foi invertida na tela em 16/08, e havia ~19
     * obras para passar uma a uma.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: zero arquivos retorna {@code SEM_ARQUIVOS}.
     */
    public String status() {
        if (arquivosAnalisados == 0) return "SEM_ARQUIVOS";
        if (arquivosCegos > 0) return "CONCLUIDO_SEM_REFERENCIA";
        if (falasPendentes > 0) return "CONCLUIDO_COM_PENDENCIAS";
        return "CONCLUIDO";
    }
}
