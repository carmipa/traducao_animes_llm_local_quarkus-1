package org.traducao.projeto.auditorConteudoLegendas.domain;

/**
 * PROPÓSITO DE NEGÓCIO: identifica qual escopo de análise de legenda o usuário
 * escolheu nas abas do painel — auditar só o arquivo original (EN), só o
 * traduzido (PT-BR) ou comparar os dois.
 *
 * <p>INVARIANTES DO DOMÍNIO: {@link #AMBAS} exige os dois arquivos e executa as
 * regras comparativas; {@link #ORIGINAL} e {@link #TRADUZIDO} exigem apenas um
 * arquivo e executam as regras de arquivo único.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #porNome(String)} devolve
 * {@link #AMBAS} para valor ausente ou desconhecido, preservando o comportamento
 * histórico do endpoint (compatível com chamadas que não enviam o campo).
 */
public enum ModoAuditoria {
    ORIGINAL,
    TRADUZIDO,
    AMBAS;

    /**
     * PROPÓSITO DE NEGÓCIO: converte o rótulo vindo da requisição em modo válido.
     * <p>INVARIANTES DO DOMÍNIO: a comparação ignora caixa e espaços.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula, em branco ou não mapeada
     * resulta em {@link #AMBAS} (default seguro e retrocompatível).
     */
    public static ModoAuditoria porNome(String valor) {
        if (valor == null || valor.isBlank()) {
            return AMBAS;
        }
        try {
            return ModoAuditoria.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return AMBAS;
        }
    }

    public boolean isArquivoUnico() {
        return this == ORIGINAL || this == TRADUZIDO;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: distingue "modo ausente" (retrocompatível, vira AMBAS)
     * de "modo preenchido porém inválido" (deve ser rejeitado com HTTP 400).
     * <p>INVARIANTES DO DOMÍNIO: nulo/em branco é reconhecido (ausente); um valor
     * preenchido só é reconhecido se casar com um dos modos, ignorando caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor desconhecido devolve {@code false}.
     */
    public static boolean reconhece(String valor) {
        if (valor == null || valor.isBlank()) {
            return true;
        }
        try {
            ModoAuditoria.valueOf(valor.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
