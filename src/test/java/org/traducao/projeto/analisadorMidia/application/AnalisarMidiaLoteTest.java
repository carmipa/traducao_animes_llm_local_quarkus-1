package org.traducao.projeto.analisadorMidia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;
import org.traducao.projeto.analisadorMidia.infrastructure.adapters.FfprobeAdapter;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato de LOTE da Análise de Mídia (Opção 1) — todos os
 * arquivos são analisados e a falha de um deles não aborta os demais. É a rede de segurança
 * para a paralelização (bounded) do ffprobe: o comportamento observável não pode mudar.
 *
 * <p>INVARIANTES DO DOMÍNIO: cada arquivo vira sucesso (resultado) ou falha; o total bate; a
 * falha isolada é atribuída ao arquivo certo e não contamina os sucessos.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: contagem divergente ou má atribuição de falha reprova.
 */
class AnalisarMidiaLoteTest {

    private static final String FFPROBE_JSON = """
        {"format":{"format_name":"matroska","size":"1000","duration":"1440.0","bit_rate":"5000000"},
         "streams":[{"index":0,"codec_type":"video","codec_name":"h264","width":1920,"height":1080,"r_frame_rate":"24/1"}]}
        """;

    private static AnalisarMidiaUseCase useCase(FfprobeAdapter ffprobe) {
        ClassificadorLegendaService classificador = new ClassificadorLegendaService();
        return new AnalisarMidiaUseCase(
            ffprobe,
            new TelemetriaService(),
            new LocalizadorVideosService(),
            classificador,
            new TelemetriaMidiaMapper(),
            new RelatorioMidiaTextoFormatter(classificador));
    }

    @Test
    @DisplayName("lote analisa todos os arquivos e a falha de um não aborta os demais")
    void loteAnalisaTodosEUmaFalhaNaoAborta(@TempDir Path pasta) throws IOException {
        Files.createFile(pasta.resolve("A.mkv"));
        Files.createFile(pasta.resolve("B.mkv"));
        Files.createFile(pasta.resolve("C.mkv"));

        FfprobeAdapter ffprobe = new FfprobeAdapter() {
            @Override
            protected String executarFfprobeJson(Path caminhoVideo) {
                if (caminhoVideo.getFileName().toString().equals("B.mkv")) {
                    throw new RuntimeException("ffprobe falhou em B");
                }
                return FFPROBE_JSON;
            }
        };

        ResultadoAnaliseLote r = useCase(ffprobe).executar(pasta, null);

        assertEquals(2, r.resultados().size(), "A e C devem ser analisados apesar da falha de B");
        assertEquals(1, r.falhas().size(), "só B deve falhar");
        assertEquals("B.mkv", r.falhas().get(0).arquivo(), "a falha deve ser atribuída ao arquivo certo");
    }
}
