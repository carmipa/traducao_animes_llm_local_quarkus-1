package org.traducao.projeto.analisadorMidia.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica o contrato JSON publicado no SSE da Análise de Mídia (o que o front
 * renderiza em cartões/tabelas): campos estruturados presentes e SEM vazar o
 * caminho local nem os logs internos (via {@code @JsonIgnore}).
 */
class ResultadoAnaliseLoteSerializacaoTest {

    @Test
    void serializaContratoEstruturadoSemCaminhosNemLogs() throws Exception {
        LegendaInfo leg = new LegendaInfo(2, 0, "eng", "ASS", "ass", "Full",
            "ASS (estilizada)", "ASS", "TEXTO", true, true, false, true, false, false, 1400.0, 0.5);

        AuditoriaResultado res = new AuditoriaResultado(
            Path.of("C:/segredo/local/ep.mkv"), "ep.mkv",
            new ContainerInfo("matroska", 1000L, 1440.0, 5000L, "libebml"),
            List.of(), List.of(), List.of(leg),
            List.of(new CapituloInfo(1, "Abertura", 0.0, 60.0)),
            List.of(new AnexoInfo("font.ttf", "application/x-truetype-font", 123L)),
            List.of("linha de log secreta"));

        ResultadoAnaliseLote lote = new ResultadoAnaliseLote(
            List.of(res), List.of(new FalhaAnalise("quebrado.mkv", "ffprobe falhou")));

        String json = new ObjectMapper().writeValueAsString(lote);

        // Contrato que o front consome (cartões + tabela de legendas + falhas).
        assertTrue(json.contains("\"nomeArquivo\":\"ep.mkv\""), json);
        assertTrue(json.contains("\"categoria\":\"TEXTO\""));
        assertTrue(json.contains("\"traduzivel\":true"));
        assertTrue(json.contains("\"exigeOcr\":false"));
        assertTrue(json.contains("\"capitulos\""));
        assertTrue(json.contains("\"anexos\""));
        assertTrue(json.contains("\"falhas\""));
        assertTrue(json.contains("quebrado.mkv"));

        // NÃO deve vazar o caminho local nem os logs internos.
        assertFalse(json.contains("caminhoArquivo"), json);
        assertFalse(json.contains("segredo"), json);
        assertFalse(json.contains("logsAuditoria"), json);
        assertFalse(json.contains("linha de log secreta"), json);
    }
}
