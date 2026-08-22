package org.traducao.projeto.qualidadeTraducao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: o português usa dois-pontos onde o inglês usa vírgula, e isso não é
 * falante inventado.
 *
 * <h2>O prejuízo que originou</h2>
 * Retradução completa de 2026-08-21: das 102 pendências, 11 foram desta regra e as 11 eram
 * tradução CORRETA recusada. Recusada a tradução, a fala volta para o INGLÊS na legenda final.
 *
 * <h2>A medição que autorizou o desenho</h2>
 * Todo o histórico de recusas desta regra, 112 pares distintos, separado pelo número de
 * palavras antes dos dois-pontos — e a separação é limpa nos dois sentidos:
 * <pre>
 *   3+ palavras   38 pares / 25 prefixos   SEM EXCECAO oração portuguesa legítima
 *   1-2 palavras  74 pares / 44 prefixos   SEM EXCECAO nome de personagem ou rótulo
 * </pre>
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Prefixo com três ou mais palavras é oração, não rótulo — não caracteriza invenção.</li>
 *   <li>Rótulo de uma ou duas palavras continua recusado, inclusive os que imitam discurso
 *       relatado ({@code "Haruhime disse:"}, nascido de {@code "Haruhime View"}).</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar num caso doente devolve inglês à legenda. Reprovar num caso-controle é pior: a
 * narração inventada volta a passar, que é o dano que esta regra existe para impedir.
 */
@QuarkusTest
@DisplayName("oração não é rótulo de falante")
class OracaoNaoEhRotuloDeFalanteTest {

    @Inject
    ValidadorTraducaoService validador;

    /** As falas REAIS recusadas em 21/08, byte a byte como o log as registrou. */
    @Test
    @DisplayName("CASO DOENTE: as falas de 21/08 passam a ser aceitas")
    void asFalasDeVinteUmDeAgostoPassam() {
        assertDoesNotThrow(() -> validador.validarPar(
            "Then you say \"Please, help yourself.\"", "Então você diz: \"Por favor, sirva-se.\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "The question is, do you believe in your good luck?",
            "A questão é: você acredita na sua boa sorte?"));
        assertDoesNotThrow(() -> validador.validarPar(
            "The question is, when do we take the ZZ.", "A questão é: quando tomaremos o ZZ?"));
        assertDoesNotThrow(() -> validador.validarPar(
            "We'll make it clear that the Titans aren't the only force around.",
            "Nao vamos deixar duvidas: os Titans nao sao a unica forca em jogo."));
        assertDoesNotThrow(() -> validador.validarPar(
            "I tell them, \"Sorry, but no.\"", "Eu digo a eles: \"Desculpe, mas nao.\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "And you ask, \"Is Leina here?\"", "E você pergunta: \"Leina está aqui?\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "Remember this, \"A bird in a cage is nothing but a tool.\"",
            "Lembre-se disso: \"Um pássaro em uma gaiola nao passa de uma ferramenta.\""));
    }

    /**
     * O CASO-CONTROLE, e ele é o que impede a correção de virar buraco: rótulo de uma ou duas
     * palavras continua sendo recusado. Todos abaixo saíram do histórico real.
     */
    @Test
    @DisplayName("CASO SÃO: rótulo de falante continua recusado")
    void rotuloDeFalanteContinuaRecusado() {
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Fire the cannon now!", "Narrador: Dispare o canhão agora!"));
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Done!", "Linha 1: Pronto!"));
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Haruhime View", "\"Haruhime disse:\""),
            "invencao de discurso relatado com 2 palavras tem de continuar recusada");
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Where's Syr?", "Bell: Onde está a Syr?"));
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Get going.", "Hestia: Vá logo."));
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Critical energy!", "Gryps 2: Energia crítica!"),
            "nome composto de duas palavras tambem e rotulo");
    }

    /**
     * O LIMITE CONHECIDO, escrito para não virar surpresa: {@code "Rygart exclamou:"} tem duas
     * palavras e é tradução correta de {@code "Rygart boasted,"}, então continua recusado. Não
     * dá para liberar pela forma — {@code "Haruhime disse:"} é idêntico em forma e é invenção.
     * Um falso-positivo em 112 pares medidos.
     */
    @Test
    @DisplayName("limite declarado: nome + verbo de elocução com 2 palavras ainda cai")
    void oLimiteQuePermanece() {
        assertThrows(RuntimeException.class, () -> validador.validarPar(
                "Rygart boasted, \"Tonight's the night!\"", "Rygart exclamou: \"Esta é a noite!\""),
            "se este teste mudar, a regra passou a aceitar nome+verbo — e a medicao que "
                + "autoriza precisa vir junto, porque 'Haruhime disse' tem a mesma forma");
    }
}
