package org.traducao.projeto.analisadorMidia.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.analisadorMidia.infrastructure.adapters.FfprobeAdapter;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que a Análise de Mídia (Opção 1) trata a
 * telemetria como dataset PERMANENTE — mídias de lotes anteriores não são
 * apagadas ao analisar um novo lote, reanalisar a mesma mídia deduplica em vez
 * de duplicar, e nenhuma pasta {@code relatorios/} é criada junto dos vídeos.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa um {@link FfprobeAdapter} falso (sem ffprobe
 * real) e um {@link TelemetriaService} próprio; a suíte roda com
 * {@code kronos.dir.base} redirecionado, então a telemetria vai para a árvore
 * descartável e a leitura do JSON canônico reflete apenas este teste.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer perda de histórico, duplicação
 * indevida ou criação de pasta junto da mídia dispara asserção JUnit.
 */
class AnalisarMidiaTelemetriaTest {

    private static final String FFPROBE_JSON = """
        {"format":{"format_name":"matroska","size":"1000","duration":"1440.0","bit_rate":"5000000"},
         "streams":[{"index":0,"codec_type":"video","codec_name":"h264","width":1920,"height":1080,"r_frame_rate":"24/1"}]}
        """;

    private static FfprobeAdapter ffprobeFake() {
        return new FfprobeAdapter() {
            @Override
            protected String executarFfprobeJson(Path caminhoVideo) {
                return FFPROBE_JSON;
            }
        };
    }

    private static List<String> midiasPersistidas() throws IOException {
        Path canonico = DiretorioBaseKronos.resolver("logs", "telemetria_compartilhada.json");
        JsonNode midias = new ObjectMapper().readTree(canonico.toFile()).get("midias");
        List<String> nomes = new ArrayList<>();
        if (midias != null) {
            midias.forEach(m -> nomes.add(m.path("nomeArquivo").asText()));
        }
        return nomes;
    }

    @Test
    void novoLoteNaoApagaHistoricoEReanaliseDeduplicaSemCriarPasta(
        @TempDir Path pastaA, @TempDir Path pastaB) throws IOException {

        Files.createFile(pastaA.resolve("A.mkv"));
        Files.createFile(pastaB.resolve("B.mkv"));

        TelemetriaService telemetria = new TelemetriaService();
        ClassificadorLegendaService classificador = new ClassificadorLegendaService();
        AnalisarMidiaUseCase useCase = new AnalisarMidiaUseCase(
            ffprobeFake(),
            telemetria,
            new LocalizadorVideosService(),
            classificador,
            new TelemetriaMidiaMapper(),
            new RelatorioMidiaTextoFormatter(classificador));

        // Lote 1: mídia A.
        useCase.executar(pastaA, null);
        assertTrue(midiasPersistidas().contains("A.mkv"), "A deveria estar registrada");

        // Lote 2: mídia B, em pasta separada. Sem limparLote(), A permanece.
        useCase.executar(pastaB, null);
        List<String> aposLote2 = midiasPersistidas();
        assertTrue(aposLote2.contains("A.mkv"), "A NÃO pode ser apagada ao analisar o lote de B");
        assertTrue(aposLote2.contains("B.mkv"), "B deveria estar registrada");
        assertEquals(2, aposLote2.size(), "dataset deve conter exatamente A e B");

        // Reanálise de A: deduplica por nome, não cria terceira entrada.
        useCase.executar(pastaA, null);
        List<String> aposReanalise = midiasPersistidas();
        assertEquals(2, aposReanalise.size(), "reanalisar A não pode duplicar; dataset continua com A e B");
        assertEquals(1, aposReanalise.stream().filter("A.mkv"::equals).count(), "A deve aparecer uma única vez");

        // Nenhuma pasta relatorios/ criada junto das mídias.
        assertFalse(Files.exists(pastaA.resolve("relatorios")), "não deve criar relatorios/ junto da mídia A");
        assertFalse(Files.exists(pastaB.resolve("relatorios")), "não deve criar relatorios/ junto da mídia B");
        Path paiA = pastaA.getParent();
        if (paiA != null) {
            assertFalse(Files.exists(paiA.resolve("relatorios")),
                "não deve criar relatorios/ no diretório pai da mídia");
        }
    }
}
