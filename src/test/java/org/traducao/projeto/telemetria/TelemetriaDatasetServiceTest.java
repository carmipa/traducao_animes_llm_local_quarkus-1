package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O dataset público carrega SOMENTE métricas: sem textos de legenda (avisos
 * viram contagem), sem caminhos de máquina (detalhe descartado, episódio
 * reduzido ao nome do arquivo). Estes testes são o contrato de anonimização
 * declarado no README do repositório do dataset.
 */
class TelemetriaDatasetServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private TelemetriaResumo resumoDeTeste() {
        LlmTelemetria traducao = new LlmTelemetria(
            "C:\\animes\\86\\legendas\\episodio01.ass", "tower-7b", 900, 800, 100, 400_000L,
            List.of("Fala mantida sem tradução: I'll never forgive you", "Evento 12 suspeito"),
            "86 (Eighty-Six)", "Temporada 1", "2026-07-09T10:00:00Z");
        OperacaoTelemetria operacao = new OperacaoTelemetria(
            "Remux (mkvmerge)", "Videos: C:\\animes\\86 | Legendas: C:\\animes\\86\\legendas-finais",
            30_000L, 11, 11, 11, "2026-07-09T10:05:00Z");
        return new TelemetriaResumo(
            10, 1, 900, 444L, 100, List.of(), List.of(traducao), List.of(operacao),
            new RevisaoLoreTelemetriaResumo(0, 0, 0, 0, null), 5, 2, 0.0, 10, 0L, 0L, 3,
            7, 4, 3);
    }

    @Test
    void datasetNaoContemTextosDeLegendaNemCaminhos() {
        ObjectNode dataset = TelemetriaDatasetService.montarDatasetSanitizado(resumoDeTeste(), mapper);
        String json = dataset.toString();

        assertFalse(json.contains("errosOcorridos"), "avisos devem virar contagem, nunca texto");
        assertFalse(json.contains("never forgive"), "texto de fala não pode vazar para o dataset");
        assertFalse(json.contains("detalhe"), "campo detalhe (caminhos) deve ser descartado");
        assertFalse(json.contains("C:\\") || json.contains("C:/"), "nenhum caminho de máquina no dataset");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida identificação, versão e métricas essenciais do
     * dataset consumido externamente.
     *
     * <p>INVARIANTES DO DOMÍNIO: o schema atual é a versão 2 e mantém episódios
     * sanitizados, contagens de avisos e modelo da inferência.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mudança incompatível no contrato reprova
     * o teste antes da publicação.
     */
    @Test
    void datasetPreservaMetricasEIdentificacaoPublica() {
        ObjectNode dataset = TelemetriaDatasetService.montarDatasetSanitizado(resumoDeTeste(), mapper);

        assertEquals("kronos-anime-translation-telemetry-dataset", dataset.get("dataset").asText());
        assertEquals(2, dataset.get("versaoFormato").asInt());
        assertEquals(900, dataset.get("resumo").get("totalLinhasTraduzidas").asInt());
        assertEquals(5, dataset.get("resumo").get("alucinacoesLlmPrevenidas").asInt());

        var traducao = dataset.get("traducoesLlm").get(0);
        assertEquals("episodio01.ass", traducao.get("episodio").asText(), "episódio sem diretórios");
        assertEquals(2, traducao.get("quantidadeAvisos").asInt());
        assertEquals("tower-7b", traducao.get("modeloLlm").asText());

        var operacao = dataset.get("operacoes").get(0);
        assertEquals("Remux (mkvmerge)", operacao.get("tipo").asText());
        assertEquals(11, operacao.get("itensCorrigidos").asInt());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o dataset publique uma fotografia única
     * do host atual, incluindo todas as GPUs sem aliases manuais.
     *
     * <p>INVARIANTES DO DOMÍNIO: GPU principal pertence à lista detectada e não
     * existem campos legados de override capazes de misturar computadores.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer campo inconsistente falha o
     * contrato do formato antes da publicação.
     */
    @Test
    void datasetIncluiAmbienteExecucaoSanitizadoQuandoDisponivel() {
        AmbienteExecucaoDataset ambiente = new AmbienteExecucaoDataset(
            "Avell",
            "560",
            "Intel Core i9-14900HX",
            "NVIDIA GeForce RTX 5060 Laptop GPU",
            List.of("Intel UHD Graphics", "NVIDIA GeForce RTX 5060 Laptop GPU"),
            32,
            "Windows 11",
            "amd64",
            true
        );

        ObjectNode dataset = TelemetriaDatasetService.montarDatasetSanitizado(resumoDeTeste(), mapper, ambiente);
        var ambienteJson = dataset.get("ambienteExecucao");
        String json = ambienteJson.toString();

        assertEquals("Avell", ambienteJson.get("fabricante").asText());
        assertEquals("NVIDIA GeForce RTX 5060 Laptop GPU", ambienteJson.get("gpuPrincipal").asText());
        assertEquals(2, ambienteJson.get("gpusDetectadas").size());
        assertEquals(32, ambienteJson.get("ramTotalGb").asInt());
        assertTrue(ambienteJson.get("hardwareColetadoAutomaticamente").asBoolean());
        assertFalse(ambienteJson.has("gpuPublicaConfigurada"));
        assertFalse(ambienteJson.has("gpuDetectadaSistema"));
        assertFalse(json.contains("PNP") || json.contains("PCI\\") || json.contains("SERIAL"));
        assertFalse(json.contains("C:\\") || json.contains("Users"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o notebook publique a GPU dedicada usada
     * para inferência como principal, sem confundi-la com o vídeo integrado Intel.
     *
     * <p>INVARIANTES DO DOMÍNIO: a GPU escolhida pertence à lista detectada e
     * NVIDIA/RTX tem precedência sobre Intel UHD/Iris.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: seleção diferente da dedicada reprova o
     * teste e bloqueia a publicação do formato inconsistente.
     */
    @Test
    void selecionaGpuDedicadaNoNotebookHibrido() {
        List<String> gpus = List.of("Intel UHD Graphics", "NVIDIA GeForce RTX 5060 Laptop GPU");

        assertEquals("NVIDIA GeForce RTX 5060 Laptop GPU",
            AmbienteExecucaoDatasetService.selecionarGpuPrincipal(gpus));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o desktop publique a Radeon RX detectada
     * como GPU principal sem depender de configuração compartilhada.
     *
     * <p>INVARIANTES DO DOMÍNIO: quando existe uma única GPU válida, ela é
     * preservada literalmente como principal.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista vazia retorna nulo; placa presente
     * que não for selecionada reprova o teste.
     */
    @Test
    void selecionaRadeonDetectadaNoDesktop() {
        assertEquals("AMD Radeon RX 7800 XT",
            AmbienteExecucaoDatasetService.selecionarGpuPrincipal(List.of("AMD Radeon RX 7800 XT")));
        assertNull(AmbienteExecucaoDatasetService.selecionarGpuPrincipal(List.of()));
    }

    @Test
    void contagemDeAvisosReconstituiTotalRealAPartirDoMarcadorDeOmissao() {
        // Telemetria compactada: 30 avisos + linha-resumo → total real = 30 + 625.
        var avisos = new java.util.ArrayList<String>();
        for (int i = 0; i < 30; i++) {
            avisos.add("Aviso " + i);
        }
        avisos.add("(+625 avisos omitidos — íntegra no relatório da operação em relatorios/)");

        assertEquals(655, TelemetriaDatasetService.contarAvisos(avisos));
        assertEquals(2, TelemetriaDatasetService.contarAvisos(List.of("a", "b")));
        assertEquals(0, TelemetriaDatasetService.contarAvisos(null));
    }

    @Test
    void apenasNomeDeArquivoCobreBarrasDosDoisSistemas() {
        assertEquals("ep.ass", TelemetriaDatasetService.apenasNomeDeArquivo("C:\\pasta\\sub\\ep.ass"));
        assertEquals("ep.ass", TelemetriaDatasetService.apenasNomeDeArquivo("/home/user/ep.ass"));
        assertEquals("ep.ass", TelemetriaDatasetService.apenasNomeDeArquivo("ep.ass"));
        assertNull(TelemetriaDatasetService.apenasNomeDeArquivo(null));
    }
}
