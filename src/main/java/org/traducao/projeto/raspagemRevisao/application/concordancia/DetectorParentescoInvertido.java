package org.traducao.projeto.raspagemRevisao.application.concordancia;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: pega a troca de PARENTESCO entre o original e a tradução — "father"
 * virando "mãe", "sister" virando "irmão". É erro de outra natureza que a concordância: a
 * frase fica gramaticalmente perfeita e a família da cena muda.
 *
 * <h2>Por que a evidência tem de ser inequívoca</h2>
 * A regra só age quando o inglês menciona UMA das duas relações. Numa fala que cita pai E mãe
 * — "my father told my mother" — não há como saber a qual delas o termo português se refere, e
 * acusar ali seria adivinhar. O bloqueio pela presença simultânea é o que separa detectar de
 * chutar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Três condições, todas obrigatórias: a relação esperada está no original, a oposta NÃO
 *       está, e a oposta aparece na tradução.</li>
 *   <li>Os seis pares são simétricos — pai/mãe, filho/filha, irmão/irmã, nos dois sentidos.
 *       Acrescentar um lado sem o outro faz a regra acusar numa direção e calar na inversa.</li>
 *   <li>Só acrescenta motivo; nunca reescreve.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem evidência inequívoca, não acrescenta nada — e isso não significa "o parentesco está
 * certo", significa "esta regra não pôde julgar". Nunca lança.
 */
public final class DetectorParentescoInvertido implements RegraDeRevisao {

    private static final Pattern PAI_EN = Pattern.compile("\\b(father|dad|daddy)\\b", LexicoGenero.FLAGS);
    private static final Pattern MAE_EN =
        Pattern.compile("\\b(mother|mom|mommy|mum|mummy)\\b", LexicoGenero.FLAGS);
    private static final Pattern FILHO_EN = Pattern.compile("\\bson\\b", LexicoGenero.FLAGS);
    private static final Pattern FILHA_EN = Pattern.compile("\\bdaughter\\b", LexicoGenero.FLAGS);
    private static final Pattern IRMAO_EN = Pattern.compile("\\bbrother\\b", LexicoGenero.FLAGS);
    private static final Pattern IRMA_EN = Pattern.compile("\\bsister\\b", LexicoGenero.FLAGS);

    private static final Pattern PAI_PT = Pattern.compile("\\b(pai|papai)\\b", LexicoGenero.FLAGS);
    private static final Pattern MAE_PT =
        Pattern.compile("\\b(mãe|mae|mamãe|mamae)\\b", LexicoGenero.FLAGS);
    private static final Pattern FILHO_PT = Pattern.compile("\\bfilho\\b", LexicoGenero.FLAGS);
    private static final Pattern FILHA_PT = Pattern.compile("\\bfilha\\b", LexicoGenero.FLAGS);
    private static final Pattern IRMAO_PT = Pattern.compile("\\b(irmão|irmao)\\b", LexicoGenero.FLAGS);
    private static final Pattern IRMA_PT = Pattern.compile("\\b(irmã|irma)\\b", LexicoGenero.FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: percorre os seis pares de parentesco nos dois sentidos.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige evidência inequívoca no original.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem evidência, o conjunto fica intacto.
     */
    @Override
    public void detectar(String original, String texto, Set<String> motivos) {
        acusarSeInvertido(original, texto, motivos, PAI_EN, MAE_EN, MAE_PT,
            "Original menciona pai, mas a tradução usa mãe");
        acusarSeInvertido(original, texto, motivos, MAE_EN, PAI_EN, PAI_PT,
            "Original menciona mãe, mas a tradução usa pai");
        acusarSeInvertido(original, texto, motivos, FILHO_EN, FILHA_EN, FILHA_PT,
            "Original menciona filho, mas a tradução usa filha");
        acusarSeInvertido(original, texto, motivos, FILHA_EN, FILHO_EN, FILHO_PT,
            "Original menciona filha, mas a tradução usa filho");
        acusarSeInvertido(original, texto, motivos, IRMAO_EN, IRMA_EN, IRMA_PT,
            "Original menciona irmão, mas a tradução usa irmã");
        acusarSeInvertido(original, texto, motivos, IRMA_EN, IRMAO_EN, IRMAO_PT,
            "Original menciona irmã, mas a tradução usa irmão");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica a comparação de UM par somente quando o inglês fornece
     * evidência inequívoca da relação.
     *
     * <p>INVARIANTES DO DOMÍNIO: a presença SIMULTÂNEA das duas relações no original bloqueia
     * a heurística — numa fala que cita pai e mãe, o termo português pode se referir a
     * qualquer um dos dois, e associar seria chute.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não registra diagnóstico especulativo.
     */
    private void acusarSeInvertido(String original, String texto, Set<String> motivos,
            Pattern esperadaEn, Pattern opostaEn, Pattern opostaPt, String descricao) {
        if (esperadaEn.matcher(original).find()
            && !opostaEn.matcher(original).find()
            && opostaPt.matcher(texto).find()) {
            motivos.add(descricao);
        }
    }
}
