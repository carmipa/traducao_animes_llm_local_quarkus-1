package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa por regressão o contrato do {@link IsoladorQuebraDialogo} —
 * isolar o {@code \N} mid-sentence do diálogo antes do LLM e reaplicá-lo depois — sem
 * depender de rede, LLM ou legenda real.
 *
 * <p>INVARIANTES DO DOMÍNIO: só quebra mid-sentence é isolada; borda é preservada; a
 * reaplicação reinsere a mesma quantidade de quebras; ambos os métodos são puros.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer desvio de contagem, de gate de borda ou de
 * reaplicação reprova a suíte.
 */
class IsoladorQuebraDialogoTest {

    private final IsoladorQuebraDialogo isolador = new IsoladorQuebraDialogo();

    @Test
    @DisplayName("\\N no meio da frase vira espaço e conta 1 quebra")
    void quebraNoMeioViraEspacoEConta() {
        IsoladorQuebraDialogo.FalaIsolada r = isolador.isolar("put up \\Nwith this");

        assertEquals("put up with this", r.textoSemQuebra());
        assertEquals(1, r.quebras());
    }

    @Test
    @DisplayName("\\N mid-sentence preservando a tag {\\q2} de estilo no início")
    void quebraNoMeioComTagDeEstiloNoInicio() {
        IsoladorQuebraDialogo.FalaIsolada r = isolador.isolar("{\\q2}give \\Nhim the classic");

        assertEquals("{\\q2}give him the classic", r.textoSemQuebra());
        assertEquals(1, r.quebras());
    }

    @Test
    @DisplayName("texto sem \\N é devolvido intacto com zero quebras")
    void semQuebraDevolveIntacto() {
        IsoladorQuebraDialogo.FalaIsolada r = isolador.isolar("{\\i1}Ola mundo");

        assertEquals("{\\i1}Ola mundo", r.textoSemQuebra());
        assertEquals(0, r.quebras());
    }

    @Test
    @DisplayName("\\N de borda (sem texto depois) é estrutural e não é isolado")
    void quebraDeBordaNaoEhIsolada() {
        IsoladorQuebraDialogo.FalaIsolada r = isolador.isolar("A flor floresce\\N");

        assertEquals("A flor floresce\\N", r.textoSemQuebra());
        assertEquals(0, r.quebras());
    }

    @Test
    @DisplayName("dois \\N mid-sentence contam 2 quebras")
    void doisMidSentenceContamDois() {
        IsoladorQuebraDialogo.FalaIsolada r = isolador.isolar("um \\Ndois \\Ntres");

        assertEquals("um dois tres", r.textoSemQuebra());
        assertEquals(2, r.quebras());
    }

    @Test
    @DisplayName("reaplicar insere 1 \\N perto do meio, em fronteira de palavra")
    void reaplicarInsereUmaQuebraNoMeio() {
        String r = isolador.reaplicar("Por que temos que aguentar isso", 1);

        assertEquals(1, contarQuebras(r), "deve ter exatamente uma quebra");
        assertFalse(r.contains("  "), "não deve deixar espaço duplo");
        assertTrue(r.replace("\\N", " ").equals("Por que temos que aguentar isso"),
            "removendo a quebra deve voltar ao texto original");
    }

    @Test
    @DisplayName("reaplicar com zero quebras é no-op")
    void reaplicarZeroEhNoOp() {
        assertEquals("texto qualquer", isolador.reaplicar("texto qualquer", 0));
    }

    @Test
    @DisplayName("reaplicar sem espaço disponível devolve o texto inalterado")
    void reaplicarSemEspacoDevolveInalterado() {
        assertEquals("palavraunica", isolador.reaplicar("palavraunica", 1));
    }

    @Test
    @DisplayName("isolar+reaplicar preserva o texto visível (round-trip da quebra)")
    void roundTripPreservaTextoVisivel() {
        String original = "Why do we have to put up \\Nwith this";
        IsoladorQuebraDialogo.FalaIsolada isolada = isolador.isolar(original);
        String reaplicado = isolador.reaplicar(isolada.textoSemQuebra(), isolada.quebras());

        assertEquals(1, contarQuebras(reaplicado));
        assertEquals("Why do we have to put up with this", reaplicado.replace("\\N", " "));
    }

    private static int contarQuebras(String texto) {
        int n = 0;
        int i = texto.indexOf("\\N");
        while (i >= 0) {
            n++;
            i = texto.indexOf("\\N", i + 2);
        }
        return n;
    }
}
