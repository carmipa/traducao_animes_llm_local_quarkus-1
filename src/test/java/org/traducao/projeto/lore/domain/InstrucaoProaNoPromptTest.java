package org.traducao.projeto.lore.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mantém no prompt a instrução que impede {@code Heading NNN} — a proa, o
 * rumo de navegação — de ser publicado como se fosse título ou cabeçalho de documento. É a classe
 * que <b>apaga informação</b> em vez de trocá-la, e por isso veio antes das outras no plano.
 *
 * <h2>O prejuízo MEDIDO no acervo PUBLICADO — 2026-09-14</h2>
 * <pre>
 * Heading 030. Four rooftop units...  -> Cabecalho 030. Quatro unidades no telhado...
 * Heading 060, distance 800.          -> Titulo 060, distancia 800.
 * Heading 2-8-0. Distance 5,000.      -> Distancia 5.000.        (o rumo SUMIU do arquivo)
 * </pre>
 *
 * <h2>O que a medição no modelo mostrou, e que muda a leitura do defeito</h2>
 * Rodando o prompt de produção no aya-expanse-8b (temperatura 0.3, 3 repetições, 73 casos, sem
 * cache), a classe é <b>viva</b> e maior do que o acervo mostrava:
 * <pre>
 *                                              sem a instrucao      com a instrucao
 * Heading 030 ...                              segmento APAGADO 3/3   "Rumo 030"      3/3
 * Heading 2-8-0. Distance 5,000.               "Titulo: 2-8-0"  3/3   "Direcao: 2-8-0" 3/3
 * Karius! How's our heading look?              "cabecalho"      3/3   "rumo/direcao"   2/3
 * 28 usos de "heading" como VERBO de movimento  intactos              intactos
 * 20 falas sem rumo (efeito ima)                 0/60                  0/60
 * 22 casos da classe irma (RESERVADOS)          inalterado            inalterado
 * </pre>
 * A terceira linha é a mais importante: {@code "Karius! How's our heading look?"} estava
 * <b>correto</b> no arquivo publicado ({@code "Como esta a nossa rota?"}) e o pipeline de hoje o
 * quebra. Ele entrou no conjunto como caso-controle e a medição o promoveu a defeito — o gabarito
 * veio do critério declarado antes de medir, não da vontade de aumentar o placar.
 *
 * <h2>Por que NÃO existe guarda nova nesta frente</h2>
 * Em {@code "Heading 060, distance 800."} a instrução faz o modelo <b>inventar</b> o rumo
 * ({@code "Direcao: 180 graus"}), que é erro fluente: plausível e falso. A guarda que fecha isso
 * <b>já existia</b> e foi consultada, não reescrita — o
 * {@code VerificadorIdentificadorNumerico} reprova com
 * {@code "sumiu: [060, 800], surgiu: [180]"}, a fala vira pendência e é publicada em inglês.
 * Falhar fechado aqui é melhor que publicar um rumo inventado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Vale para todas as obras: é vocabulário militar, não lore de nenhuma delas.</li>
 *   <li>Escrita em positivo. A variante que acrescentava "não é título nem cabeçalho" foi
 *       medida e <b>perdeu</b>: estragou o falante em 1 de 3 no primeiro caso.</li>
 *   <li>O texto aqui é o texto EXATO que foi medido, acento por acento. Medir a forma sem acento
 *       e entregar a forma com acento já se mostrou diferente neste projeto.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha de asserção nomeando o que falta no prompt.
 */
@DisplayName("O prompt mantem a instrucao de proa, e a mantem em positivo")
class InstrucaoProaNoPromptTest {

    private static final String PROMPT = ContextoPrompt.montar("0083", "Lore de teste.");

    @Test
    @DisplayName("a instrucao esta presente, com as duas exigencias que a medicao cobrou")
    void instrucaoPresenteComAsDuasExigencias() {
        assertTrue(PROMPT.contains("\"Heading\" seguida de número"),
            "o prompt perdeu a instrucao de proa");
        assertTrue(PROMPT.contains("PROA"),
            "a instrucao tem de nomear o que a palavra significa");
        assertTrue(PROMPT.contains("Preserve o número e traduza como indicação de rumo"),
            "sem a exigencia do NUMERO, 'Heading 2-8-0' perde o rumo e sobra so a distancia");
    }

    @Test
    @DisplayName("CASO-CONTROLE: a instrucao NAO cita a forma errada -- a variante que citava perdeu")
    void instrucaoNaoNomeiaAFormaErrada() {
        String prioridades = PROMPT.substring(
            PROMPT.indexOf("Prioridades de tradução:"),
            PROMPT.indexOf("Lore e terminologia obrigatória:"));
        assertFalse(prioridades.toLowerCase().contains("cabeçalho"),
            "a variante que dizia 'nao e titulo nem cabecalho' foi medida e estragou o falante");
        assertFalse(prioridades.toLowerCase().contains("título de documento"),
            "mesmo motivo: instrucao em positivo");
    }

    @Test
    @DisplayName("as duas instrucoes de rumo convivem: relogio e proa sao classes diferentes")
    void relogioEProaConvivem() {
        int relogio = PROMPT.indexOf("horas de relógio");
        int proa = PROMPT.indexOf("\"Heading\" seguida de número");
        int lore = PROMPT.indexOf("Lore e terminologia obrigatória:");
        assertTrue(relogio > 0 && proa > 0,
            "as duas instrucoes tem de existir: relogio=" + relogio + " proa=" + proa);
        assertTrue(proa < lore,
            "a instrucao de proa tem de ficar no bloco de prioridades, antes da lore");
    }
}
