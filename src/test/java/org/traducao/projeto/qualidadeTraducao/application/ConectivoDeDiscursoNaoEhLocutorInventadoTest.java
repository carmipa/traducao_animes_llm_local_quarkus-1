package org.traducao.projeto.qualidadeTraducao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: impede que tradução CORRETA de conectivo de discurso seja recusada como
 * "locutor inventado" e vire pendência.
 *
 * <h2>O prejuízo medido</h2>
 * Na tradução do 86 de 14/08/2026, dois episódios ficaram {@code .parcial} por causa disto, e as
 * duas falas estavam certas:
 * <pre>
 *   ep 07  EN "She means, ..."           PT "Ela quer dizer: \"Depois que fomos mortos.\""
 *   ep 06  EN "She greeted him with..."  PT "Ela o saudara com: \"E uma manha esplendida...\""
 * </pre>
 * Duas causas somadas: o verbo composto {@code quer dizer} não estava na lista de conectivos, e
 * o pronome oblíquo em {@code Ela O saudara} quebrava o padrão, que só previa sujeito colado ao
 * verbo.
 *
 * <p>Efeito prático: episódio inteiro não recebe o arquivo final, porque tradução parcial não
 * vira entrega. Duas falas legítimas bloquearam dois episódios.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Conectivo de discurso presente no original NÃO é locutor inventado.</li>
 *   <li>Locutor de fato inventado CONTINUA sendo recusado — a lista poupa conectivo conhecido,
 *       não qualquer coisa antes de dois-pontos.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar aqui significa que uma das duas classes voltou: fala boa virando pendência, ou
 * narração inventada passando para a legenda.
 */
@QuarkusTest
@DisplayName("conectivo de discurso não é locutor inventado")
class ConectivoDeDiscursoNaoEhLocutorInventadoTest {

    @Inject
    ValidadorTraducaoService validador;

    /** As duas falas REAIS que bloquearam os episódios 06 e 07 do 86. */
    @Test
    @DisplayName("CASO DOENTE: as duas falas do 86 que viraram pendência")
    void asDuasFalasDo86SaoAceitas() {
        assertDoesNotThrow(() -> validador.validarPar(
                "She means, \"After we were killed.\"",
                "Ela quer dizer: \"Depois que fomos mortos.\""),
            "'Ela quer dizer:' foi recusado como locutor inventado — é tradução correta de "
                + "'She means' e bloqueou o episódio 07 do 86");
    }

    /**
     * O LIMITE CAIU EM 2026-08-21, e caiu do jeito que este teste exigia.
     *
     * <p>A versao anterior congelava {@code "Ela o saudara com: ..."} como RECUSADO e dizia:
     * <i>"alargar exige medir antes quantas falas do acervo o padrao passaria a poupar e
     * quantas narracoes inventadas escapariam junto"</i>. A medicao foi feita sobre TODO o
     * historico de recusas da regra — 112 pares distintos — e a separacao por numero de
     * palavras antes dos dois-pontos veio limpa nos dois sentidos:
     * <pre>
     *   3+ palavras   38 pares / 25 prefixos   SEM EXCECAO oracao portuguesa legitima
     *   1-2 palavras  74 pares / 44 prefixos   SEM EXCECAO nome de personagem ou rotulo
     * </pre>
     * O complemento entre verbo e dois-pontos ({@code com}) deixou de importar: o que decide
     * agora e o prefixo ser oracao. O caso-controle contra o afrouxamento que este teste temia
     * — {@code "Narrador:"} passar — esta logo abaixo e continua verde.
     *
     * <p>Detalhe do acervo: {@code Ela o saudara com} tem QUATRO palavras.
     */
    @Test
    @DisplayName("o limite caiu: complemento entre verbo e dois-pontos deixou de recusar")
    void oLimiteQueCaiu() {
        assertDoesNotThrow(() -> validador.validarPar(
                "She greeted him with, \"It's a splendid morning for you!\"",
                "Ela o saudara com: \"É uma manhã esplêndida para você!\""),
            "a fala que manteve o episodio 06 do 86 parcial desde 14/08 tem de passar agora");
    }

    /** Os conectivos que já eram aceitos continuam aceitos. */
    @Test
    @DisplayName("os conectivos antigos continuam passando")
    void conectivosAntigosContinuam() {
        assertDoesNotThrow(() -> validador.validarPar(
            "They said \"Now we're reborn.\"",
            "Eles disseram: \"Agora nascemos de novo.\""));
        assertDoesNotThrow(() -> validador.validarPar(
            "He said, \"That can wait till later!\"",
            "Ele disse: \"Isso pode esperar!\""));
    }

    /**
     * O CASO-CONTROLE: locutor de fato inventado continua recusado. Sem este assert, a correção
     * teria virado cegueira — o detector existe porque o modelo às vezes narra quem fala em vez
     * de traduzir a fala.
     */
    @Test
    @DisplayName("CASO SÃO: locutor inventado de verdade CONTINUA sendo recusado")
    void locutorInventadoContinuaRecusado() {
        assertThrows(RuntimeException.class, () -> validador.validarPar("Done!", "Linha 1: Pronto!"),
            "locutor inventado passou — o detector ficou cego e narração falsa iria para a "
                + "legenda");
        assertThrows(RuntimeException.class, () -> validador.validarPar(
                "Fire the cannon now!",
                "Narrador: Dispare o canhão agora!"),
            "'Narrador:' não existe no original e precisa continuar sendo recusado");
    }
}
