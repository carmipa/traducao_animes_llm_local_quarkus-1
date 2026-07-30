package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * PROPÓSITO DE NEGÓCIO: quando uma corrente de frase partida vai ao LLM numa chamada só,
 * aparece um defeito que a validação de contagem NÃO enxerga — o modelo devolve o número
 * certo de linhas e MOVE o conteúdo entre elas. A linha 2 recebe o texto da linha 3, uma
 * linha some, um rótulo de locutor é inventado. Como cada evento tem tempo próprio na tela,
 * conteúdo deslocado é pior que a maiúscula que o agrupamento veio consertar. Esta guarda
 * reprova a corrente inteira para que ela volte ao fluxo de uma fala por chamada.
 *
 * <p>Medido no acervo Unicorn (2026-07-29), NAS 402 correntes que passaram na contagem —
 * ou seja, o que a validação atual deixa passar: 4,2% das 887 linhas com tamanho fora de
 * escala, 0,7% quase vazias, 0,3% com marcador {@code [[TAGn]]} perdido e 0,3% com locutor
 * inventado. Sem esta guarda, o agrupamento troca 30% de conector errado por 4% de conteúdo
 * errado — e conteúdo errado não tem conserto a jusante.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Julga a corrente INTEIRA: uma linha reprovada reprova o grupo, porque o
 *       deslocamento move texto ENTRE linhas — salvar uma e descartar outra deixaria a
 *       legenda com duas versões do mesmo trecho.</li>
 *   <li>Compara o texto MASCARADO enviado com o MASCARADO recebido; a desmascaração e a
 *       reaplicação de quebra vêm depois e não são responsabilidade daqui.</li>
 *   <li>A razão de tamanho só vale a partir de 3 palavras: em fala curta ("Sim!", "Vá!") a
 *       variação natural entre inglês e português estoura qualquer faixa.</li>
 *   <li>Classe sem estado; só JDK + Spring.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Listas de tamanhos diferentes reprovam (a contagem já deveria ter barrado). Linha nula ou
 * vazia reprova. Grupo de uma linha só é sempre aprovado — não há corrente para deslocar.
 */
@Component
public class GuardaCorrenteTraduzida {

    private static final Pattern PADRAO_PLACEHOLDER = Pattern.compile("\\[\\[TAG(\\d+)]]");
    /** Rótulo de falante que o modelo às vezes inventa: {@code Mineva: ...}. */
    private static final Pattern PADRAO_LOCUTOR =
        Pattern.compile("^[\"'\\s]*\\p{Lu}[\\p{L}'-]{1,18}(?: \\p{Lu}[\\p{L}'-]{1,18})?:\\s");

    /** Faixa de expansão tolerada entre o original e a tradução, em palavras. */
    private static final double PISO_RAZAO = 0.5;
    private static final double TETO_RAZAO = 1.9;
    /** Abaixo disto a razão não é sinal — fala curta varia demais entre os idiomas. */
    private static final int PALAVRAS_MINIMAS = 3;

    /**
     * PROPÓSITO DE NEGÓCIO: veredito sobre uma corrente traduzida, com o motivo da recusa
     * para log e telemetria.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code motivo} é {@code null} exatamente quando
     * {@code aceita} é {@code true}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro, sem validação.
     */
    public record Veredito(boolean aceita, String motivo) {
        static Veredito ok() {
            return new Veredito(true, null);
        }

        static Veredito recusa(String motivo, int linha) {
            return new Veredito(false, motivo + " (linha " + (linha + 1) + ")");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aprova ou reprova a tradução de uma corrente antes que ela entre
     * na legenda.
     *
     * <p>INVARIANTES DO DOMÍNIO: reprova na PRIMEIRA violação encontrada, na ordem
     * marcador → locutor → quase vazia → razão de tamanho; o motivo devolvido nomeia a
     * violação e a linha (base 1).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança; entrada inconsistente vira recusa.
     *
     * @param mascaradosOriginais linhas enviadas ao LLM, já mascaradas
     * @param mascaradosTraduzidos linhas recebidas do LLM, ainda mascaradas
     */
    public Veredito avaliar(List<String> mascaradosOriginais, List<String> mascaradosTraduzidos) {
        if (mascaradosOriginais == null || mascaradosTraduzidos == null) {
            return new Veredito(false, "corrente sem conteudo");
        }
        if (mascaradosOriginais.size() != mascaradosTraduzidos.size()) {
            return new Veredito(false, "contagem divergente");
        }
        if (mascaradosOriginais.size() < 2) {
            return Veredito.ok();
        }
        for (int i = 0; i < mascaradosOriginais.size(); i++) {
            String origem = mascaradosOriginais.get(i);
            String destino = mascaradosTraduzidos.get(i);
            if (destino == null || destino.isBlank()) {
                return Veredito.recusa("linha vazia", i);
            }
            if (!marcadores(origem).equals(marcadores(destino))) {
                return Veredito.recusa("marcador perdido ou trocado", i);
            }
            if (PADRAO_PLACEHOLDER.matcher(destino).replaceAll("").contains("[[")) {
                return Veredito.recusa("marcador inventado", i);
            }
            String textoOrigem = semMarcadores(origem);
            String textoDestino = semMarcadores(destino);
            if (PADRAO_LOCUTOR.matcher(textoDestino).find()
                && !PADRAO_LOCUTOR.matcher(textoOrigem).find()) {
                return Veredito.recusa("locutor inventado", i);
            }
            int palavrasOrigem = contarPalavras(textoOrigem);
            int palavrasDestino = contarPalavras(textoDestino);
            if (palavrasOrigem < PALAVRAS_MINIMAS) {
                continue;
            }
            if (palavrasDestino <= 1) {
                return Veredito.recusa("linha quase vazia", i);
            }
            if (palavrasDestino > palavrasOrigem * TETO_RAZAO
                || palavrasDestino < palavrasOrigem * PISO_RAZAO) {
                return Veredito.recusa(
                    "tamanho fora de escala (" + palavrasOrigem + "->" + palavrasDestino + ")", i);
            }
        }
        return Veredito.ok();
    }

    private Set<String> marcadores(String texto) {
        Matcher m = PADRAO_PLACEHOLDER.matcher(texto == null ? "" : texto);
        return m.results().map(r -> r.group(1)).collect(Collectors.toSet());
    }

    private String semMarcadores(String texto) {
        return PADRAO_PLACEHOLDER.matcher(texto == null ? "" : texto).replaceAll(" ").strip();
    }

    private int contarPalavras(String texto) {
        if (texto.isBlank()) {
            return 0;
        }
        return texto.split("\\s+").length;
    }
}
