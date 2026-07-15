package org.traducao.projeto.legendasExtracao.infrastructure.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorAssStrategy;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorPgsStrategy;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorSrtStrategy;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorStrategy;
import org.traducao.projeto.legendasExtracao.domain.FormatoLegenda;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.legendasExtracao.infrastructure.adapters.FfmpegAdapter;
import org.traducao.projeto.legendasExtracao.infrastructure.adapters.MkvToolNixAdapter;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza — antes e depois da subfase D-Ext — a
 * composição CDI dos extratores de vídeo e das strategies de formato, que hoje é
 * montada por producers de coleção. Congela o contrato de agregação consumido por
 * {@code ExtrairLegendaUseCase} para que a mudança do LOCAL dos producers (de
 * {@code traducao.RestClientConfig} para a config própria de {@code legendasExtracao})
 * não altere quem é injetado nem a resolução por extensão/formato.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code List<ExtratorVideoPort>} contém exatamente {@link MkvToolNixAdapter}
 *       e {@link FfmpegAdapter}, sem duplicatas nem implementações inesperadas.</li>
 *   <li>{@code List<ExtratorStrategy>} contém exatamente {@link ExtratorAssStrategy},
 *       {@link ExtratorSrtStrategy} e {@link ExtratorPgsStrategy}, idem.</li>
 *   <li>Cada formato (ASS/SRT/PGS) tem exatamente uma strategy compatível; cada
 *       extensão de contêiner conhecida (.mkv/.mp4) tem exatamente um adapter
 *       compatível; extensão desconhecida não tem adapter compatível.</li>
 *   <li>Comparações por CONJUNTO/tipo, nunca por ordem de lista.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Divergência de tamanho, tipo inesperado, duplicata ou mudança na resolução de
 * compatibilidade reprova o teste — sinalizando regressão do contrato CDI antes
 * ou depois da movimentação dos producers.
 */
@QuarkusTest
class ExtratoresInjecaoIT {

    @Inject
    List<ExtratorVideoPort> extratoresVideoPort;

    @Inject
    List<ExtratorStrategy> extratoresStrategy;

    @Test
    @DisplayName("List<ExtratorVideoPort>: exatamente MkvToolNix + Ffmpeg, sem duplicatas nem inesperados")
    void colecaoDeAdaptadoresDeVideo() {
        assertEquals(2, extratoresVideoPort.size(), "Esperados exatamente 2 ExtratorVideoPort");
        assertEquals(1, contar(extratoresVideoPort, MkvToolNixAdapter.class), "Exatamente um MkvToolNixAdapter");
        assertEquals(1, contar(extratoresVideoPort, FfmpegAdapter.class), "Exatamente um FfmpegAdapter");
        assertTrue(
            extratoresVideoPort.stream().allMatch(a -> a instanceof MkvToolNixAdapter || a instanceof FfmpegAdapter),
            "Nenhuma implementação inesperada de ExtratorVideoPort");
    }

    @Test
    @DisplayName("List<ExtratorStrategy>: exatamente ASS + SRT + PGS, sem duplicatas nem inesperados")
    void colecaoDeStrategies() {
        assertEquals(3, extratoresStrategy.size(), "Esperadas exatamente 3 ExtratorStrategy");
        assertEquals(1, contar(extratoresStrategy, ExtratorAssStrategy.class), "Exatamente uma ExtratorAssStrategy");
        assertEquals(1, contar(extratoresStrategy, ExtratorSrtStrategy.class), "Exatamente uma ExtratorSrtStrategy");
        assertEquals(1, contar(extratoresStrategy, ExtratorPgsStrategy.class), "Exatamente uma ExtratorPgsStrategy");
        assertTrue(
            extratoresStrategy.stream().allMatch(s ->
                s instanceof ExtratorAssStrategy || s instanceof ExtratorSrtStrategy || s instanceof ExtratorPgsStrategy),
            "Nenhuma implementação inesperada de ExtratorStrategy");
    }

    @Test
    @DisplayName("Cada formato tem exatamente uma strategy compatível")
    void resolucaoPorFormato() {
        assertResolveUnica(FormatoLegenda.ASS, ExtratorAssStrategy.class);
        assertResolveUnica(FormatoLegenda.SRT, ExtratorSrtStrategy.class);
        assertResolveUnica(FormatoLegenda.PGS, ExtratorPgsStrategy.class);
    }

    @Test
    @DisplayName("Cada contêiner conhecido tem exatamente um adapter; desconhecido não tem")
    void resolucaoPorExtensao() {
        List<ExtratorVideoPort> mkv = compativeis(Path.of("episodio.mkv"));
        assertEquals(1, mkv.size(), "Exatamente um adapter compatível com .mkv");
        assertTrue(mkv.get(0) instanceof MkvToolNixAdapter, ".mkv deve resolver para MkvToolNixAdapter");

        List<ExtratorVideoPort> mp4 = compativeis(Path.of("episodio.mp4"));
        assertEquals(1, mp4.size(), "Exatamente um adapter compatível com .mp4");
        assertTrue(mp4.get(0) instanceof FfmpegAdapter, ".mp4 deve resolver para FfmpegAdapter");

        assertEquals(0, compativeis(Path.of("episodio.xyz")).size(),
            "Extensão desconhecida não deve ter adapter compatível");
    }

    private void assertResolveUnica(FormatoLegenda formato, Class<? extends ExtratorStrategy> esperada) {
        List<ExtratorStrategy> compativeis = extratoresStrategy.stream().filter(s -> s.suporta(formato)).toList();
        assertEquals(1, compativeis.size(), "Formato " + formato + " deve ter exatamente uma strategy compatível");
        assertTrue(esperada.isInstance(compativeis.get(0)),
            "Formato " + formato + " deve resolver para " + esperada.getSimpleName());
    }

    private List<ExtratorVideoPort> compativeis(Path video) {
        return extratoresVideoPort.stream().filter(a -> a.suporta(video)).toList();
    }

    private static long contar(List<?> lista, Class<?> tipo) {
        return lista.stream().filter(tipo::isInstance).count();
    }
}
