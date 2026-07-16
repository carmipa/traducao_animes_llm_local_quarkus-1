package org.traducao.projeto.traducao.infrastructure.legenda;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EscritorLegendaAssTest {

    @TempDir
    Path tempDir;

    private final EscritorLegendaAss escritor = new EscritorLegendaAss();

    @Test
    void preservaCabecalhoSemNormalizarFonteImplicitamente() throws Exception {
        String cabecalho = """
            [Script Info]
            ScriptType: v4.00+

            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: Dialogue,.VnBook-Antiqua,75

            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            """;
        DocumentoLegenda documento = new DocumentoLegenda(
            cabecalho,
            List.of(new EventoLegenda(0, "Dialogue", "Dialogue",
                "Dialogue: 0,0:00:01.00,0:00:02.00,Dialogue,,0,0,0,,",
                "Não é mais um sonho.")),
            "\n",
            false
        );

        Path saida = tempDir.resolve("saida.ass");
        escritor.escrever(saida, documento);

        String conteudo = Files.readString(saida, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains("Style: Dialogue,.VnBook-Antiqua,75"));
        assertTrue(conteudo.contains("Não é mais um sonho."));
    }
}
