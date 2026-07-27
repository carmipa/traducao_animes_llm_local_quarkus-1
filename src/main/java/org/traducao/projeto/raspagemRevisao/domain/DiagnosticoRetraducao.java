package org.traducao.projeto.raspagemRevisao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: as métricas que separam uma revisão linguística de uma retradução
 * acidental do arquivo inteiro — quantas falas eram comparáveis e quantas ainda estão em inglês.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os contadores são a fotografia do documento DEPOIS da recuperação pelo cache. Medir antes
 *       bloquearia arquivos que a sincronização acabou de consertar.</li>
 *   <li>{@code deveBloquear} é derivado dos dois contadores por
 *       {@link PoliticaRetraducao#excedeLimiarRetraducaoEmMassa(int, int)} e vem junto para que
 *       quem consome não reimplemente o limiar.</li>
 *   <li>Record imutável de domínio: só JDK, sem framework, sem I/O.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Portador de métricas; não lança e não altera arquivos.
 *
 * @param falasAuditaveis falas de diálogo com original comparável
 * @param falasNaoTraduzidas quantas delas continuam idênticas ao inglês
 * @param deveBloquear se o arquivo deve ser recusado como retradução em massa
 */
public record DiagnosticoRetraducao(
    int falasAuditaveis,
    int falasNaoTraduzidas,
    boolean deveBloquear
) {
}
