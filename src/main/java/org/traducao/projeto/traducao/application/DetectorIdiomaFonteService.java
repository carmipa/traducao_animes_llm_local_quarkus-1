package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.core.texto.FronteiraTermoAss;

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
 *       ou ≥2 diacríticos com ao menos um em palavra de inicial minúscula) E AUSÊNCIA de
 *       sinal de inglês (lista ampliada de function words + apóstrofo tipográfico normalizado).
 *       Uma linha inglesa nunca é classificada como já-no-alvo — deixar inglês sem traduzir é o
 *       erro a evitar; reenviar um PT já pronto ao LLM (custo de eco) é o tradeoff aceito.</li>
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

    // Palavras que só existem em inglês — presença ⇒ NÃO é português (manda traduzir). Lista
    // AMPLIADA (function words de alta frequência sem homógrafo em PT). Excluídos de propósito
    // por colisão com PT: "to" (tô/to), "come" (ele come), "no"/"do"/"me"/"a"/"as" etc.
    //
    // A fronteira vem de FronteiraTermoAss porque \N ocupa DOIS caracteres e o N é letra: com
    // um lookbehind puro, a stopword colada na quebra fica INVISÍVEL. Medido no acervo em
    // 2026-08-05: 67.923 falas, 6.744 com stopword inglesa colada na quebra, e em 166 delas o
    // inglês era invisível por inteiro. Dessas 166, ZERO tinham também evidência de PT — ou
    // seja, nenhuma chegou a ser classificada como já-no-alvo. O defeito é LATENTE, não
    // realizado, e o conserto é para não depender dessa coincidência.
    private static final Pattern SINAL_INGLES = Pattern.compile(
        "(?i)" + FronteiraTermoAss.INICIO + "(the|you|your|and|is|are|was|were|this|that|with|what|have|has"
        + "|for|not|it|its|we|they|he|she|will|would|can't|don't|i'm|it's|isn't|of|from|about"
        + "|my|his|him|her|our|them|be|been|go|get|got|here|there|now|when|where|who|how|why"
        + "|make|take|know|want|need|before|after|into|onto|over|just|than|then|does|did|back"
        + "|down|up|out|off|said|only|one|two|but|if|let|way|day|god|hey|yeah|too|gonna|wanna|gotta)"
        + FronteiraTermoAss.FIM);

    // Stopwords FRANCESAS sem colisão com português — presença ⇒ NÃO é português (manda traduzir).
    //
    // O PREJUÍZO QUE OBRIGOU A EXISTIR, medido em 14/08/2026 nas faixas francesas do Memories:
    // 208 de 963 falas (21,6%) eram classificadas como "já no idioma-alvo" e NUNCA chegariam ao
    // LLM — ficariam em francês no arquivo final sem sequer virar pendência. Na mesma medição a
    // faixa INGLESA dos mesmos três filmes deu 0 de 2.402 (0,0%), o que prova que o furo é
    // específico do francês, não frouxidão geral do detector.
    //
    // A causa são as duas vias abaixo, ambas calibradas só contra fonte inglesa:
    //   - SINAL_PORTUGUES traz "à" e "nos", que são francês corrente ("Nous rentrons à la base");
    //   - o ramo por diacríticos aceita é/è/ê/à/ç/ô, que o francês usa o tempo todo
    //     ("Je reviens bientôt, Cécile." tem dois acentos em palavra minúscula).
    //
    // Lista CONSERVADORA: cada forma foi conferida como inexistente em português. Ficaram de
    // fora, de propósito, os homógrafos "de", "se", "me", "te", "la", "ou", "mais" e "que" —
    // acusar qualquer um deles mandaria retraduzir fala portuguesa legítima, que é o dano oposto.
    private static final Pattern SINAL_FRANCES = Pattern.compile(
        "(?i)" + FronteiraTermoAss.INICIO + "(le|les|des|du|est|sont|vous|nous|je|ne|pas|qui"
        + "|dans|avec|sur|elle|elles|il|ils|une|être|très|bien|tout|toute|moi|toi|lui|leur"
        + "|cette|ces|mon|ton|aux|au|on|et|donc|alors|quoi|oui|non|rien|encore|aussi|comme"
        + "|quand|faut|peut|veux|sais|suis|était|avait|fait|chose|toujours|ici"
        + "|maintenant|parce|pourquoi|comment|beaucoup|peu|trop|assez|déjà|puis|depuis"
        // Segunda leva, 14/08/2026: formas colhidas nas 21 falas que sobraram da primeira
        // medição ("O.K., frappez à la porte pour voir", "Ça explique cet abominable décor",
        // "Transmettez ça à notre armée"). Mesmo critério das anteriores — nenhuma é palavra
        // portuguesa. Continuam FORA "tu", "me", "la", "en", "mais" e "entre", que são.
        + "|pour|ça|cet|un|notre|votre|tes|ses|soyez|êtes|avez|allez|ont|sommes"
        // QUATRO FORMAS SAÍRAM DAQUI depois de entrarem por engano, e as três primeiras foram
        // pegas medindo contra legenda PORTUGUESA do acervo, não por releitura:
        //   partir  — português corrente ("vamos partir");
        //   jamais  — português corrente, mesma grafia ("a Federação jamais abandonará");
        //   mes     — é "mês" SEM ACENTO ("Cheguei aqui ha um mes");
        //   sera    — é "será" SEM ACENTO ("você sera enviado para a batalha");
        //   vos     — pronome português ("eu vos digo"), e "vós" sem acento.
        // As duas do meio são o agravante deste acervo em particular: falta acento em parte das
        // traduções, e forma portuguesa desacentuada cai exatamente na grafia francesa. Palavra
        // candidata a esta lista precisa ser conferida SEM acento também.
        + "|sans|sous|vers|autre|autres|même|chez|quelque|quelques|sûr|prêt|prêts|trouver"
        + "|dire|faire|voir|savoir|venir|donner|monde|temps|homme|femme|jour|nuit)"
        + FronteiraTermoAss.FIM);

    // ELISÃO com apóstrofo — assinatura estrutural do francês que o português não tem.
    // "c'est", "l'a", "d'un", "qu'il", "j'ai", "n'est", "s'enfoncer". Pega a fala francesa curta
    // que não tem nenhuma stopword da lista acima, e é barata: uma consoante elidível, apóstrofo,
    // vogal. O inglês elide com apóstrofo também ("don't", "it's"), mas ali o padrão é
    // LETRA+'+consoante/s no FIM da palavra, não consoante isolada no começo.
    //
    // A fronteira vem de FronteiraTermoAss, e não de um lookbehind próprio, pelo mesmo motivo dos
    // dois padrões acima: o \N da quebra ASS termina em N, que é LETRA. Com "(?<![\p{L}])" a
    // elisão colada na quebra — "...avant\Nqu'il puisse..." — ficaria invisível, que é justamente
    // a forma como ela aparece na legenda real do Memories.
    private static final Pattern ELISAO_FRANCESA = Pattern.compile(
        "(?i)" + FronteiraTermoAss.INICIO + "(?:[cdjlmnst]|qu)'\\p{L}");

    // Stopwords portuguesas sem colisão com inglês — presença ⇒ forte sinal de PT.
    // Mesma fronteira do sinal inglês, e pelo mesmo motivo: a palavra colada na quebra do ASS
    // não pode desaparecer da contagem. Aqui a falha era do lado SEGURO (perder evidência de PT
    // manda traduzir de novo, custo de eco), mas as duas metades da decisão precisam enxergar o
    // texto com a mesma régua — senão o viés de segurança passa a depender de qual lado falha.
    private static final Pattern SINAL_PORTUGUES = Pattern.compile(
        "(?i)" + FronteiraTermoAss.INICIO + "(não|você|vocês|está|então|isso|aquele|aquela|porque|também|já"
        + "|é|à|dos|das|nas|nos|uma|meu|minha|seu|sua|ele|ela|eles|elas|nós|muito|aqui|agora"
        + "|mesmo|nada|tudo|gente|coisa|verdade|obrigado|obrigada|desculpe|vamos|quê|cadê)"
        + FronteiraTermoAss.FIM);

    /**
     * PROPÓSITO DE NEGÓCIO: responde se a fala-fonte já está no idioma-alvo e, portanto, deve
     * ser mantida sem passar pelo LLM.
     *
     * <p>INVARIANTES DO DOMÍNIO: só decide {@code true} com evidência de PT e sem sinal de
     * inglês; alvo não-PT, texto nulo ou linha curta ⇒ {@code false}. O apóstrofo tipográfico
     * (’) é normalizado para reto antes da checagem, para as contrações inglesas
     * ("don't"/"can't") não escaparem. O ramo por diacríticos exige que ao menos um acento
     * esteja numa palavra de inicial minúscula (palavra comum PT), pois nomes próprios
     * acentuados do inglês ("André"/"Chloé") são capitalizados e não indicam idioma-fonte PT.
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
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ")
            .replace('’', '\'').trim();
        if (limpo.length() < MIN_CARACTERES) {
            return false;
        }
        if (SINAL_INGLES.matcher(limpo).find()) {
            return false;
        }
        // ANTES do sinal português, e a ordem é o conserto: "Nous rentrons à la base" casa "à" na
        // lista PT e sairia como já-traduzida. Perguntar primeiro se é francês resolve as duas
        // vias de uma vez, porque quem chega aqui com stopword francesa ou elisão não é PT.
        if (SINAL_FRANCES.matcher(limpo).find() || ELISAO_FRANCESA.matcher(limpo).find()) {
            return false;
        }
        if (SINAL_PORTUGUES.matcher(limpo).find()) {
            return true;
        }
        // Ramo por diacríticos: exige ≥2 acentos PT E que ao menos um esteja em palavra de
        // inicial minúscula (palavra comum). Nomes próprios acentuados em inglês são
        // capitalizados, então uma linha inglesa como "André loves Chloé." não qualifica.
        long diacriticos = 0;
        boolean acentoEmPalavraComum = false;
        for (String palavra : limpo.split("\\s+")) {
            if (palavra.isEmpty()) {
                continue;
            }
            boolean inicialMinuscula = Character.isLowerCase(palavra.charAt(0));
            boolean palavraTemAcento = false;
            for (int i = 0; i < palavra.length(); i++) {
                if (DIACRITICOS_PT.indexOf(Character.toLowerCase(palavra.charAt(i))) >= 0) {
                    diacriticos++;
                    palavraTemAcento = true;
                }
            }
            if (inicialMinuscula && palavraTemAcento) {
                acentoEmPalavraComum = true;
            }
        }
        return diacriticos >= 2 && acentoEmPalavraComum;
    }
}
