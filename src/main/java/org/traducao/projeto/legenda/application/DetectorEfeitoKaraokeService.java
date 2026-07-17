package org.traducao.projeto.legenda.application;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: reconhece se um evento .ass/.ssa é efeito de
 * karaokê/música (e não fala de diálogo), para que nenhuma fatia funcional
 * (tradução, revisão, correção) mexa em música — responsabilidade exclusiva do
 * fluxo de karaokê. É a regra única compartilhada, agora residente no peer
 * {@code legenda}, consumível por qualquer fatia sem acoplamento reverso.
 *
 * <p>Cobre as duas formas em que o karaokê aparece nos arquivos .ass:
 * <ul>
 *   <li>Karaokê "cru": tags de timing {@code \k}, {@code \kf}, {@code \ko}
 *       por sílaba, como sai do fansub antes de aplicar template.</li>
 *   <li>Saída do Kara Templater do Aegisub: as tags {@code \k} são consumidas
 *       na aplicação do template e viram uma linha por sílaba/letra com
 *       transformações animadas ({@code \t(...)}, {@code \frx}, {@code \fad},
 *       {@code \pos}) e quase nenhum texto visível.</li>
 * </ul>
 *
 * <p>INVARIANTES DO DOMÍNIO: distingue música de diálogo pela assinatura de tags
 * e pela densidade de texto visível; preserva letra em japonês/romaji (kana/kanji
 * ou estilo marcado como japonês) para nunca destruí-la, enquanto karaokê/música
 * em idiomas latinos com texto traduzível pode seguir para tradução. Em caso de
 * dúvida o viés é preservar: deixar uma linha de música sem traduzir custa menos
 * que corromper romaji. A classe é sem estado (stateless) e depende apenas de
 * JDK e Spring.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas ou em branco devolvem
 * {@code false} (não classificam o evento como música/karaokê) e nenhum método
 * lança — cada consulta é uma decisão booleana determinística sobre o
 * texto/estilo fornecido.
 */
@Service
public class DetectorEfeitoKaraokeService {

    // Tags de timing de karaoke ASS (\k, \kf, \ko, etc.)
    private static final Pattern TAG_KARAOKE_PATTERN = Pattern.compile("\\\\[kK][fo]?\\d");
    // Transformação animada \t(...): diálogo comum praticamente nunca usa;
    // templates de karaokê e letreiros animados sempre usam.
    private static final Pattern TAG_TRANSFORMACAO_PATTERN = Pattern.compile("\\\\t\\(");
    private static final Pattern TAG_POSICIONAMENTO_COMPLEXO_PATTERN = Pattern.compile("\\\\(pos|move|i?clip|org)\\(");
    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern ESCRITA_JAPONESA_PATTERN = Pattern.compile("[\\p{IsHiragana}\\p{IsKatakana}\\p{IsHan}]");
    private static final Pattern TEXTO_ALFANUMERICO_PATTERN = Pattern.compile("[\\p{L}\\d]");
    private static final Pattern ESTILO_MUSICA_PATTERN = Pattern.compile(
        "(?i)\\b(song|music|karaoke|opening|ending|theme|insert|op|ed|sing|lyrics?)\\b");
    private static final Pattern ESTILO_JAPONES_ROMAJI_PATTERN = Pattern.compile(
        "(?i)\\b(romaji|jp|jpn|japanese|japones|japon[eê]s|kana|kanji)\\b");
    // Palavra inteiramente decomponível em sílabas japonesas romanizadas
    // (Hepburn): "n" solto ou consoante opcional (com geminada kk/ss/tt/pp ou
    // dígrafo sh/ch/ts/ky/...) seguida de vogal. "fuminijirareru" casa;
    // "flor", "fly", "the" não casam (encontro consonantal/consoante final).
    private static final Pattern PALAVRA_ROMAJI_PATTERN = Pattern.compile(
        "^(?:n|(?:([kgsztdnhbpmyrwfjv])\\1?|sh|ch|ts|ky|gy|ny|hy|my|ry|by|py)?[aeiou])+$");
    private static final Pattern NAO_ASCII_PATTERN = Pattern.compile("[^\\x00-\\x7F]");
    private static final int MIN_CHARS_TAGS_POSICIONAMENTO_COMPLEXO = 45;

    /**
     * Karaokê cru: só as tags de timing {@code \k}. Usado onde ignorar demais
     * tem custo alto (tradução: letreiros/títulos com {@code \t} e texto curto
     * DEVEM ser traduzidos e têm a mesma assinatura do template de karaokê).
     */
    public boolean temTagKaraoke(String texto) {
        return texto != null && TAG_KARAOKE_PATTERN.matcher(texto).find();
    }

    /**
     * Estilo cujo NOME indica música (Opening, Ending, Song, Karaoke...).
     * Usado pelo módulo novoKaraoke para delimitar o que é bloco musical
     * mesmo quando o evento individual tem poucas tags (ex.: linha inteira
     * de tradução da letra com apenas {@code \pos}).
     */
    public boolean eEstiloDeMusica(String estilo) {
        return estilo != null && ESTILO_MUSICA_PATTERN.matcher(estilo).find();
    }

    /**
     * Karaokê em qualquer forma (cru ou pós-template). Usado nos fluxos de
     * revisão/correção, onde ignorar um letreiro já traduzido é inofensivo e o
     * risco real é mexer em música.
     */
    public boolean eEfeitoKaraoke(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        if (TAG_KARAOKE_PATTERN.matcher(texto).find()) {
            return true;
        }
        return eSaidaDeTemplateKaraoke(texto);
    }

    /**
     * Karaoke/música que deve ficar intacto: letra em japonês (kana/kanji) ou
     * estilos explicitamente marcados como japonês/romaji. Karaoke em inglês,
     * francês etc. não entra aqui e pode seguir para tradução/revisão.
     */
    public boolean devePreservarKaraokeOriginal(String estilo, String texto) {
        if (!temIndicadorDeMusica(estilo, texto)) {
            return false;
        }
        if (estilo != null && ESTILO_JAPONES_ROMAJI_PATTERN.matcher(estilo).find()) {
            return true;
        }
        String visivel = extrairTextoVisivel(texto);
        return ESCRITA_JAPONESA_PATTERN.matcher(visivel).find() || pareceLetraRomaji(visivel);
    }

    /**
     * Heurística determinística de romaji: todas as palavras (mínimo 2, com ao
     * menos 6 letras somadas) precisam se decompor em sílabas japonesas.
     * Fecha o buraco real do 86 T1: a linha de ED "fuminijirareru dake no
     * hana" (estilo "Opening", tags leves) passava pelos filtros de densidade
     * e chegava ao LLM, que a "traduzia" com alucinações diferentes a cada
     * frame. Letra ocidental escapa da heurística por encontros consonantais,
     * consoante final ou acentuação — em caso de dúvida o viés é preservar:
     * deixar uma linha de música sem traduzir custa menos que destruir romaji.
     */
    private boolean pareceLetraRomaji(String visivel) {
        String normalizado = visivel.toLowerCase()
            .replace('ā', 'a').replace('ī', 'i').replace('ū', 'u')
            .replace('ē', 'e').replace('ō', 'o')
            .replace("'", "");
        if (NAO_ASCII_PATTERN.matcher(normalizado).find()) {
            return false;
        }
        int palavras = 0;
        int letras = 0;
        for (String palavra : normalizado.split("[^a-z]+")) {
            if (palavra.isEmpty()) {
                continue;
            }
            if (!PALAVRA_ROMAJI_PATTERN.matcher(palavra).matches()) {
                return false;
            }
            palavras++;
            letras += palavra.length();
        }
        return palavras >= 2 && letras >= 6;
    }

    /**
     * Indica letra de música/karaokê com texto visível traduzível e sem sinal
     * claro de japonês/romaji original. Usado para não bloquear OP/ED em inglês
     * ou outros idiomas latinos só porque o estilo contém "song/karaoke".
     */
    public boolean eKaraokeOuMusicaTraduzivel(String estilo, String texto) {
        if (!temIndicadorDeMusica(estilo, texto) || devePreservarKaraokeOriginal(estilo, texto)) {
            return false;
        }
        return TEXTO_ALFANUMERICO_PATTERN.matcher(extrairTextoVisivel(texto)).find();
    }

    /**
     * Sílaba/letra de música pós-template: há transformação animada e o texto
     * visível é ínfimo perto do bloco de tags (ex.: {@code {\r\pos(369,23)
     * \t(1160,1450,\frx-50...)}I}). Uma fala real com efeito pontual tem
     * proporção inversa — mais texto do que tag.
     */
    private boolean eSaidaDeTemplateKaraoke(String texto) {
        boolean temTransformacao = TAG_TRANSFORMACAO_PATTERN.matcher(texto).find();
        boolean temPosicionamentoComplexo = TAG_POSICIONAMENTO_COMPLEXO_PATTERN.matcher(texto).find();
        if (!temTransformacao && !temPosicionamentoComplexo) {
            return false;
        }
        String visivel = PADRAO_REMOVE_TAGS_ASS.matcher(texto).replaceAll("")
            .replace("\\N", " ")
            .strip();
        boolean altaDensidadeTags = visivel.length() * 3 < texto.length();
        if (temTransformacao) {
            return altaDensidadeTags;
        }

        int tamanhoTags = texto.length() - visivel.length();
        return altaDensidadeTags && tamanhoTags >= MIN_CHARS_TAGS_POSICIONAMENTO_COMPLEXO;
    }

    private boolean temIndicadorDeMusica(String estilo, String texto) {
        return temTagKaraoke(texto)
            || (estilo != null && ESTILO_MUSICA_PATTERN.matcher(estilo).find());
    }

    private String extrairTextoVisivel(String texto) {
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
