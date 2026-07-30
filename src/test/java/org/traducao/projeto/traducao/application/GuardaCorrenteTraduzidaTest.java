package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa por regressão o contrato da {@link GuardaCorrenteTraduzida} —
 * barrar a corrente em que o LLM devolveu a contagem certa mas MOVEU o conteúdo entre as
 * linhas, defeito que a validação de contagem não enxerga.
 *
 * <p>INVARIANTES DO DOMÍNIO: casos reais medidos no Unicorn em 2026-07-29 (deslocamento,
 * marcador inventado, locutor inventado) têm que reprovar; tradução legítima, com a
 * expansão natural do português, tem que passar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: aprovar um deslocamento — ou reprovar uma tradução boa
 * — reprova a suíte.
 */
class GuardaCorrenteTraduzidaTest {

    private final GuardaCorrenteTraduzida guarda = new GuardaCorrenteTraduzida();

    @Test
    @DisplayName("Tradução boa passa, mesmo com a expansão natural do português")
    void aceitaCorrenteLegitima() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("The Earth Federation government is about to host a ceremony",
                "at the Prime Minister's residence, \"Laplace\","),
            List.of("O governo da Federação Terrestre está prestes a realizar uma cerimônia",
                "na residência do Primeiro-Ministro, \"Laplace\","));

        assertTrue(v.aceita(), "tradução correta não pode ser barrada");
        assertNull(v.motivo());
    }

    @Test
    @DisplayName("Caso real: 5 palavras viraram 13 porque o conteúdo da linha seguinte migrou")
    void reprovaDeslocamentoDeConteudo() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("Yet, as we've now realized humanity's long-awaited dream",
                "of a unified world government,"),
            List.of("Entretanto, agora que realizamos o sonho há muito esperado da humanidade",
                "podemos olhar para trás e ver os problemas do antigo sistema"));

        assertFalse(v.aceita(), "deslocamento tem contagem certa e conteúdo errado");
        assertNotNull(v.motivo());
        assertTrue(v.motivo().contains("tamanho"), "motivo esperado sobre tamanho: " + v.motivo());
    }

    @Test
    @DisplayName("Marcador [[TAGn]] perdido reprova a corrente")
    void reprovaMarcadorPerdido() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("[[TAG0]]Audrey and Banagher are reunited aboard the Nahel Argama,",
                "but the ship is attacked by Full Frontal."),
            List.of("Audrey e Banagher se reencontram a bordo da Nahel Argama,",
                "mas a nave é atacada por Full Frontal."));

        assertFalse(v.aceita());
        assertTrue(v.motivo().contains("marcador"), v.motivo());
    }

    @Test
    @DisplayName("Caso real: o modelo inventou um marcador vazio [[]]")
    void reprovaMarcadorInventado() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("a mobile suit far more suited to the task,",
                "more fitting to entrust Laplace's Box to."),
            List.of("um mobile suit muito mais adequado à tarefa,", "[[]]"));

        assertFalse(v.aceita());
    }

    @Test
    @DisplayName("Caso real: o modelo inventou o rótulo de falante 'Mineva:'")
    void reprovaLocutorInventado() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("She moves at a speed no one can follow,",
                "three times the speed of the others!"),
            List.of("Ela se move a uma velocidade que ninguém acompanha,",
                "Mineva: Audrey, olhe para mim agora."));

        assertFalse(v.aceita());
        assertTrue(v.motivo().contains("locutor"), v.motivo());
    }

    @Test
    @DisplayName("Locutor que já vinha no original não é invenção")
    void aceitaLocutorQueJaExistiaNoOriginal() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("Mineva: I have something to tell you now,",
                "something I could not say before."),
            List.of("Mineva: Tenho algo a lhe dizer agora,",
                "algo que eu não podia dizer antes."));

        assertTrue(v.aceita(), v.motivo());
    }

    @Test
    @DisplayName("Linha que quase sumiu reprova")
    void reprovaLinhaQuaseVazia() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("The Vist Foundation has kept the secret for a century,",
                "and now the time has come to reveal it."),
            List.of("A Fundação Vist guardou o segredo por um século,", "E."));

        assertFalse(v.aceita());
    }

    @Test
    @DisplayName("Fala curta não é julgada por razão de tamanho")
    void falaCurtaEscapaDaRazao() {
        GuardaCorrenteTraduzida.Veredito v = guarda.avaliar(
            List.of("Whatever it is,", "promise me."),
            List.of("Seja o que for,", "prometa-me."));

        assertTrue(v.aceita(), v.motivo());
    }

    @Test
    @DisplayName("Grupo de uma linha só é sempre aprovado — não há corrente para deslocar")
    void grupoUnitarioPassa() {
        assertTrue(guarda.avaliar(List.of("Fear not."), List.of("Não tema.")).aceita());
    }

    @Test
    @DisplayName("Entrada inconsistente vira recusa, não exceção")
    void entradaInconsistenteRecusa() {
        assertFalse(guarda.avaliar(null, List.of("x")).aceita());
        assertFalse(guarda.avaliar(List.of("a", "b"), List.of("x")).aceita());
        assertFalse(guarda.avaliar(List.of("a", "b"), Arrays.asList("x", null)).aceita());
        assertFalse(guarda.avaliar(List.of("a", "b"), List.of("x", "  ")).aceita());
    }
}
