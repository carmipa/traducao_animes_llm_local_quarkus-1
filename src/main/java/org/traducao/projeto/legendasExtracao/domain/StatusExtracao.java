package org.traducao.projeto.legendasExtracao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: Classifica o desfecho da tentativa de extrair a legenda
 * de um único vídeo, para a UI e a telemetria distinguirem "não tinha a faixa"
 * de "falhou de verdade" de "já existia" — informação que Paulo usa para decidir
 * se reprocessa, troca de formato ou ignora o item.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada vídeo processado termina em exatamente um
 * status. {@link #JA_EXISTE} nunca sobrescreve arquivo; {@link #TIMEOUT} é
 * sempre separado de {@link #FALHA} para a telemetria contabilizá-los à parte.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: enum puro, não lança. O rótulo é sempre
 * não-nulo (definido no construtor).
 */
public enum StatusExtracao {
    SUCESSO("Sucesso"),
    FAIXA_NAO_ENCONTRADA("Faixa não encontrada"),
    JA_EXISTE("Já existe (preservado)"),
    FALHA("Falha"),
    TIMEOUT("Timeout");

    private final String rotulo;

    StatusExtracao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
