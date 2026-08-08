package org.traducao.projeto.traducaoKaraoke.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: permitir que uma linha de karaokê pintada <b>letra a letra</b> — o
 * gradiente de cor que os fansubs aplicam no OP/ED — seja traduzida sem perder o efeito visual.
 * Separa o texto que o espectador lê das cores que o colorem, para que só o texto vá ao LLM e as
 * MESMAS cores voltem distribuídas sobre a tradução.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido no Guilty Crown em 07/08/2026: das 31 linhas recusadas numa execução do corretor,
 * <b>28 caíram por "Marcadores de formatação ([[TAGn]])"</b>. A causa é aritmética — uma linha
 * como {@code {\blur4.5\3c&H6500B1&}S{\3c&H7114AA&}o{\3c&H7C26A4&},…} tem uma tag por LETRA,
 * então o mascarador produz 10 a 30 marcadores intercalados e nenhum LLM os devolve na ordem. A
 * única que passou pelo validador saiu assim:
 * <pre>
 *   So, everything that makes me whole
 *     -> So, eu e evidentementereithyingthathingthatmakes mea whole wholed
 * </pre>
 * São <b>445 linhas</b> nessa forma só nessa obra — 38% do OP/ED. Sem esta classe, ou se perde a
 * tradução delas, ou se perde o gradiente.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A PALETA é preservada: {@link #recompor} nunca inventa cor nem altera as existentes —
 *       apenas as reposiciona sobre o texto novo, na mesma ordem.</li>
 *   <li>Timing de sílaba ({@code \k}, {@code \kf}, {@code \ko}) e animação ({@code \t(}) VETAM a
 *       decomposição: ali a tag está amarrada ao tempo do áudio, e redistribuir a quebraria.</li>
 *   <li>Quebra de linha ({@code \N}) veta: o texto traduzido não tem como saber onde o verso
 *       partia, e adivinhar produziria a quebra no lugar errado. Medido no Guilty Crown: 0 das
 *       445 linhas têm {@code \N}, então o veto não custa nada lá — é blindagem para o resto do
 *       acervo, não teoria.</li>
 *   <li>Só há gradiente com pelo menos {@value #MINIMO_BLOCOS} blocos: abaixo disso o mascarador
 *       comum dá conta, e mexer seria risco sem ganho.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link #decompor} devolve {@link Optional#empty()} para tudo que não for gradiente elegível —
 * nulo, vazio, poucos blocos, presença de {@code \k}/{@code \t(}/{@code \N} ou texto visível em
 * branco. {@link #recompor} devolve o texto ORIGINAL quando a tradução vem nula ou em branco.
 * Falha fechada em toda a classe: na dúvida, o pipeline segue pelo caminho antigo e a linha fica
 * como está, nunca corrompida.
 */
public record GradienteKaraoke(List<String> tags, String textoVisivel, String original) {

    /** Abaixo disto não é gradiente por letra, e o mascarador comum já resolve. */
    public static final int MINIMO_BLOCOS = 10;

    private static final Pattern BLOCO_TAGS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern TAG_TIMING_KARAOKE = Pattern.compile("\\\\[kK][fo]?\\d");
    private static final Pattern TAG_TRANSFORMACAO = Pattern.compile("\\\\t\\(");

    public GradienteKaraoke {
        tags = List.copyOf(tags);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: separa a linha em (cores na ordem) + (texto que o espectador lê), para
     * que o LLM receba uma frase limpa em vez de um mosaico de marcadores.
     *
     * <p>INVARIANTES DO DOMÍNIO: a ordem dos blocos é a ordem de leitura; texto solto antes do
     * primeiro bloco é preservado no início do texto visível. Não altera a entrada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@link Optional#empty()} — nunca lança — para
     * entrada nula/vazia, menos de {@value #MINIMO_BLOCOS} blocos, presença de {@code \k},
     * {@code \t(} ou {@code \N}, ou texto visível em branco.
     */
    public static Optional<GradienteKaraoke> decompor(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        // Vetos: aqui a tag NAO e decoracao, e mexer nela quebraria o que ela sustenta.
        if (TAG_TIMING_KARAOKE.matcher(texto).find()
            || TAG_TRANSFORMACAO.matcher(texto).find()
            || texto.contains("\\N")) {
            return Optional.empty();
        }

        List<String> tags = new ArrayList<>();
        StringBuilder visivel = new StringBuilder();
        Matcher m = BLOCO_TAGS.matcher(texto);
        int cursor = 0;
        while (m.find()) {
            visivel.append(texto, cursor, m.start());
            tags.add(m.group());
            cursor = m.end();
        }
        visivel.append(texto.substring(cursor));

        if (tags.size() < MINIMO_BLOCOS || visivel.toString().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new GradienteKaraoke(tags, visivel.toString(), texto));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a linha pronta para o arquivo, com a tradução vestindo as
     * cores originais — o espectador lê em português e vê o mesmo gradiente do fansub.
     *
     * <p>INVARIANTES DO DOMÍNIO: as tags saem na ordem original e nenhuma é alterada. A
     * distribuição é PROPORCIONAL: com a tradução mais longa que o original, cores vizinhas cobrem
     * mais de uma letra; mais curta, o gradiente é amostrado e alguma cor não aparece. Isso é
     * deliberado — preservar a paleta inteira à custa da posição produziria tags coladas sem
     * texto entre elas, onde só a última pintaria de fato.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tradução nula ou em branco devolve o {@link #original}
     * intacto — uma linha sem tradução vale mais que uma linha destruída.
     */
    public String recompor(String traducao) {
        if (traducao == null || traducao.isBlank()) {
            return original;
        }
        String limpo = BLOCO_TAGS.matcher(traducao).replaceAll("").strip();
        if (limpo.isEmpty()) {
            return original;
        }

        int totalTags = tags.size();
        int totalChars = limpo.length();
        StringBuilder saida = new StringBuilder(original.length());
        int tagAnterior = -1;
        for (int i = 0; i < totalChars; i++) {
            int alvo = (int) ((long) i * totalTags / totalChars);
            if (alvo > tagAnterior) {
                // Emite TODAS as tags puladas: com traducao mais curta que o original, saltar
                // direto perderia atributos que so aparecem em blocos do meio (blur/fad de
                // reentrada), e nao apenas cor.
                for (int t = tagAnterior + 1; t <= alvo; t++) {
                    saida.append(tags.get(t));
                }
                tagAnterior = alvo;
            }
            saida.append(limpo.charAt(i));
        }
        // Sobra quando a divisao inteira nao alcanca a ultima tag: anexa no fim para que
        // nenhuma cor da paleta seja silenciosamente descartada.
        for (int t = tagAnterior + 1; t < totalTags; t++) {
            saida.append(tags.get(t));
        }
        return saida.toString();
    }
}
