package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova o DETECTOR de frase partida entre eventos — o sinal que impede a
 * revisão 3.1 de "completar" um pedaço cortado com conteúdo inventado. Medido no Unicorn ep22:
 * {@code "Today, I learned a secret that affects"} traduzido isolado virou
 * {@code "afeta profundamente"} (o LLM inventou o complemento).
 *
 * <h2>Por que este teste, e por que CONSERVADOR</h2>
 * O detector só afirma continuação quando o atual NÃO termina em pontuação terminal E o próximo
 * começa em letra minúscula. O viés é NÃO marcar: um falso negativo deixa a fala seguir o fluxo
 * normal (no pior caso, o fabrico volta); um falso positivo apenas a preserva em inglês (honesto),
 * nunca corrompe. A regra oposta — "guarda que reprova código correto é pior que guarda nenhuma" —
 * é o que este contra-teste protege: frase completa e começo de frase nova NÃO podem ser marcados.
 */
class RevisarLegendasFragmentoCrossEventoTest {

    @Test
    @DisplayName("marca fragmento continuado; NAO confunde frase completa nem comeco de frase nova")
    void detectaFragmentoSemFalsoPositivo() {
        // DOENTE: sem pontuacao terminal + proximo comeca minusculo -> e continuacao
        assertTrue(RevisarLegendasUseCase.continuaNoProximoEvento(
                "Today, I learned a secret that affects", "the whole world, and I must act."),
            "o caso REAL do Unicorn ep22 tem de ser reconhecido como fragmento");

        // LEGITIMO 1 (A1): frase completa termina em pontuacao -> NAO e fragmento
        assertFalse(RevisarLegendasUseCase.continuaNoProximoEvento(
                "Today I learned a secret.", "the whole world is different now."),
            "pontuacao terminal fecha a frase — marcar aqui seria reprovar o certo");

        // LEGITIMO 2 (A1): proximo comeca MAIUSCULO (frase nova) -> conservador NAO marca
        assertFalse(RevisarLegendasUseCase.continuaNoProximoEvento(
                "Get out of there", "Now!"),
            "comeco de frase nova nao e continuacao inequivoca; o vies e nao marcar");

        // borda: tags e \N nao contam — compara o texto VISIVEL
        assertTrue(RevisarLegendasUseCase.continuaNoProximoEvento(
                "{\\i1}and then he said", "{\\i1}that we should leave."),
            "as tags ASS nao mudam a pergunta 'a frase continua?'");

        // falha fechada
        assertFalse(RevisarLegendasUseCase.continuaNoProximoEvento(null, "x"));
        assertFalse(RevisarLegendasUseCase.continuaNoProximoEvento("x", "   "));
    }
}
