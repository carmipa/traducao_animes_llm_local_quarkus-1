package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: por fidelidade à fonte, remove da legenda traduzida as aspas que
 * ENVOLVEM a fala inteira quando o original (EN) não as tinha. O LLM às vezes adiciona
 * {@code "..."} em volta de um diálogo que a fonte não citava (visto no 08th: {@code Of course!}
 * → {@code "Claro!"}); essas aspas espúrias poluem a legenda. Este serviço as retira sem tocar
 * em nada mais — a fonte é a autoridade sobre o que está entre aspas.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só remove quando o texto VISÍVEL do traduzido (fora as tags {@code {...}} de borda) está
 *       envolto por um ÚNICO par de aspas de borda e o original NÃO está envolto por aspas —
 *       se a fonte já citava, as aspas são legítimas e ficam.</li>
 *   <li>Não remove quando há mais de um par (ex.: {@code "A" e "B"}) — não é um envelope; aspas
 *       simples {@code '...'} internas são preservadas.</li>
 *   <li>Tags de estilo de borda ({@code {\i1}}...{@code {\i0}}) são preservadas; só o par de
 *       aspas ao redor do texto sai. Classe sem estado; só JDK + Spring.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Traduzido {@code null}/vazio, sem aspas de borda, ou com a fonte já citada é devolvido intacto;
 * nunca lança.
 */
@Component
public class NormalizadorAspasService {

    private static final Pattern TAGS_PREFIXO = Pattern.compile("^(?:\\{[^}]*})+");
    private static final Pattern TAGS_SUFIXO = Pattern.compile("(?:\\{[^}]*})+$");

    /**
     * PROPÓSITO DE NEGÓCIO: devolve o traduzido sem as aspas de borda espúrias.
     * <p>INVARIANTES DO DOMÍNIO: descrito na classe — só age se o PT está envolto e o EN não.
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada intocável devolve o próprio traduzido.
     */
    public String normalizar(String original, String traduzido) {
        if (traduzido == null || traduzido.isBlank()) {
            return traduzido;
        }
        String prefixo = extrair(TAGS_PREFIXO, traduzido);
        String sufixo = extrair(TAGS_SUFIXO, traduzido);
        if (prefixo.length() + sufixo.length() >= traduzido.length()) {
            return traduzido; // só tags, sem texto visível
        }
        String meio = traduzido.substring(prefixo.length(), traduzido.length() - sufixo.length()).strip();
        if (!envolvidoPorAspas(meio)) {
            return traduzido; // PT não está envolto por um par de aspas de borda
        }
        if (original != null && envolvidoPorAspas(textoVisivel(original).strip())) {
            return traduzido; // a FONTE já citava: aspas legítimas, não remove
        }
        String semAspas = meio.substring(1, meio.length() - 1).strip();
        return prefixo + semAspas + sufixo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: decide se o texto está envolto por um ÚNICO par de aspas de borda
     * (retas {@code "..."} ou curvas {@code “...”}) — o envelope que o LLM adiciona indevidamente.
     * <p>INVARIANTES DO DOMÍNIO: retas exigem exatamente 2 aspas duplas (só o par de borda, sem
     * duplas internas → não confunde {@code "A" e "B"}); curvas exigem uma de abertura e uma de
     * fechamento. Aspas simples {@code '...'} não contam como envelope.
     * <p>COMPORTAMENTO EM CASO DE FALHA: texto com menos de 2 caracteres devolve {@code false}.
     */
    private boolean envolvidoPorAspas(String s) {
        if (s.length() < 2) {
            return false;
        }
        char a = s.charAt(0);
        char b = s.charAt(s.length() - 1);
        if (a == '"' && b == '"') {
            return contar(s, '"') == 2;
        }
        if (a == '“' && b == '”') {
            return contar(s, '“') == 1 && contar(s, '”') == 1;
        }
        return false;
    }

    private String textoVisivel(String t) {
        String p = extrair(TAGS_PREFIXO, t);
        String s = extrair(TAGS_SUFIXO, t);
        if (p.length() + s.length() >= t.length()) {
            return "";
        }
        return t.substring(p.length(), t.length() - s.length());
    }

    private static String extrair(Pattern p, String t) {
        Matcher m = p.matcher(t);
        return m.find() ? m.group() : "";
    }

    private static int contar(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
