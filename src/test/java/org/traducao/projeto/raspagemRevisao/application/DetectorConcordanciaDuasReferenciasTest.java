package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: sela as regras de gênero contra falas com DUAS referências de gênero,
 * SEM perder a detecção que a correção quase levou junto.
 *
 * <h2>O defeito medido</h2>
 * A regra "original usa 'she' sem referência masculina, mas a tradução contém 'ele'" guardava-se
 * com {@code \bhe\b}, que não casa {@code "him"} nem {@code "his"}. A mensagem prometia "sem
 * referência masculina" e o código verificava só o pronome sujeito — mensagem e verificação
 * discordavam. Resultado gravado no cache em 2026-07-28 (Guilty Crown 07_Track4, evento 10):
 * <pre>
 *   EN  "Oh, I know about him!  Hiromi said she saw it all."
 *   PT  "Eu sei sobre ele! Hiromi disse que viu tudo."       (correto)
 *   ->  "Eu sei sobre ela! Hiromi disse que viu tudo."       (regressão persistida)
 * </pre>
 * A fala tem duas referências — {@code him} e {@code she} — e o detector enxergava uma só.
 *
 * <h2>O custo que a correção quase escondeu</h2>
 * Fechar o guarda para exigir ausência de QUALQUER referência masculina fazia sumir, junto com
 * seis falsos positivos, o ÚNICO acerto real da mesma rodada:
 * <pre>
 *   EN  "She's got a violent older brother, and I happened to know her, so..."
 *   PT  "Ele tem um irmão mais velho violento, ..."          <- "Ele" está errado
 * </pre>
 * {@code "brother"} ancora o masculino e cala a regra larga; {@code SUJEITO_ELE_COM_SHE} não
 * pega porque {@code "tem"} não está em {@code VERBOS_SUJEITO} (lista curada de propósito).
 * Trocar seis falsos positivos por um defeito real invisível não é conserto — daí a regra por
 * POSIÇÃO, que olha quem abre a fala.
 *
 * <h2>Medição</h2>
 * A regra nova de sujeito inicial dispara <b>0 vezes</b> nas 59.931 falas do acervo atual e
 * <b>1 vez</b> nas 98.027 falas dos backups — exatamente a fala defeituosa acima.
 *
 * <p>NOTA sobre uma hipótese descartada: cheguei a suspeitar que o {@code \N} da legenda colasse
 * na palavra seguinte e matasse a fronteira de {@code \bhe\b}. Não acontece —
 * {@code removerTagsAss} troca {@code \N} por espaço antes de qualquer regra. Casos que
 * dependiam disso não discriminam versão nenhuma e por isso não estão aqui.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada caso suprimido vem com o vizinho que precisa continuar
 * acusando. Os dois últimos testes não distinguem a versão antiga da nova — são guardas de
 * regressão, provando que fechar o guarda não matou a detecção legítima.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o detector devolve {@link ResultadoDeteccaoConcordancia}
 * sem lançar; suspeito/não-suspeito é o contrato observado.
 */
class DetectorConcordanciaDuasReferenciasTest {

    private final DetectorConcordanciaService detector = new DetectorConcordanciaService();

    @Test
    @DisplayName("'him' no original é referência masculina — 'ele' na tradução não é suspeito")
    void himNoOriginalAbsolveEleNaTraducao() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "Oh, I know about him!  Hiromi said she saw it all.",
            "Eu sei sobre ele! Hiromi disse que viu tudo.");

        assertFalse(r.suspeito(), "com 'him' no original, 'ele' está ancorado: " + r.motivos());
    }

    @Test
    @DisplayName("sujeito inicial trocado continua sendo pego, mesmo com âncora masculina na fala")
    void sujeitoInicialTrocadoContinuaSendoPego() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "She's got a violent older brother, and I happened to know her, so...",
            "Ele tem um irmão mais velho violento, e eu acabei conhecendo ela, então...");

        assertTrue(r.suspeito(), "sujeito trocado no início da fala é divergência objetiva");
        assertTrue(r.motivos().stream().anyMatch(m -> m.contains("sujeito trocado")),
            "o motivo deve apontar o sujeito, não a ausência de referência: " + r.motivos());
    }

    @Test
    @DisplayName("sujeito inicial coerente não dispara a regra de posição")
    void sujeitoInicialCoerenteNaoDispara() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "She's got a violent older brother, and I happened to know her, so...",
            "Ela tem um irmão mais velho violento, e eu acabei conhecendo ela, então...");

        assertFalse(r.suspeito(), "tradução correta não pode ser acusada: " + r.motivos());
    }

    /** Guarda de regressão: não distingue as versões, prova que a detecção legítima sobreviveu. */
    @Test
    @DisplayName("sem qualquer referência masculina, 'ele' continua sendo acusado")
    void semReferenciaMasculinaContinuaSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "She's got a violent older sister, and she knows it.",
            "Ele tem uma irmã mais velha violenta, e ela sabe disso.");

        assertTrue(r.suspeito(), "sem nenhuma âncora masculina, 'ele' deve continuar acusando");
    }

    /** Guarda de regressão do lado espelhado. */
    @Test
    @DisplayName("sem qualquer referência feminina, 'ela' continua sendo acusada")
    void semReferenciaFemininaContinuaSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "He fought his brother and he won.",
            "Ela lutou contra o irmão e ele venceu.");

        assertTrue(r.suspeito(), "sem nenhuma âncora feminina, 'ela' deve continuar acusando");
    }
}
