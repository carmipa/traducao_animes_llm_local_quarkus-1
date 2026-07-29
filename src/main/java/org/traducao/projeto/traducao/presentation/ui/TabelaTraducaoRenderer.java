package org.traducao.projeto.traducao.presentation.ui;

import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: Monta a tabela por arquivo do lote de tradução
 * (Arquivo | Lore | Falas | Cache | Traduzidas | Pend. | Tempo | Ritmo | Status)
 * para o console da UI, dando a Paulo a visão granular que o "sucesso" agregado
 * escondia.
 *
 * <p>{@code Pend.} substitui a antiga coluna {@code Avisos}: avisos incluem
 * mensagens informativas (cache invalidado por lore, reprocessamento confirmado)
 * que não deixam nenhuma fala por traduzir, então a coluna respondia a pergunta
 * errada — quem lê a tabela quer saber o que FALTA. {@code Tempo} e {@code Ritmo}
 * respondem "qual episódio segurou o lote", que antes só saía cronometrando o log
 * à mão.
 *
 * <p>INVARIANTES DO DOMÍNIO: larguras ajustadas ao maior valor; só de
 * apresentação — não decide nada sobre a tradução. Ritmo é derivado
 * ({@code falasTraduzidas / tempo}) e some quando o tempo é zero, e não fabrica
 * "0/min" para arquivo bloqueado que nunca chegou ao LLM.
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
        linhas.add(new String[]{"Arquivo", "Lore", "Falas", "Cache", "Traduzidas", "Pend.", "Tempo", "Ritmo", "Status"});
        for (ResultadoTraducaoArquivo r : resultados) {
            linhas.add(new String[]{
                nvl(r.arquivo()),
                nvl(r.lore()),
                String.valueOf(r.falasTraduziveis()),
                String.valueOf(r.falasDoCache()),
                String.valueOf(r.falasTraduzidas()),
                String.valueOf(r.falasPendentes()),
                formatarDuracao(r.tempoMs()),
                formatarRitmo(r.falasTraduzidas(), r.tempoMs()),
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

    /**
     * PROPÓSITO DE NEGÓCIO: escreve a duração de um episódio na forma que Paulo lê
     * de relance no console ({@code 6m48s}, {@code 42s}), sem obrigar a converter
     * milissegundos de cabeça.
     *
     * <p>INVARIANTES DO DOMÍNIO: zero ou negativo devolve travessão — arquivo
     * bloqueado ou com falha não tem duração medida e não pode aparecer como
     * "0s", que se leria como "foi instantâneo".
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; não lança.
     */
    static String formatarDuracao(long ms) {
        if (ms <= 0) {
            return "—";
        }
        long totalSegundos = ms / 1000;
        long minutos = totalSegundos / 60;
        long segundos = totalSegundos % 60;
        return minutos > 0 ? minutos + "m" + segundos + "s" : segundos + "s";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dá a vazão do episódio em falas por minuto — a medida
     * que compara tamanhos de lote e modelos entre si, coisa que "tempo total"
     * sozinho não permite porque episódios têm contagens de falas diferentes.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige tempo E falas positivos. Sem tempo medido
     * não há vazão; com zero falas traduzidas (tudo veio do cache) a divisão
     * daria "0/min", que se leria como lentidão em vez de ausência de trabalho.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: função pura; não lança.
     */
    static String formatarRitmo(int falasTraduzidas, long ms) {
        if (ms <= 0 || falasTraduzidas <= 0) {
            return "—";
        }
        return Math.round(falasTraduzidas * 60000.0 / ms) + "/min";
    }
}
