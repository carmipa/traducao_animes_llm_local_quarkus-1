package org.traducao.projeto.traducao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a ênfase degenerada some da legenda sem que nenhuma letra do
 * texto se perca.
 *
 * <p>Todas as entradas abaixo são FALAS REAIS da saída do Guilty Crown, encontradas por revisão
 * adversarial em 08/08/2026 e medidas no acervo (305 arquivos): 16 ênfases vazias e 73 espaços
 * órfãos. O defeito é ANTERIOR às mudanças de 07-08/08 — a fala da "consideração" sai idêntica
 * na tradução antiga e na nova.
 */
class SaneadorEnfaseDegeneradaTest {

    /**
     * PROPÓSITO DE NEGÓCIO: o caso que abriu o assunto. O inglês enfatiza "consideration"; em
     * português a palavra muda de lugar e a ênfase fica em volta de nada.
     */
    @Test
    @DisplayName("enfase VAZIA some — fala real do ep05")
    void enfaseVaziaSome() {
        assertEquals("Não preciso da sua consideração. Entender?",
            SaneadorEnfaseDegenerada.sanear("Não preciso da sua consideração {\\i1}{\\i0}. Entender?"));
    }

    @Test
    @DisplayName("espaco orfao antes da pontuacao some, e a enfase real FICA")
    void espacoOrfaoSome() {
        assertEquals("Você viu {\\i1}aquele quarto{\\i0}?",
            SaneadorEnfaseDegenerada.sanear("Você viu {\\i1}aquele quarto{\\i0} ?"));
        assertEquals("Não, {\\i1}sinto muito{\\i0}.",
            SaneadorEnfaseDegenerada.sanear("Não, {\\i1}sinto muito{\\i0} ."));
        assertEquals("Em que você está baseando {\\i1}esse{\\i0}?!",
            SaneadorEnfaseDegenerada.sanear("Em que você está baseando {\\i1}esse{\\i0} ?!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE, e o mais importante do arquivo. Ênfase LEGÍTIMA é do
     * fansub, não deste saneador — mexer nela seria destruir intenção de quem legendou.
     */
    @Test
    @DisplayName("enfase com conteudo real fica INTACTA")
    void enfaseLegitimaFicaIntacta() {
        for (String fala : new String[]{
            "The Apsaras {\\i1}will{\\i0} be completed.",
            "{\\i1}Eledore!",
            "{\\i1}Roger!{\\i0}",
            "Você viu {\\i1}aquele quarto{\\i0}?",
            "{\\b1}Nao se mexa!{\\b0}"}) {
            assertEquals(fala, SaneadorEnfaseDegenerada.sanear(fala),
                "fala com enfase legitima nao pode ser tocada: " + fala);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: tag que NÃO é ênfase pode ter par vazio legítimo — posição, cor e
     * fonte mudam o que se vê na tela, e um bloco vazio ali pode ser intencional do typesetter.
     */
    @Test
    @DisplayName("tag de posicao/cor NUNCA e tocada, mesmo vazia")
    void tagDePosicaoNuncaEhTocada() {
        assertEquals("{\\pos(960,50)}{\\an8}Texto",
            SaneadorEnfaseDegenerada.sanear("{\\pos(960,50)}{\\an8}Texto"));
        assertEquals("{\\3c&HFF&}{\\1c&H00&}Cartaz",
            SaneadorEnfaseDegenerada.sanear("{\\3c&HFF&}{\\1c&H00&}Cartaz"));
    }

    @Test
    @DisplayName("negrito, sublinhado e riscado tambem sao saneados")
    void outrosParesDeEnfase() {
        assertEquals("Texto.", SaneadorEnfaseDegenerada.sanear("Texto {\\b1}{\\b0}."));
        assertEquals("Texto.", SaneadorEnfaseDegenerada.sanear("Texto {\\u1}{\\u0}."));
        assertEquals("Texto.", SaneadorEnfaseDegenerada.sanear("Texto {\\s1}{\\s0}."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: nenhuma LETRA pode se perder. Este é o invariante que separa
     * saneamento de mutilação.
     */
    @Test
    @DisplayName("INVARIANTE: nenhuma letra do texto visivel se perde")
    void nenhumaLetraSePerde() {
        for (String fala : new String[]{
            "Não preciso da sua consideração {\\i1}{\\i0}. Entender?",
            "Você viu {\\i1}aquele quarto{\\i0} ?",
            "Sim. Teremos um jantar saboroso enquanto você me conta toda a história do {\\i1}{\\i0} .",
            "Mas eles também não me ouvem {\\i1}{\\i0} —"}) {
            String antes = fala.replaceAll("\\{[^}]*\\}", "").replaceAll("\\s+", "");
            String depois = SaneadorEnfaseDegenerada.sanear(fala)
                .replaceAll("\\{[^}]*\\}", "").replaceAll("\\s+", "");
            assertEquals(antes, depois, "letra perdida ao sanear: " + fala);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o corretor pode rodar duas vezes sobre a mesma pasta; a segunda
     * passada não pode alterar nada.
     */
    @Test
    @DisplayName("idempotente: sanear duas vezes da o mesmo resultado")
    void idempotente() {
        for (String fala : new String[]{
            "Não preciso da sua consideração {\\i1}{\\i0}. Entender?",
            "Você viu {\\i1}aquele quarto{\\i0} ?",
            "{\\i1}Roger!{\\i0}"}) {
            String uma = SaneadorEnfaseDegenerada.sanear(fala);
            assertEquals(uma, SaneadorEnfaseDegenerada.sanear(uma), "nao e idempotente: " + fala);
        }
    }

    @Test
    @DisplayName("FALHA FECHADA: nulo e branco voltam como vieram")
    void falhaFechada() {
        assertEquals(null, SaneadorEnfaseDegenerada.sanear(null));
        assertEquals("   ", SaneadorEnfaseDegenerada.sanear("   "));
        assertEquals("{\\i1}{\\i0}", SaneadorEnfaseDegenerada.sanear("{\\i1}{\\i0}"),
            "fala que SO tem o par vazio ficaria vazia: devolve a entrada, nunca publica nada");
    }
}
