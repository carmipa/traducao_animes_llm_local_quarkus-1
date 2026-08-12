package org.traducao.projeto.raspagemRevisao.application.concordancia;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: o vocabulário de gênero do português — substantivos, adjetivos,
 * particípios, tratamentos e verbos de ligação — num lugar só. É a matéria-prima das regras de
 * concordância, e não uma regra em si: aqui não se decide nada, só se declara o que é palavra
 * masculina e o que é feminina.
 *
 * <h2>Por que existe separado</h2>
 * As mesmas listas alimentam regras que agora moram em classes diferentes — a nominal usa
 * {@code SUBST_FEM} com artigo e com adjetivo, a de pronomes usa {@code PARTIC_MASC} no
 * predicado, a de tratamentos usa {@code TRATAMENTO_FEM}. Duplicar o vocabulário faria as
 * cópias divergirem no dia em que alguém acrescentasse "aventureira" numa e esquecesse a
 * outra, e a regra que ficou para trás passaria a errar em silêncio.
 *
 * <p>Isto NÃO contradiz a preferência do projeto por duplicação consciente sobre acoplamento:
 * aquela vale entre FATIAS. Dentro da mesma fatia, vocabulário compartilhado é uma fonte de
 * verdade, não uma dependência cruzada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cada lista masculina tem a feminina correspondente, na MESMA ordem de conceitos. Uma
 *       palavra acrescentada de um lado sem o par do outro cria assimetria: a regra passa a
 *       acusar num sentido e a calar no outro.</li>
 *   <li>São fragmentos de alternância de regex, sem âncora e sem grupo — quem monta o
 *       {@link Pattern} decide a fronteira e o agrupamento.</li>
 *   <li>Classe de dados: sem estado, sem lógica, não instanciável.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não executa nada e não lança. Um fragmento mal formado só se manifesta quando alguém o
 * compila num {@code Pattern} — por isso as regras que os usam têm testes próprios.
 */
public final class LexicoGenero {

    /** Case-insensitive com classes Unicode: a legenda tem acento e caixa mista. */
    public static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS;

    public static final String SUBST_FEM =
        "menina|garota|moça|moca|mulher|deusa|princesa|heroina|heroína|rainha|senhora|"
            + "irmã|irma|mãe|mae|filha|avó|tia|amiga|dama|donzela|aventureira|sacerdotisa|"
            + "feiticeira|amazona|ladra|ladrona|deusa|moça|moca";

    public static final String SUBST_MASC =
        "menino|garoto|moço|moco|homem|deus|príncipe|principe|irmão|irmao|pai|filho|avô|"
            + "tio|amigo|rei|herói|heroi|aventureiro|novato|campeão|campeao|rapaz|"
            + "sacerdote|mago|ladrao|ladrão|deus|garoto";

    /** Antepostos incluem possessivos ({@code meu}, {@code seu}) — "meu menina" é erro. */
    public static final String ADJ_MASC =
        "novo|velho|pequeno|meu|seu|nosso|pronto|cansado|sozinho|animado|nervoso|"
            + "preocupado|furioso|surpreso|certo|errado|bom|mau|satisfeito|"
            + "irritado|confuso|ansioso|fraco|lindo|feio|bravo|loco|louco|"
            + "assustado|machucado|ferido|ocupado|perdido|vivo|morto|bêbado|bebado|doido";

    public static final String ADJ_FEM =
        "nova|velha|pequena|minha|sua|nossa|pronta|cansada|sozinha|animada|nervosa|"
            + "preocupada|furiosa|surpresa|certa|errada|boa|má|ma|satisfeita|"
            + "irritada|confusa|ansiosa|fraca|linda|feia|brava|loca|louca|"
            + "assustada|machucada|ferida|ocupada|perdida|viva|morta|bêbada|bebada|doida";

    /**
     * Pospostos NÃO trazem possessivo: "a garota minha" não é construção corrente, e incluir
     * {@code minha} aqui faria a regra acusar inversões poéticas legítimas.
     */
    public static final String ADJ_MASC_POSPOSTO =
        "novo|velho|pequeno|pronto|cansado|sozinho|animado|nervoso|preocupado|furioso|"
            + "surpreso|certo|errado|bom|mau|satisfeito|irritado|confuso|ansioso|fraco|"
            + "lindo|feio|bravo|loco|louco|assustado|machucado|ferido|ocupado|perdido|"
            + "vivo|morto|bêbado|bebado|doido";

    public static final String ADJ_FEM_POSPOSTO =
        "nova|velha|pequena|pronta|cansada|sozinha|animada|nervosa|preocupada|furiosa|"
            + "surpresa|certa|errada|boa|má|ma|satisfeita|irritada|confusa|ansiosa|fraca|"
            + "linda|feia|brava|loca|louca|assustada|machucada|ferida|ocupada|perdida|"
            + "viva|morta|bêbada|bebada|doida";

    /** Particípio predicativo — subconjunto do adjetivo, sem os de qualidade pura. */
    public static final String PARTIC_MASC =
        "cansado|pronto|preocupado|animado|nervoso|sozinho|furioso|surpreso|certo|errado|"
            + "satisfeito|irritado|confuso|ansioso|loco|louco|assustado|machucado|ferido|"
            + "ocupado|perdido|vivo|morto|bêbado|bebado|doido";

    public static final String PARTIC_FEM =
        "cansada|pronta|preocupada|animada|nervosa|sozinha|furiosa|surpresa|certa|errada|"
            + "satisfeita|irritada|confusa|ansiosa|loca|louca|assustada|machucada|ferida|"
            + "ocupada|perdida|viva|morta|bêbada|bebada|doida";

    public static final String TRATAMENTO_MASC = "senhor|moço|moco|garoto|rapaz|cara|homem|menino";
    public static final String TRATAMENTO_FEM = "senhora|moça|moca|garota|menina|dama|donzela";

    /** Verbo de ligação que introduz predicativo — é ele que amarra o adjetivo ao sujeito. */
    public static final String VERBO_AUX =
        "está|esta|estava|é|era|foi|será|sera|ficou|parece|continua|ficará|ficara|estará|estara|"
            + "estão|estao|foram|eram|serão|serao|ficaram|parecem|continuam";

    public static final String VERBO_IMPERATIVO =
        "diga|fale|fala|pergunte|pergunte|avise|mande|manda|chame|chama|espere|espera|"
            + "olhe|olha|escute|escuta|veja|ve|ouça|ouca|deixe|deixa";

    // ------------------------------------------------------------------
    // Marcas de gênero do INGLÊS. Vivem aqui porque três famílias de regras as consultam —
    // pronomes cruzados, tratamentos e predicado —, e uma segunda cópia divergiria.
    // ------------------------------------------------------------------

    /**
     * TODA referência feminina, não só o pronome sujeito. A largura é deliberada e tem
     * cicatriz: a guarda que usava apenas {@code \bshe\b} não casava {@code her}/{@code hers},
     * e "He gave it to her" com "ela" na tradução disparava um motivo cuja mensagem afirmava
     * não haver referência feminina no original.
     */
    public static final Pattern PRONOME_FEMININO_EN = Pattern.compile(
        "\\b(she|her|hers|girl|woman|lady|mother|mom|sister|daughter|"
            + "princess|goddess|queen|heroine|miss|mrs|ms|madam|ma'am|female|wife|aunt|"
            + "grandma|grandmother|niece|waitress|actress|hostess)\\b", FLAGS);

    /**
     * Espelho do anterior. Mesma cicatriz, medida em 2026-07-28 no Guilty Crown: com
     * {@code \bhe\b}, a fala "Oh, I know about him! Hiromi said she saw it all." acusava o
     * "ele" correto, porque {@code him} não casa {@code he} — e a correção foi persistida no
     * cache como "Eu sei sobre ela".
     */
    public static final Pattern PRONOME_MASCULINO_EN = Pattern.compile(
        "\\b(he|him|his|boy|man|guy|father|dad|brother|son|prince|god|king|"
            + "hero|mr|sir|male|husband|uncle|grandpa|grandfather|nephew|waiter|actor)\\b", FLAGS);

    /** Pronomes isolados — usados onde a regra precisa da marca EXATA, não da família. */
    public static final Pattern HER_EN = Pattern.compile("\\bher\\b", FLAGS);
    public static final Pattern HIM_EN = Pattern.compile("\\bhim\\b", FLAGS);
    public static final Pattern SHE_EN = Pattern.compile("\\bshe\\b", FLAGS);
    public static final Pattern HE_EN = Pattern.compile("\\bhe\\b", FLAGS);

    private LexicoGenero() {
    }
}
