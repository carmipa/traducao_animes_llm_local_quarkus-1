package org.traducao.projeto.correcaoLegendas.domain;

import java.util.List;

/**
 * Resultado da correção: {@code curados} conta ARQUIVOS modificados;
 * {@code falasCuradas} e {@code corrigidosLlm} contam FALAS (linhas) — a
 * telemetria usa apenas contagens de falas para não misturar unidades.
 */
public record ResultadoCorrecaoLegendas(
    int curados,
    int falasCuradas,
    int corrigidosLlm,
    int semAlteracao,
    int semPar,
    int traducaoAusente,
    int totalErros,
    List<String> erros,
    String relatorioJson
) {
    public boolean teveErros() {
        return totalErros > 0;
    }

    public int totalArquivos() {
        return curados + semAlteracao;
    }

    public int totalArquivosAnalisados() {
        return curados + semAlteracao + semPar + totalErros;
    }
}
