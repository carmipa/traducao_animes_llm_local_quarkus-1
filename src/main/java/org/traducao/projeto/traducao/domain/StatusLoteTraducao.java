package org.traducao.projeto.traducao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Desfecho do LOTE de tradução (vários arquivos), para a
 * UI/telemetria pararem de mostrar "sucesso" quando houve arquivos com falha.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivado dos status por arquivo — todos concluídos
 * (com ou sem ressalvas) → {@code CONCLUIDO}; nenhum concluído → {@code FALHOU};
 * mistura → {@code CONCLUIDO_COM_FALHAS}. {@code CANCELADO} é reservado para
 * interrupção explícita do usuário.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #consolidar(List)} nunca lança; lote
 * vazio devolve {@code FALHOU}.
 */
public enum StatusLoteTraducao {
    CONCLUIDO("Concluído"),
    CONCLUIDO_COM_FALHAS("Concluído com falhas"),
    FALHOU("Falhou"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusLoteTraducao(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public static StatusLoteTraducao consolidar(List<ResultadoTraducaoArquivo> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return FALHOU;
        }
        long ok = resultados.stream().filter(r ->
            r.status() == StatusArquivoTraducao.CONCLUIDO || r.status() == StatusArquivoTraducao.PARCIAL).count();
        if (ok == resultados.size()) {
            return CONCLUIDO;
        }
        if (ok == 0) {
            return FALHOU;
        }
        return CONCLUIDO_COM_FALHAS;
    }
}
