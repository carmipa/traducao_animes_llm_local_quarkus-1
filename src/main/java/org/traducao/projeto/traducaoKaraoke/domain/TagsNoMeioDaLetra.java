package org.traducao.projeto.traducaoKaraoke.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: deixar traduzir a linha de letra cuja tag fica no MEIO da frase — a cor
 * que muda numa palavra, o gradiente curto demais para o {@link GradienteKaraoke} — sem pedir ao
 * modelo que devolva marcador nenhum. O texto viaja limpo; as tags voltam recolocadas sobre a
 * tradução, cada uma na posição que ocupava no original.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido na 4.1 aplicada ao acervo em 09/10/2026: das 71 recusas em 12 obras, <b>70 eram do
 * Guilty Crown e todas desta forma</b>, três frases do OP_S2 repetidas em 10 episódios:
 * <pre>
 *   {\fad(0,0)\blur4.5\3c&amp;H4331EA&amp;}that your eyes were given to you to {\c&amp;HEAEEEB&amp;}acknowledge others,
 *     -&gt; "que os seus olhos foram dados a você para reconhecer os outros," DESCARTADA (marcador perdido)
 *   {\blur4.5\3c&amp;HF6D6B3&amp;}N{\3c&amp;HDCDDD2&amp;}o{…}t{…}i{…}c{…}e        (6 blocos, abaixo do mínimo do gradiente)
 *     -&gt; o modelo leu "[[TAG0]]N[[TAG1]]o…" e pediu contexto em 3 linhas
 *   {\blur4.5\fad(100,350)\3c&amp;HF6D6B3&amp;}r{…}e{…}l{…}y                  (4 blocos)
 *     -&gt; "Relatório recebido. Início da transmissão." (marcador perdido)
 * </pre>
 * A primeira tinha tradução CERTA jogada fora; as outras duas eram lixo produzido pelo próprio
 * mascaramento, que entrega ao modelo letras soltas entre marcadores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Nenhuma tag é inventada, alterada ou descartada: {@link #recompor} emite todos os blocos
 *       do original, na mesma ordem.</li>
 *   <li>A posição é PROPORCIONAL à do original, e a natureza dela é preservada: tag que abria
 *       palavra volta abrindo palavra, tag que antecedia espaço volta antes de espaço, tag no
 *       meio da palavra (gradiente) fica onde a proporção mandar. Sem isso a cor trocaria no
 *       meio de "pa|ra", que é a forma visível do defeito.</li>
 *   <li>Vetos — a tag aqui NÃO é decoração: timing de sílaba ({@code \k}, mesma regra do
 *       {@link GradienteKaraoke}), quebra de verso {@code \N} (a tradução não diz onde o verso
 *       partia) e modo desenho {@code \p1}+ (o "texto" é vetor, nunca idioma).</li>
 *   <li>É aproximação declarada: a palavra que recebe a cor é a que ocupa a mesma região da
 *       frase, não necessariamente a equivalente semântica. Por isso este caminho é só o
 *       SEGUNDO tiro, depois do mascarador — onde o mascarador acerta, o alinhamento feito pelo
 *       próprio modelo continua valendo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link #decompor} devolve {@link Optional#empty()} — nunca lança — para entrada nula ou em
 * branco, sem tag nenhuma, com veto ou com texto visível em branco. {@link #recompor} devolve o
 * {@link #original} intacto quando a tradução vem nula, em branco ou só com tags. Falha fechada:
 * na dúvida, a linha fica como está, nunca meio montada.
 *
 * @param textoVisivel o texto que o espectador lê, sem tag nenhuma — é o que vai ao modelo
 * @param blocos       as tags na ordem do original, cada uma com a posição no texto visível
 * @param original     a linha intacta, devolvida quando não há o que recompor
 */
public record TagsNoMeioDaLetra(String textoVisivel, List<Bloco> blocos, String original) {

    /**
     * Uma tag e a posição, no texto visível, onde ela começava a valer.
     *
     * @param tag     o bloco {@code {...}} literal
     * @param posicao quantos caracteres visíveis vinham antes dele
     */
    public record Bloco(String tag, int posicao) {
    }

    private static final Pattern BLOCO_TAGS = Pattern.compile("\\{[^}]*\\}");
    /** {@code \p0} desliga o desenho; só {@code \p1} em diante transforma o texto em vetor. */
    private static final Pattern MODO_DESENHO = Pattern.compile("\\\\p[1-9]");

    /** Onde a tag estava em relação às palavras — é isso que a recomposição preserva. */
    private enum Fronteira { INICIO_DE_PALAVRA, ANTES_DE_ESPACO, DENTRO_DA_PALAVRA }

    public TagsNoMeioDaLetra {
        blocos = List.copyOf(blocos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa a linha em (texto que o espectador lê) + (tags com a posição de
     * cada uma), para que o modelo receba a frase sem marcador nenhum para perder.
     *
     * <p>INVARIANTES DO DOMÍNIO: a ordem dos blocos é a ordem do original; o texto fora das tags
     * é preservado caractere a caractere; a entrada não é alterada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} — nunca lança — para entrada
     * nula ou em branco, nenhuma tag, {@code \k}, {@code \N}, {@code \p1}+ ou texto visível em
     * branco.
     */
    public static Optional<TagsNoMeioDaLetra> decompor(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        if (GradienteKaraoke.TAG_TIMING_KARAOKE.matcher(texto).find()
            || texto.contains("\\N")
            || MODO_DESENHO.matcher(texto).find()) {
            return Optional.empty();
        }
        List<Bloco> blocos = new ArrayList<>();
        StringBuilder visivel = new StringBuilder();
        Matcher m = BLOCO_TAGS.matcher(texto);
        int cursor = 0;
        while (m.find()) {
            visivel.append(texto, cursor, m.start());
            blocos.add(new Bloco(m.group(), visivel.length()));
            cursor = m.end();
        }
        visivel.append(texto.substring(cursor));
        if (blocos.isEmpty() || visivel.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new TagsNoMeioDaLetra(visivel.toString(), blocos, texto));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a linha pronta para o arquivo — a tradução com as tags do
     * original recolocadas, para que a cor da palavra e o gradiente continuem na tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: todos os blocos saem, na ordem original e sem alteração; a
     * posição de cada um é a proporcional, encaixada na fronteira do mesmo tipo que ocupava
     * (início de palavra, antes de espaço) quando a tradução tem uma; posições nunca regridem,
     * então dois blocos nunca trocam de ordem; tag que vinha DEPOIS do último caractere continua
     * no fim. Tag que o modelo tenha inventado na resposta é descartada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução nula, em branco ou sem texto depois de limpar as
     * tags devolve o {@link #original} intacto.
     */
    public String recompor(String traducao) {
        if (traducao == null || traducao.isBlank()) {
            return original;
        }
        String limpo = BLOCO_TAGS.matcher(traducao).replaceAll("").strip();
        if (limpo.isEmpty()) {
            return original;
        }
        int[] posicoes = new int[blocos.size()];
        int anterior = 0;
        for (int i = 0; i < blocos.size(); i++) {
            posicoes[i] = Math.max(anterior, posicaoNaTraducao(blocos.get(i).posicao(), limpo));
            anterior = posicoes[i];
        }
        StringBuilder saida = new StringBuilder(original.length() + limpo.length());
        int proximo = 0;
        for (int i = 0; i <= limpo.length(); i++) {
            while (proximo < blocos.size() && posicoes[proximo] == i) {
                saida.append(blocos.get(proximo).tag());
                proximo++;
            }
            if (i < limpo.length()) {
                saida.append(limpo.charAt(i));
            }
        }
        return saida.toString();
    }

    private int posicaoNaTraducao(int posicaoOriginal, String limpo) {
        int total = textoVisivel.length();
        int novo = limpo.length();
        if (posicaoOriginal <= 0) {
            return 0;
        }
        if (posicaoOriginal >= total) {
            return novo;
        }
        int alvo = (int) Math.round((double) posicaoOriginal * novo / total);
        alvo = Math.max(0, Math.min(novo, alvo));
        return switch (fronteira(textoVisivel, posicaoOriginal)) {
            case INICIO_DE_PALAVRA -> maisProximo(alvo, limpo, true);
            case ANTES_DE_ESPACO -> maisProximo(alvo, limpo, false);
            case DENTRO_DA_PALAVRA -> alvo;
        };
    }

    private static Fronteira fronteira(String texto, int posicao) {
        if (Character.isWhitespace(texto.charAt(posicao))) {
            return Fronteira.ANTES_DE_ESPACO;
        }
        return Character.isWhitespace(texto.charAt(posicao - 1))
            ? Fronteira.INICIO_DE_PALAVRA
            : Fronteira.DENTRO_DA_PALAVRA;
    }

    /**
     * A fronteira do mesmo tipo mais próxima do alvo, só no INTERIOR da tradução: a borda (0 ou o
     * fim) faria a cor engolir a frase inteira ou não pintar nada. Sem candidata, fica o alvo.
     */
    private static int maisProximo(int alvo, String limpo, boolean inicioDePalavra) {
        int melhor = -1;
        for (int q = 1; q < limpo.length(); q++) {
            boolean candidata = inicioDePalavra
                ? Character.isWhitespace(limpo.charAt(q - 1)) && !Character.isWhitespace(limpo.charAt(q))
                : Character.isWhitespace(limpo.charAt(q));
            if (candidata && (melhor < 0 || Math.abs(q - alvo) < Math.abs(melhor - alvo))) {
                melhor = q;
            }
        }
        return melhor < 0 ? alvo : melhor;
    }
}
