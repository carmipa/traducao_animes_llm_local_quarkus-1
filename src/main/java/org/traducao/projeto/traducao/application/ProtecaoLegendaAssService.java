package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Blindagens compartilhadas para linhas ASS/SSA antes e depois de chamadas a
 * IA/serviços externos. Centraliza os casos perigosos encontrados em fansubs:
 * clips vetoriais longos, letras soltas pós-template e preâmbulos alucinados.
 */
@Service
public class ProtecaoLegendaAssService {

    private static final Pattern PADRAO_DESENHO_VETORIAL = Pattern.compile("\\\\p[1-9]\\d*");
    private static final Pattern PADRAO_REMOVE_TAGS_ASS = Pattern.compile("\\{[^}]+}");
    private static final Pattern PADRAO_TAG_ASS_PESADA = Pattern.compile(
        "\\\\(?:t\\(|pos\\(|move\\(|i?clip\\(|org\\(|fad\\(|fr[xyz]|blur|bord|[13]c&)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PADRAO_CLIP_LONGO = Pattern.compile(
        "\\\\i?clip\\([^)]{300,}\\)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern PADRAO_ESTILO_TECNICO = Pattern.compile(
        "(?i)\\b(signs?|title|ep\\s*title|next\\s*ep|opening|ending|op|ed|song|karaoke|lyrics?|credits?)\\b"
    );
    private static final Pattern PADRAO_CAMINHO_TRADUZIDO = Pattern.compile(
        "(?i)(?:legendas[_-]?ptbr|traducao[_-]?ptbr|traduzidas|revisao|_pt-?br\\b|\\bpt-?br_)"
    );
    private static final Pattern PADRAO_PREAMBULO_LLM = Pattern.compile(
        "^(?:sa[ií]da|resposta|tradu[çc][ãa]o|translation|output)\\s*:",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final int LIMIAR_TEXTO_CURTO_EFEITO = 8;

    public boolean temDesenhoVetorial(String texto) {
        return texto != null && PADRAO_DESENHO_VETORIAL.matcher(texto).find();
    }

    public boolean deveIgnorarIntervencaoIa(String estilo, String texto) {
        return deveBloquearAntesDoLlm(estilo, texto, 2);
    }

    public boolean respostaSuspeita(String original, String traduzido) {
        return respostaAssPesadaSuspeita(original, traduzido);
    }

    public boolean deveBloquearLinhaAntesDoLlm(String estilo, String texto, long repeticoesTextoVisivel) {
        return deveBloquearAntesDoLlm(estilo, texto, repeticoesTextoVisivel);
    }

    public String textoVisivel(String texto) {
        return extrairTextoVisivelAss(texto);
    }

    public boolean caminhoPareceTraduzido(Path arquivoEntrada) {
        return caminhoPareceLegendaTraduzida(arquivoEntrada);
    }

    static boolean respostaAssPesadaSuspeita(String original, String traduzido) {
        if (original == null || traduzido == null) {
            return false;
        }
        String visivelOriginal = extrairTextoVisivelAss(original);
        String visivelTraduzido = extrairTextoVisivelAss(traduzido);
        if (visivelTraduzido.isBlank()) {
            return false;
        }

        boolean originalComTagsPesadas = PADRAO_TAG_ASS_PESADA.matcher(original).find();
        boolean altaDensidadeTags = original.length() > 40
            && Math.max(1, visivelOriginal.length()) * 3 < original.length();
        boolean textoOriginalCurto = visivelOriginal.length() <= LIMIAR_TEXTO_CURTO_EFEITO;
        if (!originalComTagsPesadas || !altaDensidadeTags || !textoOriginalCurto) {
            return false;
        }

        if (PADRAO_PREAMBULO_LLM.matcher(visivelTraduzido).find()) {
            return true;
        }
        if (visivelOriginal.length() <= 3 && visivelTraduzido.length() > visivelOriginal.length() + 8) {
            return true;
        }
        return visivelTraduzido.length() > Math.max(20, visivelOriginal.length() * 3);
    }

    static String extrairTextoVisivelAss(String texto) {
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

    static boolean deveBloquearAntesDoLlm(String estilo, String texto, long repeticoesTextoVisivel) {
        if (texto == null) {
            return false;
        }
        String visivel = extrairTextoVisivelAss(texto);
        if (visivel.isEmpty()) {
            return true;
        }

        boolean temTagsPesadas = PADRAO_TAG_ASS_PESADA.matcher(texto).find();
        if (!temTagsPesadas) {
            return false;
        }

        boolean altaDensidadeTags = texto.length() > 40
            && Math.max(1, visivel.length()) * 3 < texto.length();
        if (!altaDensidadeTags) {
            return false;
        }

        if (visivel.length() <= 3) {
            return true;
        }

        boolean clipLongo = PADRAO_CLIP_LONGO.matcher(texto).find();
        boolean estiloTecnico = estilo != null && PADRAO_ESTILO_TECNICO.matcher(estilo).find();
        return clipLongo && visivel.length() <= 30 && (repeticoesTextoVisivel >= 2 || estiloTecnico);
    }

    static boolean caminhoPareceLegendaTraduzida(Path arquivoEntrada) {
        if (arquivoEntrada == null) {
            return false;
        }
        String normalizado = arquivoEntrada.toString()
            .replace('\\', '/')
            .replace(' ', '_');
        return PADRAO_CAMINHO_TRADUZIDO.matcher(normalizado).find();
    }
}
