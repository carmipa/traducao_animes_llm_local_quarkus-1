package org.traducao.projeto.traducao.domain;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: limpar a ênfase que sobrou torta na legenda traduzida — o par
 * {@code {\i1}…{\i0}} que acabou envolvendo NADA, e o espaço órfão que fica antes da pontuação
 * quando a ênfase se desloca. São defeitos que o espectador VÊ na tela.
 *
 * <h2>O prejuízo que originou</h2>
 * Encontrado por revisão adversarial em 08/08/2026, sobre a saída real do Guilty Crown, e depois
 * medido no acervo inteiro (305 arquivos): <b>16 ênfases vazias</b> e <b>73 espaços órfãos</b>.
 * <pre>
 *   I don't need your {\i1}consideration{\i0}. Understand?
 *     -> Não preciso da sua consideração {\i1}{\i0}. Entender?     &lt;- enfase em volta do vazio
 *   Did you see {\i1}that room{\i0}?
 *     -> Você viu {\i1}aquele quarto{\i0} ?                        &lt;- " ?" na tela
 * </pre>
 * A causa: a tag marca UMA PALAVRA do inglês; em português ela muda de posição, e o modelo
 * devolve os marcadores fora do lugar. Nenhuma guarda pegava isso porque, tecnicamente, os
 * marcadores ESTÃO todos lá — o desmascaramento é bem-sucedido e a fala passa por íntegra.
 *
 * <p>Verificado na mesma medição que o defeito é ANTERIOR a qualquer mudança de 07-08/08/2026:
 * a fala da "consideração" sai byte a byte idêntica na tradução antiga e na nova. Não é
 * regressão; é defeito de sempre que ninguém tinha olhado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Só age no INEQUÍVOCO: par abre/fecha sem nada entre eles (ou só espaço), e espaço
 *       imediatamente antes de pontuação logo após o fecha. Ênfase com qualquer conteúdo real é
 *       intocada — quem decide o que enfatizar é o fansub, não este saneador.</li>
 *   <li>Nunca remove texto visível: a saída tem exatamente as mesmas letras da entrada, só sem
 *       o par vazio e sem o espaço órfão.</li>
 *   <li>Vale para os quatro pares de ênfase do ASS ({@code i}, {@code b}, {@code u}, {@code s});
 *       tag de posição, cor ou fonte NUNCA é tocada — ali o vazio pode ser intencional.</li>
 *   <li>Idempotente: aplicar duas vezes dá o mesmo resultado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entrada nula ou em branco volta como veio. Nunca lança, nunca devolve vazio a partir de texto
 * não vazio — na dúvida, devolve a entrada intacta.
 */
public final class SaneadorEnfaseDegenerada {

    /**
     * Par de ênfase abrindo e fechando sem nada útil entre eles. Restrito a i/b/u/s de propósito:
     * {@code {\pos(..)}{\an8}} vizinhos são normais e não podem ser tocados.
     */
    private static final Pattern PAR_VAZIO =
        Pattern.compile("\\{\\\\([ibus])1\\}\\s*\\{\\\\\\1 ?0\\}");

    /** Espaço sobrando entre o fecha-ênfase e a pontuação que o segue. */
    private static final Pattern ESPACO_ORFAO =
        Pattern.compile("(\\{\\\\[ibus] ?0\\})\\s+([.,;:!?…]+)");

    /** Espaço duplicado que a remoção do par vazio pode deixar para trás. */
    private static final Pattern ESPACO_DUPLO = Pattern.compile("(?<=\\S) {2,}(?=\\S)");

    /**
     * Espaço que sobra ANTES da pontuação depois que o par vazio some — "consideração ." vira
     * "consideração.". Restrito a {@code .,;:!?} e reticências: em português nenhum deles admite
     * espaço antes. O travessão ({@code —}) fica FORA de propósito, porque ali o espaço é
     * legítimo e tirá-lo grudaria a fala interrompida na palavra anterior.
     */
    private static final Pattern ESPACO_ANTES_DE_PONTUACAO =
        Pattern.compile("(?<=\\S)\\s+(?=[.,;:!?…]+(?:\\s|$))");

    private SaneadorEnfaseDegenerada() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a fala pronta para a tela, sem ênfase vazia nem espaço solto
     * antes da pontuação.
     *
     * <p>INVARIANTES DO DOMÍNIO: só remove marcação degenerada e espaço; o texto visível sai com
     * as mesmas letras. Idempotente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} ou texto em branco volta como veio; se o
     * saneamento esvaziar o texto visível — o que não deveria acontecer —, a ENTRADA é devolvida
     * intacta, porque publicar vazio é pior que publicar torto.
     */
    public static String sanear(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String saneado = texto;
        // Laco: {\i1}{\i1}{\i0}{\i0} aninhado so some com passadas sucessivas, e um limite
        // explicito evita laco infinito se algum padrao futuro nao convergir.
        for (int passada = 0; passada < 5; passada++) {
            String antes = saneado;
            saneado = PAR_VAZIO.matcher(saneado).replaceAll("");
            saneado = ESPACO_ORFAO.matcher(saneado).replaceAll("$1$2");
            if (saneado.equals(antes)) {
                break;
            }
        }
        saneado = ESPACO_ANTES_DE_PONTUACAO.matcher(saneado).replaceAll("");
        saneado = ESPACO_DUPLO.matcher(saneado).replaceAll(" ");
        saneado = saneado.strip();

        if (saneado.isBlank() && !texto.isBlank()) {
            return texto;
        }
        return saneado;
    }
}
