package org.traducao.projeto.legendasExtracao.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.infrastructure.config.ExtratorProperties;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre a identificação de faixas de legenda em contêineres não-MKV (mp4/mov/…)
 * a partir do JSON do {@code ffprobe -show_streams}, sem ffprobe real: substitui
 * o seam de processo externo ({@code executarIdentificacao}).
 */
class FfmpegAdapterTest {

    private static FfmpegAdapter comJson(String json) {
        return new FfmpegAdapter(new ExtratorProperties(), new ObjectMapper()) {
            @Override
            protected String executarIdentificacao(Path videoPath) {
                return json;
            }
        };
    }

    @Test
    void identificaLegendaMovTextComDisposition() {
        String json = """
            {"streams":[
              {"index":0,"codec_type":"video","codec_name":"h264"},
              {"index":1,"codec_type":"audio","codec_name":"aac","tags":{"language":"eng"}},
              {"index":2,"codec_type":"subtitle","codec_name":"mov_text","codec_long_name":"MOV text",
               "tags":{"language":"eng","title":"English"},"disposition":{"default":1,"forced":0}}
            ]}
            """;

        List<FaixaLegenda> faixas = comJson(json).identificarFaixas(Path.of("filme.mp4"));

        assertEquals(1, faixas.size());
        FaixaLegenda leg = faixas.get(0);
        assertEquals(2, leg.id());
        assertEquals("mov_text", leg.codec());
        assertEquals("MOV text", leg.codecId());
        assertEquals("eng", leg.idioma());
        assertEquals("English", leg.nome());
        assertTrue(leg.isDefault());
        assertFalse(leg.isForced());
    }

    @Test
    void semLegendaRetornaListaVazia() {
        String json = """
            {"streams":[{"index":0,"codec_type":"video","codec_name":"h264"}]}
            """;
        assertTrue(comJson(json).identificarFaixas(Path.of("raw.mp4")).isEmpty());
    }

    @Test
    void idiomaAusenteCaiEmUnd() {
        String json = """
            {"streams":[{"index":1,"codec_type":"subtitle","codec_name":"mov_text"}]}
            """;
        List<FaixaLegenda> faixas = comJson(json).identificarFaixas(Path.of("x.mp4"));
        assertEquals(1, faixas.size());
        assertEquals("und", faixas.get(0).idioma());
    }

    @Test
    void jsonInvalidoViraExtratorException() {
        assertThrows(ExtratorException.class,
            () -> comJson("nao e json").identificarFaixas(Path.of("x.mp4")));
    }

    @Test
    void suportaMp4EMovMasNaoMkv() {
        FfmpegAdapter a = comJson("{}");
        assertTrue(a.suporta(Path.of("a.mp4")));
        assertTrue(a.suporta(Path.of("a.mov")));
        assertFalse(a.suporta(Path.of("a.mkv")));
    }

    /**
     * E3 da auditoria de 08/10/2026 (reproduzido na aplicacao): a extracao grava em "X_TrackN.ass.part"
     * e o ffmpeg, sem -f, respondia "Unable to choose an output format" -- toda extracao por ffmpeg
     * falhava. Usa o ffmpeg REAL: o defeito so existe no comportamento do binario. Sem ffmpeg, o
     * teste sai como NAO VERIFICADO (assumption), nunca como aprovado.
     */
    @Test
    void extraiFaixaAssParaTemporarioPartComOFfmpegReal(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir)
            throws Exception {
        boolean temFfmpeg;
        try {
            temFfmpeg = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start().waitFor() == 0;
        } catch (java.io.IOException e) {
            temFfmpeg = false;
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(temFfmpeg, "ffmpeg ausente: NAO VERIFICADO");
        java.nio.file.Path ass = java.nio.file.Files.writeString(dir.resolve("s.ass"), String.join("\n",
            "[Script Info]", "ScriptType: v4.00+", "", "[V4+ Styles]",
            "Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, "
                + "Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, "
                + "MarginR, MarginV, Encoding",
            "Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,1,0,2,10,10,10,1",
            "", "[Events]", "Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text",
            "Dialogue: 0,0:00:00.50,0:00:01.50,Default,,0,0,0,,Hello there", ""));
        java.nio.file.Path video = dir.resolve("v.mkv");
        int rc = new ProcessBuilder("ffmpeg", "-y", "-v", "error", "-f", "lavfi", "-i", "color=c=black:s=160x120:d=2",
            "-i", ass.toString(), "-map", "0", "-map", "1", "-c:v", "mpeg4", "-c:s", "ass", video.toString())
            .redirectErrorStream(true).start().waitFor();
        org.junit.jupiter.api.Assumptions.assumeTrue(rc == 0, "nao consegui montar o video de teste: NAO VERIFICADO");
        java.nio.file.Path saida = dir.resolve("v_Track1.ass.part");

        new FfmpegAdapter(new ExtratorProperties(), new ObjectMapper()).extrairTrilha(video, 1, saida);

        assertTrue(java.nio.file.Files.readString(saida).contains("Hello there"),
            "a faixa ASS tem de ser extraida para o temporario .part");
    }
}
