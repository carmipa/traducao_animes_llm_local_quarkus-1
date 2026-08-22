package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o itálico sai e que NADA MAIS sai junto.
 *
 * <p>INVARIANTES DO DOMÍNIO: o token some, o bloco sobrevive quando tem outro override, e o
 * bloco que fica vazio é descartado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: se um caso misto perder o bloco inteiro, a legenda perde
 * posição/cor/quebra — dano invisível em teste de texto e visível na tela.
 */
class RemovedorItalicoTest {

    private final RemovedorItalico removedor = new RemovedorItalico();

    /** O caso de ÊNFASE que originou tudo: o par que ficou vazio no Guilty Crown ep14. */
    @Test
    @DisplayName("par de enfase some inteiro, texto intacto")
    void parDeEnfaseSomeEDeixaOTextoIntacto() {
        assertEquals("there's no love in this kind of killing.",
            removedor.remover("there's no {\\i1}love{\\i0} in this kind of killing."));
    }

    /** Narração: 7,5% do acervo. Abre a fala e nao fecha. */
    @Test
    @DisplayName("italico de narracao some sem deixar bloco vazio")
    void italicoDeNarracaoSome() {
        assertEquals("Vamos para o campo de batalha.",
            removedor.remover("{\\i1}Vamos para o campo de batalha."));
        // "Estou do seu lado!{\\i0}" NAO entra aqui: e o caso do i0 orfao, coberto em
        // i0OrfaoDeixaAFalaIntacta. Fechar sem abrir significa que o italico veio do Style.
        assertEquals("Chegamos.", removedor.remover("{\\i1}Chegamos.{\\i0}"));
    }

    /**
     * O CONTRA-CASO que importa: 340 blocos do acervo misturam italico com outra coisa.
     * Apagar o bloco destruiria quebra automatica, posicao, cor e fade.
     */
    @Test
    @DisplayName("bloco MISTO perde so o italico e mantem o resto")
    void blocoMistoMantemOsDemaisOverrides() {
        assertEquals("{\\q2}Texto", removedor.remover("{\\q2\\i1}Texto"));
        assertEquals("{\\fade(1781,0)\\bord0\\an7\\c&HFFFFFF&\\pos(57.6,253.6)}MOBILE SUIT",
            removedor.remover(
                "{\\fade(1781,0)\\bord0\\an7\\i1\\c&HFFFFFF&\\pos(57.6,253.6)}MOBILE SUIT"));
    }

    /** Fala sem italico e devolvida byte a byte; nulo nao lanca. */
    @Test
    @DisplayName("sem italico nada muda")
    void semItalicoNadaMuda() {
        String intacta = "{\\an8\\pos(10,20)}Nada de italico aqui.";
        assertEquals(intacta, removedor.remover(intacta));
        assertEquals("Sem tag nenhuma.", removedor.remover("Sem tag nenhuma."));
        assertEquals(null, removedor.remover(null));
    }

    /**
     * O CASO QUE INVERTE O EFEITO: quando o primeiro token e um DESLIGA, o italico veio do
     * {@code Style:} do cabecalho. Remover o desliga ACENDERIA o italico -- o oposto da regra.
     * Falha fechada: a fala fica intacta, byte a byte.
     */
    @Test
    @DisplayName("i0 orfao: fala intacta, porque o italico vem do Style")
    void i0OrfaoDeixaAFalaIntacta() {
        String herda = "{\\an8\\i0}Isso e tudo o que voce pode fazer?";
        assertEquals(herda, removedor.remover(herda));
        String fim = "Estou do seu lado!{\\i0}";
        assertEquals(fim, removedor.remover(fim));
        String duplo = "Desca, tenente!{\\i0}\\NVoce me ouve?!{\\i0}";
        assertEquals(duplo, removedor.remover(duplo));
    }

    /**
     * CONTRA-CASO do anterior: quando o {@code \i1} vem primeiro, o par e da propria fala e
     * sai inteiro. Sem este caso, "nao mexer quando ha \i0" passaria como se fosse a regra.
     */
    @Test
    @DisplayName("i1 antes de i0: o par e da fala e sai inteiro")
    void i1AntesDeI0RemoveOParInteiro() {
        assertEquals("Agora este e um teste digno!",
            removedor.remover("Agora {\\i1}este{\\i0} e um teste digno!"));
    }

    /** Idempotente: rodar duas vezes nao muda nada na segunda. */
    @Test
    @DisplayName("idempotente")
    void idempotente() {
        String uma = removedor.remover("{\\i1}A{\\i0} e {\\i1}B{\\i0}");
        assertEquals(uma, removedor.remover(uma));
        assertEquals("A e B", uma);
    }
}
