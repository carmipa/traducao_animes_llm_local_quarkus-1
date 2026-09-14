package org.traducao.projeto.lore.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mantém no prompt de tradução a instrução que impede posição dada em
 * horas de relógio de ser publicada como hora do dia. Sem esta catraca a instrução é uma linha
 * de texto que a próxima refatoração do template apaga sem deixar rastro — e o defeito volta
 * calado, porque nenhuma outra camada enxerga a diferença entre {@code "Two o'clock!"} traduzido
 * como {@code "2 horas!"} e como {@code "Meia-noite!"}: os dois são português impecável.
 *
 * <h2>O prejuízo MEDIDO no acervo PUBLICADO — 2026-09-14</h2>
 * 134 pares (original, publicado) casados pela chave completa do evento ASS, com 29 expressões
 * de relógio examinadas. Nove defeitos, cinco deles com o VALOR corrompido:
 * <pre>
 * 11 o'clock! Enemy has opened fire!      -> Meia-noite! O inimigo abriu fogo!   (onze -> doze)
 * Two o'clock! Two enemy vessels...       -> Meia-noite! Duas naves...           (dois -> doze)
 * Twelve midnight?                        -> Meia-noite e doze?                  (numero inventado)
 * It will commence at 2300 hours.         -> Comecara ao meio-dia.               (23h -> 12h)
 * Tomorrow at 2:00, I'll destroy...       -> Amanha, ao meio-dia, eu destruirei  (2h  -> 12h)
 * </pre>
 *
 * <h2>Por que a instrução, e não uma guarda que reprova</h2>
 * Guarda que reprova devolve a fala ao INGLÊS na legenda, e a decisão do projeto é legibilidade.
 * A medição no aya-expanse-8b (temperatura 0.3, 3 repetições, 58 casos, prompt de produção de
 * cada obra, sem cache) fechou assim:
 * <pre>
 *                                        sem a instrucao   com a instrucao
 * "Two o'clock!" com o valor preservado        0/3               3/3
 * "11 o'clock!" com o valor preservado         2/3               3/3
 * 13 rumos que JA estavam corretos            39/39             39/39
 *  7 horas do dia verdadeiras                  7/7               7/7
 * 20 falas SEM relogio que ganharam            0/60              0/60
 *    vocabulario de relogio (efeito ima)
 * </pre>
 * O último número é o que autoriza a adoção: a cláusula "Sieg Zeon" de 09/09/2026 injetou o
 * termo em 13 de 20 falas justamente por NOMEAR a forma indesejada. Esta instrução é escrita em
 * positivo e não cita {@code meia-noite}. A variante que citava foi medida e <b>reprovou</b>:
 * piorou o controle misto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A instrução vive no bloco de prioridades, que vale para TODAS as 69 obras — o defeito
 *       não é de obra nenhuma em particular, é de vocabulário militar.</li>
 *   <li>Escrita em positivo: não nomeia a forma errada, para não servir de ímã.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de asserção nomeando o que falta no prompt.
 */
@DisplayName("O prompt mantem a instrucao de rumo de relogio, e a mantem em positivo")
class InstrucaoRumoDeRelogioNoPromptTest {

    private static final String PROMPT = ContextoPrompt.montar("0083", "Lore de teste.");

    @Test
    @DisplayName("a instrucao esta presente, com as tres exigencias que a medicao cobrou")
    void instrucaoPresenteComAsTresExigencias() {
        assertTrue(PROMPT.contains("horas de relógio"),
            "o prompt perdeu a instrucao de rumo de relogio");
        assertTrue(PROMPT.contains("DIREÇÃO relativa"),
            "a instrucao tem de dizer que e direcao, nao hora do dia");
        assertTrue(PROMPT.contains("preserve o número"),
            "a exigencia do VALOR e a que pega os cinco casos de numero corrompido");
        assertTrue(PROMPT.contains("indicação de altura"),
            "'Twelve o'clock high' perde a altura sem esta parte");
        assertTrue(PROMPT.contains("somente quando o original estiver falando de horário"),
            "sem a ressalva, hora do dia verdadeira vira rumo");
    }

    @Test
    @DisplayName("CASO-CONTROLE: a instrucao NAO cita a forma errada, para nao virar ima")
    void instrucaoNaoNomeiaAFormaErrada() {
        String prioridades = PROMPT.substring(
            PROMPT.indexOf("Prioridades de tradução:"),
            PROMPT.indexOf("Lore e terminologia obrigatória:"));
        assertFalse(prioridades.toLowerCase().contains("meia-noite"),
            "citar 'meia-noite' no prompt e a forma do desastre da clausula Sieg Zeon: "
                + "13 injecoes em 20 falas. A variante que citava foi medida e reprovou.");
        assertFalse(prioridades.toLowerCase().contains("meio-dia"),
            "mesmo motivo: instrucao em positivo, sem exibir a forma indesejada");
    }

    @Test
    @DisplayName("a instrucao entra ANTES da lore, no bloco de prioridades")
    void instrucaoEntraNoBlocoDePrioridades() {
        int prioridades = PROMPT.indexOf("Prioridades de tradução:");
        int instrucao = PROMPT.indexOf("horas de relógio");
        int lore = PROMPT.indexOf("Lore e terminologia obrigatória:");
        assertTrue(prioridades < instrucao && instrucao < lore,
            "a instrucao tem de estar entre o cabecalho de prioridades e a lore, e ficou em "
                + instrucao + " (prioridades=" + prioridades + ", lore=" + lore + ")");
    }
}
