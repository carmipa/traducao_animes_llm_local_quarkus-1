package org.traducao.projeto.traducao.presentation.ui;

import org.traducao.projeto.traducao.domain.ResultadoTraducaoArquivo;
import org.traducao.projeto.traducao.domain.ResumoPendencia;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: monta o bloco de fechamento do lote de tradução — tempo,
 * vazão, destino das falas e as pendências agrupadas por causa —, respondendo no
 * console as perguntas que o operador fazia depois de cada rodada: quanto demorou,
 * quanto rendeu, o que sobrou e por quê.
 *
 * <p>Até 2026-07-29 o fechamento imprimia apenas a contagem de arquivos por status
 * ("0 concluído(s), 1 parcial(is)"). Tempo por episódio e causa das pendências já
 * eram medidos pelo use case e gravados em {@code logs/telemetria_traducao.json},
 * mas não chegavam à tela: para saber que um episódio levou 6min48s ou que as seis
 * pendências eram todas ECO de diálogo era preciso abrir o JSON à mão. Este renderer
 * não mede nada novo — só deixa de descartar o que já vinha no
 * {@link ResultadoTraducaoArquivo}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Puramente de apresentação: não decide status, não relê arquivo de telemetria
 *       e não altera nada do lote.</li>
 *   <li>Só arquivos que efetivamente rodaram entram no tempo e na vazão. Bloqueado e
 *       falho têm {@code tempoMs == 0} e diluiriam a média para baixo, fazendo um lote
 *       parecer mais lento do que foi.</li>
 *   <li>Linha omitida é diferente de linha zerada: PENDÊNCIAS só aparece quando há
 *       pendência. Uma linha "PENDÊNCIAS 0" em lote limpo treina o olho a ignorar a
 *       seção justamente quando ela passa a ter conteúdo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista nula ou vazia devolve string vazia. Não lança.
 */
public final class RelatorioLoteRenderer {

    private static final String LARGURA = "=".repeat(72);

    private RelatorioLoteRenderer() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: consolida o lote inteiro no bloco final do console.
     *
     * <p>INVARIANTES DO DOMÍNIO: percentual calculado sobre falas traduzíveis; seções
     * sem dado são omitidas em vez de impressas zeradas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula/vazia devolve {@code ""}.
     */
    public static String render(List<ResultadoTraducaoArquivo> resultados, String rotuloStatus, int tamanhoLote) {
        if (resultados == null || resultados.isEmpty()) {
            return "";
        }

        long concluidos = contar(resultados, StatusArquivoTraducao.CONCLUIDO);
        long parciais = contar(resultados, StatusArquivoTraducao.PARCIAL);
        long naoProcessados = resultados.size() - concluidos - parciais;

        int traduziveis = somar(resultados, ResultadoTraducaoArquivo::falasTraduziveis);
        int peloLlm = somar(resultados, ResultadoTraducaoArquivo::falasTraduzidas);
        int doCache = somar(resultados, ResultadoTraducaoArquivo::falasDoCache);
        int pendentes = somar(resultados, ResultadoTraducaoArquivo::falasPendentes);

        // Só quem rodou de verdade conta para tempo e vazão — ver invariante da classe.
        List<ResultadoTraducaoArquivo> medidos = resultados.stream().filter(r -> r.tempoMs() > 0).toList();
        long tempoTotalMs = medidos.stream().mapToLong(ResultadoTraducaoArquivo::tempoMs).sum();

        StringBuilder sb = new StringBuilder("\n").append(LARGURA).append('\n');
        sb.append("  [").append(rotuloStatus).append("] TRADUÇÃO LOCAL VIA LLM\n");
        sb.append(LARGURA).append('\n');

        sb.append(linha("LOTE", resultados.size() + " arquivo(s) — " + concluidos + " concluído(s), "
            + parciais + " parcial(is), " + naoProcessados + " falha/bloqueio"));

        if (tempoTotalMs > 0) {
            StringBuilder tempo = new StringBuilder(TabelaTraducaoRenderer.formatarDuracao(tempoTotalMs)).append(" total");
            if (medidos.size() > 1) {
                tempo.append(" · ").append(TabelaTraducaoRenderer.formatarDuracao(tempoTotalMs / medidos.size()))
                    .append("/arquivo");
            }
            tempo.append(" · ").append(TabelaTraducaoRenderer.formatarRitmo(peloLlm, tempoTotalMs));
            if (tamanhoLote > 0) {
                tempo.append(" · lote=").append(tamanhoLote);
            }
            sb.append(linha("TEMPO", tempo.toString()));
        }

        StringBuilder falas = new StringBuilder(traduziveis + " traduzível(is) → " + peloLlm + " pelo LLM");
        if (traduziveis > 0) {
            // pt-BR fixo: o relatório é lido em português e o teste congela "97,6%" /
            // "100,0%". String.format sem Locale segue o default da JVM e em CI Linux
            // (en_US) imprimia ponto, falhando a suíte sem mudança de lógica.
            falas.append(String.format(Locale.forLanguageTag("pt-BR"), " (%.1f%%)",
                100.0 * peloLlm / traduziveis));
        }
        falas.append(" · ").append(doCache).append(" do cache · ").append(pendentes).append(" pendente(s)");
        sb.append(linha("FALAS", falas.toString()));

        String porCausa = agruparPendencias(resultados);
        if (!porCausa.isEmpty()) {
            sb.append(linha("PENDÊNCIAS", porCausa));
        }

        String piores = episodiosMaisLentos(medidos);
        if (!piores.isEmpty()) {
            sb.append(linha("MAIS LENTOS", piores));
        }

        sb.append(LARGURA).append('\n');
        return sb.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: soma as pendências de todos os arquivos por
     * categoria×causa, transformando dezenas de avisos soltos em uma linha que diz
     * onde atacar primeiro.
     *
     * <p>INVARIANTES DO DOMÍNIO: ordena por quantidade decrescente — o maior ofensor
     * aparece primeiro; combinações distintas nunca se fundem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem pendência devolve {@code ""}.
     */
    private static String agruparPendencias(List<ResultadoTraducaoArquivo> resultados) {
        Map<String, Integer> acumulado = new LinkedHashMap<>();
        for (ResultadoTraducaoArquivo r : resultados) {
            for (ResumoPendencia p : r.pendenciasPorCausa()) {
                acumulado.merge(p.categoria() + "/" + p.causaRaiz(), p.quantidade(), Integer::sum);
            }
        }
        return acumulado.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(e -> e.getKey() + " " + e.getValue())
            .reduce((a, b) -> a + " · " + b)
            .orElse("");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aponta os episódios que seguraram o lote, para o operador
     * saber onde olhar sem reler a tabela inteira num lote de 50 arquivos.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aparece com mais de três arquivos medidos — abaixo
     * disso a tabela já mostra tudo e o destaque seria ruído.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code ""} quando não se aplica.
     */
    private static String episodiosMaisLentos(List<ResultadoTraducaoArquivo> medidos) {
        if (medidos.size() <= 3) {
            return "";
        }
        return medidos.stream()
            .sorted((a, b) -> Long.compare(b.tempoMs(), a.tempoMs()))
            .limit(3)
            .map(r -> r.arquivo() + " (" + TabelaTraducaoRenderer.formatarDuracao(r.tempoMs()) + ")")
            .reduce((a, b) -> a + " · " + b)
            .orElse("");
    }

    private static String linha(String rotulo, String valor) {
        return String.format("  %-11s %s%n", rotulo, valor);
    }

    private static long contar(List<ResultadoTraducaoArquivo> resultados, StatusArquivoTraducao status) {
        return resultados.stream().filter(r -> r.status() == status).count();
    }

    private static int somar(List<ResultadoTraducaoArquivo> resultados,
                             java.util.function.ToIntFunction<ResultadoTraducaoArquivo> campo) {
        return resultados.stream().mapToInt(campo).sum();
    }
}
