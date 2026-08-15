package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: congela as correções mineradas do acervo em 2026-08-04 — o nome
 * <b>Fa Yuiry</b> e o termo <b>Mobile Suit</b> —, e sobretudo congela o que elas NÃO podem tocar.
 *
 * <h2>Como estas entradas nasceram</h2>
 * Mineração sobre 61.051 falas do cache, e não observação avulsa. A fala cujo texto visível
 * INTEIRO é {@code "Fa"} aparece 51 vezes (Zeta 43, ZZ 8) e só 8 preservaram o nome; a que é
 * {@code "Mobile Suit"} aparece 149 (ZZ 95, Zeta 54), a maioria em CARTÃO DE TÍTULO.
 *
 * <p>Ambos os termos <b>já estavam</b> em {@code termosProtegidos()} e foram corrompidos assim
 * mesmo — aquele conjunto isenta da checagem de resíduo, não restaura grafia. Quem restaura é
 * {@code correcoesTerminologia}, e é isso que estes testes exercitam.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A restauração é CONDICIONAL: só age quando o ORIGINAL em inglês traz o canônico. Um
 *       {@code "Fire!"} qualquer nunca é tocado, porque não traz "Fa" no inglês.</li>
 *   <li>Canônico de UMA palavra é comparado com sensibilidade à CAIXA — {@code "fa"} minúsculo
 *       não dispara nada.</li>
 *   <li>{@code "Móvel de Combate"} continua INTOCADO. É decisão registrada do dono do acervo
 *       (aceitável por contexto, junto de "unidade móvel"), reconfirmada em 04/08 ao aprovar as
 *       outras quatro. Um teste que só provasse as correções deixaria essa exclusão sem rede.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falhar nas correções significa nome de personagem virando substantivo comum na legenda.
 * Falhar na exclusão significa reescrever tradução que o dono do acervo declarou legítima.
 */
class CorrecaoFaEMobileSuitTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();

    private static final Map<String, String> ZZ = org.traducao.projeto.lore.LoreDeTeste.terminologia("gundam_zz");
    private static final Map<String, String> ZETA = org.traducao.projeto.lore.LoreDeTeste.obra("gundam_zeta").correcoesTerminologia();

    @Test
    @DisplayName("Fa: o nome e restaurado nas cinco formas medidas")
    void faERestauradoNasCincoFormas() {
        assertEquals("Fa!", enforcador.reforcar("Fa!", "Fogo!", ZETA), "27 falas no acervo");
        assertEquals("Fa!", enforcador.reforcar("Fa!", "Fala!", ZETA), "7 falas");
        assertEquals("Fa...", enforcador.reforcar("Fa...", "Fá...", ZETA), "6 falas");
        assertEquals("Fa!", enforcador.reforcar("Fa!", "Pá!", ZETA), "1 fala");
        assertEquals("Fa!", enforcador.reforcar("Fa!", "Fale!", ZETA), "1 fala");
    }

    @Test
    @DisplayName("Fa: restaurado tambem dentro de fala MAIOR — os casos de colisao medidos")
    void faERestauradoEmFalaMaior() {
        assertEquals("Fa, Katz!", enforcador.reforcar("Fa! Katz!", "Fala, Katz!", ZETA));
        assertEquals("Fa, decolando!", enforcador.reforcar("Fa, taking off!", "Fá, decolando!", ZETA));
        assertEquals("Fa! Estamos em menor número!",
            enforcador.reforcar("Fa! We're outnumbered!", "Fogo! Estamos em menor número!", ZETA));
    }

    @Test
    @DisplayName("Fa: fogo de VERDADE nao e tocado — a condicao e o ingles")
    void fogoLegitimoNaoEToccado() {
        assertEquals("Fogo!", enforcador.reforcar("Fire!", "Fogo!", ZETA),
            "o ingles nao traz 'Fa': a restauracao nem chega a ser tentada");
        assertEquals("Abram fogo!", enforcador.reforcar("Open fire!", "Abram fogo!", ZETA));
        assertEquals("Cessar fogo!", enforcador.reforcar("Cease fire!", "Cessar fogo!", ZZ));
    }

    @Test
    @DisplayName("Mobile Suit: as quatro formas-ruim aprovadas sao restauradas")
    void mobileSuitERestaurado() {
        assertEquals("Mobile Suit",
            enforcador.reforcar("MOBILE SUIT", "MÓVEL DE ASSALTO", ZZ), "74 falas no acervo");
        assertEquals("Mobile Suit",
            enforcador.reforcar("MOBILE SUIT", "Móvel de Assento", ZZ), "31 falas");
        assertEquals("Mobile Suit",
            enforcador.reforcar("MOBILE SUIT", "MÓVEL DE GUERRA", ZZ), "2 falas");
        assertEquals("Mobile Suit",
            enforcador.reforcar("Mobile Suit", "Mobil Suit", ZETA), "2 falas");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a EXCLUSÃO tem tanto valor quanto a inclusão, e é mais frágil —
     * ninguém sente falta de uma regra que não existe.
     *
     * <p>INVARIANTES DO DOMÍNIO: {@code "Móvel de Combate"} e {@code "unidade móvel"} são
     * aceitáveis por contexto, por decisão do dono do acervo, e continuam passando mesmo com o
     * inglês trazendo o canônico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 101 falas do run de ZZ que o enforcer deveria deixar em
     * paz seriam reescritas.
     */
    @Test
    @DisplayName("unidade movel segue INTOCADA por decisao do dono do acervo")
    void exclusoesDeliberadasSeguemDeFora() {
        assertEquals("unidade móvel", enforcador.reforcar("mobile suit", "unidade móvel", ZZ),
            "decisao registrada em CorrecoesTerminologiaGundamUc: aceitavel por contexto");
        assertEquals("unidades móveis", enforcador.reforcar("mobile suits", "unidades móveis", ZZ));
        assertEquals("MS", enforcador.reforcar("Mobile Suit", "MS", ZETA),
            "MS e abreviacao oficial, nao forma-ruim");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: congela a REVISÃO de 05/08/2026 sobre {@code "Móvel de Combate"} —
     * incluindo o custo que ela cobra.
     *
     * <p>A exclusão anterior valia para DIÁLOGO e continua certa lá. O que mudou é que a mesma
     * forma aparece em <b>8 cartões de título</b>, onde o inglês vem {@code "MOBILE SUIT"} em
     * caixa alta e não há contexto que a salve: é o nome da franquia escrito errado na tela.
     * Separação medida no acervo: 8 cartões contra 1 fala de diálogo.
     *
     * <p>INVARIANTES DO DOMÍNIO: o cartão é restaurado; e a fala de diálogo PERDE o "de combate",
     * porque o inglês dela também traz {@code "combat mobile suit"} e a restauração a alcança.
     * O segundo caso está aqui com asserção justamente para o custo não virar surpresa depois.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o segundo assert quebrar, alguém tornou a regra
     * seletiva — o que é bom, e esta nota pode sair.
     */
    @Test
    @DisplayName("Movel de Combate: cartao restaurado, e a fala de dialogo paga o preco")
    void movelDeCombateEntraComCustoDeclarado() {
        assertEquals("Mobile Suit", enforcador.reforcar("MOBILE SUIT", "Móvel de Combate", ZZ),
            "cartao de titulo: o nome da franquia na tela");
        assertEquals("Meu Geze tem a mobilidade de um Mobile Suit!",
            enforcador.reforcar("My Geze has the mobility of a combat mobile suit!",
                "Meu Geze tem a mobilidade de um móvel de combate!", ZZ),
            "custo aceito: a unica fala de dialogo perde o \"de combate\"");
    }
}
