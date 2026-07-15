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
 * Cobre a identificação de faixas de legenda a partir do JSON do
 * {@code mkvmerge --identify}, sem MKVToolNix real: substitui o seam de processo
 * externo ({@code executarIdentificacao}) por saída canônica.
 */
class MkvToolNixAdapterTest {

    private static MkvToolNixAdapter comJson(String json) {
        return new MkvToolNixAdapter(new ExtratorProperties(), new ObjectMapper()) {
            @Override
            protected String executarIdentificacao(Path mkvPath) {
                return json;
            }
        };
    }

    @Test
    void identificaSomenteFaixasDeLegendaComSuasPropriedades() {
        String json = """
            {"tracks":[
              {"id":0,"type":"video","codec":"HEVC","properties":{}},
              {"id":1,"type":"audio","codec":"FLAC","properties":{"language":"jpn"}},
              {"id":2,"type":"subtitles","codec":"SubStationAlpha",
               "properties":{"codec_id":"S_TEXT/ASS","language":"eng","track_name":"Full",
                             "default_track":true,"forced_track":false}},
              {"id":3,"type":"subtitles","codec":"HDMV PGS",
               "properties":{"codec_id":"S_HDMV/PGS","language":"por","forced_track":true}}
            ]}
            """;

        List<FaixaLegenda> faixas = comJson(json).identificarFaixas(Path.of("ep.mkv"));

        assertEquals(2, faixas.size());

        FaixaLegenda ass = faixas.get(0);
        assertEquals(2, ass.id());
        assertEquals("S_TEXT/ASS", ass.codecId());
        assertEquals("eng", ass.idioma());
        assertEquals("Full", ass.nome());
        assertTrue(ass.isDefault());
        assertFalse(ass.isForced());

        FaixaLegenda pgs = faixas.get(1);
        assertEquals("S_HDMV/PGS", pgs.codecId());
        assertEquals("por", pgs.idioma());
        assertTrue(pgs.isForced());
        assertEquals("Sem Titulo", pgs.nome()); // track_name ausente
    }

    @Test
    void semLegendasRetornaListaVazia() {
        String json = """
            {"tracks":[{"id":0,"type":"video","codec":"H264","properties":{}}]}
            """;
        assertTrue(comJson(json).identificarFaixas(Path.of("raw.mkv")).isEmpty());
    }

    @Test
    void idiomaAusenteCaiEmUnd() {
        String json = """
            {"tracks":[{"id":1,"type":"subtitles","codec":"ASS","properties":{"codec_id":"S_TEXT/ASS"}}]}
            """;
        List<FaixaLegenda> faixas = comJson(json).identificarFaixas(Path.of("x.mkv"));
        assertEquals(1, faixas.size());
        assertEquals("und", faixas.get(0).idioma());
    }

    @Test
    void jsonInvalidoViraExtratorException() {
        assertThrows(ExtratorException.class,
            () -> comJson("nao e json").identificarFaixas(Path.of("x.mkv")));
    }

    @Test
    void suportaApenasMkvEWebm() {
        MkvToolNixAdapter a = comJson("{}");
        assertTrue(a.suporta(Path.of("a.mkv")));
        assertTrue(a.suporta(Path.of("a.webm")));
        assertFalse(a.suporta(Path.of("a.mp4")));
    }
}
