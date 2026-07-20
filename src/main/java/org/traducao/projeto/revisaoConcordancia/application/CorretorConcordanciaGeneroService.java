package org.traducao.projeto.revisaoConcordancia.application;

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

    // Substantivos de gênero INEQUÍVOCO (pessoa/ser com gênero fixo). Ambíguos ficam de fora.
    private static final String SUBST_FEM =
        "menina|garota|moça|moca|mulher|deusa|princesa|rainha|senhora|irmã|irma|mãe|mae|filha|"
            + "tia|amiga|dama|donzela|aventureira|sacerdotisa|feiticeira|amazona|ladra|heroína|heroina";
    private static final String SUBST_MASC =
        "menino|garoto|moço|moco|homem|deus|príncipe|principe|rei|senhor|irmão|irmao|pai|filho|"
            + "tio|amigo|rapaz|herói|heroi|aventureiro|sacerdote|mago|ladrão|ladrao|príncipe";

    // Artigos/determinantes/contrações masculinos e o feminino correspondente (índice a índice).
    private static final String[] ART_MASC = {"o", "um", "este", "esse", "aquele", "do", "no", "ao", "pelo", "num"};
    private static final String[] ART_FEM  = {"a", "uma", "esta", "essa", "aquela", "da", "na", "à", "pela", "numa"};

    private static final Pattern ART_MASC_COM_SUBST_FEM =
        Pattern.compile("(?<![\\p{L}\\p{N}])(" + String.join("|", ART_MASC) + ")(\\s+)(" + SUBST_FEM + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ART_FEM_COM_SUBST_MASC =
        Pattern.compile("(?<![\\p{L}\\p{N}])(" + String.join("|", ART_FEM) + ")(\\s+)(" + SUBST_MASC + ")(?![\\p{L}\\p{N}])",
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
        Pattern.compile("(?<![\\p{L}\\p{N}])(ela)(\\s+(?:" + VERBO_LIGACAO + ")\\s+)(" + String.join("|", ADJ_MASC) + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ELE_COM_ADJ_FEM =
        Pattern.compile("(?<![\\p{L}\\p{N}])(ele)(\\s+(?:" + VERBO_LIGACAO + ")\\s+)(" + String.join("|", ADJ_FEM) + ")(?![\\p{L}\\p{N}])",
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
