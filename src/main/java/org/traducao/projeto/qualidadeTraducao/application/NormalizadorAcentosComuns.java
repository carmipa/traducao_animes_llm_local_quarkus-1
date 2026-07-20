package org.traducao.projeto.qualidadeTraducao.application;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe acentos em palavras que o LLM às vezes devolve sem acento
 * (visto no 08th: {@code infancia}, {@code ate}). Age SÓ sobre formas cuja grafia sem acento
 * NUNCA é uma palavra válida em português — assim a correção é determinística e não muda
 * sentido. Complementa a revisão gramatical do modelo com uma rede determinística barata.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Dicionário CURADO e conservador: só entram formas cuja versão sem acento não existe em
 *       PT ({@code nao}, {@code voce}, {@code tambem}, {@code ate}, {@code infancia}, ...). São
 *       DELIBERADAMENTE excluídos homógrafos e casos que dependem de sentido — {@code esta}
 *       (this)≠{@code está} (is), {@code e} (and)≠{@code é} (is), {@code as vezes} (the times)≠
 *       {@code às vezes} (sometimes) — para nunca introduzir erro.</li>
 *   <li>Substituição por fronteira de palavra (não casa {@code ate} dentro de {@code Kate}),
 *       preservando a caixa do achado (Nao→Não, NAO→NÃO, nao→não).</li>
 *   <li>Classe sem estado; só JDK + Spring. Não conhece cache, LLM nem legenda.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto {@code null}/vazio ou sem nenhuma forma do dicionário é devolvido intacto; nunca lança.
 */
@Component
public class NormalizadorAcentosComuns {

    // Forma-sem-acento (nunca palavra válida em PT) -> forma acentuada canônica.
    private static final Map<String, String> CORRECOES = Map.ofEntries(
        Map.entry("nao", "não"),
        Map.entry("voce", "você"),
        Map.entry("voces", "vocês"),
        Map.entry("tambem", "também"),
        Map.entry("ate", "até"),
        Map.entry("alem", "além"),
        Map.entry("apos", "após"),
        Map.entry("atras", "atrás"),
        Map.entry("infancia", "infância"),
        Map.entry("ninguem", "ninguém"),
        Map.entry("alguem", "alguém"),
        Map.entry("porem", "porém")
    );

    private static final Pattern PALAVRA;
    static {
        // Alternância ordenada da mais longa para a mais curta (voces antes de voce).
        String alternancia = CORRECOES.keySet().stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .reduce((a, b) -> a + "|" + b).orElse("");
        PALAVRA = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(" + alternancia + ")(?![\\p{L}\\p{N}])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o texto com os acentos repostos nas formas do dicionário.
     * <p>INVARIANTES DO DOMÍNIO: só troca formas do dicionário curado, por fronteira de palavra,
     * preservando a caixa; nada mais é tocado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto {@code null}/vazio devolve a si mesmo.
     */
    public String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        Matcher m = PALAVRA.matcher(texto);
        StringBuilder sb = new StringBuilder(texto.length());
        while (m.find()) {
            String achado = m.group(1);
            String base = CORRECOES.get(achado.toLowerCase(Locale.ROOT));
            m.appendReplacement(sb, Matcher.quoteReplacement(aplicarCaixa(achado, base)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String aplicarCaixa(String achado, String base) {
        if (achado.length() > 1 && achado.chars().allMatch(Character::isUpperCase)) {
            return base.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(achado.charAt(0))) {
            return Character.toUpperCase(base.charAt(0)) + base.substring(1);
        }
        return base;
    }
}
