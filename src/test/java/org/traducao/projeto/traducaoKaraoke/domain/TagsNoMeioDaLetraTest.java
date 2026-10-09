package org.traducao.projeto.traducaoKaraoke.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a linha de letra com tag no MEIO — as três frases do OP_S2 do
 * Guilty Crown que ficaram em inglês na 4.1 de 09/10/2026 — volta traduzida com TODAS as tags
 * do original, na ordem, e com a troca de cor caindo em fronteira de palavra.
 *
 * <h2>De onde vem cada gabarito (A3)</h2>
 * As linhas são as cruas do {@code Guilty Crown - 13_Track4_PT-BR.ass}. O esperado sai de
 * invariantes que não dependem da implementação: nenhuma tag some nem muda de ordem; tag que
 * abria palavra continua abrindo palavra (senão a cor troca no meio de "pa|ra"); o texto
 * visível é exatamente a tradução. As posições concretas são a proporção calculada à mão —
 * {@code acknowledge} começa no caractere 36 de 55; na tradução de 63, o alvo é 41 e o início de
 * palavra mais próximo é 42 ("reconhecer").
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cada asserção nomeia a invariante quebrada e mostra a linha produzida.
 */
class TagsNoMeioDaLetraTest {

    private static final Pattern BLOCO = Pattern.compile("\\{[^}]*\\}");

    private static final String PREFIXO = "{\\fad(0,0)\\blur4.5\\3c&H4331EA&}";
    private static final String OLHOS = PREFIXO + "that your eyes were given to you to {\\c&HEAEEEB&}acknowledge others,";
    private static final String OLHOS_PULSANDO = PREFIXO + "that your eyes were given to you to "
        + "{\\c&HEAEEEB&}{\\t(\\c&H39383C&)}acknowledge {\\c&HEAEEEB&}others,";
    private static final String OLHOS_PT = "que os seus olhos foram dados a você para reconhecer os outros,";

    private static final String NOTICE = "{\\blur4.5\\3c&HF6D6B3&}N{\\3c&HDCDDD2&}o{\\3c&HD6D9C4&}t"
        + "{\\3c&HD9D2A9&}i{\\3c&HE2C9AA&}c{\\3c&HF4BDCB&}e";
    private static final String RELY = "{\\blur4.5\\fad(100,350)\\3c&HF6D6B3&}r{\\3c&HD7DED9&}e"
        + "{\\3c&HDCCD9E&}l{\\3c&HF4BDCB&}y";

    private static List<String> tags(String linha) {
        List<String> saida = new ArrayList<>();
        Matcher m = BLOCO.matcher(linha);
        while (m.find()) {
            saida.add(m.group());
        }
        return saida;
    }

    private static String visivel(String linha) {
        return BLOCO.matcher(linha).replaceAll("");
    }

    @Test
    @DisplayName("decompoe a frase do Guilty Crown: o modelo recebe a frase inteira, sem marcador")
    void decompoeAFraseDosOlhos() {
        TagsNoMeioDaLetra t = TagsNoMeioDaLetra.decompor(OLHOS).orElseThrow();
        assertEquals("that your eyes were given to you to acknowledge others,", t.textoVisivel());
        assertEquals(List.of(new TagsNoMeioDaLetra.Bloco(PREFIXO, 0),
            new TagsNoMeioDaLetra.Bloco("{\\c&HEAEEEB&}", 36)), t.blocos());
    }

    @Test
    @DisplayName("a cor de 'acknowledge others' volta em INICIO DE PALAVRA, nunca no meio de 'pa|ra'")
    void corVoltaEmInicioDePalavra() {
        String saida = TagsNoMeioDaLetra.decompor(OLHOS).orElseThrow().recompor(OLHOS_PT);
        assertEquals(PREFIXO + "que os seus olhos foram dados a você para {\\c&HEAEEEB&}reconhecer os outros,", saida);
    }

    @Test
    @DisplayName("variante animada: as tres tags do miolo voltam todas, na ordem, cada uma abrindo palavra")
    void varianteAnimadaPreservaTodasAsTags() {
        String saida = TagsNoMeioDaLetra.decompor(OLHOS_PULSANDO).orElseThrow().recompor(OLHOS_PT);
        assertEquals(tags(OLHOS_PULSANDO), tags(saida), () -> "tag sumiu ou trocou de ordem: " + saida);
        assertEquals(OLHOS_PT, visivel(saida), "o texto visivel tem de ser exatamente a traducao");
        assertEquals(PREFIXO + "que os seus olhos foram dados a você para {\\c&HEAEEEB&}{\\t(\\c&H39383C&)}"
            + "reconhecer os {\\c&HEAEEEB&}outros,", saida);
    }

    @Test
    @DisplayName("gradiente curto (Notice, 6 blocos): as 6 cores voltam, uma por letra, a primeira no inicio")
    void gradienteCurtoVoltaLetraALetra() {
        String saida = TagsNoMeioDaLetra.decompor(NOTICE).orElseThrow().recompor("Perceba");
        assertEquals("{\\blur4.5\\3c&HF6D6B3&}P{\\3c&HDCDDD2&}e{\\3c&HD6D9C4&}rc{\\3c&HD9D2A9&}e"
            + "{\\3c&HE2C9AA&}b{\\3c&HF4BDCB&}a", saida);
    }

    @Test
    @DisplayName("gradiente de 4 blocos (rely) com traducao mais longa: nenhuma cor se perde")
    void gradienteDeQuatroBlocos() {
        String saida = TagsNoMeioDaLetra.decompor(RELY).orElseThrow().recompor("confie");
        assertEquals(tags(RELY), tags(saida));
        assertEquals("confie", visivel(saida));
        assertTrue(saida.startsWith("{\\blur4.5\\fad(100,350)\\3c&HF6D6B3&}c"), saida);
    }

    @Test
    @DisplayName("tag antes de ESPACO volta antes de espaco")
    void tagAntesDeEspacoVoltaAntesDeEspaco() {
        String saida = TagsNoMeioDaLetra.decompor("{\\b1}hold on{\\b0} to it").orElseThrow()
            .recompor("segure firme nisso");
        assertEquals("{\\b1}segure firme{\\b0} nisso", saida);
    }

    @Test
    @DisplayName("posicoes nunca regridem: tag de palavra empurrada para frente nao ultrapassa a seguinte")
    void posicoesNuncaRegridem() {
        // Conta feita a mao: "one two three" tem 13 visiveis; {\i1} abre "two" (4) e {\i0} fica
        // no miolo dela (5). Na traducao de 26, o alvo do primeiro e 8 e o inicio de palavra
        // mais proximo e 11; o do segundo, que nao encaixa, e 10. Sem a trava, o {\i0} sairia
        // na posicao 10, ANTES do {\i1} na 11 — tags trocadas de ordem.
        String linha = "one {\\i1}t{\\i0}wo three";
        String saida = TagsNoMeioDaLetra.decompor(linha).orElseThrow().recompor("ab cdefghi jklmnopqrstuvwx");
        assertEquals(tags(linha), tags(saida), () -> "a ordem das tags trocou: " + saida);
        assertEquals("ab cdefghi {\\i1}{\\i0}jklmnopqrstuvwx", saida);
    }

    @Test
    @DisplayName("tag inventada pelo modelo na resposta e descartada; a moldura e a do original")
    void tagInventadaPeloModeloEDescartada() {
        String saida = TagsNoMeioDaLetra.decompor(OLHOS).orElseThrow()
            .recompor("{\\i1}" + OLHOS_PT + "{\\i0}");
        assertEquals(tags(OLHOS), tags(saida));
    }

    @Test
    @DisplayName("traducao nula, em branco ou so com tags devolve o ORIGINAL intacto")
    void semTraducaoUtilVoltaOOriginal() {
        TagsNoMeioDaLetra t = TagsNoMeioDaLetra.decompor(OLHOS).orElseThrow();
        assertEquals(OLHOS, t.recompor(null));
        assertEquals(OLHOS, t.recompor("   "));
        assertEquals(OLHOS, t.recompor("{\\i1}{\\i0}"));
    }

    @Test
    @DisplayName("vetos: timing de silaba, quebra de verso, desenho, nenhuma tag, so tag")
    void vetos() {
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor("{\\k20}Ka{\\k30}ze"), "\\k amarra a tag ao audio");
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor("{\\fad(1,1)}one{\\c&H1&} line\\Ntwo"), "\\N");
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor("{\\p1}m 0 0 l 10 0 10 10{\\p0}"), "desenho e vetor");
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor("sem tag nenhuma"));
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor("{\\c&H1&}{\\c&H2&}"));
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor(null));
        assertEquals(Optional.empty(), TagsNoMeioDaLetra.decompor("  "));
    }

    @Test
    @DisplayName("A1: tags que carregam o mesmo sinal dos vetos sem serem vetadas (\\pos, \\p0, \\fad, \\n) passam")
    void fronteiraDosVetos() {
        assertTrue(TagsNoMeioDaLetra.decompor("{\\pos(10,10)}hold {\\c&H1&}on").isPresent(), "\\pos nao e desenho");
        assertTrue(TagsNoMeioDaLetra.decompor("{\\p0}hold {\\c&H1&}on").isPresent(), "\\p0 DESLIGA o desenho");
        assertTrue(TagsNoMeioDaLetra.decompor("{\\fad(100,100)}hold {\\c&H1&}on").isPresent(), "\\fad nao e \\k");
        assertTrue(TagsNoMeioDaLetra.decompor("{\\fs20}hold\\n{\\c&H1&}on").isPresent(), "\\n minusculo nao e \\N");
    }
}
