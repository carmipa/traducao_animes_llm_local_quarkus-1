package org.traducao.projeto.traducaoKaraoke.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: garante que uma letra de VÁRIOS versos (separados por {@code \N} no ASS)
 * volte do LLM com TODOS os versos — nunca só o primeiro.
 *
 * <h2>O prejuízo que originou, medido em 24/09/2026</h2>
 * O adaptador do LLM devolve UMA entrada por linha da resposta, e o karaokê ficava com
 * {@code getFirst()} sem conferir quantas vieram. O aya transforma o {@code \N} do verso em quebra
 * de linha REAL (saída crua: {@code "Nós caminhamos...tempo.\nVocê nunca sabe o amor..."}), então o
 * segundo verso era descartado em silêncio — e às vezes sobrava um {@code \} solto, que a legenda
 * desenha literalmente na tela. No cache real do karaokê, antes das retraduções: 9 letras
 * distintas de 2+ versos, com 14 pares original→tradução, 7 deles truncados (em 6 das 9 letras) —
 * {@code "Have a little break\NWe're running through the lights, out of breath"} virou
 * só "Faça uma pequena pausa" em 12 episódios do Unicorn, e {@code "God, what a judgement\NIs my
 * punishment"} virou "Deus, que julgamento! \" em 8.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Uma linha de resposta é aceita como está (o modelo pode fundir os versos numa frase).</li>
 *   <li>N linhas só são aceitas quando N é EXATAMENTE o número de versos enviados — viram versos
 *       unidos por {@code \N}, a mesma estrutura do original. Outra contagem é resposta que não
 *       corresponde à letra (comentário, verso inventado ou perdido) e é RECUSADA: a linha fica no
 *       idioma original, que é o lado que não destrói nada.</li>
 *   <li>A barra invertida solta no fim de um verso é o resto do {@code \N} que o modelo tentou
 *       escrever. Nunca é texto legítimo de legenda e é removida.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entrada nula ou sem linha utilizável devolve {@link Optional#empty()}. Nunca lança.
 */
public final class VersosDaLetra {

    private static final String QUEBRA = "\\N";
    private static final Pattern TAGS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern BARRA_SOLTA_NO_FIM = Pattern.compile("\\s*\\\\+\\s*$");

    /**
     * Fração mínima do tamanho do original que uma tradução SEM quebra precisa ter para não ser
     * suspeita de verso perdido. Medido no cache real do karaokê em 24/09/2026 (unidade: PAR
     * original→tradução distinto): 14 pares de 9 letras de 2+ versos; os 7 truncados ficaram entre
     * 0,33 e 0,73 do tamanho do original, e as 7 fusões legítimas dos versos numa frase entre 1,00
     * e 1,23. (Uma primeira versão deste texto chamou os 14 PARES de "letras" — corrigido pela
     * revisão adversarial.)
     *
     * <p>LIMITAÇÃO DECLARADA (A8): o limiar foi escolhido olhando esse MESMO conjunto — não havia
     * amostra reservada. Por isso a suspeita nunca apaga nada: só faz retraduzir, e o custo de um
     * falso positivo é uma chamada ao LLM.
     */
    static final double FRACAO_MINIMA_SEM_QUEBRA = 0.85;

    private VersosDaLetra() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: quantos versos a letra tem na tela — segmentos com texto separados
     * por {@code \N}, depois de tirar as tags.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo ou em branco conta 0.
     */
    public static int contar(String texto) {
        if (texto == null) {
            return 0;
        }
        int versos = 0;
        for (String parte : TAGS.matcher(texto).replaceAll("").split(Pattern.quote(QUEBRA), -1)) {
            if (!parte.isBlank()) {
                versos++;
            }
        }
        return versos;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transforma as linhas que o LLM devolveu no texto de UMA letra, sem
     * perder verso.
     *
     * <p>INVARIANTES DO DOMÍNIO: ver a classe — 1 linha passa; N linhas só com N == versos
     * enviados, unidas por {@code \N}; qualquer outra contagem é recusada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista nula/vazia, só linhas em branco ou contagem que não
     * bate devolvem {@link Optional#empty()} — o chamador mantém a letra original.
     */
    public static Optional<String> unirResposta(List<String> linhas, int versosEnviados) {
        if (linhas == null) {
            return Optional.empty();
        }
        List<String> uteis = new ArrayList<>();
        for (String linha : linhas) {
            String limpa = semBarraSolta(linha);
            if (!limpa.isBlank()) {
                uteis.add(limpa);
            }
        }
        if (uteis.isEmpty()) {
            return Optional.empty();
        }
        if (uteis.size() == 1) {
            return Optional.of(uteis.getFirst());
        }
        if (uteis.size() != versosEnviados) {
            return Optional.empty();
        }
        return Optional.of(String.join(QUEBRA, uteis));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tira do fim do texto a barra invertida solta — o resto de um
     * {@code \N} que o modelo começou e não terminou. A legenda a desenharia literalmente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nulo vira vazio.
     */
    public static String semBarraSolta(String texto) {
        if (texto == null) {
            return "";
        }
        return BARRA_SOLTA_NO_FIM.matcher(texto.strip()).replaceAll("").strip();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: diz se uma tradução JÁ GRAVADA no cache tem a marca do defeito do
     * verso perdido, para ser retraduzida em vez de reaproveitada para sempre.
     *
     * <p>INVARIANTES DO DOMÍNIO: exige as DUAS marcas juntas — original com 2+ versos E tradução
     * sem quebra que termina em barra solta ou tem menos de {@link #FRACAO_MINIMA_SEM_QUEBRA} do
     * tamanho visível do original. Tradução com {@code \N} nunca é suspeita. Suspeita não apaga
     * nada: o chamador só deixa de reaproveitar e manda ao LLM de novo (regra A8).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer nulo devolve {@code false}.
     */
    public static boolean suspeitaDeVersoPerdido(String original, String traduzido) {
        if (original == null || traduzido == null || contar(original) < 2) {
            return false;
        }
        String visivelTraduzido = TAGS.matcher(traduzido).replaceAll("").strip();
        if (visivelTraduzido.contains(QUEBRA)) {
            return false;
        }
        if (BARRA_SOLTA_NO_FIM.matcher(visivelTraduzido).find()) {
            return true;
        }
        String visivelOriginal = TAGS.matcher(original).replaceAll("").replace(QUEBRA, " ").strip();
        return visivelTraduzido.length() < FRACAO_MINIMA_SEM_QUEBRA * visivelOriginal.length();
    }
}
