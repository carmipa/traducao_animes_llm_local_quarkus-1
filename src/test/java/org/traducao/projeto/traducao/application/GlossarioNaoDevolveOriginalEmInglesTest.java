package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: o reforço de glossário jamais pode devolver a fala EM INGLÊS. Ele existe
 * para canonizar uma tradução, e a única saída pior que não agir é entregar o original.
 *
 * <h2>Como este teste nasceu</h2>
 * Em 04/08/2026 um comentário foi escrito em {@code EnforcadorGlossarioFala} afirmando que o risco
 * estava <i>"Congelado em GlossarioNaoDevolveOriginalEmInglesTest"</i>. <b>O teste não existia.</b>
 * A citação foi commitada e publicada; quem lesse o comentário concluiria que havia rede onde não
 * havia nenhuma. Uma auditoria adversarial encontrou a citação falsa e o vazamento que ela alegava
 * cobrir, e este arquivo é a dívida sendo paga.
 *
 * <h2>A assimetria que causa o vazamento</h2>
 * Quem DECIDE agir é {@link EnforcadorGlossarioFala#chaveDeFala(String)}, que normaliza
 * {@code \N}, {@code \n} e {@code \h} para espaço. Quem REESCREVE é
 * {@code FronteiraTermoAss.padraoIgnorandoCaixa}, cujo separador interno aceita espaço e
 * {@code \N} — mas <b>não</b> {@code \h}. Um termo de duas palavras separado por {@code \h} faz a
 * chave casar e a reescrita não casar. E {@code replaceAll} sem casamento devolve a entrada
 * INTACTA, que aqui é o original em inglês.
 *
 * <p>O separador NÃO foi alargado: medido no acervo em 04/08 (61.051 falas), nome composto
 * separado por {@code \h} aparece <b>ZERO</b> vezes, e regra sem medição é palpite. A guarda
 * resolve a classe inteira sem inventar casamento — reescrita que não mudou nada significa que o
 * glossário não se aplicou.
 *
 * <h2>O QUE ESTE TESTE NÃO COBRE — e por que não mente sobre isso</h2>
 * O vazamento exige termo de DUAS palavras separado por {@code \h}, e o glossário tem uma única
 * entrada de UMA palavra ({@code "roger"}). Tentou-se construir o caso pela API pública e ele
 * <b>não é alcançável</b>: com termo de uma palavra o {@code \h} adjacente não atrapalha o
 * casamento — {@code "Roger\h!"} vira {@code "Entendido\h!"} corretamente.
 *
 * <p>Portanto a guarda em {@code reforcar} é proteção POR CONSTRUÇÃO, sem teste que a exercite.
 * Isso está escrito aqui em vez de ser afirmado como coberto, porque foi exatamente a afirmação
 * de cobertura inexistente que originou este arquivo. Quando a primeira entrada de duas palavras
 * entrar no glossário, o caso passa a ser alcançável e ESTE é o lugar de congelá-lo.
 *
 * <h2>Invariantes do domínio (estes sim, medidos aqui)</h2>
 * <ul>
 *   <li>O termo de uma palavra continua sendo canonizado, com tags e pontuação preservadas.</li>
 *   <li>A quebra {@code \N} adjacente ao termo não impede a reescrita.</li>
 *   <li>Fala fora do glossário passa intacta; entrada degenerada devolve a tradução.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A legenda entregue traz a fala em inglês no lugar da tradução, e o portão de qualidade não vê:
 * o texto veio do próprio original, então bate com ele por definição.
 */
class GlossarioNaoDevolveOriginalEmInglesTest {

    /** Espaço duro do ASS, montado em runtime para o literal não virar escape do fonte. */
    private static final String ESPACO_DURO = "\\" + "h";

    private static final String QUEBRA = "\\" + "N";

    private final EnforcadorGlossarioFala enforcador = new EnforcadorGlossarioFala();

    @Test
    @DisplayName("termo do glossario com UMA palavra continua sendo canonizado")
    void casoQueFuncionaSegueFuncionando() {
        assertEquals("{\\i1}Entendido.", enforcador.reforcar("{\\i1}Roger.", "Roger."),
            "o unico termo do glossario hoje tem de continuar valendo");
        assertEquals("Entendido!!", enforcador.reforcar("Roger!!", "Roger!!"));
    }

    @Test
    @DisplayName("quebra \\N adjacente ao termo nao impede a reescrita")
    void quebraDeLinhaAdjacenteNaoImpedeAReescrita() {
        assertEquals("Entendido." + QUEBRA,
            enforcador.reforcar("Roger." + QUEBRA, "Roger."),
            "a fronteira aceita a quebra; a reescrita tem de acompanhar");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: registra, com asserção, que o enforcer só age quando a fala INTEIRA
     * é o termo — e que a quebra no MEIO da fala a torna outra fala, não o termo.
     *
     * <p>Nasceu de uma premissa errada minha: escrevi este teste esperando que
     * {@code "Sim.\NRoger."} fosse canonizado. Não é, e está certo não ser — a chave vira
     * {@code "sim roger"}, que não é entrada do glossário.
     */
    @Test
    @DisplayName("fala que CONTEM o termo, sem ser o termo, passa intacta")
    void falaQueApenasContemOTermoNaoEReescrita() {
        assertEquals("Sim. Roger.",
            enforcador.reforcar("Sim." + QUEBRA + "Roger.", "Sim. Roger."),
            "o enforcer e de fala INTEIRA: 'sim roger' nao e entrada do glossario");
    }

    @Test
    @DisplayName("espaco duro \\h adjacente ao termo de uma palavra nao quebra o casamento")
    void espacoDuroAdjacenteNaoQuebraOCasamento() {
        assertEquals("Entendido" + ESPACO_DURO + "!",
            enforcador.reforcar("Roger" + ESPACO_DURO + "!", "Entendido!"),
            "com termo de UMA palavra o \\h fica FORA do termo e a fronteira o aceita; "
                + "o vazamento exige \\h ENTRE duas palavras do termo, hoje inalcancavel");
    }

    @Test
    @DisplayName("fala fora do glossario passa intacta")
    void falaForaDoGlossarioNaoEToccada() {
        assertEquals("Boa noite.", enforcador.reforcar("Good night.", "Boa noite."));
    }

    @Test
    @DisplayName("original nulo ou vazio devolve a traducao sem lancar")
    void degradaSemLancar() {
        assertEquals("Entendido.", enforcador.reforcar(null, "Entendido."));
        assertEquals("Entendido.", enforcador.reforcar("   ", "Entendido."));
        assertEquals("Entendido.", enforcador.reforcar("{\\pos(10,10)}", "Entendido."));
    }
}
