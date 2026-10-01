package org.traducao.projeto.traducaoKaraoke.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a regra que impede a letra de vários versos de perder o segundo
 * verso na volta do LLM (F5, 24/09/2026). Os textos são os do cache real do karaokê do Unicorn.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada regra é exercitada nos DOIS lados da fronteira (A1) — o defeito
 * e o legítimo que carrega o mesmo sinal superficial.
 */
class VersosDaLetraTest {

    private static final String N = "\\N";

    @Test
    @DisplayName("conta versos pelo \\N visivel, ignorando tags e quebra de borda")
    void contaVersos() {
        assertEquals(2, VersosDaLetra.contar("{\\fad(200,200)}We had paced back and forth all that time"
            + N + "You do never know that love I felt" + N));
        assertEquals(1, VersosDaLetra.contar("{\\fad(200,200)}Do you feel alone"));
        assertEquals(0, VersosDaLetra.contar(null));
    }

    @Test
    @DisplayName("duas linhas para dois versos viram os dois versos unidos por \\N — nenhum se perde")
    void duasLinhasParaDoisVersosSaoUnidas() {
        Optional<String> r = VersosDaLetra.unirResposta(
            List.of("Nós caminhamos de um lado para o outro durante todo aquele tempo.",
                "Você nunca sabe o amor que eu senti."), 2);
        assertEquals("Nós caminhamos de um lado para o outro durante todo aquele tempo." + N
            + "Você nunca sabe o amor que eu senti.", r.orElseThrow());
    }

    @Test
    @DisplayName("A1: uma linha para dois versos (o modelo fundiu numa frase) e aceita como esta")
    void umaLinhaFundidaEhAceita() {
        assertEquals("Eu posso ser uma garota comum, enquanto estive bem ao seu lado.",
            VersosDaLetra.unirResposta(List.of("Eu posso ser uma garota comum, enquanto estive bem ao seu lado."), 2)
                .orElseThrow());
    }

    @Test
    @DisplayName("contagem que nao bate (2 linhas para 1 verso) e RECUSADA — nunca vira meia letra")
    void contagemQueNaoBateERecusada() {
        assertTrue(VersosDaLetra.unirResposta(List.of("Você se sente sozinho?", "Nota: traduzi livremente."), 1).isEmpty());
        assertTrue(VersosDaLetra.unirResposta(List.of("a", "b", "c"), 2).isEmpty());
        assertTrue(VersosDaLetra.unirResposta(List.of(), 1).isEmpty());
        assertTrue(VersosDaLetra.unirResposta(null, 1).isEmpty());
    }

    @Test
    @DisplayName("a barra solta (resto do \\N que o modelo nao terminou) sai; o texto fica")
    void barraSoltaSai() {
        assertEquals("Passei todo esse tempo andando para lá e para cá." + N + "Você nunca sabe o amor que eu senti.",
            VersosDaLetra.unirResposta(List.of("Passei todo esse tempo andando para lá e para cá. \\",
                "Você nunca sabe o amor que eu senti."), 2).orElseThrow());
        assertEquals("Deus, que julgamento!", VersosDaLetra.semBarraSolta("Deus, que julgamento! \\"));
        assertEquals("sem barra", VersosDaLetra.semBarraSolta("sem barra"));
    }

    @Test
    @DisplayName("cache truncado e suspeito; A1: a fusao legitima dos mesmos versos nao e")
    void suspeitaDeVersoPerdidoNaFronteira() {
        String original = "{\\fad(200,200)}Have a little break" + N + "We’re running through the lights, out of breath";
        assertTrue(VersosDaLetra.suspeitaDeVersoPerdido(original, "{\\fad(200,200)}Faça uma pequena pausa"),
            "0,33 do tamanho do original: o segundo verso sumiu");
        assertFalse(VersosDaLetra.suspeitaDeVersoPerdido(original,
            "{\\fad(200,200)}Faça uma pequena pausa, estamos correndo pelas luzes, sem fôlego"),
            "A1: os dois versos fundidos numa frase sao legitimos");
        assertTrue(VersosDaLetra.suspeitaDeVersoPerdido("God, what a judgement" + N + "Is my punishment",
            "Deus, que julgamento! \\"), "barra solta no fim e a marca do verso perdido");
        assertFalse(VersosDaLetra.suspeitaDeVersoPerdido("God, what a judgement" + N + "Is my punishment",
            "Deus, que julgamento." + N + "Esta é minha punição?"), "traducao com \\N nunca e suspeita");
        assertFalse(VersosDaLetra.suspeitaDeVersoPerdido("Do you feel alone", "Só?"),
            "letra de UM verso nunca e suspeita, por mais curta que seja a traducao");
    }
}
