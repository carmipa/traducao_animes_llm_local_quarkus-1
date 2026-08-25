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

    /**
     * Palavras candidatas: 4+ letras, para não gastar consulta com artigo e preposição.
     *
     * <h2>NÃO baixe para 3, e a tentativa está documentada porque quase passou</h2>
     * Em 14/08/2026 a tradução do 86 saiu com 48 ocorrências de {@code sao} e 11 de {@code vao}
     * sem acento, e a causa parecia ser este filtro. A medição de risco olhou as 151 formas de
     * três letras do diálogo, viu que a regra de identidade de {@link #apenasAcentuacoes}
     * rejeitava as perigosas ({@code san->sã}, {@code six->sic}, {@code two->tão}) e concluiu que
     * três seria seguro.
     *
     * <p>A suíte derrubou: {@code mae} vira {@code mãe}, e {@code mae} é <b>前</b> em romaji. O
     * dano já é conhecido do projeto — 100 ocorrências nos 50 episódios do Unicorn. A medição
     * tinha olhado só o português; o acervo é bilíngue por natureza, e romaji de três letras é
     * comum ({@code mae}, {@code kai}, {@code yme}).
     *
     * <p>O conserto de {@code são} e {@code vão} foi feito onde não há esse risco: a lista
     * NOMINAL de {@code NormalizadorAcentosComuns}, que é a mesma casa de {@code nao} e
     * {@code voce} — e ambas dão zero ocorrência no acervo justamente por estarem lá.
     */
    private static final Pattern PALAVRA = Pattern.compile("\\p{L}{4,}");

    /**
     * As formas em que o {@code ão} chega destruído, medidas na saída do 86 em 2026-08-15:
     * {@code Esquadroao}, {@code Esquadroo}, {@code Esquadroe}, {@code Esquadroa}. A ordem
     * importa — {@code oao} vem primeiro para não ser cortado como {@code ao} pela alternativa
     * mais curta.
     */
    private static final Pattern TERMINACAO_QUEBRADA = Pattern.compile("(?:oao|oo|oe|oa)$");

    /** Piso do resultado: sem ele, uma sílaba de romaji como {@code paoa} viraria {@code pão}. */
    private static final int MINIMO_RESULTADO = 6;
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
            // TODAS as acentuacoes possiveis, nao a primeira. O `break` que estava aqui pegava a
            // que o hunspell devolvesse primeiro, e a ordem dele nao e uma opiniao sobre a frase:
            // em 24/08/2026 isso gravou `lingua -> lingua'` (com acento no A) no lugar de
            // `lingua` com acento no I, em duas falas do acervo.
            //
            // E a MESMA cicatriz do `avo -> avo'/avo^`, que ja tinha sido resolvida em outro
            // ponto do projeto exigindo candidata unica — e nunca foi aplicada aqui.
            java.util.List<String> acentuacoes = new java.util.ArrayList<>();
            for (String sugestao : e.getValue()) {
                if (sugestao != null
                    && !sugestao.equals(errada)
                    && semAcento(sugestao).equalsIgnoreCase(semAcento(errada))
                    && !semAcento(sugestao).equals(sugestao)) {
                    acentuacoes.add(sugestao);
                }
            }
            // DUAS acentuacoes validas e AMBIGUIDADE, e desempate aqui vira palavra inventada.
            // Recusar custa a correcao; escolher errado custa a legenda.
            if (acentuacoes.size() == 1) {
                seguras.put(errada, acentuacoes.get(0));
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
        Map<String, String> correcoes = new LinkedHashMap<>(apenasAcentuacoes(sugestoes));
        correcoes.putAll(reparosDeTerminacaoAo(dicionario, cands, correcoes.keySet()));
        return aplicar(texto, correcoes);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conserta o {@code ão} que o modelo destrói — {@code Esquadroo},
     * {@code Esquadroe}, {@code Esquadroa}, {@code Esquadroao} no lugar de {@code Esquadrão}.
     *
     * <h2>O prejuízo que originou</h2>
     * Medido na tradução do 86 Part 1 em 2026-08-15, depois de a lore ser unificada: o termo
     * {@code Spearhead} foi restaurado com sucesso, e mesmo assim a legenda saiu com
     * <i>"Como comandante do <b>Esquadroo</b> Spearhead"</i>. O cache prova que quem escreveu
     * assim foi o MODELO — e o mesmo modelo escreve {@code Esquadrão} corretamente em outras
     * falas. Dez ocorrências em onze episódios. Não é termo de lore: é palavra comum do
     * português, e por isso nenhum glossário deveria precisar conhecê-la.
     *
     * <h2>Por que não pedir a sugestão ao dicionário</h2>
     * Medido antes de escolher o desenho, e o resultado descarta a rota óbvia: o hunspell
     * detecta as quatro formas como erro, mas a sugestão CERTA não é a primeira em nenhuma
     * delas, e em três das quatro nem aparece na lista. Aplicar a primeira sugestão produziria
     * {@code Esquadro-o} e {@code Esquadro a} — pior que o defeito.
     *
     * <h2>Invariantes do domínio</h2>
     * <ul>
     *   <li><b>O dicionário é o VALIDADOR nas duas pontas.</b> Só entra palavra que ele
     *       REJEITA, e só sai troca que ele ACEITA. A regra não tem como inventar palavra.</li>
     *   <li>Não toca no que a passada de acento já resolveu — aquela é mais específica.</li>
     *   <li>Piso de {@value #MINIMO_RESULTADO} letras no resultado: sem ele, uma sílaba de
     *       romaji como {@code paoa} viraria {@code pão}. O piso mantém a regra no território
     *       de palavra longa, que é onde o defeito foi medido ({@code Esquadr} + {@code ão}).</li>
     * </ul>
     *
     * <h2>Comportamento em caso de falha</h2>
     * Dicionário indisponível devolve mapa vazio — a passada inteira é no-op e o texto sai
     * intacto, nunca corrompido por palpite.
     *
     * @param candidatas palavras extraídas do texto
     * @param jaCorrigidas palavras que a passada de acentuação já resolveu
     * @return de-para apenas das trocas que o dicionário aceitou
     */
    public static Map<String, String> reparosDeTerminacaoAo(
            DicionarioOrtograficoPort dicionario, Set<String> candidatas, Set<String> jaCorrigidas) {
        Set<String> suspeitas = new LinkedHashSet<>();
        for (String palavra : candidatas) {
            if (!jaCorrigidas.contains(palavra) && TERMINACAO_QUEBRADA.matcher(palavra).find()) {
                suspeitas.add(palavra);
            }
        }
        if (suspeitas.isEmpty()) {
            return Map.of();
        }
        // Só as que o dicionário NÃO conhece: palavra válida terminada em -oa/-oe/-oo
        // ("pessoa", "canoa", "heroe" não; mas "lagoa" sim) jamais é tocada.
        Set<String> desconhecidas = dicionario.desconhecidas(suspeitas);
        Map<String, String> propostas = new LinkedHashMap<>();
        for (String errada : desconhecidas) {
            Matcher m = TERMINACAO_QUEBRADA.matcher(errada);
            if (!m.find()) {
                continue;
            }
            String proposta = errada.substring(0, m.start()) + "ão";
            if (proposta.length() >= MINIMO_RESULTADO) {
                propostas.put(errada, proposta);
            }
        }
        if (propostas.isEmpty()) {
            return Map.of();
        }
        // A segunda trava: o resultado precisa EXISTIR. "Npessoa" -> "Npessão" é recusado aqui,
        // e foi o único falso positivo que a medição do 86 encontrou.
        Set<String> invalidas = dicionario.desconhecidas(new LinkedHashSet<>(propostas.values()));
        Map<String, String> aceitas = new LinkedHashMap<>();
        propostas.forEach((errada, proposta) -> {
            if (!invalidas.contains(proposta)) {
                aceitas.put(errada, proposta);
            }
        });
        return aceitas;
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


