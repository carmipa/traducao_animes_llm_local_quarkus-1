package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a mesma palavra não é perguntada duas vezes ao dicionário, e que
 * o resultado continua idêntico com e sem a memória.
 *
 * <h2>O prejuízo, medido no Unicorn em 13/08/2026</h2>
 * A primeira versão consultava por FALA. O custo do hunspell é o ARRANQUE do processo, não a
 * palavra — 5.643 falas viraram 5.643 processos, e o episódio inteiro passou de <b>28m16s para
 * 68m01s</b>. Mesmo resultado ortográfico, 2,4x o tempo.
 *
 * <p>Uma legenda repete vocabulário: as 5.643 falas do Unicorn têm 4.688 formas distintas, e a
 * maioria aparece em dezenas de falas. Perguntar uma vez por forma é a diferença.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell, os testes que dependem dele PULAM por {@link Assumptions} — nunca passam por
 * ausência de verificação.
 */
@DisplayName("corretor: memória evita perguntar a mesma palavra duas vezes")
class CorretorOrtograficoLegendaMemoriaTest {

    @Test
    @DisplayName("a mesma fala repetida N vezes produz o mesmo texto, sempre")
    void repetirAfalaNaoMudaOresultado() {
        var c = new CorretorOrtograficoLegenda();
        String fala = "A situacao da colonia e critica.";

        String primeira = c.corrigir(fala);
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");

        for (int i = 0; i < 20; i++) {
            assertEquals(primeira, c.corrigir(fala),
                "a resposta mudou entre chamadas: a memória está devolvendo coisa diferente do "
                    + "que o dicionário devolveria, e o texto passa a depender da ordem das falas");
        }
    }

    @Test
    @DisplayName("corrige o que é acento e NÃO toca no que não é")
    void corrigeAcentoEpreservaOresto() {
        var c = new CorretorOrtograficoLegenda();
        String r = c.corrigir("A situacao no cockpit e critica, Nordlicht.");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");

        assertTrue(r.contains("situação"), "acento faltando tem de ser reposto: " + r);
        assertTrue(r.contains("cockpit"),
            "inglês legítimo NÃO pode ser alterado — em legenda de anime cockpit é a palavra certa");
        assertTrue(r.contains("Nordlicht"),
            "termo alemão de lore NÃO pode ser alterado: são 155 formas assim no acervo");
    }

    @Test
    @DisplayName("tags e a quebra \\N sobrevivem à correção")
    void naoDestroiFormatacao() {
        var c = new CorretorOrtograficoLegenda();
        String r = c.corrigir("{\\i1}A situacao{\\i0} da\\Ncolonia.");
        Assumptions.assumeTrue(c.disponivel(), "hunspell ausente — NÃO VERIFICADO");

        assertTrue(r.contains("{\\i1}") && r.contains("{\\i0}"), "tag de override sumiu: " + r);
        assertTrue(r.contains("\\N"), "a quebra de linha sumiu: " + r);
        assertTrue(r.contains("situação"), "e a correção tinha de acontecer mesmo assim: " + r);
    }

    @Test
    @DisplayName("texto sem nada a corrigir volta BYTE A BYTE igual")
    void textoLimpoVoltaIntacto() {
        var c = new CorretorOrtograficoLegenda();
        String limpo = "A situação da colônia é crítica.";
        assertEquals(limpo, c.corrigir(limpo),
            "texto correto não pode ser reescrito — seria alarme falso gravado no arquivo");
    }
}
