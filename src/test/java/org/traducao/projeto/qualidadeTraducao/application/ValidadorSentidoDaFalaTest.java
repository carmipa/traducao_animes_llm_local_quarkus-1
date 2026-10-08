package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova as três reprovações de SENTIDO que o portão de par ganhou em
 * 08/10/2026 — eco (inclusive com aspas que o normalizador tira depois), polaridade invertida e
 * pergunta perdida. Todas têm português limpo na saída; só o original prova o defeito.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada caso RUIM saiu do acervo real (caches de 08/10) ou da tradução
 * do zero de 08/10; cada caso BOM carrega o MESMO sinal do ruim (A1) — a mesma abertura "No", o
 * mesmo "?", a mesma fala idêntica — e tem de passar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: reprovação de um caso BOM é falso positivo (a fala iria para
 * nova tentativa e, esgotada, sairia em inglês); aprovação de um RUIM é a regressão do defeito.
 */
class ValidadorSentidoDaFalaTest {

    private final ValidadorTraducaoService validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

    @ParameterizedTest(name = "[{index}] RUIM polaridade: {0} -> {1}")
    @DisplayName("polaridade invertida: o original diz não e a legenda diz sim — reprova")
    @CsvSource(delimiter = '|', value = {
        "No.|Sim.",
        "No... it's too late for that. It's selfish.|Sim... é tarde demais para isso. É egoísta.",
        "No... There are still men who even now call it that.|Sim... Ainda há homens que até agora a chamam assim.",
        "No, I'd also like to believe it.|Sim, também gostaria de acreditar nisso.",
        "N-No.|S-Sim."
    })
    void polaridadeInvertidaReprova(String original, String traduzido) {
        AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar(original, traduzido));
        assertTrue(e.getMessage().startsWith("Polaridade invertida"), e.getMessage());
    }

    @ParameterizedTest(name = "[{index}] BOM polaridade: {0} -> {1}")
    @DisplayName("A1: mesma abertura negativa, resposta certa — passa")
    @CsvSource(delimiter = '|', value = {
        "No.|Não.",
        "No way!|Claro que não!",
        "No problem.|Claro, sem problema.",
        "No, I'd also like to believe it.|Não, eu também gostaria de acreditar nisso.",
        "Nope.|Nem pensar.",
        "Nobody came.|Ninguém veio.",
        "Not yet.|Certo, ainda não."
    })
    void respostaNegativaCertaPassa(String original, String traduzido) {
        assertDoesNotThrow(() -> validador.validarPar(original, traduzido));
    }

    @ParameterizedTest(name = "[{index}] RUIM pergunta: {0} -> {1}")
    @DisplayName("pergunta perdida: o original pergunta e a legenda afirma — reprova")
    // a aspa simples e o caractere de citacao padrao do CsvSource; "'Our\"?" e dado real do 0083
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "A rat?!|Que porco colorido!",
        "Ensign Reccoa is?|Reccoa é uma tenente.",
        "'Our\"?|Nosso",
        "\"His\"?|Dele.",
        "3:30? Only 30 minutes left!|Ainda faltam apenas 30 minutos!",
        "What good will creating Cyber-Newtypes do anyway?!|Criar Cyber-Newtypes só trará mais sofrimento!",
        "A date? Be glad to!|Que bom, sim!"
    })
    void perguntaPerdidaReprova(String original, String traduzido) {
        AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar(original, traduzido));
        assertTrue(e.getMessage().startsWith("Pergunta perdida"), e.getMessage());
    }

    @ParameterizedTest(name = "[{index}] BOM pergunta: {0} -> {1}")
    @DisplayName("A1: o mesmo \"?\" no original, PT legítimo sem interrogação ou com ela — passa")
    @CsvSource(delimiter = '|', value = {
        "A rat?!|Um rato?!",
        "Better we take it apart than strangers do, right?|É melhor nós desmontarmos do que estranhos.",
        "You can't criticize me for using Mineva Zabi anymore, can you, Judau?|Você não pode mais me criticar, Judau.",
        "Didn't I tell you before that I've nothing to say to you?!|Eu já te disse que não tenho nada a dizer para você!",
        "Would you calm down?!|Acalme-se!",
        "Why are you allied with a Frankish warship?!|Por que você está aliado a um navio franco!",
        "I wonder if she made it to Earth all right?|Eu me pergunto se ela chegou à Terra bem.",
        "I suppose that means I'm not a fit commander?|Suponho que isso signifique que não sou um comandante à altura.",
        "You're an accomplice, remember?!|Você é cúmplice, lembre-se!"
    })
    void perguntaVertidaLegitimamentePassa(String original, String traduzido) {
        assertDoesNotThrow(() -> validador.validarPar(original, traduzido));
    }

    @Test
    @DisplayName("eco com aspas: '\"Damn it!\"' é o inglês que o normalizador publicaria — reprova na tentativa")
    void ecoEnvoltoEmAspasReprova() {
        for (String eco : new String[]{"\"Damn it!\"", "Damn it!", "“Damn it!”"}) {
            AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
                () -> validador.validarPar("Damn it!", eco), eco);
            assertTrue(e.getMessage().startsWith("modelo devolveu o texto original sem tradução"), e.getMessage());
        }
    }

    @Test
    @DisplayName("A1 do eco: nome devolvido igual (com ou sem aspas) e fonte que já citava — passam")
    void identicaLegitimaENaoEcoPassam() {
        assertDoesNotThrow(() -> validador.validarPar("Kamille!", "\"Kamille!\""));
        assertDoesNotThrow(() -> validador.validarPar("Kamille!", "Kamille!"));
        assertDoesNotThrow(() -> validador.validarPar("Damn it!", "Droga!"));
        assertDoesNotThrow(() -> validador.validarPar("\"Deliberately\"?", "\"Deliberadamente\"?"));
    }

    /** Os pares de REGISTRO SOCIAL declarados em 08/10 na lore do 0083 e do 86, com os casos medidos. */
    private final ValidadorTraducaoService comRegistro = new ValidadorTraducaoService(LoreAtivaFake.comPares(
        java.util.List.of("Uraki", "Kou"), java.util.List.of("Keith", "Chuck"), java.util.List.of("Handler One", "Lena")));

    @ParameterizedTest(name = "[{index}] registro trocado: {0} -> {1} (reparo: {2})")
    @DisplayName("registro social trocado: reprova e o reparo devolve o termo do original")
    @CsvSource(delimiter = '|', value = {
        "Uraki, you're with me.|Kou, você está comigo.|Uraki, você está comigo.",
        "Keith! Uraki!|Keith! Kou!|Keith! Uraki!",
        "Give it up, Keith.|Desista, Chuck.|Desista, Keith.",
        "Handler One to Pleiades:|Lena para Plêiades:|Handler One para Plêiades:",
        "Handler One.|Lena.|Handler One."
    })
    void registroSocialTrocadoReprovaERepara(String original, String traduzido, String reparadoEsperado) {
        AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
            () -> comRegistro.validarPar(original, traduzido));
        assertTrue(e.getMessage().startsWith("Entidade trocada"), e.getMessage());
        org.junit.jupiter.api.Assertions.assertEquals(reparadoEsperado,
            comRegistro.repararTrocaDeEntidade(original, traduzido));
    }

    @ParameterizedTest(name = "[{index}] registro preservado: {0} -> {1}")
    @DisplayName("A1 do registro: o mesmo nome no original, tratamento preservado — passa")
    @CsvSource(delimiter = '|', value = {
        "Kou!|Kou!",
        "Uraki! Above you!|Uraki! Acima de você!",
        "Kou Uraki, reporting.|Kou Uraki, apresentando-se.",
        "Handler One to Undertaker:|Lena (Handler One) para Shin (Undertaker):",
        "Thank you, Nina.|Obrigado, Nina."
    })
    void registroPreservadoPassa(String original, String traduzido) {
        assertDoesNotThrow(() -> comRegistro.validarPar(original, traduzido));
    }

    @Test
    @DisplayName("detector de eco: o envelope só sai quando a FONTE não citava")
    void envelopeDeAspasSoSaiQuandoAFonteNaoCitava() {
        DetectorTraducaoIdenticaService detector = new DetectorTraducaoIdenticaService(LoreAtivaFake.vazia());
        assertTrue(detector.pareceNaoTraduzida("Damn it!", "\"Damn it!\""));
        assertTrue(detector.pareceNaoTraduzida("Damn it!", "“Damn it!”"));
        // fonte já entre aspas: a comparação é das falas com aspas, como sempre foi
        assertTrue(detector.pareceNaoTraduzida("\"Damn it!\"", "\"Damn it!\""));
        // duas citações não são envelope: não se mexe
        assertFalse(detector.pareceNaoTraduzida("Damn it! Damn it!", "\"Damn\" it! \"Damn\" it!"));
    }
}
