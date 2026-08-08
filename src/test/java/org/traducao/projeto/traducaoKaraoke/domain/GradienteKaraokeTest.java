package org.traducao.projeto.traducaoKaraoke.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o gradiente de karaokê sobrevive à tradução — mesma paleta,
 * mesma ordem, texto em português.
 *
 * <p>Os dois casos de entrada NÃO são inventados: são as duas formas que o acervo do Guilty Crown
 * realmente usa, medidas em 07/08/2026 — 139 linhas com blocos de cor pura e 306 com cor mais
 * {@code blur}/{@code fad} repetidos. Juntas, as 445 que faziam o LLM devolver
 * {@code "So, eu e evidentementereithyingthathingthatmakes mea whole wholed"}.
 */
class GradienteKaraokeTest {

    /** Forma 1 (139 linhas): primeiro bloco completo, intermediários só de cor. */
    private static final String SO_EVERYTHING =
        "{\\blur4.5\\fad(200,200)\\3c&H6500B1&}S{\\3c&H7114AA&}o{\\3c&H7C26A4&},{\\3c&H802DA2&} "
            + "{\\3c&H87399E&}e{\\3c&H914A99&}v{\\3c&H995595&}e{\\3c&HA26690&}r{\\3c&HA9718C&}y"
            + "{\\3c&HAC7788&}t{\\3c&HA56D82&}h{\\3c&H9D637C&}i{\\3c&H975B77&}n{\\3c&H8E5070&}g";

    /** Forma 2 (306 linhas): cada bloco repete blur/fad junto da cor. */
    private static final String KIZUITE =
        "{\\blur4.5\\3c&HF6D6B3&}K{\\3c&HDCDDD2&\\blur4.5\\fad(100,150)}i{\\3c&HD6D9C4&}z"
            + "{\\3c&HD9D2A9&}u{\\3c&HE2C9AA&}i{\\3c&HF4BDCB&}t{\\3c&HF6D6B3&}e{\\3c&HDCDDD2&}!"
            + "{\\3c&HD6D9C4&}?{\\3c&HD9D2A9&}~{\\3c&HE2C9AA&}-";

    private static final Pattern BLOCO = Pattern.compile("\\{[^}]*\\}");

    @Test
    @DisplayName("decompoe a linha real em paleta ordenada + texto limpo")
    void decompoeLinhaReal() {
        GradienteKaraoke g = GradienteKaraoke.decompor(SO_EVERYTHING).orElseThrow(
            () -> new AssertionError("a linha real do Guilty Crown tem de ser elegivel"));

        assertEquals("So, everything", g.textoVisivel(),
            "o LLM precisa receber a frase legivel, nao o mosaico de marcadores");
        assertEquals(14, g.tags().size(), "uma tag por letra, na ordem de leitura");
        assertEquals("{\\blur4.5\\fad(200,200)\\3c&H6500B1&}", g.tags().getFirst(),
            "o primeiro bloco carrega blur/fad e tem de sair intacto");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o coração da FASE 1 — traduzir sem perder o efeito. Este é o caso que
     * produziu a saída corrompida em produção.
     */
    @Test
    @DisplayName("recompoe a traducao com a MESMA paleta, na mesma ordem")
    void recompoeComAMesmaPaleta() {
        GradienteKaraoke g = GradienteKaraoke.decompor(SO_EVERYTHING).orElseThrow();
        String saida = g.recompor("Tudo que me completa");

        assertEquals("Tudo que me completa", BLOCO.matcher(saida).replaceAll(""),
            "o texto visivel da saida tem de ser exatamente a traducao");
        assertEquals(g.tags(), extrairTags(saida),
            "a paleta tem de sair inteira e na ordem — nenhuma cor perdida, nenhuma inventada");
        assertTrue(saida.startsWith("{\\blur4.5\\fad(200,200)\\3c&H6500B1&}T"),
            "a primeira cor tem de pintar a primeira letra da traducao: " + saida);
    }

    @Test
    @DisplayName("a segunda forma do acervo (blur/fad repetidos) tambem recompoe")
    void recompoeSegundaForma() {
        GradienteKaraoke g = GradienteKaraoke.decompor(KIZUITE).orElseThrow();
        String saida = g.recompor("Percebe");
        assertEquals("Percebe", BLOCO.matcher(saida).replaceAll(""));
        assertEquals(g.tags(), extrairTags(saida),
            "com a traducao MAIS CURTA que o original, as cores excedentes ainda tem de aparecer "
                + "— senao a paleta encolhe a cada retraducao");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE do veto. Timing de sílaba amarra a tag ao áudio;
     * redistribuir destruiria a sincronia do karaokê.
     */
    @Test
    @DisplayName("VETO: linha com \\k (timing de silaba) nao e decomposta")
    void vetaTimingDeSilaba() {
        String comK = "{\\k25\\3c&H6500B1&}S{\\k12\\3c&H7114AA&}o{\\k30\\3c&H7C26A4&},"
            + "{\\k11}e{\\k9}v{\\k14}e{\\k22}r{\\k17}y{\\k13}t{\\k19}h{\\k10}i{\\k16}n{\\k21}g";
        assertEquals(Optional.empty(), GradienteKaraoke.decompor(comK),
            "com \\k a tag e tempo, nao decoracao — redistribuir dessincronizaria a musica");
    }

    @Test
    @DisplayName("VETO: linha com \\t( (animacao) nao e decomposta")
    void vetaTransformacaoAnimada() {
        String comT = "{\\t(0,500,\\frx30)\\3c&H1&}a{\\3c&H2&}b{\\3c&H3&}c{\\3c&H4&}d{\\3c&H5&}e"
            + "{\\3c&H6&}f{\\3c&H7&}g{\\3c&H8&}h{\\3c&H9&}i{\\3c&HA&}j{\\3c&HB&}k";
        assertEquals(Optional.empty(), GradienteKaraoke.decompor(comT));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o veto que nasceu de cicatriz. {@code \N} é DOIS caracteres e o
     * {@code N} é letra — já corrompeu 6 arquivos deste acervo em 2026-08-07. Sem saber onde o
     * verso partia, recompor colocaria a quebra no lugar errado.
     */
    @Test
    @DisplayName("VETO: linha com \\N (quebra de verso) nao e decomposta")
    void vetaQuebraDeVerso() {
        String comN = "{\\3c&H1&}a{\\3c&H2&}b{\\3c&H3&}c\\N{\\3c&H4&}d{\\3c&H5&}e{\\3c&H6&}f"
            + "{\\3c&H7&}g{\\3c&H8&}h{\\3c&H9&}i{\\3c&HA&}j{\\3c&HB&}k";
        assertEquals(Optional.empty(), GradienteKaraoke.decompor(comN),
            "sem saber onde o verso partia, a quebra sairia no lugar errado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. Linha comum não pode entrar neste caminho — o
     * mascarador normal já dá conta e mexer seria risco sem ganho.
     */
    @Test
    @DisplayName("linha de UMA tag nao e gradiente — segue pelo caminho antigo")
    void linhaComumNaoEhGradiente() {
        assertEquals(Optional.empty(),
            GradienteKaraoke.decompor("{\\blur3\\fad(200,200)}These days you love me no more"),
            "esta linha ja traduz bem hoje: 595 do Guilty Crown estao nesta forma");
        assertEquals(Optional.empty(), GradienteKaraoke.decompor(""));
        assertEquals(Optional.empty(), GradienteKaraoke.decompor(null));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: falha fechada. Sem tradução utilizável, a linha volta como estava —
     * nunca meio montada.
     */
    @Test
    @DisplayName("FALHA FECHADA: traducao vazia devolve o original byte a byte")
    void traducaoVaziaDevolveOriginal() {
        GradienteKaraoke g = GradienteKaraoke.decompor(SO_EVERYTHING).orElseThrow();
        assertEquals(SO_EVERYTHING, g.recompor(null));
        assertEquals(SO_EVERYTHING, g.recompor("   "));
        assertEquals(SO_EVERYTHING, g.recompor("{\\3c&H1&}"),
            "resposta so com tag e resposta sem texto: mantem o original");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: idempotência sobre a própria saída. O corretor pode rodar duas vezes
     * na mesma pasta, e a segunda passada não pode encolher o gradiente.
     */
    @Test
    @DisplayName("recompor a propria saida preserva a paleta (reexecucao nao degrada)")
    void reexecucaoNaoDegrada() {
        GradienteKaraoke g = GradienteKaraoke.decompor(SO_EVERYTHING).orElseThrow();
        String primeira = g.recompor("Tudo que me completa");
        GradienteKaraoke g2 = GradienteKaraoke.decompor(primeira).orElseThrow(
            () -> new AssertionError("a saida tem de continuar sendo um gradiente valido"));
        assertEquals(g.tags().size(), g2.tags().size(),
            "a paleta nao pode encolher a cada passada");
        assertEquals("Tudo que me completa", g2.textoVisivel());
    }

    private static java.util.List<String> extrairTags(String texto) {
        java.util.List<String> encontradas = new java.util.ArrayList<>();
        Matcher m = BLOCO.matcher(texto);
        while (m.find()) {
            encontradas.add(m.group());
        }
        assertFalse(encontradas.isEmpty(), "saida sem nenhuma tag: " + texto);
        return encontradas;
    }
}
