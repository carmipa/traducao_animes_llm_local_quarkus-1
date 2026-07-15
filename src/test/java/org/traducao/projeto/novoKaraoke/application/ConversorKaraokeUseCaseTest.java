package org.traducao.projeto.novoKaraoke.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducao.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversorKaraokeUseCaseTest {

    @TempDir
    Path tempDir;

    @Test
    void arquivoSemMusicaEhCopiadoByteIdentico() throws Exception {
        Path origem = tempDir.resolve("sem-musica.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        byte[] original = """
            [Script Info]\r
            PlayResY: 1080\r
            \r
            [V4+ Styles]\r
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding\r
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1\r
            \r
            [Events]\r
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text\r
            Comment: 0,0:00:00.00,0:00:01.00,Default,,0,0,0,,template preservado\r
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\r
            """.getBytes(StandardCharsets.UTF_8);
        Files.write(origem, original);

        novoConversor().converterArquivo(origem, destino, true);

        assertEquals(new String(original, StandardCharsets.UTF_8),
            Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8));
    }

    @Test
    void deduplicaRomajiEPtBrNoMesmoTempoELimpaTagsVisiveis() throws Exception {
        Path origem = tempDir.resolve("karaoke.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\pos(100,80)}[]Não importa o quanto eu deseje, nada muda [![TAG1]]\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,Não importa o quanto eu deseje, nada muda"));
        assertFalse(saida.contains("aigan shitemo"));
        assertFalse(saida.contains("[]"));
        assertFalse(saida.contains("TAG1"));
    }

    @Test
    void preservaEventoCurtoSemCoberturaRealMesmoPertoDaLinhaPrincipal() throws Exception {
        Path origem = tempDir.resolve("curta.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:10.00,0:00:12.00,Opening,,0,0,0,,{\\pos(100,40)}Linha principal da música\n"
            + "Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:10.00,0:00:12.00,Karaoke Simples,,0,0,0,,Linha principal da música"));
        assertTrue(saida.contains("Dialogue: 0,0:00:19.00,0:00:20.00,Opening,,0,0,0,,{\\pos(100,80)}Ei"));
    }

    @Test
    void kfxApenasSilabicoViraLinhaSimplesENaoArquivoGrande() throws Exception {
        Path origem = tempDir.resolve("kfx-silabico.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 1,0:00:01.00,0:00:01.30,Opening,,0,0,0,,{\\pos(100,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}fu\n"
            + "Dialogue: 1,0:00:01.02,0:00:01.28,Opening,,0,0,0,,{\\pos(101,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}fu\n"
            + "Dialogue: 1,0:00:01.30,0:00:01.60,Opening,,0,0,0,,{\\pos(130,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}mi\n"
            + "Dialogue: 1,0:00:01.60,0:00:01.90,Opening,,0,0,0,,{\\pos(160,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}ni\n"
            + "Dialogue: 1,0:00:01.90,0:00:02.30,Opening,,0,0,0,,{\\pos(190,40)\\clip(0,0,200,60)\\t(0,100,\\blur4\\fscx120)}hana\n",
            StandardCharsets.UTF_8);

        var resultado = novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:02.30,Karaoke Simples,,0,0,0,,fu mi ni hana"));
        assertFalse(saida.contains("\\t("));
        assertFalse(saida.contains("\\clip("));
        assertFalse(saida.contains("Style: Opening"));
        assertEquals(5, resultado.getEventosKaraokeRemovidos());
        assertEquals(0, resultado.getEventosPreservadosPorSeguranca());
        assertTrue(resultado.getTamanhoNovoBytes() < resultado.getTamanhoOriginalBytes());
    }

    @Test
    void kfxComLayersDuplicadosPrefereLegendaOcidentalSimples() throws Exception {
        Path origem = tempDir.resolve("kfx-duas-faixas.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,30,100,30,0,3000)\\t(0,3000,\\frz1)}ki\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,30,130,30,0,3000)\\t(0,3000,\\frz1)}mi\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(160,30,160,30,0,3000)\\t(0,3000,\\frz1)}no\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,30,100,30,0,3000)\\t(0,3000,\\frz1)}ki\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,30,130,30,0,3000)\\t(0,3000,\\frz1)}mi\n"
            + "Dialogue: 2,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(160,30,160,30,0,3000)\\t(0,3000,\\frz1)}no\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(100,1050,100,1050,0,3000)\\t(0,3000,\\frz1)}O\n"
            + "Dialogue: 1,0:00:01.00,0:00:04.00,Opening,,0,0,0,,{\\move(130,1050,130,1050,0,3000)\\t(0,3000,\\frz1)}i\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:04.00,Karaoke Simples,,0,0,0,,Oi"), saida);
        assertFalse(saida.contains("ki mi no"));
        assertFalse(saida.contains("\\move("));
        assertFalse(saida.contains("\\t("));
    }

    @Test
    void kfxLetraPorLetraContinuoEhCortadoPorFraseComEspacos() throws Exception {
        // KFX real (86): cada letra é um evento, a frase inteira fica na tela ao
        // mesmo tempo e a frase seguinte começa EXATAMENTE quando a anterior
        // termina — o gap nunca separa; o corte tem que vir do vale de concorrência.
        Path origem = tempDir.resolve("kfx-letra-a-letra.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        StringBuilder corpo = new StringBuilder(cabecalho());
        corpo.append(eventosPorLetra("0:00:01.00", "0:00:05.00", "Voce pode"));
        corpo.append(eventosPorLetra("0:00:05.00", "0:00:09.00", "Nada muda"));
        Files.writeString(origem, corpo.toString(), StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke Simples,,0,0,0,,Voce pode"), saida);
        assertTrue(saida.contains("Dialogue: 0,0:00:05.00,0:00:09.00,Karaoke Simples,,0,0,0,,Nada muda"), saida);
        assertFalse(saida.contains("VocepodeNadamuda"), saida);
    }

    @Test
    void deduplicaCamadasComJanelasQuaseIdenticasENaoSoIguais() throws Exception {
        // romaji e tradução simultâneos raramente terminam no MESMO centésimo;
        // a deduplicação precisa agrupar por sobreposição, não por janela exata
        Path origem = tempDir.resolve("janelas-quase-iguais.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:04.96,Opening,,0,0,0,,{\\pos(100,40)}aigan shitemo kongan shitemo kawaranai ya, mou\n"
            + "Dialogue: 0,0:00:01.00,0:00:05.00,Opening,,0,0,0,,{\\pos(100,80)}Não importa o quanto eu deseje, nada muda\n",
            StandardCharsets.UTF_8);

        novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertTrue(saida.contains("Dialogue: 0,0:00:01.00,0:00:05.00,Karaoke Simples,,0,0,0,,Não importa o quanto eu deseje, nada muda"), saida);
        assertFalse(saida.contains("aigan shitemo"), saida);
    }

    @Test
    void blocoKfxQueViraLinhaImplausivelEhPreservadoIntacto() throws Exception {
        // sem vale de concorrência não há como separar as frases: melhor manter o
        // efeito original do que emitir uma parede de texto de 29 segundos
        Path origem = tempDir.resolve("kfx-irreconstruivel.ass");
        Path destino = Files.createDirectory(tempDir.resolve("saida"));
        Files.writeString(origem, cabecalho()
            + eventosPorLetra("0:00:01.00", "0:00:30.00", "Frase longa demais"),
            StandardCharsets.UTF_8);

        var resultado = novoConversor().converterArquivo(origem, destino, true);

        String saida = Files.readString(destino.resolve(origem.getFileName()), StandardCharsets.UTF_8);
        assertFalse(saida.contains("Karaoke Simples"), saida);
        assertTrue(saida.contains("{\\pos(100.0,40)\\t(0,100,\\blur4\\fscx120)}F"), saida);
        assertEquals(0, resultado.getEventosKaraokeRemovidos());
        assertTrue(resultado.getEventosPreservadosPorSeguranca() > 0);
    }

    /** Um evento Dialogue por letra visível, todos na janela inteira da frase (KFX letra-por-letra). */
    private static String eventosPorLetra(String inicio, String fim, String frase) {
        StringBuilder eventos = new StringBuilder();
        double x = 100;
        for (char letra : frase.toCharArray()) {
            if (letra == ' ') {
                x += 40; // espaço não vira evento: só o salto em X marca a palavra
                continue;
            }
            eventos.append("Dialogue: 1,").append(inicio).append(',').append(fim)
                .append(",Opening,,0,0,0,,{\\pos(").append(x).append(",40)\\t(0,100,\\blur4\\fscx120)}")
                .append(letra).append('\n');
            x += 20;
        }
        return eventos.toString();
    }

    @Test
    void ignoraArquivosAuxiliaresQuandoHaEpisodiosPrincipais() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala comum.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86_-_NCOP01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Opening,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);
        Files.writeString(origem.resolve("[DB]86 Special Edition Senya_-_SP_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Especial.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_01_(Dual Audio)_Track6_PT-BR.ass"), processados);
    }

    @Test
    void processaAuxiliaresQuandoPastaTemApenasAuxiliares() throws Exception {
        Path origem = Files.createDirectory(tempDir.resolve("origem"));
        Path destino = tempDir.resolve("saida");
        Files.writeString(origem.resolve("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), cabecalho()
            + "Dialogue: 0,0:00:01.00,0:00:03.00,Ending,,0,0,0,,Letra auxiliar.\n",
            StandardCharsets.UTF_8);

        List<String> processados = novoConversor().simular(origem, destino).stream()
            .map(r -> r.getArquivoOrigem())
            .toList();

        assertEquals(List.of("[DB]86_-_NCED01_(10bit)_Track2_PT-BR.ass"), processados);
    }

    private static ConversorKaraokeUseCase novoConversor() {
        ConversorKaraokeUseCase conversor = new ConversorKaraokeUseCase();
        conversor.detectorKaraoke = new DetectorEfeitoKaraokeService();
        conversor.logStream = new LogStreamSilencioso();
        return conversor;
    }

    private static String cabecalho() {
        return """
            [Script Info]
            PlayResY: 1080

            [V4+ Styles]
            Format: Name,Fontname,Fontsize,PrimaryColour,SecondaryColour,OutlineColour,BackColour,Bold,Italic,Underline,StrikeOut,ScaleX,ScaleY,Spacing,Angle,BorderStyle,Outline,Shadow,Alignment,MarginL,MarginR,MarginV,Encoding
            Style: Default,Arial,48,&H00FFFFFF,&H000000FF,&H00000000,&H96000000,0,0,0,0,100,100,0,0,1,2,1,2,30,30,30,1

            [Events]
            Format: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
            """;
    }

    private static final class LogStreamSilencioso extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // Testes unitarios nao precisam de SSE nem arquivo de log.
        }
    }
}
