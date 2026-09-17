package org.traducao.projeto.revisaoLore.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.ResultadoDeteccaoLore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: um DESCRITOR comum do inglês localizado corretamente ({@code City} →
 * {@code Cidade}, {@code Operation} → {@code Operação}) com o nome ao lado preservado NÃO é defeito
 * de lore — e a tela 3.2 deixou de acusá-lo. Vale também para a sigla com/sem pontos
 * ({@code A.E.U.G.} ≡ {@code AEUG}).
 *
 * <h2>O prejuízo, medido na corrida do Zeta Gundam em 17/09/2026</h2>
 * Das 394 falas pendentes, 100% vinham da regra de nome próprio, e a maioria era descritor comum
 * traduzido certo: {@code Gate}→{@code Porta} (54), {@code Colony}→{@code Colônia} (46),
 * {@code City}→{@code Cidade} (32), {@code Operation}→{@code Operação} (28),
 * {@code Director}→{@code Diretor} (24). E o maior ofensor único era a sigla {@code A.E.U.G.} (72),
 * escrita {@code AEUG} sem os pontos. Esse ruído afogava o achado REAL do mesmo lote —
 * {@code Jupitris}→{@code Júpiter}, {@code Bosnia}→{@code Bósnia}.
 *
 * <h2>Invariantes do domínio</h2>
 * Os contra-testes são a metade que importa (regra A1): a fronteira só está calibrada se a MESMA
 * forma superficial — descritor localizado — for aceita quando o nome sobrevive e RECUSADA quando o
 * nome foi trocado. Silenciar a regra inteira passaria nos testes positivos e cegaria a tela.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem diz se a tela ficou tagarela (voltou a acusar tradução correta) ou surda (deixou de
 * ver nome de verdade trocado).
 */
class DescritorLocalizadoCalaAcusacaoTest {

    private final DetectorTermosLoreService detector = new DetectorTermosLoreService();

    @Test
    @DisplayName("city->cidade: 'Von Braun City' -> 'Cidade Von Braun' deixa de ser acusado")
    void cityLocalizadaCalaAAcusacao() {
        ResultadoDeteccaoLore r = detector.auditar(
            "The fleet is headed for Von Braun City.",
            "A frota esta rumando para a Cidade Von Braun.");

        assertFalse(r.suspeito(), () ->
            "descritor 'City' traduzido para 'Cidade' com o nome 'Von Braun' preservado nao e "
                + "defeito de lore. No Zeta valia 32 acusacoes. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: 'Von Braun City' com o NOME trocado continua acusado")
    void cityNaoCalaNomeTrocado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "The fleet is headed for Von Braun City.",
            "A frota esta rumando para a Cidade Von Brown.");

        assertTrue(r.suspeito(),
            "o descritor 'City' foi localizado, mas 'Braun' virou 'Brown' — nome trocado tem de "
                + "continuar acusando. A regra cala o descritor, nao a fala inteira.");
    }

    @Test
    @DisplayName("colony->colonia: 'Colony 30' no meio da fala deixa de ser acusado")
    void colonyLocalizadaCalaAAcusacao() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Side 1's Colony 30 held a rally against the Earth Federation.",
            "A Colonia 30 realizou um comicio contra a Federacao da Terra.");

        assertFalse(r.suspeito(), () ->
            "'Colony' traduzido para 'Colonia' nao e defeito de lore. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("operation->operacao: 'Operation Maelstrom' -> 'Operacao Maelstrom' cala")
    void operationLocalizadaCalaAAcusacao() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Bright commences Operation Maelstrom soon.",
            "Bright inicia Operacao Maelstrom em breve.");

        assertFalse(r.suspeito(), () ->
            "'Operation' traduzido para 'Operacao' com 'Maelstrom' preservado. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: 'Operation Maelstrom' com o NOME traduzido continua acusado")
    void operationNaoCalaNomeTraduzido() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Bright commences Operation Maelstrom soon.",
            "Bright inicia Operacao Tempestade em breve.");

        assertTrue(r.suspeito(),
            "'Maelstrom' virou 'Tempestade' — nome de operacao traduzido tem de continuar acusando.");
    }

    @Test
    @DisplayName("director->diretor: 'Director Hayato' -> 'Diretor Hayato' cala")
    void directorLocalizadoCalaAAcusacao() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Oh, Director Hayato!",
            "Oh, Diretor Hayato!");

        assertFalse(r.suspeito(), () ->
            "'Director' traduzido para 'Diretor' com 'Hayato' preservado. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: 'Director Hayato' com o NOME trocado continua acusado")
    void directorNaoCalaNomeTrocado() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Oh, Director Hayato!",
            "Oh, Diretor Kai!");

        assertTrue(r.suspeito(),
            "'Hayato' virou 'Kai' — nome trocado tem de continuar acusando mesmo com o titulo "
                + "'Director' localizado.");
    }

    @Test
    @DisplayName("laboratory->laboratorio: 'Murasame Laboratory' -> 'Laboratorio Murasame' cala")
    void laboratoryLocalizadoCalaAAcusacao() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Tell the Murasame Laboratory to watch for activities.",
            "Informe o Laboratorio Murasame para monitorar atividades.");

        assertFalse(r.suspeito(), () ->
            "'Laboratory' traduzido para 'Laboratorio' com 'Murasame' preservado. Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("sigla com/sem pontos: 'A.E.U.G.' preservada como 'AEUG' deixa de ser acusada")
    void siglaComPontosPreservadaSemPontosCala() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Have you piloted the A.E.U.G.'s new mobile suit before?",
            "Voce ja pilotou o novo Mobile Suit da AEUG antes?");

        assertFalse(r.suspeito(), () ->
            "'A.E.U.G.' e 'AEUG' sao a mesma sigla; os pontos sao estilo, nao lore. Era o maior "
                + "ofensor unico do Zeta (72). Motivos: " + r.motivos());
    }

    @Test
    @DisplayName("CONTRA-TESTE: sigla realmente ausente do PT continua acusada")
    void siglaAusenteContinuaAcusada() {
        ResultadoDeteccaoLore r = detector.auditar(
            "Have you piloted the A.E.U.G.'s new mobile suit before?",
            "Voce ja pilotou o novo Mobile Suit da ESFF antes?");

        assertTrue(r.suspeito(),
            "a sigla 'A.E.U.G.' nao aparece no PT nem com nem sem pontos ('ESFF' e outra coisa) — "
                + "tem de continuar acusando. A normalizacao de pontos nao cega a comparacao.");
    }
}
