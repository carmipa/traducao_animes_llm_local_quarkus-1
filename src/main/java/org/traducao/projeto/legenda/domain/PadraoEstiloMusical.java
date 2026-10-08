package org.traducao.projeto.legenda.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: fonte ÚNICA da pergunta "o NOME deste estilo declara música?". Antes
 * desta classe a mesma pergunta tinha duas respostas no código, e elas discordavam em
 * <b>1.385 linhas do acervo</b>.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido em 07/08/2026 por {@code MedicaoDivergenciaPadraoMusicalIT}, sobre 1.719.242 falas:
 *
 * <table>
 *   <tr><th>estilo</th><th>linhas</th><th>PoliticaEstiloMusical</th><th>DetectorEfeitoKaraoke</th></tr>
 *   <tr><td>OP2, ED2, OP_S2, ED_S2, ED_S2_roma, OP_S2_roma</td><td>1.263</td><td>não</td><td>musical</td></tr>
 *   <tr><td>ED_English, ED2-English</td><td>52</td><td>não</td><td>musical</td></tr>
 *   <tr><td>Gundam 0083 ED3 Lyrics (+A)</td><td>15</td><td>não</td><td>musical</td></tr>
 *   <tr><td>Other songs, insertita</td><td>55</td><td>musical</td><td>não</td></tr>
 * </table>
 *
 * A {@code CatracaRegraDuplicadaEntreFatiasTest} já DECLARAVA essa duplicação desde 2026-08-03
 * — "o padrão largo já alcançava OP_S2, mas eEstiloDeMusica usava o de {@code \b}" —, mas
 * contar a cópia não faz as duas convergirem.
 *
 * <h2>Por que as duas discordavam</h2>
 * <ul>
 *   <li>A Política usava {@code \b}, e {@code _} é caractere de palavra: {@code \bop\b} não
 *       alcança {@code OP_S2}. É a MESMA razão pela qual {@code OPL2} precisou de entrada
 *       nominal no {@code application.yml}.</li>
 *   <li>O Detector usava lookaround de LETRA — que resolve o {@code _} e o dígito —, mas não
 *       conhecia {@code romaji}, e o {@code s} de {@code songs} quebrava o lookahead.</li>
 *   <li>Só a Política tinha a varredura por SUBSTRING, que é o que alcança {@code insertita} e
 *       qualquer variante grudada que o fansub invente.</li>
 * </ul>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Este é o ÚNICO lugar do projeto onde a forma do nome musical é decidida. Um segundo
 *       padrão fora daqui é a classe de defeito que esta classe existe para eliminar.</li>
 *   <li>A decisão é a UNIÃO das duas capacidades anteriores — substring E fronteira de letra —,
 *       nunca a interseção: reduzir alcance faria letra de música voltar a ser traduzida.</li>
 *   <li>Decide pelo NOME do estilo apenas. Não olha conteúdo da fala: quem faz isso é
 *       {@code DetectorEfeitoKaraokeService}, e ele continua proprietário dessa parte.</li>
 *   <li>Um {@code false} significa "o nome não declara música" — NÃO significa "é diálogo".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Estilo {@code null} ou em branco devolve {@code false}. Nunca lança.
 */
public final class PadraoEstiloMusical {

    /**
     * Palavras que, GRUDADAS a outras, ainda declaram música: {@code insertita} (visto no
     * DanMachi), {@code Songbird}. A varredura por substring é o que a Política sempre teve e o
     * Detector não — remover isto faria 55 linhas do acervo voltarem a ser traduzidas.
     */
    private static final String[] SUBSTRING_MUSICAL = {
        "song", "music", "karaoke", "romaji", "opening", "ending", "theme", "insert", "sing",
        "lyric",
        // "romanji" — com N, e NAO e digitacao errada nossa: e como o estilo esta escrito no
        // acervo, em 2.116 linhas do DanMachi. "romaji" nao casa "Romanji" por substring, e o
        // furo era o UNICO do acervo: dos 115 estilos distintos, este era o unico com cara de
        // musica que a politica nao reconhecia.
        //
        // O PREJUIZO, e foi GRAVADO em 24/08/2026: a reposicao de acento da 3.3 escreveu
        //     "kanarazu mata ai ni iku kara"  ->  "kanarazu mata aí ni iku kara"
        // em seis linhas de letra. O `ai` do romaji e 愛 ("amor"), nao o adverbio portugues.
        // E a MESMA cicatriz de 19/08/2026, quando "mae" (前) virou "mãe" em 103 linhas —
        // repetida cinco dias depois por uma letra a mais no nome do estilo. Desfeito do backup.
        "romanji"
    };

    /**
     * Abreviações curtas ({@code OP}, {@code ED}) exigem fronteira, senão {@code "Ed Sheeran"} ou
     * qualquer estilo com {@code "ed"} no meio viraria música.
     *
     * <p>A fronteira é de LETRA ({@code \p{L}}), não {@code \b}: com {@code \b} o sublinhado é
     * caractere de palavra e {@code OP_S2}/{@code ED_S2} NÃO casavam — os estilos que somam
     * 1.263 das 1.385 linhas de divergência medidas.
     */
    private static final Pattern ABREVIACAO_MUSICAL =
        Pattern.compile("(?i)(?<!\\p{L})(op|ed)(?!\\p{L})");

    private PadraoEstiloMusical() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde se o NOME do estilo declara música.
     *
     * <p>INVARIANTES DO DOMÍNIO: substring OU abreviação com fronteira de letra. A ordem não
     * importa para o resultado; a substring vem primeiro por ser mais barata.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null}/em branco devolve {@code false}.
     *
     * @param estilo nome do estilo ASS
     * @return {@code true} quando o nome declara música
     */
    public static boolean nomeDeclaraMusica(String estilo) {
        if (estilo == null || estilo.isBlank()) {
            return false;
        }
        String minusculo = estilo.toLowerCase(Locale.ROOT);
        for (String palavra : SUBSTRING_MUSICAL) {
            if (minusculo.contains(palavra)) {
                return true;
            }
        }
        return ABREVIACAO_MUSICAL.matcher(estilo).find();
    }

    /**
     * {@code OP}/{@code ED} grudado a no máximo uma letra e seguido de dígito: {@code OPL2},
     * {@code EDA1}. É a forma que {@link #ABREVIACAO_MUSICAL} não alcança por construção (a letra
     * grudada derrota a fronteira) e que {@code Edit} ou {@code Default} nunca têm.
     */
    private static final Pattern ABREVIACAO_GRUDADA_COM_NUMERO =
        Pattern.compile("(?i)(?<!\\p{L})(op|ed)\\p{L}?\\d");

    /**
     * PROPÓSITO DE NEGÓCIO: o nome do estilo SUGERE música, ainda que não a declare — pergunta
     * FROUXA, que serve só para ALERTAR que a régua de {@link #nomeDeclaraMusica} pode estar
     * estreita demais. Nunca decide se um evento é música.
     *
     * <h2>O prejuízo que originou, medido em 24/09/2026</h2>
     * O alerta "[ SUSPEITO ] estilo não reconhecido como música" disparava para todo estilo com 5+
     * eventos fora da música — o {@code Default} do diálogo e as {@code Signs} de todo arquivo. Na
     * tela do Karaokê ele aparecia em cada episódio, e alarme que sempre toca ensina a ignorar o
     * alarme no dia em que ele importa (foi um estilo {@code OPL2} que passou despercebido).
     *
     * <p>INVARIANTES DO DOMÍNIO: é SUPERCONJUNTO de {@link #nomeDeclaraMusica} — o que declara
     * também sugere. Continua decidindo só pelo NOME.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null}/em branco devolve {@code false}.
     */
    public static boolean nomeSugereMusica(String estilo) {
        return nomeDeclaraMusica(estilo)
            || (estilo != null && ABREVIACAO_GRUDADA_COM_NUMERO.matcher(estilo).find());
    }

    /**
     * O fansub rotula a camada de tradução da letra: {@code OP - English}, {@code ED - EN},
     * {@code Song ENG}, {@code Tradução}. Veio da 4.1 ({@code ClassificadorLetraKaraokeService}),
     * que decide por ele, sem mudar uma letra do padrão.
     */
    private static final Pattern NOME_DECLARA_INGLES =
        Pattern.compile("(?i)\\b(english|eng|en|translation|tradu[cç][aã]o)\\b");

    /**
     * PROPÓSITO DE NEGÓCIO: fonte ÚNICA da pergunta "o NOME do estilo declara a camada em
     * inglês (a tradução da letra)?". Até 08/10/2026 só a Tradução de Karaokê (4.1) a fazia, e
     * a auditoria (1.4) não tinha como perguntar sem depender de outra fatia: acusava CRITICAL
     * "romaji alterado" em 110 linhas {@code Song ENG} do 86 que a 4.1 traduz de propósito
     * (auditoria de 08/10/2026, A9, medido no acervo).
     *
     * <p>INVARIANTES DO DOMÍNIO: decide só pelo NOME; o padrão é o mesmo que a 4.1 sempre usou
     * (fronteira {@code \b}), para a decisão dela não mudar. Um {@code true} não diz que a linha
     * é música nem que não tem japonês — quem combina os sinais é quem pergunta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null}/em branco devolve {@code false}.
     */
    public static boolean nomeDeclaraIngles(String estilo) {
        return estilo != null && !estilo.isBlank() && NOME_DECLARA_INGLES.matcher(estilo).find();
    }
}
