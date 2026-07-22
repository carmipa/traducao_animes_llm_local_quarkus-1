package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: garante que a TRADUÇÃO não altere, invente ou apague um identificador
 * numérico da fonte. O valor semântico de um número é invariável: numa legenda ele é a
 * designação de uma unidade militar, uma altitude, uma frequência de rádio ou uma data, e
 * trocá-lo corrompe o sentido de forma indetectável pelo espectador. A corrida de 2026-07-22
 * produziu o caso que motivou esta classe: {@code "04th Team!"} foi publicado como
 * {@code "Equipe 08!"} — o número mudou em silêncio, sem qualquer rastro em log ou telemetria,
 * provavelmente por contaminação do contexto de lore da obra ({@code 08th MS Team}).
 *
 * <p>O LLM NÃO recebe autoridade para corrigir a fonte. Se a lore considerar que o fansub
 * errou, a correção tem de vir de regra determinística e versionada (o mapa de terminologia),
 * nunca de uma reescrita silenciosa do modelo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo identificador numérico presente no ORIGINAL deve continuar presente na tradução.</li>
 *   <li>A comparação é por VALOR, não por grafia: são aceitas as reescritas legítimas do
 *       português — separador de milhar ({@code 9500} → {@code 9.500}), vírgula decimal
 *       ({@code 0.5} → {@code 0,5}), queda do ordinal inglês ({@code 12th} → {@code 12}) e
 *       ordinal português ({@code 04th} → {@code 04ª}).</li>
 *   <li>Tags ASS/SSA e códigos de quebra são removidos antes da comparação: os números de
 *       {@code \pos(720,650)} são formatação, não conteúdo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * {@link #divergencia(String, String)} devolve uma mensagem legível nomeando os valores que
 * sumiram (a fala permanece pendente) ou {@code null} quando todos sobreviveram. Não lança.
 *
 * <p>LIMITE DECLARADO: a verificação cobre APENAS remoção e alteração. A INVENÇÃO de um número
 * que não existia no original é deliberadamente tolerada, porque a tradução legitimamente
 * numeraliza quantidades escritas por extenso ("eighteen" → "18") e reprovar isso reintroduziria
 * a classe de falso-positivo que esta fase eliminou. Na prática o caso perigoso já é coberto:
 * quando o modelo troca um número, o valor original desaparece e a troca é detectada por aí.
 */
@Component
public class VerificadorIdentificadorNumerico {

    private static final Pattern PADRAO_TAG = Pattern.compile("\\{[^{}]*}");
    /** Ordinal inglês colado ao número: {@code 12th}, {@code 1st}, {@code 04th}. */
    private static final Pattern ORDINAL_INGLES = Pattern.compile("(?i)(\\d)(st|nd|rd|th)");
    /** Ordinal/grau português colado ao número: {@code 4ª}, {@code 1º}, {@code 30°}. */
    private static final Pattern ORDINAL_PORTUGUES = Pattern.compile("(\\d)[ºª°]");

    /**
     * PROPÓSITO DE NEGÓCIO: aponta qual identificador numérico do original não sobreviveu à
     * tradução, para que a fala fique pendente com diagnóstico em vez de publicar um número
     * trocado.
     *
     * <p>INVARIANTES DO DOMÍNIO: compara CONJUNTOS de valores (não multiplicidade), de modo que
     * repetir ou desdobrar um número na tradução não reprova; só a ausência do valor reprova.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: argumento nulo devolve {@code null} (nada a verificar);
     * nunca lança.
     *
     * @param original fala de origem, ainda com tags ASS
     * @param traduzido tradução candidata
     * @return mensagem com os valores perdidos, ou {@code null} se todos sobreviveram
     */
    public String divergencia(String original, String traduzido) {
        if (original == null || traduzido == null) {
            return null;
        }
        Set<String> naFonte = valores(original);
        if (naFonte.isEmpty()) {
            return null;
        }
        Set<String> naTraducao = valores(traduzido);
        List<String> perdidos = new ArrayList<>();
        for (String valor : naFonte) {
            if (!naTraducao.contains(valor)) {
                perdidos.add(valor);
            }
        }
        if (perdidos.isEmpty()) {
            return null;
        }
        return "identificador numérico alterado ou removido pela tradução: original tem "
            + naFonte + ", tradução tem " + naTraducao + " (sumiu: " + perdidos + ")";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: confirma que UM identificador numérico específico do original
     * aparece na tradução — usado pela guarda do tradutor de máquina, que avalia token a token.
     *
     * <p>INVARIANTES DO DOMÍNIO: mesma normalização de {@link #divergencia}, então as duas
     * verificações nunca discordam sobre o que conta como o mesmo número.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: token sem dígito ou argumento nulo devolve
     * {@code false} (recusa segura). Não lança.
     */
    public boolean sobrevive(String token, String traduzido) {
        if (token == null || traduzido == null) {
            return false;
        }
        Set<String> doToken = valores(token);
        return !doToken.isEmpty() && valores(traduzido).containsAll(doToken);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: extrai os VALORES numéricos de um texto, já livres de formatação e
     * de grafia local, para que a comparação enxergue só o número.
     *
     * <p>INVARIANTES DO DOMÍNIO: remove tags e quebras; derruba sufixo ordinal inglês e
     * português; descarta o separador que fica ENTRE dígitos (milhar/decimal), preservando-o
     * como fronteira em qualquer outra posição — assim {@code 500} nunca é confundido com o
     * {@code 500} interno de {@code 12500}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto sem dígito devolve conjunto vazio; não lança.
     */
    private Set<String> valores(String texto) {
        String limpo = PADRAO_TAG.matcher(texto).replaceAll(" ")
            .replace("\\N", " ").replace("\\n", " ").replace("\\h", " ");
        limpo = ORDINAL_INGLES.matcher(limpo).replaceAll("$1");
        limpo = ORDINAL_PORTUGUES.matcher(limpo).replaceAll("$1");

        Set<String> encontrados = new LinkedHashSet<>();
        StringBuilder atual = new StringBuilder();
        for (int i = 0; i < limpo.length(); i++) {
            char c = limpo.charAt(i);
            if (Character.isDigit(c)) {
                atual.append(c);
                continue;
            }
            boolean separadorEntreDigitos = ehSeparador(c)
                && atual.length() > 0
                && i + 1 < limpo.length() && Character.isDigit(limpo.charAt(i + 1));
            if (separadorEntreDigitos) {
                continue; // 9.500 e 9500 são o mesmo valor
            }
            if (atual.length() > 0) {
                encontrados.add(atual.toString());
                atual.setLength(0);
            }
        }
        if (atual.length() > 0) {
            encontrados.add(atual.toString());
        }
        return encontrados;
    }

    private static boolean ehSeparador(char c) {
        // isSpaceChar cobre o espaço NÃO SEPARÁVEL (U+00A0), que isWhitespace ignora e que
        // aparece como separador de milhar em várias saídas de tradução.
        return c == '.' || c == ',' || Character.isWhitespace(c) || Character.isSpaceChar(c);
    }
}
