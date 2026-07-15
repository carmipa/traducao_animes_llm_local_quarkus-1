package org.traducao.projeto.traducao.domain;

/**
 * PROPÓSITO DE NEGÓCIO: Desfecho da tradução de um único arquivo de legenda,
 * para a tabela por arquivo e a telemetria distinguirem sucesso limpo, sucesso
 * com ressalvas, falha e bloqueio (entrada já traduzida).
 *
 * <p>INVARIANTES DO DOMÍNIO: cada arquivo processado recebe exatamente um status.
 * {@code PARCIAL} = traduziu mas houve avisos (falas mantidas sem tradução para
 * revisão); {@code BLOQUEADO} = entrada aparentava já estar em PT-BR e a
 * retradução não foi confirmada.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: enum puro; rótulo sempre não-nulo.
 */
public enum StatusArquivoTraducao {
    CONCLUIDO("Concluído"),
    PARCIAL("Parcial"),
    FALHOU("Falhou"),
    BLOQUEADO("Bloqueado (já traduzido)");

    private final String rotulo;

    StatusArquivoTraducao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
