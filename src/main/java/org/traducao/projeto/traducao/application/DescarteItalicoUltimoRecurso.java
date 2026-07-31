package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: quando o LLM perde os marcadores {@code [[TAGn]]} de uma fala, o
 * pipeline devolve o TEXTO ORIGINAL EM INGLÊS — o espectador lê a legenda não traduzida,
 * com o itálico intacto. Este componente inverte a prioridade nesse caso: se as únicas
 * tags perdidas eram itálico, entrega a TRADUÇÃO sem itálico em vez do inglês com itálico.
 *
 * <p>Decisão do Paulo em 2026-07-31, e a medição confirma o tamanho: das 212 corrupções
 * registradas nos logs, 140 são resolvidas sem perda pelo
 * {@link SimplificadorItalicoRedundante} e <b>47 (22,2%) só têm itálico</b> — estas. As 25
 * restantes têm outra causa ({@code \pos} pesado, fala sem tag) e continuam intocadas.
 *
 * <p>Exemplos reais que hoje ficam em inglês e passam a sair em português:
 * {@code {\i1}Eledore!} · {@code {\i1}Roger!{\i0}} ·
 * {@code The Apsaras {\i1}will{\i0} be completed.}
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>É ÚLTIMO RECURSO: só age depois de o modelo já ter falhado e o pipeline estar
 *       prestes a publicar o original em inglês. Nunca antecipa nem substitui o
 *       mascaramento normal.</li>
 *   <li>Só age quando <b>TODAS</b> as tags da fala são exclusivamente de itálico. Uma
 *       única tag com posição, cor, fonte ou quebra ({@code \pos}, {@code \an8},
 *       {@code \N}) cancela o descarte — essas mudam o que se vê na tela, não só a ênfase.</li>
 *   <li>Não inventa formatação: a saída é o texto traduzido limpo, sem marcador residual.</li>
 *   <li>Sem estado; só JDK + Spring.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Devolve {@code null} quando não pode agir — o chamador mantém o comportamento anterior
 * (original em inglês). Nunca lança.
 */
@Component
public class DescarteItalicoUltimoRecurso {

    /** Marcador de mascaramento que possa ter sobrado na resposta do modelo. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\[\\[TAG\\d+]]");
    /** Bloco de override ASS. */
    private static final Pattern BLOCO = Pattern.compile("\\{[^{}]*}");
    /** O conteúdo do bloco é SÓ itálico? */
    private static final Pattern SO_ITALICO = Pattern.compile("(\\\\i[01]?(?![0-9]))+");

    /**
     * PROPÓSITO DE NEGÓCIO: tenta salvar a tradução descartando apenas o itálico.
     *
     * <p>INVARIANTES DO DOMÍNIO: devolve {@code null} — e não uma string vazia — sempre que
     * qualquer tag da fala não for exclusivamente itálico, para que o chamador preserve o
     * comportamento de manter o original. Texto traduzido em branco também devolve
     * {@code null}: publicar vazio seria pior que publicar inglês.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer entrada nula devolve {@code null}.
     *
     * @param tags as tags originais extraídas pelo mascaramento, na ordem
     * @param traduzidoMascarado a resposta do modelo, ainda com (ou sem) marcadores
     * @return a tradução sem itálico, ou {@code null} quando o descarte não é seguro
     */
    public String salvarSemItalico(List<String> tags, String traduzidoMascarado) {
        if (tags == null || tags.isEmpty() || traduzidoMascarado == null) {
            return null;
        }
        for (String tag : tags) {
            if (!ehSomenteItalico(tag)) {
                return null;
            }
        }
        String limpo = PLACEHOLDER.matcher(traduzidoMascarado).replaceAll("");
        limpo = BLOCO.matcher(limpo).replaceAll("");
        limpo = limpo.replaceAll("\\s{2,}", " ").strip();
        return limpo.isBlank() ? null : limpo;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: informa se uma tag do mascaramento é puramente itálico.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code {\i1}} e {@code {\i}} são itálico; {@code \N},
     * {@code {\an8\i1}} e qualquer bloco com outro override NÃO são.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: tag nula/vazia devolve {@code false}.
     */
    public boolean ehSomenteItalico(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String t = tag.strip();
        if (!t.startsWith("{") || !t.endsWith("}")) {
            return false;  // escapes estruturais como \N caem aqui
        }
        String miolo = t.substring(1, t.length() - 1);
        return !miolo.isBlank() && SO_ITALICO.matcher(miolo).matches();
    }
}
