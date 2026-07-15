package org.traducao.projeto.traducaoKaraoke.application;

import jakarta.enterprise.context.ApplicationScoped;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducaoKaraoke.domain.ClasseLinhaKaraoke;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decide o destino de cada evento de música: preservar (letra original),
 * traduzir (camada em inglês) ou não tocar (efeito KFX / já em PT-BR).
 * <p>
 * O problema central que este classificador resolve: cantores japoneses
 * misturam inglês no meio da letra ("kimi no heart ni fly away"). A heurística
 * estrita de romaji do {@link DetectorEfeitoKaraokeService} exige que TODAS as
 * palavras sejam silabáveis em japonês — uma única palavra inglesa derruba a
 * detecção e a letra original iria ao LLM. Aqui a decisão é por EVIDÊNCIA:
 * partículas/palavras japonesas romanizadas inequívocas votam em "original",
 * palavras gramaticais inequívocas de inglês votam em "tradução", e o estilo
 * do evento (Romaji/JP vs English) decide antes de qualquer análise de texto.
 * Em caso de dúvida o viés é PRESERVAR — o mesmo princípio de todo o projeto:
 * deixar uma linha sem traduzir custa menos que destruir a letra original.
 */
@ApplicationScoped
public class ClassificadorLetraKaraokeService {

    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern ESCRITA_JAPONESA_PATTERN =
        Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    // "rom" cobre abreviações reais de fansub como o estilo "ED-ROM" (86).
    private static final Pattern ESTILO_JAPONES_ROMAJI_PATTERN = Pattern.compile(
        "(?i)\\b(rom|romaji|jp|jpn|japanese|japones|japon[eê]s|kana|kanji)\\b");
    private static final Pattern ESTILO_INGLES_PATTERN = Pattern.compile(
        "(?i)\\b(english|eng|en|translation|tradu[cç][aã]o)\\b");
    // Mesma decomposição silábica Hepburn do DetectorEfeitoKaraokeService.
    private static final Pattern PALAVRA_ROMAJI_PATTERN = Pattern.compile(
        "^(?:n|(?:([kgsztdnhbpmyrwfjv])\\1?|sh|ch|ts|ky|gy|ny|hy|my|ry|by|py)?[aeiou])+$");
    private static final Pattern ACENTO_PORTUGUES_PATTERN = Pattern.compile("[ãõçáéíóúâêôà]");

    /**
     * Palavras gramaticais de inglês que não colidem com romaji nem com PT-BR.
     * Uma camada de TRADUÇÃO em inglês sempre carrega várias delas; a letra
     * japonesa com inglês misturado usa palavras de conteúdo (heart, fly,
     * dream), quase nunca a gramática ("the", "of", "with"...).
     */
    private static final Set<String> INGLES_FORTE = Set.of(
        "the", "you", "your", "yours", "my", "mine", "we", "our", "ours", "it", "its",
        "is", "are", "was", "were", "be", "been", "being", "this", "that", "these", "those",
        "of", "and", "but", "or", "if", "in", "on", "at", "for", "with", "from", "by",
        "will", "would", "can", "could", "should", "shall", "must", "not", "don", "won",
        "what", "when", "where", "who", "whom", "how", "why", "all", "every", "never",
        "always", "there", "here", "have", "has", "had", "get", "got", "let", "just",
        "still", "only", "even", "into", "through", "without");

    /**
     * Partículas e palavras japonesas romanizadas inequívocas (não são palavras
     * de inglês nem de português). Duas ocorrências bastam para cravar letra
     * original mesmo com inglês misturado no meio.
     */
    private static final Set<String> ROMAJI_FORTE = Set.of(
        "wa", "ga", "wo", "ni", "mo", "ne", "yo", "ka", "kara", "made", "dake", "demo",
        "kedo", "koto", "mono", "desu", "masu", "nai", "shite", "suru", "kimi", "boku",
        "ore", "watashi", "anata", "minna", "itsumo", "zutto", "motto", "kitto",
        "kokoro", "yume", "sora", "hikari", "kaze", "hoshi", "namida", "sekai",
        "mirai", "ashita", "kyou", "ima", "koi", "inochi", "tsuki", "hana",
        "arigatou", "sayonara", "daijoubu", "issho", "futari", "hitori");

    /**
     * Palavras PT-BR frequentes em letras já traduzidas, para reconhecer pasta
     * reprocessada sem depender só de acentos (que já decidem sozinhos).
     */
    private static final Set<String> PORTUGUES_FORTE = Set.of(
        "voce", "nao", "meu", "minha", "seu", "sua", "por", "para", "mais", "sempre",
        "nunca", "quando", "quero", "posso", "vamos", "coracao", "mundo", "ceu",
        "amor", "sonho", "vida", "dentro", "longe", "ate", "sou", "estou");

    /** Fração mínima de palavras silabáveis em japonês no desempate final. */
    private static final double FRACAO_ROMAJI_DESEMPATE = 0.8;
    private static final int MINIMO_PALAVRAS_DESEMPATE = 3;

    private final DetectorEfeitoKaraokeService detector;

    public ClassificadorLetraKaraokeService(DetectorEfeitoKaraokeService detector) {
        this.detector = detector;
    }

    public ClasseLinhaKaraoke classificar(String estilo, String texto) {
        if (texto == null || texto.isBlank()) {
            return ClasseLinhaKaraoke.FORA_DE_MUSICA;
        }
        boolean indicaMusica = detector.eEstiloDeMusica(estilo)
            || detector.temTagKaraoke(texto)
            || detector.eEfeitoKaraoke(texto);
        if (!indicaMusica) {
            return ClasseLinhaKaraoke.FORA_DE_MUSICA;
        }
        // Sílaba/letra de KFX (cru ou pós-template): traduzir fragmento é
        // destruição garantida — o módulo Karaokê Simples é quem lida com isso.
        if (detector.temTagKaraoke(texto) || detector.eEfeitoKaraoke(texto)) {
            return ClasseLinhaKaraoke.EFEITO_KFX;
        }

        String visivel = extrairTextoVisivel(texto);
        if (visivel.isBlank()) {
            return ClasseLinhaKaraoke.EFEITO_KFX;
        }
        if (ESCRITA_JAPONESA_PATTERN.matcher(visivel).find()) {
            return ClasseLinhaKaraoke.ORIGINAL_JAPONES;
        }

        // O nome do estilo é a evidência mais confiável: fansubs rotulam as
        // camadas ("OP - Romaji" / "OP - English"). Ele decide antes do texto.
        if (estilo != null && ESTILO_JAPONES_ROMAJI_PATTERN.matcher(estilo).find()) {
            return ClasseLinhaKaraoke.ORIGINAL_JAPONES;
        }
        boolean estiloDizIngles = estilo != null && ESTILO_INGLES_PATTERN.matcher(estilo).find();

        String[] palavras = normalizar(visivel).split("[^a-z]+");
        if (pareceJaPortugues(visivel, palavras)) {
            return ClasseLinhaKaraoke.JA_PORTUGUES;
        }
        if (estiloDizIngles) {
            return ClasseLinhaKaraoke.TRADUZIVEL_INGLES;
        }
        return classificarPorEvidenciaDeTexto(palavras);
    }

    /**
     * Estilo ambíguo ("Song", "OP"...): decide contando evidências inequívocas
     * de cada idioma; empate cai na fração de palavras silabáveis em japonês.
     */
    private ClasseLinhaKaraoke classificarPorEvidenciaDeTexto(String[] palavras) {
        int sinaisIngles = 0;
        int sinaisRomaji = 0;
        int totalPalavras = 0;
        int palavrasSilabaveis = 0;
        for (String palavra : palavras) {
            if (palavra.isEmpty()) {
                continue;
            }
            totalPalavras++;
            if (INGLES_FORTE.contains(palavra)) {
                sinaisIngles++;
            } else if (ROMAJI_FORTE.contains(palavra)) {
                sinaisRomaji++;
            }
            if (PALAVRA_ROMAJI_PATTERN.matcher(palavra).matches()) {
                palavrasSilabaveis++;
            }
        }
        if (totalPalavras == 0) {
            return ClasseLinhaKaraoke.EFEITO_KFX;
        }
        if (sinaisRomaji > sinaisIngles) {
            return ClasseLinhaKaraoke.ORIGINAL_JAPONES;
        }
        if (sinaisIngles > sinaisRomaji) {
            return ClasseLinhaKaraoke.TRADUZIVEL_INGLES;
        }
        // Empate (0x0 ou 1x1): "One more time, one more chance" é letra
        // original em inglês silabável; frase inglesa comum tem encontros
        // consonantais que derrubam a fração.
        boolean parecerOriginal = totalPalavras >= MINIMO_PALAVRAS_DESEMPATE
            && (double) palavrasSilabaveis / totalPalavras >= FRACAO_ROMAJI_DESEMPATE;
        return parecerOriginal ? ClasseLinhaKaraoke.ORIGINAL_JAPONES : ClasseLinhaKaraoke.TRADUZIVEL_INGLES;
    }

    private boolean pareceJaPortugues(String visivel, String[] palavrasNormalizadas) {
        if (ACENTO_PORTUGUES_PATTERN.matcher(visivel.toLowerCase(Locale.ROOT)).find()) {
            return true;
        }
        int sinaisPt = 0;
        for (String palavra : palavrasNormalizadas) {
            if (PORTUGUES_FORTE.contains(palavra)) {
                sinaisPt++;
            }
        }
        return sinaisPt >= 2;
    }

    private static String normalizar(String visivel) {
        return visivel.toLowerCase(Locale.ROOT)
            .replace('ā', 'a').replace('ī', 'i').replace('ū', 'u')
            .replace('ē', 'e').replace('ō', 'o')
            .replace("'", "").replace("’", "");
    }

    static String extrairTextoVisivel(String texto) {
        if (texto == null) {
            return "";
        }
        return PADRAO_REMOVE_TAGS_ASS.matcher(texto)
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }
}
