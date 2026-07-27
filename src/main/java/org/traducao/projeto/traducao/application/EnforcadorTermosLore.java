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

    // A quebra de linha do ASS é a sequência literal "\N" — DOIS caracteres, e o segundo é a
    // LETRA N. Isso envenena toda fronteira de palavra por lookbehind: em "attack\NAxis" o
    // caractere imediatamente antes de "Axis" é 'N', então {@code (?<![\p{L}\p{N}])} falha e o
    // termo canônico fica INVISÍVEL para a restauração — que por isso nunca dispara.
    //
    // Medido no run completo de Gundam ZZ (47 episódios, 16.716 pares): 21,5% das falas têm a
    // quebra, 74 trazem um termo canônico colado nela, e entre elas estão 5 dos 8 casos de
    // "Axis -> Eixo" que escaparam. A separação é exata: termo colado na quebra NUNCA foi
    // corrigido; termo solto SEMPRE foi.
    //
    // Só o lado ESQUERDO precisa da alternativa: à direita do termo o caractere da quebra é a
    // barra invertida, que já não é letra nem dígito e portanto não quebra a asserção.
    private static final String INICIO_DE_TERMO = "(?:(?<=\\\\N)|(?<![\\p{L}\\p{N}]))";

    private static final String FIM_DE_TERMO = "(?![\\p{L}\\p{N}])";

    // A quebra tambem cai DENTRO de termo composto, e ai nao basta tratar a fronteira: a
    // legenda parte "Quin Mantha" em "Quin\NMantha", e Pattern.quote() procura um ESPACO
    // literal que nao esta la. Medido no run de ZZ: "Quin Mantha" ficou em 66,7% de preservacao
    // nas DUAS geracoes, antes e depois de reconhecer a quebra como fronteira — porque o furo
    // era outro. O separador interno passa a aceitar espaco OU a quebra do ASS.
    private static final String SEPARADOR_INTERNO = "(?:\\s|\\\\N)+";

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
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; comparação SENSÍVEL à caixa para termos
     * de UMA palavra (protege o homógrafo comum: "Void"≠"void", "Titans"≠"titans"), mas
     * INSENSÍVEL à caixa para termos MULTI-PALAVRA (compostos técnicos como "Mobile Suit",
     * "Beam Saber" não colidem com uma palavra comum minúscula do jeito que um nome próprio de
     * uma palavra colide) — assim "Mobile suits" no EN também conta como o canônico "Mobile Suits".
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo vazio devolve {@code 0}.
     */
    private int contarCanonico(String texto, String termo) {
        if (termo.isBlank()) {
            return 0;
        }
        boolean multiPalavra = termo.trim().indexOf(' ') >= 0;
        int flags = multiPalavra ? (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE) : 0;
        Matcher m = Pattern.compile(
            INICIO_DE_TERMO + corpoDoTermo(termo) + FIM_DE_TERMO, flags)
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
            INICIO_DE_TERMO + corpoDoTermo(termo) + FIM_DE_TERMO,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o corpo do padrão de um termo aceitando que a legenda o parta
     * numa quebra de linha. Sem isto, {@code "Quin Mantha"} nunca casa com {@code "Quin\NMantha"},
     * que é como o texto realmente chega quando o termo cai na virada da linha.
     *
     * <p>INVARIANTES DO DOMÍNIO: cada palavra continua ESCAPADA ({@code Pattern.quote}), então
     * um termo com metacaractere de regex ({@code "Gaza-C"}, {@code "A.E.U.G."}) segue sendo
     * comparado literalmente; só o separador entre palavras vira flexível. Termo de uma palavra
     * produz exatamente o mesmo padrão de antes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo vazio ou só com espaços devolve padrão vazio, que
     * não casa com nada — o chamador já trata isso antes.
     */
    private static String corpoDoTermo(String termo) {
        String[] palavras = termo.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < palavras.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR_INTERNO);
            }
            sb.append(Pattern.quote(palavras[i]));
        }
        return sb.toString();
    }
}
