package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.application.ProtecaoCamadasMusicaisService;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.trocaTipoLegenda.domain.ClassificacaoCamadas;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova a regra que separa SÍLABA DE TIMING de fala legítima —
 * a única regra nova que o desacoplamento trouxe, e a que pode comer diálogo se estiver
 * errada.
 *
 * <p>INVARIANTE CENTRAL: a ausência de espaço só significa "sílaba" DENTRO de linha
 * musical. Fora dela, "Sim!" e "Banagher!" são fala e jamais são descartados. É essa
 * conjunção que impede a regra de destruir legenda.
 */
class ClassificadorCamadaMusicalLegendaAdapterTest {

    private static final String CABECALHO = String.join("\n",
        "[Events]",
        "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
        "");

    private final ClassificadorCamadaMusicalLegendaAdapter adaptador = criar();

    private static ClassificadorCamadaMusicalLegendaAdapter criar() {
        DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();
        return new ClassificadorCamadaMusicalLegendaAdapter(
            detector, new ProtecaoCamadasMusicaisService(detector));
    }

    /**
     * Cada linha recebe uma janela PRÓPRIA, derivada do índice. Não é detalhe cosmético:
     * o pareamento de camadas do peer agrupa linhas que ocupam o MESMO tempo na tela, e
     * com janelas idênticas ele enxerga um par romaji×tradução onde só há sílabas soltas
     * — protegendo a linha em vez de deixá-la ser descartada. Num karaokê real cada
     * sílaba acende no seu próprio instante, que é o que se reproduz aqui.
     */
    private static EventoLegenda evento(int indice, String estilo, String texto) {
        String inicio = String.format("0:01:%02d.00", indice * 2);
        String fim = String.format("0:01:%02d.00", indice * 2 + 1);
        return new EventoLegenda(indice, "Dialogue", estilo,
            "Dialogue: 0," + inicio + "," + fim + "," + estilo + ",,0,0,0,fx,", texto);
    }

    private ClassificacaoCamadas classificar(List<EventoLegenda> eventos) {
        return adaptador.classificar(new DocumentoLegenda(CABECALHO, eventos, "\n", false));
    }

    @Test
    @DisplayName("fala curta SEM espaço em estilo de diálogo NUNCA é sílaba")
    void dialogoCurtoNaoEhSilaba() {
        ClassificacaoCamadas c = classificar(List.of(
            evento(0, "Default", "Sim!"),
            evento(1, "Default", "Banagher!"),
            evento(2, "Default", "{\\i1}Audrey...{\\i0}"),
            evento(3, "Dialogue", "Não!")));

        assertFalse(c.ehSilabaDeTiming(0), "\"Sim!\" é fala");
        assertFalse(c.ehSilabaDeTiming(1), "\"Banagher!\" é fala");
        assertFalse(c.ehSilabaDeTiming(2), "\"Audrey...\" é fala");
        assertFalse(c.ehSilabaDeTiming(3), "\"Não!\" é fala");
    }

    @Test
    @DisplayName("palavra isolada em estilo musical reconhecido é sílaba de timing")
    void silabaEmEstiloMusical() {
        // "OP2"/"ED2" casam no padrão amplo do peer (lookaround de letra, dígito não quebra).
        ClassificacaoCamadas c = classificar(List.of(
            evento(0, "OP2", "{\\pos(500,1050)\\blur2}feel"),
            evento(1, "OP2", "{\\pos(560,1050)\\blur2}on"),
            evento(2, "ED2", "{\\pos(620,1050)\\blur2}Ma")));

        assertTrue(c.ehSilabaDeTiming(0), "\"feel\" solto em OP2 é sílaba");
        assertTrue(c.ehSilabaDeTiming(1), "\"on\" solto em OP2 é sílaba");
        assertTrue(c.ehSilabaDeTiming(2), "\"Ma\" solto em ED2 é sílaba");
    }

    @Test
    @DisplayName("OPL2: o estilo que o padrão do peer NÃO alcança também é reconhecido")
    void opl2ReconhecidoApesarDoBuracoDoPeer() {
        // ESTILO_MUSICA_AMPLO_PATTERN do peer usa lookaround de LETRA, então "OPL2" escapa
        // (o "L" depois de "OP"). É o estilo do Gundam Unicorn RE:0096, 3.410 linhas no
        // acervo. Sem o reconhecimento próprio desta fatia, as sílabas dele sobreviviam ao
        // descarte — medido: 141 legendas de uma palavra continuavam no arquivo achatado.
        ClassificacaoCamadas c = classificar(List.of(
            evento(0, "OPL2", "{\\pos(500,1050)\\blur2}feel"),
            evento(1, "OPL2", "{\\pos(560,1050)\\blur2}on"),
            evento(2, "OPL2", "{\\fad(50,200)\\pos(960,1050)}Do you feel alone")));

        assertTrue(c.ehSilabaDeTiming(0), "\"feel\" em OPL2 é sílaba");
        assertTrue(c.ehSilabaDeTiming(1), "\"on\" em OPL2 é sílaba");
        assertFalse(c.ehSilabaDeTiming(2), "a frase inteira permanece");
    }

    @Test
    @DisplayName("o padrão próprio NÃO captura estilo de diálogo que comece com as letras op/ed")
    void padraoProprioNaoComeDialogo() {
        // Se o reconhecimento fosse por prefixo solto, "Opening Narration" e "Editor" seriam
        // musicais e sua fala curta viraria sílaba descartada.
        ClassificacaoCamadas c = classificar(List.of(
            evento(0, "Editor", "Sim!"),
            evento(1, "Operador", "Agora!"),
            evento(2, "Educacional", "Certo!")));

        assertFalse(c.ehSilabaDeTiming(0), "\"Editor\" não é estilo musical");
        assertFalse(c.ehSilabaDeTiming(1), "\"Operador\" não é estilo musical");
        assertFalse(c.ehSilabaDeTiming(2), "\"Educacional\" não é estilo musical");
    }

    @Test
    @DisplayName("frase inteira em estilo musical NÃO é sílaba — é a legenda da música")
    void fraseMusicalNaoEhSilaba() {
        ClassificacaoCamadas c = classificar(List.of(
            evento(0, "OP2", "{\\fad(50,200)\\pos(960,1050)}Do you feel alone"),
            evento(1, "OP2", "{\\pos(960,1050)}Can you hear me now")));

        assertFalse(c.ehSilabaDeTiming(0), "frase com espaço é a legenda de verdade");
        assertFalse(c.ehSilabaDeTiming(1), "frase com espaço é a legenda de verdade");
    }

    @Test
    @DisplayName("quebra \\N conta como espaço: frase em duas linhas não é sílaba")
    void quebraDeLinhaContaComoEspaco() {
        ClassificacaoCamadas c = classificar(List.of(
            evento(0, "OP2", "{\\pos(960,1050)}Have\\Na little break")));

        assertFalse(c.ehSilabaDeTiming(0));
    }

    @Test
    @DisplayName("documento nulo devolve classificação vazia sem lançar")
    void documentoNuloNaoQuebra() {
        ClassificacaoCamadas c = adaptador.classificar(null);
        assertFalse(c.devePreservar(0));
        assertFalse(c.ehSilabaDeTiming(0));
    }
}
