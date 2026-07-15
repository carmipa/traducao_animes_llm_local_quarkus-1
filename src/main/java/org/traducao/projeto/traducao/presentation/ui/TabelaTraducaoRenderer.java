package org.traducao.projeto.traducao.presentation.ui;

import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Monta a tabela por arquivo do lote de tradução
 * (Arquivo | Lore | Falas | Cache | Traduzidas | Avisos | Status) para o console
 * da UI, dando a Paulo a visão granular que o "sucesso" agregado escondia.
 *
 * <p>INVARIANTES DO DOMÍNIO: larguras ajustadas ao maior valor; só de
 * apresentação — não decide nada sobre a tradução.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem resultados, devolve string vazia; não lança.
 */
public final class TabelaTraducaoRenderer {

    private TabelaTraducaoRenderer() {
    }

    public static String render(List<ResultadoTraducaoArquivo> resultados) {
        if (resultados == null || resultados.isEmpty()) {
            return "";
        }

        List<String[]> linhas = new ArrayList<>();
        linhas.add(new String[]{"Arquivo", "Lore", "Falas", "Cache", "Traduzidas", "Avisos", "Status"});
        for (ResultadoTraducaoArquivo r : resultados) {
            linhas.add(new String[]{
                nvl(r.arquivo()),
                nvl(r.lore()),
                String.valueOf(r.falasTraduziveis()),
                String.valueOf(r.falasDoCache()),
                String.valueOf(r.falasTraduzidas()),
                String.valueOf(r.avisos()),
                r.status().getRotulo()
            });
        }

        int colunas = linhas.get(0).length;
        int[] largura = new int[colunas];
        for (String[] linha : linhas) {
            for (int c = 0; c < colunas; c++) {
                largura[c] = Math.max(largura[c], linha[c].length());
            }
        }

        StringBuilder sb = new StringBuilder("\n");
        appendLinha(sb, linhas.get(0), largura);
        appendSeparador(sb, largura);
        for (int i = 1; i < linhas.size(); i++) {
            appendLinha(sb, linhas.get(i), largura);
        }
        return sb.toString();
    }

    private static void appendLinha(StringBuilder sb, String[] celulas, int[] largura) {
        for (int c = 0; c < celulas.length; c++) {
            if (c > 0) {
                sb.append("  ");
            }
            sb.append(String.format("%-" + largura[c] + "s", celulas[c]));
        }
        sb.append('\n');
    }

    private static void appendSeparador(StringBuilder sb, int[] largura) {
        for (int c = 0; c < largura.length; c++) {
            if (c > 0) {
                sb.append("  ");
            }
            sb.append("-".repeat(largura[c]));
        }
        sb.append('\n');
    }

    private static String nvl(String valor) {
        return valor != null ? valor : "—";
    }
}
