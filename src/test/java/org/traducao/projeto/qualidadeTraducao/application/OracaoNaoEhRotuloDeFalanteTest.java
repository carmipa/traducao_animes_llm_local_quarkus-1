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
     * O LIMITE QUE CAIU (09/10/2026): {@code "Rygart exclamou:"} tem duas palavras e é tradução
     * correta de {@code "Rygart boasted,"}, e era recusado. Não dá para liberar pela forma —
     * {@code "Haruhime disse:"} é idêntico em forma e é invenção. O que separa é o original ter o
     * MESMO nome seguido de verbo de elocução. A medição que autorizou veio junto, como este teste
     * exigia: as 116 recusas reais do console, antes e depois ({@code MedicaoRecusaLocutorNoLogIT}).
     * Os dois lados da fronteira estão aqui, com o mesmo sinal (nome + verbo de elocução).
     */
    @Test
    @DisplayName("nome + verbo de elocução: aceito quando o original diz o mesmo, recusado quando inventa")
    void nomeEVerboDeElocucaoAncoradoNoOriginal() {
        assertDoesNotThrow(() -> validador.validarPar(
            "Rygart boasted, \"Tonight's the night!\"", "Rygart exclamou: \"Esta é a noite!\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "Chuchumy says welcome home.", "Chuchumy diz: \"Que bom que você voltou.\""),
            "Reconguista I, teste ponta a ponta de 09/10/2026: recusada, a fala voltava ao ingles");
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Haruhime View", "\"Haruhime disse:\""),
            "CONTROLE: o nome esta no original, o verbo nao — continua invencao");
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Where's Syr?", "Bell disse: Onde está a Syr?"),
            "CONTROLE: nem o nome esta no original");
        assertThrows(RuntimeException.class,
            () -> validador.validarPar("Chuchumy is here.", "Chuchumy diz: Estou aqui."),
            "CONTROLE: o nome esta no original sem verbo de elocucao — o 'diz' foi inventado");
    }
}
