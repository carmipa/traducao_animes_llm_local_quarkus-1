package org.traducao.projeto.legendasExtracao.presentation.ui;

import org.traducao.projeto.legendasExtracao.domain.ItemExtracao;
import org.traducao.projeto.legendasExtracao.domain.RelatorioExtracao;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Monta a tabela simples de resultado da extração
 * (Vídeo | Formato | Track | Arquivo gerado | Status) que aparece nos consoles
 * da UI web e do CLI, dando a Paulo a visão por vídeo — inclusive qual Track ID
 * foi extraído — que os contadores agregados não mostravam.
 *
 * <p>INVARIANTES DO DOMÍNIO: colunas com largura ajustada ao maior valor;
 * campos ausentes ({@code trackId}/{@code arquivoGerado} nulos) viram "—". Só de
 * apresentação — não decide nada sobre a extração.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem itens, devolve string vazia (o chamador
 * simplesmente não imprime). Não lança.
 */
public final class TabelaExtracaoRenderer {

    private static final String TRACO = "—";

    private TabelaExtracaoRenderer() {
    }

    public static String render(RelatorioExtracao relatorio) {
        List<ItemExtracao> itens = relatorio.getItens();
        if (itens.isEmpty()) {
            return "";
        }

        List<String[]> linhas = new ArrayList<>();
        linhas.add(new String[]{"Vídeo", "Formato", "Track", "Arquivo gerado", "Status"});
        for (ItemExtracao it : itens) {
            linhas.add(new String[]{
                nvl(it.video()),
                nvl(it.formato()),
                it.trackId() != null ? String.valueOf(it.trackId()) : TRACO,
                it.arquivoGerado() != null ? it.arquivoGerado() : TRACO,
                it.status().getRotulo()
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
        return valor != null ? valor : TRACO;
    }
}
