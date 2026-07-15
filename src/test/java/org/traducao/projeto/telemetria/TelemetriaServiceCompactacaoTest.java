package org.traducao.projeto.telemetria;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teto de avisos por episódio no JSON canônico: sem ele, os textos de aviso
 * dominavam o arquivo de telemetria (21,9 mil avisos ≈ 85% dos 3,5MB medidos
 * em 2026-07-09) e eram regravados a cada registro.
 */
class TelemetriaServiceCompactacaoTest {

    private static LlmTelemetria traducaoComAvisos(int quantidade) {
        return new LlmTelemetria(
            "episodio.ass", "modelo-teste", 1000, 900, 100, 60_000L,
            IntStream.range(0, quantidade).mapToObj(i -> "Aviso número " + i).toList(),
            "Anime Teste", "Temporada 1", "2026-07-09T00:00:00Z");
    }

    @Test
    void episodioComPoucosAvisosPassaIntacto() {
        LlmTelemetria original = traducaoComAvisos(10);
        assertSame(original, TelemetriaService.comAvisosLimitados(original));
    }

    @Test
    void episodioComAvisosDemaisEncolheParaAmostraComResumo() {
        LlmTelemetria compacta = TelemetriaService.comAvisosLimitados(traducaoComAvisos(500));

        List<String> avisos = compacta.errosOcorridos();
        assertEquals(31, avisos.size(), "30 avisos de amostra + 1 linha-resumo");
        assertEquals("Aviso número 0", avisos.getFirst());
        assertTrue(avisos.getLast().contains("+470 avisos omitidos"),
            "linha-resumo deve dizer quantos foram omitidos");

        // Métricas e identificação não podem mudar na compactação.
        assertEquals("episodio.ass", compacta.nomeEpisodio());
        assertEquals(1000, compacta.totalLinhas());
        assertEquals(60_000L, compacta.tempoTotalMs());
    }

    @Test
    void compactacaoEIdempotente() {
        // Regressão 2026-07-09: recompactar a lista já compactada destruía o
        // marcador real ("(+625" virava "(+1") a cada boot.
        LlmTelemetria umaVez = TelemetriaService.comAvisosLimitados(traducaoComAvisos(500));
        LlmTelemetria duasVezes = TelemetriaService.comAvisosLimitados(umaVez);

        assertSame(umaVez, duasVezes, "lista já compactada deve passar intacta");
        assertTrue(duasVezes.errosOcorridos().getLast().contains("+470 avisos omitidos"));
    }

    @Test
    void avisosNulosNaoQuebramCompactacao() {
        LlmTelemetria semAvisos = new LlmTelemetria(
            "ep.ass", "m", 1, 1, 0, 1L, null, "A", "T1", "2026-07-09T00:00:00Z");
        assertSame(semAvisos, TelemetriaService.comAvisosLimitados(semAvisos));
    }
}
