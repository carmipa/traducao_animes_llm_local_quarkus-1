package org.traducao.projeto.qualidadeTraducao.application;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
 *   <li><b>O TETO VALE SÓ PARA AS MINÚSCULAS.</b> Uma forma-ruim CAPITALIZADA é restaurada
 *       sempre, sem limite; só as minúsculas disputam o orçamento "quantas vezes o canônico
 *       aparece no original". A razão é que capitalizada no meio da frase já é nome próprio,
 *       e não há homógrafo comum a proteger ali — o teto existe para o par
 *       "Vazio"(Void) vs "vazio"(empty), não para "Char Aznable".
 *       <p>Exemplo do que o teto FAZ: "O Vazio deixou tudo vazio." → "O Void deixou tudo
 *       vazio.", nunca "tudo Void".
 *       <p>Exemplo do que ele NÃO faz mais, e por quê: limitar as capitalizadas produzia
 *       falas MEIO corrigidas — "Precisamos deter o Eixo antes que o Eixo caia!" com "Axis"
 *       uma vez no inglês saía "...deter o Axis antes que o Eixo caia!", e a auditoria
 *       seguinte APROVAVA a linha (basta uma ocorrência canônica para o detector parar de
 *       acusar). Defeito invisível, gravado no .ass e logado em verde.
 *       <p>Selado por {@code EnforcadorTermosLoreTest.capitalizadasNaoSaoLimitadasPeloTeto} e
 *       {@code CorretorLoreDeterministicoTest.tetoDeOcorrenciasNaoCorrompeONumeroQuatro}.
 *       Uma auditoria de 2026-07-27 descreveu o teto como se fosse geral e recomendou
 *       "criar um teste" que já existia duas vezes — daí esta nota estar aqui e não só
 *       no método.</li>
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
    private static final String INICIO_DE_TERMO = FronteiraTermoAss.INICIO;

    private static final String FIM_DE_TERMO = FronteiraTermoAss.FIM;

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
        return reforcarContando(original, traduzido, correcoes).texto();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mesmo reforço, informando QUANTAS restaurações foram feitas por termo
     * canônico. Só quem executa a substituição sabe contá-la: qualquer tentativa de deduzir o
     * número de fora, comparando o texto antes e depois, é uma segunda implementação da regra e
     * erra onde a regra é sutil.
     *
     * <p>Foi medido: um contador externo que comparava ocorrências do canônico não creditava nada
     * quando a forma-ruim CONTÉM o canônico — {@code "Plee"→"Ple"} (Gundam ZZ),
     * {@code "Gouf Customizado"→"Gouf Custom"} (08th MS Team), {@code "Gundam Alexandre"→"Gundam
     * Alex"} (War in the Pocket), três obras distintas —, porque o canônico já aparecia no texto
     * antes da troca e o delta dava zero. O relatório saía "nenhuma alteração" tendo reescrito o
     * arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o texto devolvido é IDÊNTICO ao de {@link #reforcar}; a contagem
     * é indexada pelo termo CANÔNICO (várias formas-ruim convergem para o mesmo destino) e soma
     * exatamente as substituições efetivadas — nunca as tentadas. Texto inalterado devolve mapa
     * vazio, e mapa vazio com texto alterado é impossível por construção.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: argumentos nulos/vazios devolvem o traduzido com mapa
     * vazio; não lança.
     *
     * @param original o texto original (EN) da fala
     * @param traduzido a fala já traduzida (PT)
     * @param correcoes mapa forma-ruim (PT) → termo canônico a restaurar
     * @return o texto reforçado e quantas restaurações por termo canônico
     */
    public Reforco reforcarContando(String original, String traduzido, Map<String, String> correcoes) {
        if (original == null || traduzido == null || correcoes == null || correcoes.isEmpty()) {
            return new Reforco(traduzido, Map.of());
        }
        String resultado = traduzido;
        Map<String, Integer> restauracoes = new LinkedHashMap<>();
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
            Restauracao passo =
                restaurarLimitado(resultado, padraoFormaRuim(formaRuim), canonico, ocorrenciasCanonico);
            resultado = passo.texto();
            if (passo.quantidade() > 0) {
                restauracoes.merge(canonico, passo.quantidade(), Integer::sum);
            }
        }
        return new Reforco(resultado, restauracoes);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: força a forma PORTUGUESA de um termo que a lore manda TRADUZIR.
     * É o inverso de {@link #reforcar}: aquele restaura o termo inglês que o modelo traduziu,
     * este traduz o termo inglês que o modelo manteve.
     *
     * <h2>A cicatriz</h2>
     * {@code "Universal Century"} estava em {@code termosProtegidos} e por isso saía em inglês
     * POR REGRA. A medição de prontidão de 22/08/2026 achou <b>7 falas do Unicorn</b>
     * publicadas como {@code "Universal Century 0096."}, e Paulo decidiu que deviam sair
     * {@code "Século Universal 0096."}. Não havia mecanismo: a lore sabia proteger em inglês e
     * restaurar do inglês, e não sabia mandar traduzir.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li>Só age quando o termo inglês está no ORIGINAL <b>e</b> sobreviveu na TRADUÇÃO. Fala
     *       que o modelo já traduziu de outro jeito não é reescrita — o mecanismo conserta o que
     *       ficou em inglês, não impõe estilo sobre tradução legítima. É a mesma disciplina que
     *       fez {@code "móvel de combate"} ficar em paz em 101 falas do ZZ.</li>
     *   <li>Fronteira de termo igual à do reforço, inclusive a alternativa do {@code \N}: sem
     *       ela, termo colado na quebra de linha fica invisível — 74 falas do ZZ nessa forma.</li>
     *   <li>Frases longas primeiro: {@code "Side Four"} antes de {@code "Four"}, senão a troca
     *       curta come a longa.</li>
     * </ul>
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer argumento nulo/vazio devolve {@code traduzido}
     * inalterado; nunca lança.
     *
     * @param original o texto original (EN) da fala
     * @param traduzido a fala já traduzida (PT)
     * @param obrigatorias mapa termo EN → forma obrigatória em PT
     */
    public String traduzirObrigatorios(
        String original, String traduzido, Map<String, String> obrigatorias) {
        if (original == null || traduzido == null || obrigatorias == null || obrigatorias.isEmpty()) {
            return traduzido;
        }
        String resultado = traduzido;
        var pares = obrigatorias.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, String> e) ->
                e.getKey() == null ? 0 : e.getKey().length()).reversed())
            .toList();
        for (Map.Entry<String, String> par : pares) {
            String termoEn = par.getKey();
            String formaPt = par.getValue();
            if (termoEn == null || termoEn.isBlank() || formaPt == null || formaPt.isBlank()) {
                continue;
            }
            if (contarCanonico(original, termoEn) == 0) {
                continue; // o inglês não trazia o termo: não há o que traduzir
            }
            resultado = padraoFormaRuim(termoEn).matcher(resultado)
                .replaceAll(java.util.regex.Matcher.quoteReplacement(formaPt));
        }
        return resultado;
    }
    /**
     * PROPÓSITO DE NEGÓCIO: resultado do reforço — o texto e o que foi feito nele.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code restauracoesPorTermo} é imutável e indexado pelo termo
     * CANÔNICO; a soma dos valores é o número de substituições efetivadas nesta fala.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: record puro, sem I/O; nunca lança.
     *
     * @param texto a fala com os termos canônicos restaurados
     * @param restauracoesPorTermo termo canônico → quantas vezes foi restaurado nesta fala
     */
    public record Reforco(String texto, Map<String, Integer> restauracoesPorTermo) {

        /**
         * PROPÓSITO DE NEGÓCIO: congela o mapa na construção.
         * <p>INVARIANTES DO DOMÍNIO: mapa nunca nulo e sempre imutável.
         * <p>COMPORTAMENTO EM CASO DE FALHA: mapa nulo vira vazio em vez de lançar.
         */
        public Reforco {
            restauracoesPorTermo = restauracoesPorTermo == null
                ? Map.of() : Map.copyOf(restauracoesPorTermo);
        }

        /**
         * PROPÓSITO DE NEGÓCIO: total de substituições nesta fala.
         * <p>INVARIANTES DO DOMÍNIO: soma dos valores do mapa.
         * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio devolve zero.
         */
        public int total() {
            return restauracoesPorTermo.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    /** Resultado de uma passada de restauração: o texto novo e quantas trocas ocorreram nele. */
    private record Restauracao(String texto, int quantidade) {}

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
            INICIO_DE_TERMO + FronteiraTermoAss.corpo(termo) + FIM_DE_TERMO, flags)
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
     * o texto inalterado e quantidade zero.
     *
     * @return o texto após a passada e QUANTAS trocas foram efetivadas — este número é a única
     *         fonte confiável da contagem, porque é medido onde a substituição acontece
     */
    private Restauracao restaurarLimitado(String texto, Pattern formaRuimPat, String canonico, int limite) {
        if (limite <= 0) {
            return new Restauracao(texto, 0);
        }
        Matcher m = formaRuimPat.matcher(texto);
        List<int[]> ocorrencias = new ArrayList<>();
        while (m.find()) {
            int prioridade = maiusculaQueSignificaNomeProprio(texto, m.start()) ? 0 : 1;
            ocorrencias.add(new int[]{m.start(), m.end(), prioridade});
        }
        if (ocorrencias.isEmpty()) {
            return new Restauracao(texto, 0);
        }
        // O TETO vale só para as MINÚSCULAS. Ele existe para proteger o homógrafo comum
        // ("vazio"=empty vs "Vazio"=Void), e uma forma-ruim CAPITALIZADA no meio da frase já é um
        // nome próprio — não há homógrafo comum a proteger ali. Limitar as capitalizadas produzia
        // falas MEIO corrigidas, que é o pior desfecho: medido, "Precisamos deter o Eixo antes que
        // o Eixo caia!" com "Axis" uma vez no inglês saía "...deter o Axis antes que o Eixo caia!",
        // e a auditoria seguinte da revisão de lore aprovava a linha como limpa (basta UMA
        // ocorrência canônica para o detector parar de acusar). Defeito invisível, gravado no .ass
        // e logado em verde.
        List<int[]> capitalizadas = ocorrencias.stream().filter(o -> o[2] == 0).toList();
        int orcamentoMinusculas = Math.max(0, limite - capitalizadas.size());
        List<int[]> minusculas = ocorrencias.stream()
            .filter(o -> o[2] == 1)
            .limit(orcamentoMinusculas)
            .toList();
        List<int[]> escolhidas = Stream.concat(capitalizadas.stream(), minusculas.stream())
            .sorted(Comparator.comparingInt(o -> o[0]))
            .toList();
        StringBuilder sb = new StringBuilder(texto.length());
        int ultimo = 0;
        for (int[] o : escolhidas) {
            sb.append(texto, ultimo, o[0]).append(canonico);
            ultimo = o[1];
        }
        sb.append(texto, ultimo, texto.length());
        return new Restauracao(sb.toString(), escolhidas.size());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se a MAIÚSCULA de uma ocorrência é evidência de nome próprio.
     *
     * <p>INVARIANTES DO DOMÍNIO: só conta a maiúscula que a POSIÇÃO não explica. Em português
     * toda frase começa com maiúscula, então {@code "Vazio"} abrindo período não distingue o
     * nome próprio ({@code Void}) da palavra comum ({@code vazio}=empty) — a caixa ali é
     * obrigatória, não escolhida. Já uma maiúscula no MEIO da frase foi uma decisão de quem
     * escreveu, e é isso que a torna prova.
     *
     * <p>Sem esta distinção o teto vazava: {@code "Vazio é perigoso. Vazio deve ser destruído."}
     * com {@code Void} uma única vez no inglês restaurava as DUAS, porque as duas estavam
     * capitalizadas por início de frase. O teto existe justamente para não converter o homógrafo
     * comum, e a abertura de período era a porta que sobrou.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: índice fora do texto ou caractere não-letra devolve
     * {@code false} — na dúvida, trata como sem evidência, que é o lado seguro (entra no teto).
     *
     * @param texto a fala inteira
     * @param inicio índice onde a ocorrência começa
     * @return {@code true} só quando é maiúscula E não é abertura de período
     */
    private static boolean maiusculaQueSignificaNomeProprio(String texto, int inicio) {
        if (inicio < 0 || inicio >= texto.length() || !Character.isUpperCase(texto.charAt(inicio))) {
            return false;
        }
        for (int i = inicio - 1; i >= 0; i--) {
            char anterior = texto.charAt(i);
            if (Character.isWhitespace(anterior) || anterior == '"' || anterior == '\''
                || anterior == '(' || anterior == '-' || anterior == '—') {
                continue;
            }
            // A quebra do ASS é "\N" — dois caracteres — e também abre linha nova.
            if (anterior == 'N' && i > 0 && texto.charAt(i - 1) == '\\') {
                return false;
            }
            return anterior != '.' && anterior != '!' && anterior != '?' && anterior != ':'
                && anterior != ';' && anterior != '…';
        }
        return false; // só espaço/pontuação antes: é abertura da fala
    }

    /**
     * PROPÓSITO DE NEGÓCIO: compila o padrão da forma-ruim em PT — que pode aparecer em
     * qualquer caixa (ex.: "a legião" minúsculo depois de artigo).
     * <p>INVARIANTES DO DOMÍNIO: fronteira de palavra; {@code CASE_INSENSITIVE + UNICODE_CASE}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: termo é escapado; não injeta regex.
     */
    private Pattern padraoFormaRuim(String termo) {
        return Pattern.compile(
            INICIO_DE_TERMO + FronteiraTermoAss.corpo(termo) + FIM_DE_TERMO,
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

}
