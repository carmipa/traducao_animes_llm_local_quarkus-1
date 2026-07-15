package org.traducao.projeto.analisadorMidia.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.ContainerInfo;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.VideoInfo;
import org.traducao.projeto.telemetria.MidiaTelemetria;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o mapeador de telemetria de mídia: extração de metadados técnicos e a
 * INVARIANTE de privacidade (caminho relativizado, sem raiz absoluta pessoal).
 * Componente extraído do AnalisarMidiaUseCase (Etapa 8).
 */
class TelemetriaMidiaMapperTest {

    private final TelemetriaMidiaMapper mapper = new TelemetriaMidiaMapper();

    @Test
    void mapeiaMetadadosTecnicosERelativizaCaminhoParaPrivacidade(@TempDir Path entrada) {
        Path video = entrada.resolve("sub").resolve("A.mkv");
        ContainerInfo container = new ContainerInfo("matroska", 2L * 1024 * 1024, 1440.0, 5000L, "enc");
        VideoInfo v = new VideoInfo(0, "H264", "H.264", 1920, 1080, 8, 24.0, "16:9", 5000L);
        LegendaInfo leg = new LegendaInfo(2, 0, "eng", "ASS", "ass", "t",
            "ASS (Estilizada)", "ASS", "TEXTO", true, true, false, false, false, false, 1400.0, 40.0);
        AuditoriaResultado resultado = new AuditoriaResultado(
            video, "A.mkv", container, List.of(v), List.of(), List.of(leg), List.of(), List.of(), List.of());

        MidiaTelemetria tel = mapper.mapear(resultado, entrada, "2026-07-14T00:00:00Z");

        // Privacidade: caminho relativizado, sem a raiz absoluta pessoal.
        assertTrue(tel.nomeArquivo().replace('\\', '/').endsWith("sub/A.mkv"), tel.nomeArquivo());
        assertFalse(tel.nomeArquivo().contains(entrada.toAbsolutePath().toString()),
            "não deve conter o caminho absoluto pessoal");

        assertEquals("matroska", tel.formatoContainer());
        assertEquals(2.0, tel.tamanhoMB(), 0.001);
        assertEquals(1440.0, tel.duracaoSegundos(), 0.001);
        assertEquals("H264", tel.codecVideo());
        assertEquals("1920x1080", tel.resolucao());
        assertEquals(24.0, tel.fps(), 0.001);
        assertEquals("2026-07-14T00:00:00Z", tel.registradoEm());

        assertEquals(1, tel.legendas().size());
        MidiaTelemetria.LegendaTelemetria lt = tel.legendas().get(0);
        assertEquals(1, lt.indexRelativo());
        assertEquals("eng", lt.idioma());
        assertEquals("ASS", lt.tipo());
        assertTrue(lt.traduzivel());
        assertEquals(40.0, lt.diferencaFimSegundos(), 0.001);
    }

    @Test
    void semTrilhaDeVideoUsaValoresPadrao(@TempDir Path entrada) {
        Path video = entrada.resolve("B.mkv");
        ContainerInfo container = new ContainerInfo("matroska", 1024L * 1024, 60.0, 0L, "enc");
        AuditoriaResultado resultado = new AuditoriaResultado(
            video, "B.mkv", container, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        MidiaTelemetria tel = mapper.mapear(resultado, entrada, "2026-07-14T00:00:00Z");

        assertEquals("N/A", tel.codecVideo());
        assertEquals("N/A", tel.resolucao());
        assertEquals(0.0, tel.fps(), 0.001);
        assertTrue(tel.legendas().isEmpty());
    }
}
