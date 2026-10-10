package org.traducao.projeto.traducao.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: trata a fala de DOIS OU MAIS LOCUTORES na mesma legenda — o diálogo com
 * travessão, {@code "- This way.\N- Right."} —, que o pipeline mandava ao modelo como uma frase só.
 * O {@code IsoladorQuebraDialogo} troca o {@code \N} por espaço, o modelo vê dois travessões, devolve
 * uma linha por locutor, e a checagem de contagem reprova. No teste ponta a ponta de 09/10/2026 isso
 * foi a maior causa de fala que ficou em inglês: 18 das 19 pendências do Reconguista I (SRT, em que
 * toda legenda de duas linhas vira {@code \N}) e as de travessão duplo do Sidonia e do Patlabor. E
 * quando passava, saía torta: {@code "- Raraiya!\N- You okay?"} virou {@code "Raraiya! -\NVocê está
 * bem?"}, sem o travessão do primeiro locutor e com a quebra no meio da fala do segundo.
 *
 * <p>Aqui a fala é separada nos textos de cada locutor, cada um vai ao modelo como fala comum, e a
 * legenda é remontada com os travessões ORIGINAIS e a quebra exatamente entre os locutores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só reconhece a forma inequívoca: segmentos separados por {@code \N}, TODOS começando por
 *       travessão ({@code -}, {@code --}, {@code –} ou {@code —}) e com letra ou dígito depois dele.
 *       Um segmento sem travessão, ou qualquer tag {@code {...}} na fala, e ela segue o caminho de
 *       sempre — a moldura de estilo tem dono próprio e não é adivinhada aqui.</li>
 *   <li>A remontagem preserva byte a byte o prefixo de cada segmento ({@code "- "}, {@code "--"},
 *       {@code "-"}) e a quantidade de quebras; o travessão que o modelo repetir no começo da
 *       tradução é tirado, para não sair dobrado.</li>
 *   <li>Classe pura: sem estado, sem I/O, sem conhecer cache nem LLM.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link #decompor} devolve {@link Optional#empty()} para tudo que não seja a forma inequívoca
 * (inclusive {@code null}). {@link #recompor} devolve {@link Optional#empty()} quando a lista de
 * traduções não tem um texto não vazio por locutor — a fala inteira fica pendente, nunca meia
 * traduzida.
 */
final class FalaDeLocutores {

    private static final Pattern SEGMENTO = Pattern.compile("^(\\s*(?:--|-|–|—)\\s*)(\\S.*)$");
    private static final Pattern TRAVESSAO_INICIAL = Pattern.compile("^\\s*(?:--|-|–|—)\\s*");

    private final List<String> prefixos;
    private final List<String> textos;

    private FalaDeLocutores(List<String> prefixos, List<String> textos) {
        this.prefixos = List.copyOf(prefixos);
        this.textos = List.copyOf(textos);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconhece a fala de vários locutores e separa o texto de cada um.
     *
     * <p>INVARIANTES DO DOMÍNIO: ao menos dois segmentos; todos com travessão e com texto; nenhuma
     * tag {@code {...}}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer outra forma, ou {@code null}, devolve vazio.
     *
     * @param original a fala como está na legenda (com {@code \N})
     * @return a fala decomposta, ou vazio quando não é diálogo de locutores
     */
    static Optional<FalaDeLocutores> decompor(String original) {
        if (original == null || !original.contains("\\N") || original.indexOf('{') >= 0) {
            return Optional.empty();
        }
        String[] partes = original.split("\\\\N", -1);
        if (partes.length < 2) {
            return Optional.empty();
        }
        List<String> prefixos = new ArrayList<>(partes.length);
        List<String> textos = new ArrayList<>(partes.length);
        for (String parte : partes) {
            Matcher m = SEGMENTO.matcher(parte);
            if (!m.matches()) {
                return Optional.empty();
            }
            String texto = m.group(2).strip();
            if (texto.codePoints().noneMatch(Character::isLetterOrDigit)) {
                return Optional.empty();
            }
            prefixos.add(m.group(1));
            textos.add(texto);
        }
        return Optional.of(new FalaDeLocutores(prefixos, textos));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta a legenda traduzida com o travessão de cada locutor e a quebra
     * entre eles, a partir das traduções dos textos separados.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma tradução por locutor, na mesma ordem de {@link #textos()}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista de tamanho diferente, ou tradução nula/vazia de algum
     * locutor, devolve vazio.
     *
     * @param traducoes a tradução do texto de cada locutor
     * @return a fala remontada, ou vazio quando falta tradução de algum locutor
     */
    Optional<String> recompor(List<String> traducoes) {
        if (traducoes == null || traducoes.size() != textos.size()) {
            return Optional.empty();
        }
        StringBuilder montada = new StringBuilder();
        for (int i = 0; i < textos.size(); i++) {
            String traducao = traducoes.get(i);
            if (traducao == null || traducao.isBlank()) {
                return Optional.empty();
            }
            String limpa = TRAVESSAO_INICIAL.matcher(traducao.strip()).replaceFirst("");
            if (limpa.isBlank()) {
                return Optional.empty();
            }
            if (i > 0) {
                montada.append("\\N");
            }
            montada.append(prefixos.get(i)).append(limpa);
        }
        return Optional.of(montada.toString());
    }

    /** O texto de cada locutor, sem o travessão, na ordem da legenda. */
    List<String> textos() {
        return textos;
    }
}
