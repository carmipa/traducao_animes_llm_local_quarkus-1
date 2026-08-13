package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.traducao.projeto.core.texto.FronteiraTermoAss;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: repõe acento usando o dicionário do sistema, alcançando o que nem o
 * dicionário escrito à mão nem a regra de terminação alcançam — {@code fatidico}, {@code minimo},
 * {@code aereo}, {@code psicicas}, medidos no Unicorn em 13/08/2026.
 *
 * <h2>A regra que torna isto seguro</h2>
 * O hunspell devolve várias sugestões, e aplicar a primeira seria temerário: para {@code colonia}
 * ele oferece {@code colônia}, mas também {@code colonial} e {@code colon}. Aqui só entra a
 * sugestão que, <b>removidos os acentos, é exatamente a palavra original</b>. Ou seja: a correção
 * só pode ACENTUAR, nunca trocar a palavra.
 *
 * <pre>
 *   fatidico -> fatídico   ACEITA   (sem acento: fatidico == fatidico)
 *   colonia  -> colônia    ACEITA   (sem acento: colonia  == colonia)
 *   colonia  -> colonial   RECUSA   (sem acento: colonial != colonia)
 *   suit     -> (nada)     RECUSA   (inglês; nenhuma sugestão é "suit" acentuado)
 * </pre>
 *
 * Com isso, palavra inexistente que não seja simples falta de acento passa intacta — resíduo de
 * inglês, nome de lore e termo técnico ficam onde estão, que é o comportamento desejado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Preserva a caixa do original: {@code Fatidico} vira {@code Fatídico}.</li>
 *   <li>Usa a fronteira do ASS — {@code \N} colado à palavra não impede a correção, e a quebra
 *       sobrevive.</li>
 *   <li>Não toca no que está dentro de {@code {...}}: tag de override não é texto humano.</li>
 *   <li>Dicionário indisponível devolve o texto BYTE A BYTE igual. Nunca inventa correção.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Texto nulo devolve nulo; sem dicionário, devolve o original. Nunca lança.
 */
public final class CorretorAcentoPorDicionario {

    /** Palavras candidatas: 4+ letras, para não gastar consulta com artigo e preposição. */
    private static final Pattern PALAVRA = Pattern.compile("\\p{L}{4,}");
    private static final Pattern TAG_ASS = Pattern.compile("\\{[^{}]*}");

    private final DicionarioOrtograficoPort dicionario;

    public CorretorAcentoPorDicionario(DicionarioOrtograficoPort dicionario) {
        this.dicionario = dicionario;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: colhe as palavras de um texto que valem uma consulta ao dicionário.
     *
     * <p>INVARIANTES DO DOMÍNIO: ignora o conteúdo das tags {@code {...}} e só devolve formas que
     * JÁ estão sem acento — palavra acentuada não precisa de correção e gastaria consulta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto nulo devolve conjunto vazio.
     */
    public static Set<String> candidatas(String texto) {
        if (texto == null || texto.isBlank()) {
            return Set.of();
        }
        String semTags = TAG_ASS.matcher(texto).replaceAll(" ");
        Set<String> fora = new LinkedHashSet<>();
        Matcher m = PALAVRA.matcher(semTags);
        while (m.find()) {
            String p = m.group();
            if (p.equals(semAcento(p))) {
                fora.add(p);
            }
        }
        return fora;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: dado o veredito do dicionário para as candidatas, monta o mapa
     * palavra-errada -> palavra-acentuada que pode ser aplicado com segurança.
     *
     * <p>INVARIANTES DO DOMÍNIO: só entra no mapa a sugestão cuja forma sem acento é idêntica à
     * palavra original. É o que impede {@code colonia} de virar {@code colonial}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada vazia devolve mapa vazio.
     *
     * @param sugestoesPorPalavra o que o dicionário devolveu, palavra -> sugestões na ordem
     */
    public static Map<String, String> apenasAcentuacoes(Map<String, Set<String>> sugestoesPorPalavra) {
        Map<String, String> seguras = new LinkedHashMap<>();
        if (sugestoesPorPalavra == null) {
            return seguras;
        }
        for (var e : sugestoesPorPalavra.entrySet()) {
            String errada = e.getKey();
            if (e.getValue() == null) {
                continue;
            }
            for (String sugestao : e.getValue()) {
                if (sugestao != null
                    && !sugestao.equals(errada)
                    && semAcento(sugestao).equalsIgnoreCase(semAcento(errada))
                    && !semAcento(sugestao).equals(sugestao)) {
                    seguras.put(errada, sugestao);
                    break;
                }
            }
        }
        return seguras;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica no texto as correções já julgadas seguras.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa a fronteira do ASS, então {@code \N} colado não esconde a
     * palavra; preserva a caixa do original; não entra em tag.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa vazio devolve o texto inalterado.
     */
    public static String aplicar(String texto, Map<String, String> correcoes) {
        if (texto == null || correcoes == null || correcoes.isEmpty()) {
            return texto;
        }
        String resultado = texto;
        for (var e : correcoes.entrySet()) {
            Pattern p = Pattern.compile(
                FronteiraTermoAss.INICIO + "(" + Pattern.quote(e.getKey()) + ")" + FronteiraTermoAss.FIM,
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            resultado = p.matcher(resultado).replaceAll(
                r -> Matcher.quoteReplacement(comCaixaDe(r.group(1), e.getValue())));
        }
        return resultado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: corrige um texto de ponta a ponta, consultando o dicionário.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma consulta por texto; sem dicionário disponível, devolve o
     * original byte a byte.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança.
     */
    public String corrigir(String texto) {
        Set<String> cands = candidatas(texto);
        if (cands.isEmpty()) {
            return texto;
        }
        Map<String, Set<String>> sugestoes = dicionario.sugestoes(cands);
        return aplicar(texto, apenasAcentuacoes(sugestoes));
    }

    /** Forma sem diacrítico, para comparar "é a mesma palavra?" sem depender do acento. */
    private static String semAcento(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static String comCaixaDe(String original, String substituta) {
        if (original.isEmpty()) {
            return substituta;
        }
        if (original.chars().allMatch(Character::isUpperCase) && original.length() > 1) {
            return substituta.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(substituta.charAt(0)) + substituta.substring(1);
        }
        return substituta;
    }
}


