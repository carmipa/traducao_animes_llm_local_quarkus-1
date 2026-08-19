package org.traducao.projeto.revisaoConcordancia.application;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: corrige DETERMINISTICAMENTE erros de concordância de GÊNERO inequívocos
 * numa legenda em português, sem depender do inglês nem do LLM. É o coração do menu "Revisão de
 * Concordância": o detector já enxergava esses erros, mas nada os consertava PT-only — aqui a
 * detecção vira correção. Cobre os casos objetivos e seguros: (1) artigo/determinante de um
 * gênero antes de substantivo de gênero conhecido oposto ("o menina" → "a menina", "uma menino"
 * → "um menino"); (2) sujeito "ela"/"ele" com predicativo adjetivo no gênero errado ("ela está
 * cansado" → "ela está cansada").
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só age sobre substantivos/adjetivos de gênero INEQUÍVOCO (listas curadas); nada ambíguo
 *       é tocado.</li>
 *   <li>Preserva a caixa inicial da palavra trocada ("O menina" → "A menina"); tags ASS e o
 *       restante da fala ficam intactos; nunca deixa a linha pior.</li>
 *   <li>Serviço sem estado; só JDK + Spring; não conhece cache, LLM nem inglês.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto {@code null}/vazio ou sem erro inequívoco devolve {@link Optional#empty()} (mantém o
 * texto); nunca lança.
 */
@Service
public class CorretorConcordanciaGeneroService {

    /**
     * Fronteira de início do termo, com a quebra {@code \N} do ASS tratada como separador — igual à
     * de {@code NormalizadorAcentosComuns}, onde o efeito está MEDIDO.
     *
     * <p><b>Aqui o efeito é NULO no acervo de hoje, e isso está declarado de propósito.</b> Medido
     * em 2026-08-04 sobre 60.891 falas traduzidas do cache, com os padrões INTEIROS (artigo +
     * substantivo, pronome + verbo de ligação + adjetivo), não com o fragmento do artigo:
     * <ul>
     *   <li>artigo MASC + substantivo FEM: 0 antes, 0 depois</li>
     *   <li>artigo FEM + substantivo MASC: 11 antes, 11 depois</li>
     *   <li>{@code ela} + adjetivo MASC: 0 antes, 0 depois</li>
     *   <li>{@code ele} + adjetivo FEM: 0 antes, 0 depois</li>
     * </ul>
     * O fansub quebra a linha 24,6% das vezes, mas nunca ENTRE o artigo e o núcleo de gênero
     * inequívoco — a quebra cai em outro ponto da fala. Contar só o fragmento {@code artigo + \s+}
     * dá +375 e +268 e sugere ganho que não existe: o par completo é que decide.
     *
     * <p>Mantida por consistência e porque a quebra pode cair ali em qualquer acervo novo; NÃO
     * conta como correção demonstrada. Se alguém precisar reduzir superfície, este é o candidato —
     * não o de acentos.
     */
    private static final String INICIO_DE_TERMO = FronteiraTermoAss.INICIO;

    // Substantivos de gênero INEQUÍVOCO (pessoa/ser com gênero fixo). Ambíguos ficam de fora.
    private static final String SUBST_FEM =
        "menina|garota|moça|moca|mulher|deusa|princesa|rainha|senhora|irmã|irma|mãe|mae|filha|"
            + "tia|amiga|dama|donzela|aventureira|sacerdotisa|feiticeira|amazona|ladra|heroína|heroina";
    private static final String SUBST_MASC =
        "menino|garoto|moço|moco|homem|deus|príncipe|principe|rei|senhor|irmão|irmao|pai|filho|"
            + "tio|amigo|rapaz|herói|heroi|aventureiro|sacerdote|mago|ladrão|ladrao|príncipe";

    // Artigos/determinantes/contrações masculinos e o feminino correspondente (índice a índice).
    // Servem ao MAPA de troca; quem entra no PADRÃO é decidido logo abaixo, e não é a mesma lista.
    private static final String[] ART_MASC = {"o", "um", "este", "esse", "aquele", "do", "no", "ao", "pelo", "num"};
    private static final String[] ART_FEM  = {"a", "uma", "esta", "essa", "aquela", "da", "na", "à", "pela", "numa"};

    /**
     * Determinantes femininos que podem ser ACUSADOS antes de substantivo masculino — a lista do
     * padrão, deliberadamente MENOR que a do mapa: <b>o {@code a} sozinho fica de fora</b>.
     *
     * <h2>O prejuízo que originou (medido em 18/08/2026, acervo inteiro)</h2>
     * {@code a} não é só artigo feminino: é a preposição invariante em gênero, e antes de
     * substantivo masculino ela está CERTA. Com o {@code a} no padrão, esta tela reescrevia
     * <pre>
     *   "Graças a Deus, você está vivo."   ->   "Graças o Deus, você está vivo."
     *   "Ore a Deus, não a mim."           ->   "Ore o Deus, não a mim."
     * </pre>
     * Medição sobre 726 legendas de {@code C:\animes} (2.142.264 eventos, 380.697 falas ao
     * alcance): a tela mudaria <b>15 falas</b> — <b>14 delas eram "a Deus" correto</b>, contra
     * <b>1</b> conserto real ({@code "Aquela garoto" -> "Aquele garoto"}, Gundam ZZ). Ou seja,
     * rodar a 3.3 no acervo hoje causaria 14 estragos para 1 acerto.
     *
     * <p>O denominador encolheu no mesmo dia, e o achado não: com os 38 {@code .parcial} fora do
     * alcance, o acervo passou a 2.050.958 eventos e 332.545 falas — as 15 falas e o único acerto
     * continuam os mesmos, porque nenhum deles estava num parcial.
     *
     * <p>A exceção não é descoberta nova: o detector da 3.1
     * ({@code DetectorConcordanciaNominal}) já a documenta e já deixa o {@code a} de fora do
     * lado feminino. O corretor desta fatia nasceu de uma segunda escrita da mesma ideia e
     * perdeu a exceção no caminho — a divergência que a regra da medição prevê para toda
     * segunda implementação.
     *
     * <h2>Por que a assimetria é correta, e não um remendo</h2>
     * O lado masculino continua com o {@code o}: {@code "o"} nunca é preposição em português, e
     * {@code "Vi o menina"} segue sendo corrigido. Tirar os dois "por simetria" mataria a única
     * família que a medição mostrou funcionando.
     *
     * <p><b>Perda declarada:</b> {@code "a menino"} (artigo feminino de verdade antes de
     * substantivo masculino) deixa de ser corrigido. No acervo inteiro, essa construção aparece
     * <b>zero</b> vez — a única ocorrência de {@code a} + substantivo masculino é {@code a Deus}.
     */
    private static final String[] ART_FEM_NO_PADRAO =
        {"uma", "esta", "essa", "aquela", "da", "na", "à", "pela", "numa"};

    private static final Pattern ART_MASC_COM_SUBST_FEM =
        Pattern.compile(INICIO_DE_TERMO + "(" + String.join("|", ART_MASC) + ")(\\s+)(" + SUBST_FEM + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ART_FEM_COM_SUBST_MASC =
        Pattern.compile(INICIO_DE_TERMO + "(" + String.join("|", ART_FEM_NO_PADRAO) + ")(\\s+)(" + SUBST_MASC + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    // Adjetivos/particípios masc ↔ fem (paralelos): base para trocar o predicativo de "ela/ele".
    private static final String[] ADJ_MASC = {
        "cansado", "pronto", "preocupado", "animado", "nervoso", "sozinho", "furioso", "surpreso",
        "certo", "errado", "satisfeito", "irritado", "confuso", "ansioso", "assustado", "machucado",
        "ferido", "ocupado", "perdido", "vivo", "morto", "bêbado", "bebado", "novo", "velho",
        "lindo", "feio", "bravo", "louco", "fraco"};
    private static final String[] ADJ_FEM = {
        "cansada", "pronta", "preocupada", "animada", "nervosa", "sozinha", "furiosa", "surpresa",
        "certa", "errada", "satisfeita", "irritada", "confusa", "ansiosa", "assustada", "machucada",
        "ferida", "ocupada", "perdida", "viva", "morta", "bêbada", "bebada", "nova", "velha",
        "linda", "feia", "brava", "louca", "fraca"};

    private static final String VERBO_LIGACAO = "está|esta|estava|é|era|foi|fica|ficou|parece|continua|se sente";
    private static final Pattern ELA_COM_ADJ_MASC =
        Pattern.compile(INICIO_DE_TERMO + "(ela)(\\s+(?:" + VERBO_LIGACAO + ")\\s+)(" + String.join("|", ADJ_MASC) + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ELE_COM_ADJ_FEM =
        Pattern.compile(INICIO_DE_TERMO + "(ele)(\\s+(?:" + VERBO_LIGACAO + ")\\s+)(" + String.join("|", ADJ_FEM) + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Map<String, String> FLIP_ART_M2F = mapaFlip(ART_MASC, ART_FEM);
    private static final Map<String, String> FLIP_ART_F2M = mapaFlip(ART_FEM, ART_MASC);
    private static final Map<String, String> FLIP_ADJ_M2F = mapaFlip(ADJ_MASC, ADJ_FEM);
    private static final Map<String, String> FLIP_ADJ_F2M = mapaFlip(ADJ_FEM, ADJ_MASC);

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala com os erros de gênero inequívocos corrigidos.
     * <p>INVARIANTES DO DOMÍNIO: só troca artigo/adjetivo de gênero conhecido; preserva a caixa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem erro corrigível devolve {@link Optional#empty()}.
     */
    public Optional<String> corrigir(String pt) {
        if (pt == null || pt.isBlank()) {
            return Optional.empty();
        }
        String r = pt;
        r = flipPrimeiroGrupo(r, ART_MASC_COM_SUBST_FEM, FLIP_ART_M2F);
        r = flipPrimeiroGrupo(r, ART_FEM_COM_SUBST_MASC, FLIP_ART_F2M);
        r = flipTerceiroGrupo(r, ELA_COM_ADJ_MASC, FLIP_ADJ_M2F);
        r = flipTerceiroGrupo(r, ELE_COM_ADJ_FEM, FLIP_ADJ_F2M);
        return r.equals(pt) ? Optional.empty() : Optional.of(r);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca a 1ª captura (artigo) pelo gênero oposto, mantendo o espaço e
     * o substantivo (grupos 2 e 3) intactos.
     * <p>INVARIANTES DO DOMÍNIO: só substitui o artigo mapeado; preserva a caixa inicial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve o texto igual.
     */
    private String flipPrimeiroGrupo(String texto, Pattern pat, Map<String, String> flip) {
        Matcher m = pat.matcher(texto);
        return m.replaceAll(res -> {
            String palavra = res.group(1);
            String novo = flip.get(palavra.toLowerCase());
            return Matcher.quoteReplacement(preservarCaixa(palavra, novo) + res.group(2) + res.group(3));
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: troca a 3ª captura (adjetivo predicativo) pelo gênero oposto,
     * mantendo o pronome e o verbo de ligação (grupos 1 e 2).
     * <p>INVARIANTES DO DOMÍNIO: só substitui o adjetivo mapeado; preserva a caixa inicial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem casamento devolve o texto igual.
     */
    private String flipTerceiroGrupo(String texto, Pattern pat, Map<String, String> flip) {
        Matcher m = pat.matcher(texto);
        return m.replaceAll(res -> {
            String palavra = res.group(3);
            String novo = flip.get(palavra.toLowerCase());
            return Matcher.quoteReplacement(res.group(1) + res.group(2) + preservarCaixa(palavra, novo));
        });
    }

    private static Map<String, String> mapaFlip(String[] de, String[] para) {
        Map<String, String> m = new java.util.HashMap<>();
        for (int i = 0; i < de.length; i++) {
            m.put(de[i].toLowerCase(), para[i]);
        }
        return Map.copyOf(m);
    }

    private static String preservarCaixa(String original, String substituto) {
        if (!original.isEmpty() && Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(substituto.charAt(0)) + substituto.substring(1);
        }
        return substituto;
    }
}
