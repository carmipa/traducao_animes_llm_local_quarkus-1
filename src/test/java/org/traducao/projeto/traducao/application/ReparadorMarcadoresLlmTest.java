package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: trava o contrato do {@link ReparadorMarcadoresLlm} — o que ele PODE
 * recuperar e, sobretudo, o que ele NUNCA pode aceitar. Os pares original/tentativa aqui são
 * reais, extraídos do log da corrida de 2026-07-22 (13 episódios do 08th MS Team), na qual
 * 393 de 412 falas perdidas caíram por marcador corrompido tendo tradução aproveitável.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo texto devolvido pelo reparo passa em {@code marcadoresPreservados}: o reparo
 *       jamais produz algo que a regra estrita reprovaria.</li>
 *   <li>Marcador interno perdido, marcador inventado e duplicação continuam REPROVADOS.</li>
 *   <li>Eco (tentativa idêntica ao original sem os marcadores) é REPROVADO, para não
 *       publicar o inglês como tradução nem roubar o retry.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Uma regressão que afrouxe a guarda quebra os testes de rejeição; uma regressão que volte a
 * descartar tradução boa quebra os testes de reparo.
 */
@DisplayName("Bug 1: reparo determinístico de marcadores [[TAGn]] perdidos pelo LLM")
class ReparadorMarcadoresLlmTest {

    private final MascaradorTags mascarador = new MascaradorTags();
    private final ReparadorMarcadoresLlm reparador = new ReparadorMarcadoresLlm(mascarador);

    /** Confirma que o reparo nunca burla a régua estrita do pipeline. */
    private void assertReparoValido(String original, Optional<String> reparado) {
        assertTrue(reparado.isPresent(), "esperava reparo determinístico");
        assertTrue(mascarador.marcadoresPreservados(original, reparado.get()),
            "o texto reparado DEVE passar na verificação estrita de marcadores");
    }

    @Test
    @DisplayName("marcador só no prefixo: repõe e aproveita a tradução (caso real, Lote 10)")
    void marcadorDePrefixoPerdidoEhReposto() {
        String original = "[[TAG0]]A bridge, huh?";
        Optional<String> reparado = reparador.reparar(original, "Uma ponte, é?");

        assertReparoValido(original, reparado);
        assertEquals("[[TAG0]]Uma ponte, é?", reparado.get());
    }

    @Test
    @DisplayName("marcadores nas duas bordas: repõe prefixo e sufixo (caso real, Lote 18)")
    void marcadoresDasDuasBordasSaoRepostos() {
        String original = "[[TAG0]]Can you read me? Karen![[TAG1]]";
        Optional<String> reparado = reparador.reparar(original, "Posso te ouvir, Karen!");

        assertReparoValido(original, reparado);
        assertEquals("[[TAG0]]Posso te ouvir, Karen![[TAG1]]", reparado.get());
    }

    @Test
    @DisplayName("variante sintática [ TAG0 ]: normaliza a grafia sem mover nada (caso real, Lote 4)")
    void varianteSintaticaEhNormalizada() {
        String original = "[[TAG0]]It's not that he was unpleasant, or that you hate him or anything.";
        Optional<String> reparado = reparador.reparar(
            original, "[ TAG0 ] Não é que ele fosse desagradável ou que você o odeie ou coisa assim.");

        assertReparoValido(original, reparado);
        assertEquals("[[TAG0]] Não é que ele fosse desagradável ou que você o odeie ou coisa assim.",
            reparado.get());
    }

    @Test
    @DisplayName("marcador só no sufixo: repõe no fim")
    void marcadorDeSufixoPerdidoEhReposto() {
        String original = "Get down![[TAG0]]";
        Optional<String> reparado = reparador.reparar(original, "Abaixem-se!");

        assertReparoValido(original, reparado);
        assertEquals("Abaixem-se![[TAG0]]", reparado.get());
    }

    @Test
    @DisplayName("marcador INTERNO perdido: rejeita — a posição da quebra não é recuperável")
    void marcadorInternoPerdidoEhRejeitado() {
        assertEquals(Optional.empty(),
            reparador.reparar("Three tanks,[[TAG0]]one directly below.", "Três tanques, um logo abaixo."),
            "sem saber onde a tradução quebraria a linha, repor o \\N seria adivinhação");
    }

    @Test
    @DisplayName("marcador INVENTADO onde o original não tinha: rejeita")
    void marcadorInventadoEhRejeitado() {
        assertEquals(Optional.empty(),
            reparador.reparar("Just seeing it makes you excited!", "[[TAG0]]Só de ver já dá um gás!"));
    }

    @Test
    @DisplayName("marcador DUPLICADO pelo LLM: rejeita")
    void marcadorDuplicadoEhRejeitado() {
        assertEquals(Optional.empty(),
            reparador.reparar("[[TAG0]]Fire!", "[[TAG0]][[TAG0]]Fogo!"));
    }

    @Test
    @DisplayName("eco sem marcador: rejeita para não publicar o inglês nem roubar o retry")
    void ecoDoOriginalEhRejeitado() {
        assertEquals(Optional.empty(), reparador.reparar("[[TAG0]]Hello", "Hello"),
            "reparar o eco devolveria o original como se fosse tradução");
    }

    @Test
    @DisplayName("original sem texto visível (só formatação): rejeita")
    void originalSemTextoVisivelEhRejeitado() {
        assertEquals(Optional.empty(), reparador.reparar("[[TAG0]][[TAG1]]", "qualquer coisa"));
    }

    @Test
    @DisplayName("entradas degeneradas (nulo/branco) não lançam e rejeitam")
    void entradasDegeneradasRejeitamSemLancar() {
        assertEquals(Optional.empty(), reparador.reparar(null, "x"));
        assertEquals(Optional.empty(), reparador.reparar("[[TAG0]]x", null));
        assertEquals(Optional.empty(), reparador.reparar("[[TAG0]]x", "   "));
    }
}
