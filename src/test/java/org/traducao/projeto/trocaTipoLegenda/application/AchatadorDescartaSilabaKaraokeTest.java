package org.traducao.projeto.trocaTipoLegenda.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;
import org.traducao.projeto.trocaTipoLegenda.domain.ClassificacaoCamadas;
import org.traducao.projeto.trocaTipoLegenda.domain.ports.ClassificadorCamadaMusicalPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que a camada de TIMING do karaokê é DESCARTADA, e não
 * achatada para o estilo de diálogo.
 *
 * <p>Medido no Gundam Unicorn RE:0096 (2026-07-29): achatar o estilo OPL2 do ep.1
 * produzia 141 linhas de até 6 caracteres — "Do", "you", "feel", "a", "lone", "Ma",
 * "ny" —, cada uma virando uma legenda branca própria sobre o vídeo. Na TV, o alvo do
 * achatamento, isso é pior que a tag animada que se queria remover. As 17 linhas com a
 * frase inteira são a legenda de verdade e permanecem.
 *
 * <p>Este teste NÃO levanta o peer {@code legenda}: passa uma classificação fixa pela
 * porta. Antes do desacoplamento isso era impossível — o achatador instanciava
 * {@code DetectorEfeitoKaraokeService} e {@code ProtecaoCamadasMusicaisService} direto.
 */
class AchatadorDescartaSilabaKaraokeTest {

    private static final String CABECALHO = String.join("\n",
        "[V4+ Styles]",
        "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour,"
            + " Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle,"
            + " Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding",
        "Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,"
            + "100,100,0,0,1,2,1,2,30,30,20,1",
        "Style: OPL2,Decorativa,60,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,"
            + "100,100,0,0,1,2,1,8,30,30,20,1",
        "",
        "[Events]",
        "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
        "");

    /** Classificação fixa: a porta responde sem o peer, que é o ponto do desacoplamento. */
    private static ClassificadorCamadaMusicalPort porta(Set<Integer> preservar, Set<Integer> silabas) {
        return documento -> new ClassificacaoCamadas(preservar, silabas);
    }

    private static DocumentoLegenda documento(List<EventoLegenda> eventos) {
        return new DocumentoLegenda(CABECALHO, eventos, "\n", false);
    }

    private static EventoLegenda evento(int indice, String estilo, String texto) {
        String prefixo = "Dialogue: 0,0:01:00.00,0:01:02.00," + estilo + ",,0,0,0,fx,";
        return new EventoLegenda(indice, "Dialogue", estilo, prefixo, texto);
    }

    @Test
    @DisplayName("sílaba de timing é REMOVIDA; a frase inteira sobrevive e vira Default")
    void descartaSilabasEMantemFrase() {
        List<EventoLegenda> eventos = new ArrayList<>(List.of(
            evento(0, "Default", "Bom dia, Banagher."),
            evento(1, "OPL2", "{\\fad(50,200)\\pos(960,1050)}Do you feel alone"),
            evento(2, "OPL2", "{\\pos(500,1050)\\blur2}Do"),
            evento(3, "OPL2", "{\\pos(560,1050)\\blur2}you"),
            evento(4, "OPL2", "{\\pos(620,1050)\\blur2}feel")));

        AchatadorEstilosDecorativosService achatador = new AchatadorEstilosDecorativosService(
            new AuditoriaFontesService(), porta(Set.of(), Set.of(2, 3, 4)));

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(documento(eventos));

        assertEquals(3, r.silabasDescartadas(), "as três sílabas deveriam sair");
        assertEquals(2, r.documento().eventos().size(), "sobram o diálogo e a frase inteira");
        assertTrue(r.houveAchatamento());

        List<String> textos = r.documento().eventos().stream().map(EventoLegenda::texto).toList();
        assertTrue(textos.contains("Bom dia, Banagher."), "diálogo intocado");
        assertTrue(textos.contains("Do you feel alone"), "frase da música vira legenda limpa");
        assertFalse(textos.contains("Do"), "sílaba não pode virar legenda");
        assertFalse(textos.contains("feel"), "sílaba não pode virar legenda");
    }

    @Test
    @DisplayName("linha preservada pela porta não é achatada nem descartada")
    void preservaCamadaOriginal() {
        List<EventoLegenda> eventos = new ArrayList<>(List.of(
            evento(0, "Default", "Bom dia."),
            evento(1, "OPL2", "{\\pos(960,1050)}kimi wa dare")));

        AchatadorEstilosDecorativosService achatador = new AchatadorEstilosDecorativosService(
            new AuditoriaFontesService(), porta(Set.of(1), Set.of()));

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(documento(eventos));

        assertEquals(0, r.silabasDescartadas());
        assertEquals(2, r.documento().eventos().size());
        EventoLegenda romaji = r.documento().eventos().get(1);
        assertEquals("OPL2", romaji.estilo(), "camada original mantém o estilo");
        assertEquals("{\\pos(960,1050)}kimi wa dare", romaji.texto(), "e mantém as tags");
    }

    @Test
    @DisplayName("sem classificação a fatia se comporta como antes de existir a porta")
    void portaVaziaNaoDescartaNada() {
        List<EventoLegenda> eventos = new ArrayList<>(List.of(
            evento(0, "Default", "Bom dia."),
            evento(1, "OPL2", "{\\pos(960,1050)}Do")));

        AchatadorEstilosDecorativosService achatador = new AchatadorEstilosDecorativosService(
            new AuditoriaFontesService(), documento -> ClassificacaoCamadas.VAZIA);

        AchatadorEstilosDecorativosService.Resultado r = achatador.achatar(documento(eventos));

        assertEquals(0, r.silabasDescartadas(), "sem classificação, nada é descartado");
        assertEquals(2, r.documento().eventos().size());
    }
}
