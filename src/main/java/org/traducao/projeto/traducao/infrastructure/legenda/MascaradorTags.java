package org.traducao.projeto.traducao.infrastructure.legenda;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.exceptions.AlucinacaoDetectadaException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Isola tags de formatação ASS/SSA (ex: {\i1}, {\pos(...)}) e códigos de quebra
 * (\N, \n, \h) do texto antes de enviar ao LLM, trocando-os por marcadores
 * [[TAGn]] que o modelo é instruído a preservar literalmente. Sem isso o LLM
 * tende a "traduzir" ou descartar as tags, corrompendo a legenda renderizada.
 */
@Component
public class MascaradorTags {

    private static final Pattern PADRAO_TAG = Pattern.compile("\\{[^{}]*}|\\\\[Nnh]");
    private static final Pattern PADRAO_PLACEHOLDER = Pattern.compile("\\[\\[TAG(\\d+)]]");
    private static final Pattern PADRAO_MODO_DESENHO = Pattern.compile(
        "\\{[^}]*\\\\p[1-9]\\d*[^}]*}", Pattern.CASE_INSENSITIVE);

    public record Mascarado(String texto, List<String> tags) {}

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que uma tradução recuperada do cache ainda
     * preserva todas as tags ASS/SSA e quebras de linha estruturais do original.
     *
     * <p>INVARIANTES DO DOMÍNIO: quantidade, conteúdo e ordem das tags devem ser
     * idênticos; somente o texto visível pode mudar durante a tradução.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve {@code false} para argumentos
     * nulos ou qualquer divergência estrutural, fazendo o chamador invalidar e
     * retraduzir a entrada em vez de publicar formatação corrompida.
     */
    public boolean preservaEstruturaOriginal(String original, String traduzido) {
        if (original == null || traduzido == null) {
            return false;
        }
        return mascarar(original).tags().equals(mascarar(traduzido).tags());
    }

    public Mascarado mascarar(String textoOriginal) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = PADRAO_TAG.matcher(textoOriginal);
        StringBuilder resultado = new StringBuilder();
        int ultimoFim = 0;
        while (matcher.find()) {
            resultado.append(textoOriginal, ultimoFim, matcher.start());
            resultado.append("[[TAG").append(tags.size()).append("]]");
            tags.add(matcher.group());
            ultimoFim = matcher.end();
        }
        resultado.append(textoOriginal, ultimoFim, textoOriginal.length());
        return new Mascarado(resultado.toString(), tags);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se um evento ASS contém texto humano que
     * pode ser enviado à tradução/revisão, excluindo desenhos e typesetting puro.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code \\p1} dentro de qualquer bloco de tags
     * ativa desenho vetorial; letras dos comandos {@code m/l/b} não contam como
     * idioma natural.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo, vazio ou apenas estrutural
     * devolve {@code false} e permanece intocado no arquivo.
     */
    public boolean contemTextoTraduzivel(String textoOriginal) {
        if (textoOriginal == null || textoOriginal.isBlank()) return false;
        
        // Ignora linhas que ativam desenhos vetoriais (ex: {\p1}m 0 0 l ...)
        if (PADRAO_MODO_DESENHO.matcher(textoOriginal).find()) {
            return false;
        }
        
        String semTags = PADRAO_TAG.matcher(textoOriginal).replaceAll("");
        if (isTypesettingComClipPesado(textoOriginal, semTags)) {
            return false;
        }

        String apenasLetrasOuNumeros = semTags.replaceAll("[^\\p{L}\\d]", "");
        
        return !apenasLetrasOuNumeros.isEmpty();
    }

    private boolean isTypesettingComClipPesado(String textoOriginal, String semTags) {
        String lower = textoOriginal.toLowerCase();
        boolean temClip = lower.contains("\\clip(") || lower.contains("\\iclip(");
        if (!temClip) {
            return false;
        }

        int tamanhoTags = textoOriginal.length() - semTags.length();
        return tamanhoTags > 200 && semTags.trim().length() <= 60;
    }

    /**
     * Restaura as tags originais nos marcadores [[TAGn]]. Se o LLM perdeu,
     * duplicou ou inventou marcadores, falha alto em vez de gravar uma
     * legenda com formatação corrompida.
     */
    public String desmascarar(String textoTraduzidoMascarado, List<String> tags) {
        List<Integer> indicesEncontrados = new ArrayList<>();
        Matcher matcher = PADRAO_PLACEHOLDER.matcher(textoTraduzidoMascarado);
        while (matcher.find()) {
            indicesEncontrados.add(Integer.parseInt(matcher.group(1)));
        }

        Set<Integer> esperados = new HashSet<>();
        for (int i = 0; i < tags.size(); i++) {
            esperados.add(i);
        }

        if (indicesEncontrados.size() != tags.size() || !new HashSet<>(indicesEncontrados).equals(esperados)) {
            throw new AlucinacaoDetectadaException(
                "Marcadores de formatacao ([[TAGn]]) corrompidos ou perdidos pelo LLM. Esperado "
                    + tags.size() + " marcador(es) " + esperados + ", recebido: " + textoTraduzidoMascarado);
        }

        StringBuilder resultado = new StringBuilder();
        matcher.reset();
        int ultimoFim = 0;
        while (matcher.find()) {
            resultado.append(textoTraduzidoMascarado, ultimoFim, matcher.start());
            resultado.append(tags.get(Integer.parseInt(matcher.group(1))));
            ultimoFim = matcher.end();
        }
        resultado.append(textoTraduzidoMascarado, ultimoFim, textoTraduzidoMascarado.length());
        return resultado.toString();
    }
}
