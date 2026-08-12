package org.traducao.projeto.raspagemRevisao.application.concordancia;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: guarda a POLÍTICA EDITORIAL de palavrão — a tradução não inventa
 * insulto que o original não tem, e não suaviza o que ele tem. É a única regra do antigo
 * {@code DetectorConcordanciaService} que <b>não</b> fala de gênero nem de concordância, e é
 * por isso que ela sai primeiro: estava na classe errada.
 *
 * <h2>As duas direções, e por que ambas importam</h2>
 * <ul>
 *   <li><b>Introduzir</b> — o modelo escreve "filho da puta" onde o original não xinga. O
 *       espectador vê agressividade que a cena não tem.</li>
 *   <li><b>Suavizar</b> — o original xinga forte e a tradução devolve "filho da mãe". A
 *       preferência registrada é preservar o insulto compatível com o original; suavizar é
 *       decisão editorial que o pipeline não tem autoridade para tomar sozinho.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só acusa com evidência nos DOIS lados: palavrão no PT ausente no EN, ou palavrão no EN
 *       virando eufemismo no PT. Palavrão presente nos dois é tradução fiel e não é acusado.</li>
 *   <li>Sem estado. A instância é criada uma vez e reusada; não guarda nada entre chamadas.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo não é esperado — o chamador já normalizou. Nunca lança; ausência de evidência
 * simplesmente não acrescenta motivo.
 */
public final class DetectorAgressividadeIntroduzida implements RegraDeRevisao {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    private static final Pattern PROFANIDADE_FORTE_PT =
        Pattern.compile("\\bfilh[oa] da puta\\b", FLAGS);

    /**
     * O {@code son of a\s*\.\.\.} no fim cobre a fala CORTADA — "You son of a..." é insulto
     * interrompido, e sem essa alternativa o original não contaria como xingamento, fazendo a
     * tradução completa parecer invenção.
     */
    private static final Pattern PROFANIDADE_FORTE_EN = Pattern.compile(
        "\\b(son of a (?:bitch|hitch)|motherfucker|fuck(?:er|ing)?|bitch|whore|bastard)\\b"
            + "|\\bson of a\\s*\\.\\.\\.", FLAGS);

    private static final Pattern EUFEMISMO_FILHO_DA_MAE =
        Pattern.compile("\\bfilho da mãe\\b", FLAGS);

    /**
     * PROPÓSITO DE NEGÓCIO: acrescenta o motivo quando a agressividade do texto traduzido não
     * corresponde à do original, nas duas direções.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige evidência nos dois lados; nunca decide por um só.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem evidência, o conjunto de motivos fica intacto.
     *
     * @param original fala em inglês, já sem tags
     * @param texto tradução PT-BR, já sem tags
     * @param motivos conjunto acumulador do detector chamador
     */
    @Override
    public void detectar(String original, String texto, Set<String> motivos) {
        if (PROFANIDADE_FORTE_PT.matcher(texto).find()
            && !PROFANIDADE_FORTE_EN.matcher(original).find()) {
            motivos.add("Tradução introduziu palavrão forte ausente no original");
        }
        if (PROFANIDADE_FORTE_EN.matcher(original).find()
            && EUFEMISMO_FILHO_DA_MAE.matcher(texto).find()) {
            motivos.add("Insulto forte do original foi suavizado contra a preferência de tradução");
        }
    }
}
