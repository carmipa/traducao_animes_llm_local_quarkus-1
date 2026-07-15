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
}
