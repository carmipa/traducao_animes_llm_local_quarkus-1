package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: detecta, de forma CONSERVADORA, quando uma fala-fonte já está no
 * idioma-alvo (PT) — para o pipeline NÃO reenviá-la ao LLM (que devolveria eco ou recusa)
 * e mantê-la como está. Nasce da constatação de que fontes contaminadas (arquivos "inglês"
 * meio-traduzidos) geravam pendências e meta-respostas em massa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só opera quando o idioma-alvo é português ({@code pt*}); qualquer outro alvo devolve
 *       {@code false} (nunca pula a tradução).</li>
 *   <li>Viés de segurança: exige EVIDÊNCIA de português (stopword-PT inexistente em inglês,
 *       ou ≥2 diacríticos PT) E AUSÊNCIA de sinal forte de inglês. Uma linha inglesa nunca é
 *       classificada como já-no-alvo — deixar inglês sem traduzir é o erro a evitar.</li>
 *   <li>Linhas muito curtas são ambíguas e nunca são puladas.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Função pura, sem I/O; texto nulo/curto ou alvo não-PT ⇒ {@code false} (manda traduzir).
 * Não lança.
 */
@Service
public class DetectorIdiomaFonteService {

    private static final Pattern TAGS_ASS = Pattern.compile("\\{[^}]*}");
    private static final int MIN_CARACTERES = 6;
    private static final String DIACRITICOS_PT = "ãõçáéíóúâêôà";

    // Palavras que só existem em inglês — presença ⇒ NÃO é português (manda traduzir).
    // "to" fica de fora de propósito (colide com o coloquial "tô"/"to" do PT).
    private static final Pattern SINAL_INGLES = Pattern.compile(
        "(?i)(?<![\\p{L}])(the|you|your|and|is|are|was|were|this|that|with|what|have|has"
        + "|for|not|it|its|we|they|he|she|will|would|can't|don't|i'm|it's|isn't|of|from|about)"
        + "(?![\\p{L}])");

    // Stopwords portuguesas sem colisão com inglês — presença ⇒ forte sinal de PT.
    private static final Pattern SINAL_PORTUGUES = Pattern.compile(
        "(?i)(?<![\\p{L}])(não|você|vocês|está|então|isso|aquele|aquela|porque|também|já"
        + "|é|à|dos|das|nas|nos|uma|meu|minha|seu|sua|ele|ela|eles|elas|nós|muito|aqui|agora"
        + "|mesmo|nada|tudo|gente|coisa|verdade|obrigado|obrigada|desculpe|vamos|quê|cadê)"
        + "(?![\\p{L}])");

    /**
     * PROPÓSITO DE NEGÓCIO: responde se a fala-fonte já está no idioma-alvo e, portanto, deve
     * ser mantida sem passar pelo LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: só decide {@code true} com evidência de PT e sem sinal de
     * inglês; alvo não-PT, texto nulo ou linha curta ⇒ {@code false}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas/curtas devolvem {@code false}.
     *
     * @param texto texto-fonte original da fala (pode conter tags/quebras ASS)
     * @param idiomaAlvo idioma de destino configurado (ex.: {@code pt-br})
     */
    public boolean jaNoIdiomaAlvo(String texto, String idiomaAlvo) {
        if (idiomaAlvo == null || !idiomaAlvo.toLowerCase(Locale.ROOT).startsWith("pt")) {
            return false;
        }
        if (texto == null) {
            return false;
        }
        String limpo = TAGS_ASS.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ").trim();
        if (limpo.length() < MIN_CARACTERES) {
            return false;
        }
        if (SINAL_INGLES.matcher(limpo).find()) {
            return false;
        }
        if (SINAL_PORTUGUES.matcher(limpo).find()) {
            return true;
        }
        long diacriticos = limpo.chars()
            .filter(c -> DIACRITICOS_PT.indexOf(Character.toLowerCase((char) c)) >= 0)
            .count();
        return diacriticos >= 2;
    }
}
