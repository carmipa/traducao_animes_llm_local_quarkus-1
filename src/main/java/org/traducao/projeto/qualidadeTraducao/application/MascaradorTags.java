package org.traducao.projeto.qualidadeTraducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: protege a INTEGRIDADE da formatação da legenda ao redor da
 * tradução. Isola tags ASS/SSA (ex: {@code {\i1}}, {@code {\pos(...)}}) e códigos de
 * quebra ({@code \N}, {@code \n}, {@code \h}) do texto, trocando-os por marcadores
 * {@code [[TAGn]]} que o LLM é instruído a preservar literalmente — sem isso o modelo
 * tende a "traduzir" ou descartar as tags, corrompendo a legenda renderizada. Regra
 * de qualidade compartilhada, residente no peer {@code qualidadeTraducao}.
 *
 * <p>INVARIANTES DO DOMÍNIO: quantidade, conteúdo e ordem das tags do original são
 * preservados na desmascaração; cada tag vira exatamente um marcador {@code [[TAGn]]}
 * sequencial; a classe é sem estado (stateless), só JDK + Spring.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: se o texto retornado pelo LLM perdeu, duplicou
 * ou inventou marcadores, {@link #desmascarar(String, List)} lança
 * {@link AlucinacaoDetectadaException} em vez de gravar formatação corrompida;
 * verificações estruturais nulas/divergentes devolvem {@code false}.
 */
@Component
public class MascaradorTags {

    private static final Pattern PADRAO_TAG = Pattern.compile("\\{[^{}]*}|\\\\[Nnh]");
    private static final Pattern PADRAO_PLACEHOLDER = Pattern.compile("\\[\\[TAG(\\d+)]]");
    private static final Pattern PADRAO_MODO_DESENHO = Pattern.compile(
        "\\{[^}]*\\\\p[1-9]\\d*[^}]*}", Pattern.CASE_INSENSITIVE);
    /** Índice impossível para um marcador legítimo (0..n-1); sinaliza marcador corrompido. */
    private static final int INDICE_MARCADOR_INVALIDO = -1;

    /**
     * PROPÓSITO DE NEGÓCIO: transporta o resultado do mascaramento — o texto pronto
     * para o LLM (com marcadores no lugar das tags) e a lista ordenada das tags
     * originais para posterior restauração.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code tags} está na ordem exata em que os marcadores
     * {@code [[TAG0]]..[[TAGn]]} aparecem em {@code texto}; ambos são imutáveis do ponto
     * de vista do contrato (o registro não copia defensivamente a lista).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: é um portador de dados puro; não valida — a
     * coerência entre {@code texto} e {@code tags} é garantida por quem o cria
     * ({@link #mascarar(String)}).
     *
     * @param texto texto com as tags substituídas por marcadores {@code [[TAGn]]}
     * @param tags tags originais, na ordem dos marcadores
     */
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

    /**
     * PROPÓSITO DE NEGÓCIO: prepara uma fala para o LLM substituindo cada tag/quebra
     * estrutural por um marcador {@code [[TAGn]]}, de modo que o modelo traduza só o
     * texto visível sem tocar na formatação.
     *
     * <p>INVARIANTES DO DOMÍNIO: os marcadores são numerados sequencialmente a partir
     * de 0, na ordem de ocorrência; o texto fora das tags é preservado sem alteração.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: para um texto sem tags, devolve o próprio texto
     * e uma lista de tags vazia; não lança para entradas bem-formadas.
     */
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
     * PROPÓSITO DE NEGÓCIO: verifica, AINDA no texto mascarado, se a tradução do LLM
     * preservou exatamente os marcadores {@code [[TAGn]]} do original — usado dentro do
     * retry para rejeitar uma resposta com marcador corrompido ANTES do desmascaramento,
     * dando ao modelo nova chance (temperatura diferente) em vez de já manter o original.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara o multiconjunto de índices de marcadores do
     * original mascarado com o do traduzido mascarado; perder, duplicar ou inventar
     * qualquer marcador reprova. Não desmascara nem altera texto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer argumento nulo devolve {@code false}
     * (trata como não preservado, forçando nova tentativa/fallback).
     */
    public boolean marcadoresPreservados(String mascaradoOriginal, String mascaradoTraduzido) {
        if (mascaradoOriginal == null || mascaradoTraduzido == null) {
            return false;
        }
        return indicesMarcadores(mascaradoOriginal).equals(indicesMarcadores(mascaradoTraduzido));
    }

    private List<Integer> indicesMarcadores(String texto) {
        List<Integer> indices = new ArrayList<>();
        Matcher matcher = PADRAO_PLACEHOLDER.matcher(texto);
        while (matcher.find()) {
            indices.add(parseIndiceMarcador(matcher.group(1)));
        }
        indices.sort(null);
        return indices;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte os dígitos de um marcador {@code [[TAGn]]} em índice,
     * blindando o pipeline contra um índice absurdo alucinado pelo LLM
     * (ex.: {@code [[TAG9999999999999]]}).
     *
     * <p>INVARIANTES DO DOMÍNIO: um índice que não cabe em {@code int} é impossível para um
     * marcador legítimo (sempre {@code 0..n-1}); é tratado como corrompido.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: em vez de propagar {@link NumberFormatException}
     * (que escaparia dos catches de alucinação e abortaria o episódio/arquivo inteiro),
     * devolve {@link #INDICE_MARCADOR_INVALIDO}, fazendo a validação de conjunto reprovar e
     * lançar {@link AlucinacaoDetectadaException} — o caminho gracioso que mantém o original.
     */
    private static int parseIndiceMarcador(String digitos) {
        try {
            return Integer.parseInt(digitos);
        } catch (NumberFormatException e) {
            return INDICE_MARCADOR_INVALIDO;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reconstrói a fala traduzida devolvendo cada tag original
     * ao lugar do respectivo marcador {@code [[TAGn]]}, produzindo a legenda final com
     * formatação intacta.
     *
     * <p>INVARIANTES DO DOMÍNIO: o conjunto de marcadores presentes no texto traduzido
     * deve ser exatamente {@code {0..tags.size()-1}} — mesma quantidade e mesmos índices
     * do mascaramento; cada marcador é substituído pela tag de mesmo índice.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o LLM perdeu, duplicou ou inventou
     * marcadores, lança {@link AlucinacaoDetectadaException} em vez de gravar uma
     * legenda com formatação corrompida.
     */
    public String desmascarar(String textoTraduzidoMascarado, List<String> tags) {
        List<Integer> indicesEncontrados = new ArrayList<>();
        Matcher matcher = PADRAO_PLACEHOLDER.matcher(textoTraduzidoMascarado);
        while (matcher.find()) {
            indicesEncontrados.add(parseIndiceMarcador(matcher.group(1)));
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
