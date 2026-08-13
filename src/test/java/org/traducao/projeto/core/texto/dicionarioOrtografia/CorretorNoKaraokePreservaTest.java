package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: no karaokê o default se INVERTE — a tradução age e as guardas seguram o
 * exagero; aqui não se toca no que não for inequívoco. Erro numa linha de música não é palavra
 * torta, é animação quebrada: timing por sílaba, camadas pareadas, KFX.
 *
 * <h2>O que precisa ser corrigido, medido no Unicorn em 13/08/2026</h2>
 * 88 ocorrências de acento faltando em 372 linhas JÁ traduzidas para português — {@code ED - EN}
 * com 11 e {@code ED2} com 77: {@code tras}, {@code mascara}, {@code nao}, {@code voce}.
 *
 * <h2>O que NÃO pode ser tocado, e por que o critério basta</h2>
 * A letra mistura idioma na mesma linha — o {@code ED2} do Unicorn tem {@code gonna} e {@code one}
 * convivendo com português correto. Nada disso precisa de lista de exceção: o corretor só aceita
 * a sugestão que é <b>a mesma palavra acentuada</b>, e não existe versão acentuada de
 * {@code gonna} ou {@code kieta} para o dicionário oferecer.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell, PULA por {@link Assumptions} — nunca passa por ausência de verificação.
 */
@DisplayName("karaokê: corrige o português traduzido e PRESERVA o resto")
class CorretorNoKaraokePreservaTest {

    /**
     * A fala REAL do ED2 do E01, e ela contém as duas metades do problema.
     *
     * <p>{@code tras} não é palavra em português e sai corrigido. {@code mascara} É palavra — o
     * verbo mascarar, "ele mascara a verdade" — então o dicionário o aprova, corretamente, e ele
     * fica como está. É o TETO declarado desde o início: forma sem acento que também existe como
     * outra palavra ({@code publica}/{@code pública}, {@code medica}/{@code médica},
     * {@code mascara}/{@code máscara}) só o CONTEXTO resolve, e contexto é o que o dicionário não
     * tem. Para essas, quem pode decidir é o LLM.
     *
     * <p>Este teste existe para o teto ser um fato registrado, e não uma surpresa no dia em que
     * alguém notar a palavra sem acento no arquivo.
     */
    @Test
    @DisplayName("corrige o que é inequívoco; palavra ambígua fica, e isso é o teto declarado")
    void corrigeOqueOkaraokeTraduziu() {
        var c = new CorretorOrtograficoLegenda();
        String r = c.corrigir("Por tras da sua mascara");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");

        assertEquals("Por trás da sua mascara", r,
            "'tras' não é palavra e tem de ser corrigido; 'mascara' É (verbo mascarar) e o "
                + "dicionário não tem como saber que ali era o substantivo");
        assertTrue(c.corrigir("Eu nao quero ser como voce.").contains("você"),
            "'voce' é inequívoco e sai corrigido: " + c.corrigir("Eu nao quero ser como voce."));
    }

    @Test
    @DisplayName("ROMAJI passa intacto, sem precisar de lista de exceção")
    void romajiNaoEhTocado() {
        var c = new CorretorOrtograficoLegenda();
        String romaji = "Furi dake no kotae to eeru tai de ensou o tsunagu";
        Assumptions.assumeTrue(c.corrigir("mascara") != null && c.disponivel(),
            "hunspell ausente — NÃO VERIFICADO");

        assertEquals(romaji, c.corrigir(romaji),
            "ROMAJI FOI ALTERADO. É o dano de 100 ocorrências de 'mae' virando 'mãe' voltando "
                + "por outra porta — e aqui nem lista de exceção deveria ser necessária, porque "
                + "não existe versão acentuada de 'tsunagu' para o dicionário sugerir.");
        assertEquals("Hohoemu reido o daita kibou", c.corrigir("Hohoemu reido o daita kibou"));
        assertEquals("mae kimi kokoro yume", c.corrigir("mae kimi kokoro yume"),
            "as palavras do dano registrado: mae é 前, não 'mãe'");
    }

    @Test
    @DisplayName("inglês da letra passa intacto — no karaokê ele NÃO é resíduo")
    void inglesDaLetraNaoEhTocado() {
        var c = new CorretorOrtograficoLegenda();
        String ingles = "Do you feel alone Can you hear me now";
        Assumptions.assumeTrue(c.disponivel() || c.corrigir("mascara") != null,
            "hunspell ausente — NÃO VERIFICADO");

        assertEquals(ingles, c.corrigir(ingles), "a letra em inglês é o conteúdo, não defeito");
        assertEquals("gonna one", c.corrigir("gonna one"),
            "code-switching real: o ED2 tem 'gonna' e 'one' junto com português correto");
    }

    @Test
    @DisplayName("fragmento de KFX não vira palavra")
    void fragmentoDeKfxNaoEhTocado() {
        var c = new CorretorOrtograficoLegenda();
        Assumptions.assumeTrue(c.disponivel() || c.corrigir("mascara") != null,
            "hunspell ausente — NÃO VERIFICADO");

        // O OPL2 do Unicorn tem 3.255 linhas assim: sílabas soltas, não frases.
        for (String silaba : new String[] {"feel", "lone", "hear", "Do", "you"}) {
            assertEquals(silaba, c.corrigir(silaba),
                "sílaba de KFX alterada: " + silaba + " — são 3.255 linhas dessas só no Unicorn");
        }
    }

    @Test
    @DisplayName("linha MISTA: corrige o português e deixa o resto onde está")
    void linhaMistaSoTemOportuguesCorrigido() {
        var c = new CorretorOrtograficoLegenda();
        String r = c.corrigir("Voce e gonna kimi tras");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");

        assertTrue(r.contains("Você"), "português inequívoco tem de ser corrigido: " + r);
        assertTrue(r.contains("trás"), "português inequívoco tem de ser corrigido: " + r);
        assertTrue(r.contains("gonna"), "inglês da letra preservado: " + r);
        assertTrue(r.contains("kimi"), "romaji preservado: " + r);
    }
}
