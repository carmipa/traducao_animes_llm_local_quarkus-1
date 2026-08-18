package org.traducao.projeto.core.texto;

import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: tira da resposta do modelo o token de template de chat que o servidor
 * deixou escapar para dentro do texto, para que um resultado CORRETO não seja jogado fora por um
 * artefato de transporte.
 *
 * <h2>O prejuízo, medido DUAS vezes, em fatias diferentes</h2>
 * <b>11/08/2026, na tradução.</b> Na comparação de modelos sobre DanMachi S01 a
 * {@code aya-expanse-8b} devolveu {@code "Bell, você não faz ideia de quão sortudo você
 * é.<|END_OF_TURN_TOKEN|>"}. A tradução estava boa; o token colado no fim quebrava a checagem de
 * marcadores {@code [[TAGn]]}, a fala era retentada três vezes com temperatura crescente,
 * falhava pelo mesmo motivo e caía no tradutor de máquina. <b>115 das 116 falas perdidas</b>
 * naquela execução — 99% — eram este token. O modelo foi reprovado por um defeito que era daqui.
 *
 * <p><b>18/08/2026, na Revisão de Lore.</b> O mesmo token, pela terceira porta. A limpeza vivia
 * dentro da fatia {@code traducao}, e o Javadoc de lá afirmava cobrir "os DOIS pontos que leem
 * {@code message.content()}" — só que a fatia {@code revisaoLore} tem o PRÓPRIO cliente, que
 * ninguém contou. Medido na auditoria das sete obras rodadas naquele dia:
 * <pre>
 *   4.903 propostas recusadas por escopo
 *   4.903 delas (100%) traziam &lt;|END_OF_TURN_TOKEN|&gt; na resposta
 *   2.616 delas (53,4%) o token era a UNICA diferenca — o modelo nao mudou nada,
 *                       e o validador leu o token como "termo que nao existe no ingles"
 * </pre>
 * Sete obras fecharam com "Falas corrigidas: 0" por causa disto. A tela auditava, sinalizava e
 * <b>nunca consertava</b>, e a causa não era o modelo.
 *
 * <h2>Por que mora no core</h2>
 * Limpar transporte é MECÂNICA, não decisão de domínio: nenhuma fatia é dona da pergunta "isto é
 * token de controle?". Enquanto a mecânica morou dentro de uma fatia, a fatia vizinha repetiu o
 * prejuízo inteiro sete dias depois. Quem lê {@code message.content()} chama daqui.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Remove só token de CONTROLE, nunca conteúdo. O padrão exige as barras verticais ou o
 *       nome exato da família Gemma.</li>
 *   <li>Não inclui {@code </s>}: é ambíguo com marcação de texto e nenhum modelo em uso o emite.
 *       Guarda que apaga conteúdo legítimo é pior que guarda nenhuma.</li>
 *   <li>Não decide nada. A validação canônica de cada fatia continua inteira depois daqui —
 *       limpar o transporte não é aprovar o resultado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Entrada nula devolve nula; texto sem token volta intacto e sem custo. Nunca lança.
 */
public final class TokenDeControleLlm {

    /**
     * A forma com barras verticais cobre Cohere ({@code <|END_OF_TURN_TOKEN|>}), ChatML
     * ({@code <|im_end|>}), Llama ({@code <|eot_id|>}) e GPT ({@code <|endoftext|>}); a segunda
     * cobre a família Gemma.
     */
    private static final Pattern TOKEN_DE_CONTROLE =
        Pattern.compile("<\\|[^|<>]{1,40}\\|>|</?(?:start|end)_of_turn>");

    private TokenDeControleLlm() {
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve a resposta sem os tokens de controle, pronta para a validação
     * de domínio da fatia que chamou.
     *
     * <p>INVARIANTES DO DOMÍNIO: só apaga o que casa o padrão; o restante do texto, incluindo
     * espaços internos, atravessa igual. As pontas são aparadas porque o token costuma vir colado
     * ao fim e deixaria espaço órfão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} devolve {@code null}.
     */
    public static String limpar(String bruto) {
        if (bruto == null) {
            return null;
        }
        return TOKEN_DE_CONTROLE.matcher(bruto).replaceAll("").strip();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde se a resposta trazia token de controle, para quem quiser
     * medir o vazamento sem alterar o texto.
     *
     * <p>INVARIANTES DO DOMÍNIO: pergunta pura, sem efeito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code null} devolve {@code false}.
     */
    public static boolean contemToken(String bruto) {
        return bruto != null && TOKEN_DE_CONTROLE.matcher(bruto).find();
    }
}
