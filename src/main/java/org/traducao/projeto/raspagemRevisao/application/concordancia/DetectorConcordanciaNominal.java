package org.traducao.projeto.raspagemRevisao.application.concordancia;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: concordância DENTRO do sintagma nominal — artigo com substantivo,
 * adjetivo com substantivo, pronome oblíquo com pronome. É a família de regras que não olha o
 * original em inglês: "a menino" está errado em português independentemente do que a fala
 * dizia lá.
 *
 * <h2>Por que é a única família que dispensa o original</h2>
 * As outras regras da revisão comparam PT com EN — precisam do {@code she}/{@code he} para
 * saber o gênero de quem se fala. Esta não: a discordância é interna ao português e visível
 * sozinha. Por isso ela roda sempre, mesmo quando o original não tem marca de gênero alguma.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O vocabulário vem de {@link LexicoGenero} — nenhuma lista de palavras mora aqui. Uma
 *       segunda cópia divergiria da primeira e a regra passaria a errar em silêncio.</li>
 *   <li>Adjetivo ANTEPOSTO aceita possessivo ({@code meu menina}); POSPOSTO não, porque "a
 *       garota minha" é inversão legítima e acusá-la seria alarme falso.</li>
 *   <li>Só acrescenta motivo; nunca reescreve o texto. Quem corrige é a revisão, e a proposta
 *       ainda passa por um portão.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto sem nenhuma das construções não acrescenta motivo. Nunca lança.
 */
public final class DetectorConcordanciaNominal implements RegraDeRevisao {

    private static final Pattern ART_MASC_COM_SUBST_FEM = Pattern.compile(
        "\\b(o|um|este|esse|aquele|do|no|ao|pelo|num)\\s+(" + LexicoGenero.SUBST_FEM + ")\\b",
        LexicoGenero.FLAGS);

    private static final Pattern ART_FEM_COM_SUBST_MASC = Pattern.compile(
        "\\b(uma|esta|essa|aquela|da|na|à|pela|numa)\\s+(" + LexicoGenero.SUBST_MASC + ")\\b",
        LexicoGenero.FLAGS);

    private static final Pattern ADJ_MASC_COM_SUBST_FEM = Pattern.compile(
        "\\b(" + LexicoGenero.ADJ_MASC + ")\\s+(" + LexicoGenero.SUBST_FEM + ")\\b",
        LexicoGenero.FLAGS);

    private static final Pattern ADJ_FEM_COM_SUBST_MASC = Pattern.compile(
        "\\b(" + LexicoGenero.ADJ_FEM + ")\\s+(" + LexicoGenero.SUBST_MASC + ")\\b",
        LexicoGenero.FLAGS);

    private static final Pattern SUBST_FEM_COM_ADJ_MASC = Pattern.compile(
        "\\b(" + LexicoGenero.SUBST_FEM + ")\\s+(" + LexicoGenero.ADJ_MASC_POSPOSTO + ")\\b",
        LexicoGenero.FLAGS);

    private static final Pattern SUBST_MASC_COM_ADJ_FEM = Pattern.compile(
        "\\b(" + LexicoGenero.SUBST_MASC + ")\\s+(" + LexicoGenero.ADJ_FEM_POSPOSTO + ")\\b",
        LexicoGenero.FLAGS);

    /**
     * O {@code a} sozinho fica FORA do segundo grupo: é a preposição invariante em gênero
     * ("disse a ele" / "disse a ela" são ambos corretos), não o artigo feminino — incluí-lo
     * fazia "a ele", construção comum e correta, ser sinalizada sempre.
     */
    private static final Pattern PRONOME_ARTIGO_ERRADO = Pattern.compile(
        "\\b(o|um|do|no|ao|pelo|lo|no)\\s+ela\\b|\\b(uma|da|na|à|pela|la)\\s+ele\\b",
        LexicoGenero.FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: percorre as sete construções e acrescenta um motivo por
     * discordância encontrada.
     *
     * <p>INVARIANTES DO DOMÍNIO: não consulta o original — a evidência é interna ao português.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem construção discordante, o conjunto fica intacto.
     *
     * @param original ignorado — esta família é interna ao português e não compara com o inglês
     * @param texto tradução PT-BR, já sem tags ASS
     * @param motivos conjunto acumulador do detector chamador
     */
    @Override
    public void detectar(String original, String texto, Set<String> motivos) {
        acusar(motivos, ART_MASC_COM_SUBST_FEM, texto,
            "Artigo/pronome demonstrativo masculino antes de substantivo feminino");
        acusar(motivos, ART_FEM_COM_SUBST_MASC, texto,
            "Artigo/pronome demonstrativo feminino antes de substantivo masculino");
        acusar(motivos, ADJ_MASC_COM_SUBST_FEM, texto,
            "Adjetivo masculino antes de substantivo feminino");
        acusar(motivos, ADJ_FEM_COM_SUBST_MASC, texto,
            "Adjetivo feminino antes de substantivo masculino");
        acusar(motivos, SUBST_FEM_COM_ADJ_MASC, texto,
            "Substantivo feminino com adjetivo/particípio masculino");
        acusar(motivos, SUBST_MASC_COM_ADJ_FEM, texto,
            "Substantivo masculino com adjetivo/particípio feminino");
        acusar(motivos, PRONOME_ARTIGO_ERRADO, texto,
            "Artigo/pronome oblíquo incompatível (o ela / a ele / lo ela)");
    }

    /**
     * Três linhas deliberadamente duplicadas do detector chamador, em vez de um utilitário
     * compartilhado: um helper comum obrigaria as duas classes a se conhecerem por causa de um
     * {@code if}, e é exatamente o tipo de acoplamento que a arquitetura deste projeto evita.
     */
    private void acusar(Set<String> motivos, Pattern padrao, String texto, String motivo) {
        if (padrao.matcher(texto).find()) {
            motivos.add(motivo);
        }
    }
}
