package org.traducao.projeto.legenda.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a fonte única do critério musical cobre TUDO que as duas
 * implementações anteriores cobriam separadamente — e que nenhum estilo de DIÁLOGO do acervo
 * é capturado.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido por {@code MedicaoDivergenciaPadraoMusicalIT} em 07/08/2026 sobre 1.719.242 falas:
 * {@code PoliticaEstiloMusical} e {@code DetectorEfeitoKaraokeService} discordavam em
 * <b>1.385 linhas, 12 estilos</b>. Cada um alcançava o que o outro não alcançava — a Política
 * tinha varredura por substring (pega {@code insertita}, {@code Other songs}) e o Detector
 * tinha fronteira de LETRA (pega {@code OP_S2}, {@code ED2}). A união é o que esta classe é.
 *
 * <h2>Por que os dois lados importam</h2>
 * Capturar de menos faz letra de música ir ao LLM e voltar corrompida em salada
 * romaji/PT-BR. Capturar demais faz a Tradução Local <b>parar de traduzir o anime</b>:
 * {@code Dialogue} e {@code Default} sozinhos somam 162.705 eventos do acervo.
 */
class PadraoEstiloMusicalTest {

    /**
     * Os 12 estilos onde os dois donos discordavam, com a contagem de linhas medida. Todos são
     * musicais de verdade — por isso a convergência é para "sim" nos doze.
     */
    private static final List<String> DIVERGENTES_MEDIDOS = List.of(
        "OP2", "ED2", "OP_S2", "ED_S2", "ED_S2_roma", "OP_S2_roma",
        "ED_English", "ED2-English",
        "Gundam 0083 ED3 Lyrics", "Gundam 0083 ED3 Lyrics A",
        "Other songs", "insertita"
    );

    /**
     * Estilos de FALA do acervo, do mais frequente ao menos. Se o padrão alcançar qualquer um,
     * a Tradução Local para de traduzir o anime.
     */
    private static final List<String> DIALOGO_DO_ACERVO = List.of(
        "Dialogue", "Default", "Logo", "Zeta Episode Title", "Dungeons", "Main Title",
        "Flashback", "Signs", "Next Ep", "Ep Title", "Ep Titles", "Titles", "08thMS", "EG",
        "Guilty Main", "HestiaFamilia", "nextep", "Swimsuit", "Paradise", "Flower", "Main",
        "Sign", "Italics", "insert", "Default-alt"
    );

    /**
     * PROPÓSITO DE NEGÓCIO: os 12 estilos que causavam a divergência agora resolvem para o
     * mesmo lado. É o teste que fecha a FASE 2.
     */
    @Test
    @DisplayName("os 12 estilos divergentes medidos sao todos reconhecidos como musica")
    void divergentesConvergemParaMusica() {
        for (String estilo : DIVERGENTES_MEDIDOS) {
            assertTrue(PadraoEstiloMusical.nomeDeclaraMusica(estilo),
                "estilo \"" + estilo + "\" causava divergencia entre os dois donos e e musical "
                    + "de verdade — tem de ser reconhecido pela fonte unica");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE, e o mais caro de violar. Um padrão largo demais
     * silencia a tradução do acervo inteiro.
     */
    @Test
    @DisplayName("nenhum estilo de dialogo do acervo e capturado")
    void naoCapturaDialogo() {
        for (String estilo : DIALOGO_DO_ACERVO) {
            if ("insert".equals(estilo)) {
                continue;   // tratado no teste seguinte: e nome de estilo MUSICAL no acervo
            }
            assertFalse(PadraoEstiloMusical.nomeDeclaraMusica(estilo),
                "estilo de DIALOGO \"" + estilo + "\" foi capturado como musica — a Traducao "
                    + "Local pararia de traduzir as falas com esse estilo");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a fronteira de LETRA é o que separa {@code OP_S2} (musical) de
     * {@code "Episode"} ou {@code "Ed Sheeran"}. Trocar por {@code \b} reabre a divergência de
     * 1.263 linhas; tirar a fronteira captura diálogo.
     */
    @Test
    @DisplayName("abreviacao curta exige fronteira de LETRA, nao \\b")
    void abreviacaoExigeFronteiraDeLetra() {
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("OP_S2"),
            "o \"_\" e caractere de palavra: com \\b este estilo escapava, e sao 170 linhas");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("ED2"), "digito tambem e fronteira");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("Copy of OP"));

        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica("Editor"),
            "\"ed\" seguido de letra NAO e abreviacao musical");
        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica("Opera House"),
            "\"op\" seguido de letra NAO e abreviacao musical");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a varredura por SUBSTRING é o que a Política sempre teve e o
     * Detector não. Removê-la faz 55 linhas do acervo voltarem a ser traduzidas.
     */
    @Test
    @DisplayName("substring alcanca a variante grudada que o fansub inventa")
    void substringAlcancaVarianteGrudada() {
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("insertita"),
            "18 linhas no DanMachi: \"insert\" grudado em \"ita\" escapa de qualquer lookaround");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("Other songs"),
            "37 linhas no Guilty Crown: o \"s\" do plural quebrava o lookahead do Detector");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("ED_Romaji1"),
            "romaji seguido de digito");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("Gundam 0083 ED3 Lyrics"),
            "\"lyrics\" nao estava na varredura da Politica");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os DOIS donos passam a responder igual. É o invariante que a
     * FASE 2 existe para criar, e o que a medição vai conferir contra o acervo.
     */
    @Test
    @DisplayName("Politica e Padrao respondem igual para todos os divergentes medidos")
    void politicaEPadraoConcordam() {
        PoliticaEstiloMusical politica = new PoliticaEstiloMusical(List.of());
        for (String estilo : DIVERGENTES_MEDIDOS) {
            assertEquals(PadraoEstiloMusical.nomeDeclaraMusica(estilo),
                politica.estiloIgnorado(estilo),
                "a Politica divergiu da fonte unica em \"" + estilo + "\" — ela tem de delegar, "
                    + "nao ter heuristica propria");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrada ausente não pode virar acusação de música.
     */
    @Test
    @DisplayName("nulo e branco nao sao musica")
    void nuloEBrancoNaoSaoMusica() {
        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica(null));
        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica(""));
        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica("   "));
    }

    /**
     * O PREJUIZO QUE ORIGINOU (24/08/2026), e ele foi GRAVADO no acervo.
     *
     * <p>A reposicao de acento da tela 3.3 escreveu, em seis linhas de letra do DanMachi:
     *
     * <pre>
     *   "kanarazu mata ai ni iku kara"  ->  "kanarazu mata aí ni iku kara"
     * </pre>
     *
     * O {@code ai} do romaji e 愛 ("amor"), nao o adverbio portugues. O veto de musica nao pegou
     * porque o estilo no arquivo se chama <b>{@code Romanji}</b>, com N — e {@code "romaji"} nao
     * casa isso por substring. Sao <b>2.116 linhas</b> no acervo com esse estilo, e dos 115
     * estilos distintos ele era o UNICO com cara de musica que a politica nao reconhecia.
     *
     * <p>E a mesma cicatriz de 19/08/2026, quando {@code mae} (前) virou {@code mãe} em 103
     * linhas — repetida cinco dias depois por causa de uma letra a mais no nome do estilo.
     */
    @Test
    @DisplayName("Romanji com N e musica: o nome errado do acervo tambem vale")
    void romanjiComNTambemEMusica() {
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("Romanji"),
            "o estilo 'Romanji' (2.116 linhas no acervo) nao foi reconhecido como musica — foi "
            + "por este furo que 'ai' virou 'aí' em letra de romaji");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("romanji"),
            "a comparacao tem de ser insensivel a caixa");
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("OP Romanji 2"),
            "grudado a outras palavras continua sendo musica");
        // O contraponto, para o remendo nao virar demolicao: 'romaji' certo continua valendo.
        assertTrue(PadraoEstiloMusical.nomeDeclaraMusica("ED - Romaji"),
            "a grafia correta parou de ser reconhecida");
        // E o caso-controle negativo: nome comum nao pode virar musica por causa do remendo.
        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica("Default"),
            "estilo de dialogo virou musica");
        assertFalse(PadraoEstiloMusical.nomeDeclaraMusica("Roman"),
            "'Roman' sozinho nao e romaji — o remendo nao pode alargar tanto");
    }
}
