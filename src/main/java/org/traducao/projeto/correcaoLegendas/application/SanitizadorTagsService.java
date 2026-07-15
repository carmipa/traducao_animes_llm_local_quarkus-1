package org.traducao.projeto.correcaoLegendas.application;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SanitizadorTagsService {

    // LLM costuma alucinar chaves {texto} como marcação de pensamento, o que quebra a linha no Aegisub.
    // Início válido de bloco ASS: "\" (override), "=" (marcador do Kara Templater)
    // ou "*" (loop do Kara Templater, ex.: {*\c&H24249D&} — visto no Gundam 0083).
    private static final Pattern TAG_INVALIDA_PATTERN = Pattern.compile("\\{([^\\\\=*}][^}]*)\\}");

    // Tags de timing de karaoke ASS: \k, \K, \kf, \ko seguidas de duração (centissegundos).
    private static final Pattern TAG_KARAOKE_PATTERN = Pattern.compile("\\\\[kK][fo]?\\d");

    public String curarTags(String originalEn, String traduzidoPt) {
        if (originalEn == null || traduzidoPt == null) {
            return traduzidoPt;
        }

        String resultado = traduzidoPt;

        // Legado: LLM (ou versões antigas deste código) corrompiam a tag do Kara Templater
        // "{=X}" para "\N=X".
        resultado = resultado.replaceAll("\\\\N=(\\d+)\\{", "{=$1}{");
        resultado = resultado.replaceAll("\\\\N=(\\d+)", "{=$1}");

        // Formatação de tela (pos, cor, an8 etc.) sempre fica no prefixo {...} do início da linha.
        // Forçamos a tradução a ter exatamente o mesmo prefixo do original — inclusive quando o
        // original não tem prefixo nenhum, caso em que qualquer {...} que apareça na tradução é
        // alucinação do LLM e precisa ser descartado, não preservado.
        String prefixoOriginal = extrairPrefixo(originalEn);
        String textoTraduzidoSemPrefixo = removerPrefixo(resultado);
        resultado = prefixoOriginal + textoTraduzidoSemPrefixo;

        resultado = escaparChavesInvalidas(resultado, originalEn);

        // Nao remover espaco em branco apos "}" aqui: falas de karaoke (OP/ED) tem
        // tags validas no meio da linha, uma por silaba/palavra (ex.: "{\k50}Ka {\k30}ra"),
        // e um regex global de limpeza de espaco pos-"}" gruda as palavras entre si.
        // O prefixo (unica parte onde sobrava espaco) ja sai sem espaco por causa do
        // trim() em removerPrefixo(...) acima.

        return resultado;
    }

    /**
     * Converte chaves {texto} que não são tags válidas do ASS em quebra de
     * linha + texto ({@code \Ntexto}), preservando o conteúdo em vez de
     * apagá-lo — em geral são alucinação do LLM. Exceção: se o MESMO bloco
     * {...} existe verbatim no original, é um comentário legítimo do arquivo
     * (nota de fansub, marcador do Kara Templater) e é mantido intacto —
     * escapá-lo tornaria visível na tela um texto que o autor deixou oculto.
     * <p>
     * Também usado pela revisão de legendas (raspagemRevisao) para consertar
     * karaokê quebrado — regra única, sem regex duplicada entre módulos.
     */
    public String escaparChavesInvalidas(String texto, String originalReferencia) {
        if (texto == null) {
            return null;
        }
        Matcher matcher = TAG_INVALIDA_PATTERN.matcher(texto);
        if (!matcher.find()) {
            return texto;
        }
        StringBuilder sb = new StringBuilder();
        matcher.reset();
        while (matcher.find()) {
            String bloco = matcher.group();
            boolean comentarioLegitimo = originalReferencia != null && originalReferencia.contains(bloco);
            String substituto = comentarioLegitimo ? bloco : "\\N" + matcher.group(1);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(substituto));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Conta as tags de timing de karaokê (\k, \K, \kf, \ko) da fala. Serve
     * para detectar traduções antigas que perderam o timing por sílaba no
     * meio da linha — a cura estrutural só restaura o prefixo, então a perda
     * precisa ao menos ser sinalizada no relatório para revisão manual.
     */
    public int contarTagsKaraoke(String texto) {
        if (texto == null) {
            return 0;
        }
        Matcher matcher = TAG_KARAOKE_PATTERN.matcher(texto);
        int contagem = 0;
        while (matcher.find()) {
            contagem++;
        }
        return contagem;
    }

    private String extrairPrefixo(String texto) {
        StringBuilder prefixo = new StringBuilder();
        int pos = 0;

        while (pos < texto.length()) {
            if (Character.isWhitespace(texto.charAt(pos))) {
                pos++;
                continue;
            }

            if (texto.charAt(pos) == '{') {
                int fechamento = texto.indexOf('}', pos);
                if (fechamento != -1 && ehTagAssValida(texto, pos, fechamento)) {
                    prefixo.append(texto.substring(pos, fechamento + 1));
                    pos = fechamento + 1;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return prefixo.toString();
    }

    private String removerPrefixo(String texto) {
        int pos = 0;
        while (pos < texto.length()) {
            if (Character.isWhitespace(texto.charAt(pos))) {
                pos++;
                continue;
            }
            
            if (texto.charAt(pos) == '{') {
                int fechamento = texto.indexOf('}', pos);
                if (fechamento != -1 && ehTagAssValida(texto, pos, fechamento)) {
                    pos = fechamento + 1;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        return texto.substring(pos).trim();
    }

    private boolean ehTagAssValida(String texto, int abertura, int fechamento) {
        if (fechamento <= abertura + 1) {
            return false;
        }
        char primeiroConteudo = texto.charAt(abertura + 1);
        return primeiroConteudo == '\\' || primeiroConteudo == '=' || primeiroConteudo == '*';
    }
}
