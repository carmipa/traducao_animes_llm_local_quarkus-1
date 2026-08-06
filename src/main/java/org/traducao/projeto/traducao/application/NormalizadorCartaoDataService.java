package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a forma do CARTÃO DE DATA — a legenda em tela cheia que anuncia
 * "30 de julho do Ano Estelar 2149" — decidindo a partir do ORIGINAL em inglês em vez de
 * confiar no que o modelo devolveu.
 *
 * <h2>O prejuízo que o originou</h2>
 * Medido no 86 em 06/08/2026: dos <b>136 cartões de data do acervo, 17 saíram fora do padrão
 * (12,5%)</b>, e o defeito era visível na tela — Paulo o notou assistindo. As formas erradas:
 * <pre>
 *   "Julho de 30, Ano Estelar 2149"    ordem invertida
 *   "Março 12º, Ano Estelar 2150"      ordem invertida
 *   "Junho 15, Ano Estelar 2148"       ordem invertida
 *   "June 16th, Ano Estelar 2148"      METADE EM INGLÊS
 *   "2 de setembro do ano estelar"     caixa perdida
 * </pre>
 * A meia-inglesa é a mais grave porque nenhuma régua a pegava: {@code June} não está na lista
 * de resíduo em inglês, e a fala não é idêntica ao original, então passa por todas as guardas.
 *
 * <h2>Por que determinístico, e não prompt</h2>
 * O cartão é o padrão mais regular que existe numa legenda: mês, dia, era, ano. Pedir ao modelo
 * que acerte 100% de um formato mecânico é gastar tentativa com o que uma regra resolve — e o
 * modelo erra 12,5% das vezes justamente por ser texto curto sem contexto de frase.
 *
 * <p>Um efeito colateral valioso: o cartão costuma vir em <b>camadas empilhadas</b> (sombra,
 * contorno, preenchimento), e cada camada ia ao LLM por conta própria e voltava diferente —
 * "7 de janeiro, Ano Estelar" numa e "7 de janeiro do Ano Estelar" na outra. Na tela isso
 * aparece como texto borrado ou fantasma. Decidir pelo original faz as três convergirem por
 * construção.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só age quando o ORIGINAL casa o padrão completo mês + dia + era + ano de 4 dígitos.
 *       Qualquer outra coisa atravessa intacta — inclusive datas do mundo real sem era.</li>
 *   <li>A ERA é copiada do original e traduzida por mapa fechado ({@code Stellar Year} →
 *       {@code Ano Estelar}). Era desconhecida faz a fala passar intacta, nunca inventa.</li>
 *   <li>Preserva o bloco de tags do INÍCIO: o cartão carrega posicionamento e fonte próprios,
 *       e reescrever a fala inteira apagaria o desenho.</li>
 *   <li>O ano é copiado dígito a dígito do original. É a blindagem contra o erro que a guarda
 *       de identificador numérico pegou no mesmo arquivo: {@code 2149} virando {@code 2049}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Original ou tradução nulos/vazios devolvem a tradução como veio. Nunca lança, nunca devolve
 * vazio — viés de preservação, igual aos outros normalizadores da cadeia.
 */
@Component
public class NormalizadorCartaoDataService {

    /** {@code July 30th, Stellar Year 2149} — mês, dia, sufixo opcional, era, ano. */
    private static final Pattern CARTAO_DATA = Pattern.compile(
        "^([A-Z][a-z]+)[ ]+(\\d{1,2})(?:st|nd|rd|th)?,[ ]+([A-Z][A-Za-z ]*?)[ ]+(\\d{4})[.]?$");

    private static final Pattern TAGS_INICIAIS = Pattern.compile("^(?:\\{[^}]*})+");

    private static final Map<String, String> MESES = Map.ofEntries(
        Map.entry("january", "janeiro"), Map.entry("february", "fevereiro"),
        Map.entry("march", "março"), Map.entry("april", "abril"),
        Map.entry("may", "maio"), Map.entry("june", "junho"),
        Map.entry("july", "julho"), Map.entry("august", "agosto"),
        Map.entry("september", "setembro"), Map.entry("october", "outubro"),
        Map.entry("november", "novembro"), Map.entry("december", "dezembro"));

    /**
     * Eras conhecidas. Mapa FECHADO de propósito: era que não está aqui faz a fala passar
     * intacta. Traduzir era por conta própria é como se inventa "d.C." onde o original dizia
     * "Stellar Year" — aconteceu, e está registrado no cache do 86 episódio 2.
     */
    private static final Map<String, String> ERAS = Map.of(
        "stellar year", "Ano Estelar",
        "imperial year", "Ano Imperial",
        "universal century", "Século Universal");

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o cartão de data na forma canônica em português, decidido
     * a partir do inglês.
     *
     * <p>INVARIANTES DO DOMÍNIO: preserva as tags iniciais da tradução recebida — são o
     * posicionamento e a fonte do cartão. O dia sai sem ordinal ("1", não "1º") porque é a
     * forma corrente em data por extenso no português brasileiro.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: original que não seja cartão de data, mês ou era
     * desconhecidos, entrada nula ou vazia ⇒ devolve {@code traduzido} sem tocar.
     *
     * @param original fala-fonte em inglês, com ou sem tags
     * @param traduzido tradução produzida pelo LLM, com ou sem tags
     */
    public String normalizar(String original, String traduzido) {
        if (original == null || traduzido == null || traduzido.isBlank()) {
            return traduzido;
        }

        Matcher cartao = CARTAO_DATA.matcher(semTags(original).strip());
        if (!cartao.matches()) {
            return traduzido;
        }

        String mes = MESES.get(cartao.group(1).toLowerCase(Locale.ROOT));
        String era = ERAS.get(cartao.group(3).toLowerCase(Locale.ROOT).strip());
        if (mes == null || era == null) {
            return traduzido;
        }

        int dia = Integer.parseInt(cartao.group(2));
        String ano = cartao.group(4);

        Matcher tags = TAGS_INICIAIS.matcher(traduzido);
        String prefixo = tags.lookingAt() ? tags.group() : "";

        return prefixo + dia + " de " + mes + " do " + era + " " + ano;
    }

    private static String semTags(String texto) {
        return texto.replaceAll("\\{[^}]*}", "");
    }
}
