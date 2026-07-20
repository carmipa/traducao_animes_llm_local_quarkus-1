package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: reforça DETERMINISTICAMENTE a terminologia oficial de uma obra
 * na legenda já traduzida. Quando o LLM traduz um termo de mundo que a lore manda manter
 * no idioma original (ex.: "Legion" virou "Legião", "Undertaker" virou "Coveiro"), este
 * serviço restaura a grafia canônica — sem marcador, sem rede e sem depender do modelo.
 * É o complemento pós-tradução do prompt de lore: o prompt PEDE ao LLM; este serviço
 * GARANTE nas formas-ruim conhecidas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só restaura uma forma-ruim quando o texto ORIGINAL (EN) contém o termo canônico
 *       correspondente — nunca altera uma tradução legítima que não veio daquele termo.</li>
 *   <li>Comparações por fronteira de palavra, ignorando caixa; o termo canônico é inserido
 *       exatamente como definido na lore.</li>
 *   <li>Aplica entradas do mapa da frase mais longa para a mais curta — evita que
 *       "Vazio"→"Void" destrua "Genoma do Vazio"→"Void Genome" antes da frase completa.</li>
 *   <li>Restaura NO MÁXIMO tantas formas-ruim quantas o termo canônico aparece no original,
 *       priorizando as capitalizadas — não corrompe um homógrafo comum minúsculo
 *       (ex.: "O Vazio deixou tudo vazio." → "O Void deixou tudo vazio.", não "tudo Void").</li>
 *   <li>Nunca pode deixar a linha PIOR: mapa vazio ou sem casamento devolve o texto
 *       traduzido inalterado (pior caso = comportamento de hoje). Classe sem estado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Argumentos nulos ou mapa vazio devolvem o texto traduzido como recebido; não lança.
 */
@Component
public class EnforcadorTermosLore {

    /**
     * PROPÓSITO DE NEGÓCIO: restaura os termos canônicos da lore na fala traduzida.
     *
     * <p>INVARIANTES DO DOMÍNIO: para cada par (forma-ruim → canônico), só substitui se o
     * original contém o canônico e a tradução contém a forma-ruim; substituição por
     * fronteira de palavra, ignorando caixa.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code original}, {@code traduzido} ou {@code correcoes}
     * nulos/vazios devolvem {@code traduzido} inalterado.
     *
     * @param original o texto original (EN) da fala
     * @param traduzido a fala já traduzida (PT)
     * @param correcoes mapa forma-ruim (PT) → termo canônico a restaurar
     * @return a fala traduzida com os termos canônicos restaurados quando aplicável
     */
    public String reforcar(String original, String traduzido, Map<String, String> correcoes) {
        if (original == null || traduzido == null || correcoes == null || correcoes.isEmpty()) {
            return traduzido;
        }
        String resultado = traduzido;
        // Frases longas primeiro: "Genoma do Vazio" antes de "Vazio".
        var pares = correcoes.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, String> e) ->
                e.getKey() == null ? 0 : e.getKey().length()).reversed())
            .toList();
        for (Map.Entry<String, String> par : pares) {
            String formaRuim = par.getKey();
            String canonico = par.getValue();
            if (formaRuim == null || formaRuim.isBlank() || canonico == null) {
                continue;
            }
            int ocorrenciasCanonico = contarCanonico(original, canonico);
            if (ocorrenciasCanonico == 0) {
                continue; // o original não tinha o termo canônico (grafia exata): não mexe
            }
            // Restaura no MÁXIMO tantas formas-ruim quantas o canônico aparece no original,
            // priorizando as capitalizadas — não corrompe o homógrafo comum minúsculo.
            resultado = restaurarLimitado(resultado, padraoFormaRuim(formaRuim), canonico, ocorrenciasCanonico);
        }
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta quantas vezes o ORIGINAL (EN) contém o termo canônico na
     * grafia EXATA — os termos de lore são nomes próprios maiúsculos ("Legion"), distinguindo-os
     * da palavra comum minúscula ("legion") que NÃO deve disparar a restauração. A contagem é o
     * TETO de restaurações desta fala, para não corromper um homógrafo comum.
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; comparação SENSÍVEL à caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo vazio devolve {@code 0}.
     */
    private int contarCanonico(String texto, String termo) {
        if (termo.isBlank()) {
            return 0;
        }
        Matcher m = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])")
            .matcher(texto);
        int total = 0;
        while (m.find()) {
            total++;
        }
        return total;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca até {@code limite} ocorrências da forma-ruim pelo canônico,
     * priorizando as capitalizadas (nomes próprios) sobre as minúsculas — assim "Vazio" (Void)
     * é restaurado sem tocar "vazio" (empty) quando o EN traz "Void" apenas uma vez.
     *
     * <p>INVARIANTES DO DOMÍNIO: no máximo {@code limite} substituições; fronteira de palavra;
     * o restante do texto é preservado verbatim; uma única varredura da fala; o canônico é
     * inserido literalmente (sem interpretação de regex).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code limite <= 0} ou ausência de casamento devolve
     * o texto inalterado.
     */
    private String restaurarLimitado(String texto, Pattern formaRuimPat, String canonico, int limite) {
        if (limite <= 0) {
            return texto;
        }
        Matcher m = formaRuimPat.matcher(texto);
        List<int[]> ocorrencias = new ArrayList<>();
        while (m.find()) {
            int prioridade = Character.isUpperCase(texto.charAt(m.start())) ? 0 : 1;
            ocorrencias.add(new int[]{m.start(), m.end(), prioridade});
        }
        if (ocorrencias.isEmpty()) {
            return texto;
        }
        // Capitalizadas primeiro; desempate pela ordem do documento. Depois reordena por posição
        // para reconstruir o texto da esquerda para a direita.
        List<int[]> escolhidas = ocorrencias.stream()
            .sorted(Comparator.<int[]>comparingInt(o -> o[2]).thenComparingInt(o -> o[0]))
            .limit(limite)
            .sorted(Comparator.comparingInt(o -> o[0]))
            .toList();
        StringBuilder sb = new StringBuilder(texto.length());
        int ultimo = 0;
        for (int[] o : escolhidas) {
            sb.append(texto, ultimo, o[0]).append(canonico);
            ultimo = o[1];
        }
        sb.append(texto, ultimo, texto.length());
        return sb.toString();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compila o padrão da forma-ruim em PT — que pode aparecer em
     * qualquer caixa (ex.: "a legião" minúsculo depois de artigo).
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; {@code CASE_INSENSITIVE + UNICODE_CASE}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo é escapado; não injeta regex.
     */
    private Pattern padraoFormaRuim(String termo) {
        return Pattern.compile(
            "(?<![\\p{L}\\p{N}])" + Pattern.quote(termo) + "(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
