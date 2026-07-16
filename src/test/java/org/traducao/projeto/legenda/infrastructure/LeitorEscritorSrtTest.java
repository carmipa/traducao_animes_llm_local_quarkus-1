package org.traducao.projeto.legenda.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.legenda.domain.DocumentoLegenda;
import org.traducao.projeto.legenda.domain.EventoLegenda;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a leitura/escrita nativa de SRT: preservação de índice e timestamps,
 * quebra interna via \N, round-trip e troca apenas do texto (o pipeline traduz
 * só o texto, mantendo tempos).
 */
class LeitorEscritorSrtTest {

    @TempDir
    Path dir;

    private final LeitorLegendaSrt leitor = new LeitorLegendaSrt();
    private final EscritorLegendaSrt escritor = new EscritorLegendaSrt();

    @Test
    void leSrtComIndiceTimestampsEMultilinha() throws IOException {
        String srt = "1\n00:00:01,000 --> 00:00:02,000\nHello\n\n"
                   + "2\n00:00:03,000 --> 00:00:04,500\nLine A\nLine B\n";
        Path f = dir.resolve("ep.srt");
        Files.writeString(f, srt);

        DocumentoLegenda doc = leitor.ler(f);

        assertEquals(2, doc.eventos().size());
        EventoLegenda e1 = doc.eventos().get(0);
        assertEquals(1, e1.indice());
        assertTrue(e1.isDialogo());
        assertEquals("Default", e1.estilo());
        assertEquals("00:00:01,000 --> 00:00:02,000", e1.prefixo());
        assertEquals("Hello", e1.texto());

        EventoLegenda e2 = doc.eventos().get(1);
        assertEquals(2, e2.indice());
        assertEquals("Line A\\NLine B", e2.texto()); // quebra interna vira \N
    }

    @Test
    void roundTripPreservaEstrutura() throws IOException {
        String srt = "1\n00:00:01,000 --> 00:00:02,000\nHello\n\n"
                   + "2\n00:00:03,000 --> 00:00:04,500\nLine A\nLine B\n\n";
        Path f = dir.resolve("ep.srt");
        Files.writeString(f, srt);

        DocumentoLegenda doc = leitor.ler(f);
        Path out = dir.resolve("out.srt");
        escritor.escrever(out, doc);

        DocumentoLegenda relido = leitor.ler(out);
        assertEquals(2, relido.eventos().size());
        assertEquals("Hello", relido.eventos().get(0).texto());
        assertEquals("00:00:03,000 --> 00:00:04,500", relido.eventos().get(1).prefixo());
        assertEquals("Line A\\NLine B", relido.eventos().get(1).texto());
    }

    @Test
    void escritaTrocaSoOTextoMantendoTempos() throws IOException {
        String srt = "1\n00:00:01,000 --> 00:00:02,000\nHello\n";
        Path f = dir.resolve("ep.srt");
        Files.writeString(f, srt);

        DocumentoLegenda doc = leitor.ler(f);
        EventoLegenda traduzido = doc.eventos().get(0).comTexto("Olá");
        DocumentoLegenda novo = new DocumentoLegenda(
            doc.cabecalho(), List.of(traduzido), doc.quebraDeLinha(), doc.comBom());

        Path out = dir.resolve("out.srt");
        escritor.escrever(out, novo);

        String conteudo = Files.readString(out);
        assertTrue(conteudo.startsWith("1"));
        assertTrue(conteudo.contains("00:00:01,000 --> 00:00:02,000"));
        assertTrue(conteudo.contains("Olá"));
    }
}
