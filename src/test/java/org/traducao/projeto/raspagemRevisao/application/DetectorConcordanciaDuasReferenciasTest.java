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

    /**
     * PROPÓSITO DE NEGÓCIO: a MESMA guarda de referência masculina, agora também na regra de
     * SUJEITO — que era a única das três irmãs que não a tinha.
     *
     * <h2>O custo, medido em 12/08/2026</h2>
     * A Revisão de Legendas varreu as 5.620 falas do Gundam Unicorn e reportou 3 problemas.
     * <b>Os três eram falso positivo</b>, e este era o primeiro (E08, linha 238):
     * <pre>
     *   EN  "He's with her, so she'll be fine."
     *   PT  "Ele está com ela, então ela estará bem."      <- CORRETO
     * </pre>
     * O original tem {@code He's} E {@code her}; o detector via só o {@code she} e acusava o
     * "Ele está". A guarda que resolve já existia doze linhas abaixo, desde 28/07 — foi
     * aplicada a uma das três regras e esta ficou de fora.
     *
     * <p>Por que importa mais que um número: um relatório que aponta 3 problemas e erra nos 3
     * ensina a ignorar o relatório. O ruído não é neutro, ele desativa a ferramenta.
     */
    @Test
    @DisplayName("Unicorn E08: 'He's with her' absolve o 'Ele está' — os dois gêneros no original")
    void heEHerNoOriginalAbsolvemOSujeitoEle() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "He's with her,\\Nso she'll be fine.",
            "Ele está com ela,\\Nentão ela estará bem.");

        assertFalse(r.suspeito(),
            "tradução correta acusada: o original tem 'He's' e 'her', não só 'she' — " + r.motivos());
    }

    /**
     * A CONTRAPROVA da correção acima: sem âncora masculina no original, a regra de sujeito
     * continua acusando. Sem este par, "consertar o falso positivo" poderia ter sido apenas
     * desligar a regra — que é como um alarme falso vira alarme nenhum.
     */
    @Test
    @DisplayName("contraprova: 'she' sozinha no original mantém o sujeito 'ele' suspeito")
    void sheSozinhaMantemSujeitoEleSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "She is with the others, so she'll be fine.",
            "Ele está com os outros, então ela estará bem.");

        assertTrue(r.suspeito(),
            "sem referência masculina no original, o sujeito 'Ele está' tem de acusar");
    }

    /** Lado espelhado da correção, para a assimetria não virar dívida. */
    @Test
    @DisplayName("espelho: 'She's with him' absolve o 'Ela está' na tradução")
    void sheEHimNoOriginalAbsolvemOSujeitoEla() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "She's with him, so he'll be fine.",
            "Ela está com ele, então ele estará bem.");

        assertFalse(r.suspeito(),
            "tradução correta acusada no lado espelhado: " + r.motivos());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: predicado de 1ª/2ª pessoa concorda com quem FALA ou com quem OUVE,
     * e o inglês não marca nenhum dos dois. {@code "You're right"} não diz se o interlocutor é
     * homem ou mulher, então um {@code she} na mesma fala — que se refere a TERCEIRA pessoa —
     * não pode julgar esse predicado.
     *
     * <h2>O custo, medido em 12/08/2026</h2>
     * Unicorn E12, linha 200 — o terceiro dos 3 falsos positivos da varredura de 5.620 falas:
     * <pre>
     *   EN  "You're right, she is enslaved."
     *   PT  "Você está certo, ela está escravizada."     <- CORRETO
     * </pre>
     * O {@code escravizada} está no feminino, como deve. O acusado foi o {@code certo}, que
     * concorda com "você". A fala trouxe DOIS motivos, um de cada par de regras de predicado —
     * por isso o strip foi aplicado às QUATRO, e não só às duas primeiras.
     */
    @Test
    @DisplayName("Unicorn E12: 'Você está certo' não é julgado pelo 'she' de terceira pessoa")
    void predicadoDeSegundaPessoaNaoEhJulgadoPeloSheDeTerceira() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "You're right, she is enslaved.",
            "Você está certo, ela está escravizada.");

        assertFalse(r.suspeito(),
            "'certo' concorda com 'você', não com 'ela' — e 'escravizada' já está no feminino: "
                + r.motivos());
    }

    /**
     * A CONTRAPROVA: o strip tira da análise apenas o predicado de 1ª/2ª pessoa. Predicado de
     * TERCEIRA pessoa no gênero errado continua sendo acusado — sem isto, o conserto teria
     * desligado a regra em vez de afiná-la.
     */
    @Test
    @DisplayName("contraprova: predicado de TERCEIRA pessoa no masculino continua suspeito")
    void predicadoDeTerceiraPessoaContinuaSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar(
            "You're right, she is enslaved.",
            "Você está certo, ela está preocupado.");

        assertTrue(r.suspeito(),
            "'ela está preocupado' é discordância real de terceira pessoa e tem de acusar");
    }
}
