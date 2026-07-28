package org.traducao.projeto.legenda.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela a medição que autoriza {@code podeSerCamadaMusical} a ser um VETO DE
 * TRADUÇÃO, e não apenas a candidatura larga do pareamento que seu Javadoc descrevia.
 *
 * <h2>Por que este teste existe</h2>
 * O Javadoc do método afirmava, como invariante, que "este critério pode ser largo sem afrouxar
 * nenhuma decisão existente: nenhum outro fluxo o consulta". Isso deixou de ser verdade: desde a
 * regra de escopo de 2026-07-25, {@code traducao.SeletorEventosTraduziveis} o consulta como veto
 * absoluto — uma linha que casa aqui NÃO é traduzida. Um critério largo virou decisão, e a largura
 * passou a precisar de prova.
 *
 * <p>A prova foi levantada em 2026-07-28 sobre o acervo inteiro: <b>490 arquivos {@code .ass}</b>,
 * dos quais saíram 104 nomes de estilo distintos. O padrão amplo captura 46 deles, e os 46 são
 * musicais de verdade — de {@code "ED - Romaji"} (222.937 eventos) a {@code "Copy of OP"} (8) e
 * {@code "Gundam 0083 ED3 Lyrics A"} (6). NENHUM estilo de diálogo foi capturado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Os nomes abaixo não são exemplos inventados: são as grafias que os fansubs realmente usaram
 *       nos arquivos deste acervo. É o que torna o teste uma medição congelada e não uma opinião.</li>
 *   <li>A fronteira é por LETRA (lookaround), não {@code \b}. Com {@code \b} o sublinhado e o dígito
 *       são caracteres de palavra, e {@code ED_S2}, {@code OP_S2}, {@code ED_Romaji1} e {@code OP2}
 *       — todos presentes no acervo — deixariam de casar. Estreitar o padrão para {@code \b} faz
 *       este teste cair, que é exatamente o alarme desejado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Queda no primeiro bloco significa música vazando para as etapas de fala. Queda no segundo
 * significa diálogo sendo silenciosamente recusado pela Tradução Local — o dano caro, porque não
 * aparece como erro, só como fala faltando.
 */
class PadraoMusicaAmploNaoCapturaDialogoTest {

    private final DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

    /** Texto neutro: quem decide nestes casos tem de ser o NOME do estilo, não o conteúdo. */
    private static final String TEXTO_NEUTRO = "Some line of text here";

    /** Os 46 estilos do acervo capturados pelo padrão amplo, em 2026-07-28. */
    private static final String[] MUSICAIS_DO_ACERVO = {
        "Copy of OP", "ED", "ED - Eng", "ED - English", "ED - Kanji", "ED - Romaji", "ED Eng",
        "ED ep 02", "ED ep 02 bg layer", "ED Layer", "ED Lyrics", "ED Rom", "ED Roma L1",
        "ED Romaji", "ED_English", "ED_Romaji1", "ED_Romaji2", "ED_Romaji3", "ED_Romaji4",
        "ED_Romaji5", "ED_S2", "ED_S2_roma", "ED2-English", "ED2-Romaji",
        "Gundam 0083 ED3 Lyrics", "Gundam 0083 ED3 Lyrics A", "insert", "Insert Song",
        "Insert Song TL", "OP", "OP - Eng", "OP - English", "OP - Kanji", "OP - Romaji", "OP Eng",
        "OP ep 01", "OP Lyrics", "OP Rom", "OP Roma", "OP Romaji", "OP Romaji 2", "OP_S2",
        "OP_S2_roma", "OP2", "Song ENG", "Song JP"
    };

    /**
     * Estilos de FALA do mesmo acervo, do mais frequente ao menos. {@code Dialogue} e
     * {@code Default} sozinhos somam 162.705 eventos: se o padrão alcançasse qualquer um deles, a
     * Tradução Local pararia de traduzir o anime.
     */
    private static final String[] DIALOGO_DO_ACERVO = {
        "Dialogue", "Default", "Logo", "Zeta Episode Title", "Dungeons", "Main Title", "Flashback",
        "Signs", "Next Ep", "Ep Title", "Titles", "08thMS", "EG", "Guilty Main", "HestiaFamilia",
        "nextep", "Swimsuit", "Paradise", "Flower"
    };

    @Test
    @DisplayName("os 46 estilos musicais do acervo são todos capturados")
    void capturaTodaMusicaDoAcervo() {
        for (String estilo : MUSICAIS_DO_ACERVO) {
            assertTrue(detector.podeSerCamadaMusical(estilo, TEXTO_NEUTRO),
                "estilo musical do acervo escapou do veto de escopo: " + estilo);
        }
    }

    @Test
    @DisplayName("nenhum estilo de diálogo do acervo é capturado")
    void naoCapturaDialogoDoAcervo() {
        for (String estilo : DIALOGO_DO_ACERVO) {
            assertFalse(detector.podeSerCamadaMusical(estilo, TEXTO_NEUTRO),
                "estilo de FALA seria recusado pela Tradução Local: " + estilo);
        }
    }

    /**
     * As quatro grafias que separam a fronteira por letra da fronteira {@code \b}. Estão isoladas
     * porque são a única razão de o padrão não usar {@code \b} — e um teste que as misturasse com as
     * outras 42 esconderia qual delas caiu.
     */
    @Test
    @DisplayName("sublinhado e dígito não quebram a fronteira, como quebrariam com \\b")
    void fronteiraPorLetraAlcancaSublinhadoEDigito() {
        for (String estilo : new String[] {"ED_S2", "OP_S2", "ED_Romaji1", "OP2"}) {
            assertTrue(detector.podeSerCamadaMusical(estilo, TEXTO_NEUTRO),
                "com fronteira \\b este estilo escaparia: " + estilo);
        }
    }

    /**
     * LACUNA MEDIDA, deliberadamente NÃO corrigida aqui — e ASSIMÉTRICA de um jeito que só apareceu
     * quando este teste foi executado.
     *
     * <p>Alguns estilos do acervo nomeiam a MÚSICA em vez do papel: {@code "Hey World"} (insert do
     * Guilty Crown), {@code "RISE LIGHT RISE"}, {@code "EVERYTHING"}. Nenhum contém palavra de papel
     * musical, então nenhum casa {@code ESTILO_MUSICA_AMPLO_PATTERN}.
     *
     * <p>Só que a canção vem em DUAS camadas, e elas têm sortes diferentes:
     * <pre>
     *   "Hey World Romaji"  -> CAPTURADO   (o sufixo "Romaji" dispara ESTILO_JAPONES_ROMAJI_PATTERN)
     *   "Hey World English" -> escapa      (nenhum sinal de papel nem de língua original)
     * </pre>
     * A metade romaji está protegida; a metade em inglês não. É o mesmo desenho do resto do sistema
     * — a língua original é defendida com mais força que a traduzível — mas aqui o efeito é que as
     * duas camadas da MESMA canção passam por caminhos diferentes.
     *
     * <p>Não estão desprotegidas na prática: quando trazem karaokê cru, {@code temTagKaraoke} as
     * pega pelo CONTEÚDO, antes de qualquer padrão de nome. O que este teste registra é que o pegam
     * por outro caminho, e que alargar o padrão de NOME para alcançá-las exigiria nova medição — o
     * risco de capturar diálogo cresce com cada palavra acrescentada. Pertence à fase de karaokê.
     */
    @Test
    @DisplayName("a camada em inglês de estilo nomeado pela música escapa; a romaji não")
    void estiloNomeadoPelaMusicaEscapaSoNaCamadaInglesa() {
        for (String soIngles : new String[] {
            "Hey World English", "RISE LIGHT RISE English", "EVERYTHING"
        }) {
            assertFalse(detector.podeSerCamadaMusical(soIngles, TEXTO_NEUTRO),
                "se este passou a casar, a lacuna foi fechada e o Javadoc acima ficou obsoleto: "
                    + soIngles);
        }
        for (String comRomaji : new String[] {"Hey World Romaji", "RISE LIGHT RISE Romaji"}) {
            assertTrue(detector.podeSerCamadaMusical(comRomaji, TEXTO_NEUTRO),
                "a camada romaji é reconhecida pela LÍNGUA, não pelo papel: " + comRomaji);
        }
    }
}
