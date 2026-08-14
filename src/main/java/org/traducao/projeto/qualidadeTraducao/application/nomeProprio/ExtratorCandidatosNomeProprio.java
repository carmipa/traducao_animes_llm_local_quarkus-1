package org.traducao.projeto.qualidadeTraducao.application.nomeProprio;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: separa, na fala em inglês, as palavras que PODEM ser nome próprio da obra
 * — e faz isso com a maior severidade possível, porque quem julga se é nome mesmo é o dicionário,
 * não este extrator.
 *
 * <h2>A cicatriz que define cada regra abaixo</h2>
 * A heurística "palavra capitalizada no meio da frase é nome obrigatório" já existiu no projeto e
 * foi REMOVIDA por medição: das 560 pendências que ela gerava, <b>323 eram falso positivo (57,7%)</b>
 * — registro em {@code RecuperarPendenciaFallbackService:249-255}. O que mudou desde então não foi
 * a heurística: foi existir dicionário de quatro idiomas para filtrar a saída dela. Este extrator é
 * de propósito a metade BURRA do par, e só se justifica porque a metade que decide veio depois.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Primeira palavra de frase NUNCA é candidata: maiúscula ali é ortografia, não nome. Vale
 *       para o começo do texto, depois de {@code . ! ? :} e depois de {@code \N} — a quebra é
 *       tratada como início de frase por ser o caso conservador.</li>
 *   <li>Conteúdo de tag ASS ({@code {...}}) não entra: nome de fonte e de efeito são capitalizados
 *       e não são fala.</li>
 *   <li>CAIXA ALTA inteira fica fora: em legenda é ênfase ou sigla, e sigla já tem tratamento
 *       próprio em {@code RecuperarPendenciaFallbackService}.</li>
 *   <li>Mínimo de três letras, sem dígito: {@code I}, {@code A} e {@code B2} não são nome de
 *       personagem.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entrada nula ou vazia devolve conjunto vazio. Classe utilitária sem estado; nada lança.
 */
final class ExtratorCandidatosNomeProprio {

    /** Tag de override ASS inteira, incluindo o conteúdo. */
    private static final Pattern TAG_ASS = Pattern.compile("\\{[^}]*}");

    /**
     * Palavra iniciada por maiúscula, só letras, três ou mais. O grupo anterior captura o que vem
     * antes para decidir se é começo de frase.
     */
    private static final Pattern PALAVRA = Pattern.compile("(\\p{L}+)");

    /** Pontuação que abre frase nova. O apóstrofo NÃO entra: {@code Kaine's} é o mesmo nome. */
    private static final String FIM_DE_FRASE = ".!?:;\"«»(-—";

    private ExtratorCandidatosNomeProprio() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve as palavras da fala que merecem ser perguntadas ao dicionário.
     *
     * <p>INVARIANTES DO DOMÍNIO: nenhuma palavra em início de frase, dentro de tag, em caixa alta
     * ou com dígito atravessa. A ordem de inserção é preservada para o relatório ficar legível.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto vazio para entrada nula, vazia ou só de tags.
     */
    static Set<String> candidatas(String original) {
        Set<String> achadas = new LinkedHashSet<>();
        if (original == null || original.isBlank()) {
            return achadas;
        }
        // As tags viram espaço, e não somem: apagar juntaria as palavras vizinhas e criaria
        // fronteira de frase onde não existia.
        String limpo = TAG_ASS.matcher(original).replaceAll(" ");
        // A quebra é tratada como início de frase — conservador de propósito.
        limpo = limpo.replace("\\N", " . ").replace("\\n", " . ");

        Matcher m = PALAVRA.matcher(limpo);
        while (m.find()) {
            String palavra = m.group(1);
            if (!formaAceitavel(palavra) || abreFrase(limpo, m.start())) {
                continue;
            }
            achadas.add(palavra);
        }
        return achadas;
    }

    /** Maiúscula inicial, três letras ou mais, e não a palavra inteira em caixa alta. */
    private static boolean formaAceitavel(String palavra) {
        if (palavra.length() < 3 || !Character.isUpperCase(palavra.charAt(0))) {
            return false;
        }
        return !palavra.equals(palavra.toUpperCase(java.util.Locale.ROOT));
    }

    /**
     * Olha para trás até achar caractere significativo. Só espaço e aspas simples separam a
     * palavra do que a precede; pontuação de fim de frase — ou o começo do texto — significa que
     * a maiúscula é obrigatória e não diz nada sobre ser nome.
     */
    private static boolean abreFrase(String texto, int inicio) {
        for (int i = inicio - 1; i >= 0; i--) {
            char c = texto.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            return FIM_DE_FRASE.indexOf(c) >= 0;
        }
        return true;
    }
}
