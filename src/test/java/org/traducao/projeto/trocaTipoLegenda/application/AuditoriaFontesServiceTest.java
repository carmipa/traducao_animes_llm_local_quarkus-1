package org.traducao.projeto.trocaTipoLegenda.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.trocaTipoLegenda.domain.AuditoriaFonteInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaFontesServiceTest {

    private final AuditoriaFontesService service = new AuditoriaFontesService();

    @Test
    void analisaEstilosCorretamenteEIdentificaFontesProblematicas() {
        String cabecalho = """
            [Script Info]
            Title: Test Legend
            Script Type: v4.00+
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1
            Style: Dialogue,.VnBook-Antiqua,75,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,-1,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1
            Style: Title,.VnArial,45,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,2,2,10,10,10,1
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello
            """;

        List<AuditoriaFonteInfo> resultado = service.analisarCabecalho(cabecalho);

        assertEquals(3, resultado.size());

        // Default: Arial (Unicode Seguro)
        AuditoriaFonteInfo infoDefault = resultado.get(0);
        assertEquals("Default", infoDefault.estilo());
        assertEquals("Arial", infoDefault.fonteAtual());
        assertEquals("Arial", infoDefault.fonteSugerida());
        assertFalse(infoDefault.problematica());

        // Dialogue: .VnBook-Antiqua (Problemática -> Arial como padrão seguro)
        AuditoriaFonteInfo infoDialogue = resultado.get(1);
        assertEquals("Dialogue", infoDialogue.estilo());
        assertEquals(".VnBook-Antiqua", infoDialogue.fonteAtual());
        assertEquals("Arial", infoDialogue.fonteSugerida());
        assertTrue(infoDialogue.problematica());

        // Title: .VnArial (Problemática -> Arial)
        AuditoriaFonteInfo infoTitle = resultado.get(2);
        assertEquals("Title", infoTitle.estilo());
        assertEquals(".VnArial", infoTitle.fonteAtual());
        assertEquals("Arial", infoTitle.fonteSugerida());
        assertTrue(infoTitle.problematica());
    }

    @Test
    void retornaVazioSeCabecalhoNuloOuSemSecaoEstilos() {
        assertTrue(service.analisarCabecalho(null).isEmpty());
        assertTrue(service.analisarCabecalho("").isEmpty());
        assertTrue(service.analisarCabecalho("[Script Info]\nTitle: Test").isEmpty());
    }

    @Test
    void encerraParsingAoAtingirProximaSecao() {
        String cabecalho = """
            [V4+ Styles]
            Format: Name, Fontname
            Style: Default,Arial
            [Events]
            Style: OutroEstilo,.VnTimes
            """;
        List<AuditoriaFonteInfo> resultado = service.analisarCabecalho(cabecalho);
        assertEquals(1, resultado.size());
        assertEquals("Default", resultado.get(0).estilo());
    }

    @Test
    void aceitaSecaoV4StylesDeSsaEComparacaoCaseInsensitive() {
        String cabecalho = """
            [V4 Styles]
            Format: Name, Fontname
            Style: Dialogue,.vnbook-antiqua
            """;

        List<AuditoriaFonteInfo> resultado = service.analisarCabecalho(cabecalho);

        assertEquals(1, resultado.size());
        assertEquals(".vnbook-antiqua", resultado.get(0).fonteAtual());
        assertEquals("Arial", resultado.get(0).fonteSugerida());
        assertTrue(resultado.get(0).problematica());
    }

    @Test
    void substituiApenasCampoFontnameDasLinhasStyle() {
        String cabecalho = """
            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: .VnBook-Antiqua,.VnArial,20
            Style: Dialogue,.VnBook-Antiqua,75
            Comment: .VnBook-Antiqua nao deve ser alterado fora de Style
            """;

        AuditoriaFontesService.ResultadoSubstituicaoCabecalho resultado =
            service.substituirFontesProblematicas(cabecalho);

        assertEquals(2, resultado.substituicoes());
        assertTrue(resultado.cabecalho().contains("Style: .VnBook-Antiqua,Arial,20"));
        assertTrue(resultado.cabecalho().contains("Style: Dialogue,Arial,75"));
        assertTrue(resultado.cabecalho().contains("Comment: .VnBook-Antiqua nao deve ser alterado fora de Style"));
    }
}
