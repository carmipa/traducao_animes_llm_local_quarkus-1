package org.traducao.projeto.traducao.domain;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Desfecho do LOTE de tradução (vários arquivos), para a
 * UI/telemetria pararem de mostrar "sucesso" quando houve arquivos com falha.
 *
 * <p>INVARIANTES DO DOMÍNIO: derivado dos status por arquivo, contando apenas os NÃO
 * bloqueados — {@code BLOQUEADO} (arquivo já traduzido num rerun) é neutro e não conta como
 * falha. Todos os não-bloqueados concluídos (ou lote 100% bloqueado) → {@code CONCLUIDO};
 * nenhum concluído → {@code FALHOU}; mistura com falha real → {@code CONCLUIDO_COM_FALHAS}.
 * {@code CANCELADO} é reservado para interrupção explícita do usuário.
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
        // BLOQUEADO = arquivo já traduzido (rerun): não é sucesso novo nem falha; fica FORA do
        // denominador para não contaminar o desfecho do lote.
        long bloqueados = resultados.stream()
            .filter(r -> r.status() == StatusArquivoTraducao.BLOQUEADO).count();
        long naoBloqueados = resultados.size() - bloqueados;
        if (naoBloqueados == 0) {
            return CONCLUIDO; // lote inteiramente já traduzido: nada falhou
        }
        long ok = resultados.stream().filter(r ->
            r.status() == StatusArquivoTraducao.CONCLUIDO || r.status() == StatusArquivoTraducao.PARCIAL).count();
        if (ok == naoBloqueados) {
            return CONCLUIDO;
        }
        if (ok == 0) {
            return FALHOU;
        }
        return CONCLUIDO_COM_FALHAS;
    }
}
