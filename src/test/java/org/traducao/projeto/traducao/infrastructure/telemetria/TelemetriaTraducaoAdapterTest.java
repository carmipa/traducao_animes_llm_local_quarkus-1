package org.traducao.projeto.traducao.infrastructure.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducaoDocumento;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o adapter que grava a telemetria própria da
 * Tradução Local — deduplicação por episódio normalizado (mais recente vence),
 * persistência dos quatro contadores e preservação de arquivo corrompido.
 * <p>INVARIANTES DO DOMÍNIO: raiz operacional isolada em {@code @TempDir}; nenhuma
 * rede; escrita atômica.
 * <p>COMPORTAMENTO EM CASO DE FALHA: divergência reprova a suíte.
 */
class TelemetriaTraducaoAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private String baseAnterior;

    @TempDir
    Path raiz;

    @BeforeEach
    void isolarRaiz() {
        baseAnterior = System.getProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, raiz.toString());
    }

    @AfterEach
    void restaurarRaiz() {
        if (baseAnterior == null) {
            System.clearProperty(DiretorioBaseKronos.PROPRIEDADE_BASE);
        } else {
            System.setProperty(DiretorioBaseKronos.PROPRIEDADE_BASE, baseAnterior);
        }
    }

    private static TelemetriaTraducao registro(String nome, String modelo, int totalLinhas, String registradoEm) {
        return new TelemetriaTraducao(nome, modelo, totalLinhas, totalLinhas, 0, 100L,
            List.of(), "Anime", "Temporada Única", registradoEm, "lore", "CONCLUIDO", List.of());
    }

    private TelemetriaTraducaoDocumento lerArquivo() throws IOException {
        Path arquivo = raiz.resolve("logs").resolve("telemetria_traducao.json");
        return mapper.readValue(arquivo.toFile(), TelemetriaTraducaoDocumento.class);
    }

    @Test
    @DisplayName("Mesmo episódio substitui o registro anterior (mais recente vence, sem append-only)")
    void substituiMesmoEpisodio() throws IOException {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        adapter.registrarTraducao(registro("ep01.ass", "modeloA", 10, "2026-01-01T00:00:00Z"));
        adapter.registrarTraducao(registro("ep01.ass", "modeloB", 20, "2026-01-02T00:00:00Z"));

        TelemetriaTraducaoDocumento doc = lerArquivo();
        assertEquals(1, doc.registros().size(), "Um único episódio consolidado");
        assertEquals("modeloB", doc.registros().get(0).modeloLlm());
        assertEquals(20, doc.registros().get(0).totalLinhas());
        assertEquals("1.1", doc.schemaVersion());
    }

    @Test
    @DisplayName("O KPI estruturado pendenciasPorCausa persiste e relê (schema 1.1)")
    void persistePendenciasPorCausa() throws IOException {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        adapter.registrarTraducao(new TelemetriaTraducao(
            "ep05.ass", "modeloX", 100, 80, 0, 500L, List.of(), "Anime", "Temporada Única",
            "2026-01-05T00:00:00Z", "lore", "PARCIAL",
            List.of(new org.traducao.projeto.traducao.domain.ResumoPendencia("DIALOGO", "MARCADORES_CORROMPIDOS", 7),
                    new org.traducao.projeto.traducao.domain.ResumoPendencia("DIALOGO", "ECO", 3))));

        var pendencias = lerArquivo().registros().get(0).pendenciasPorCausa();
        assertEquals(2, pendencias.size());
        assertEquals(7, pendencias.stream()
            .filter(p -> p.categoria().equals("DIALOGO") && p.causaRaiz().equals("MARCADORES_CORROMPIDOS"))
            .findFirst().orElseThrow().quantidade());
    }

    @Test
    @DisplayName("Deduplicação por nome normalizado (caixa/diretório/espaços)")
    void deduplicaPorNomeNormalizado() throws IOException {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        adapter.registrarTraducao(registro("EP01.ass", "modeloA", 10, "2026-01-01T00:00:00Z"));
        adapter.registrarTraducao(registro("pasta/ep01.ass", "modeloB", 20, "2026-01-02T00:00:00Z"));
        assertEquals(1, lerArquivo().registros().size());
    }

    @Test
    @DisplayName("Os quatro contadores são persistidos")
    void persisteQuatroContadores() throws IOException {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        adapter.registrarAlucinacaoPrevenida();
        adapter.registrarAlucinacaoPrevenida();
        adapter.registrarRespostaTraducaoRejeitada();
        adapter.registrarFalhaTraducaoRecuperada();
        adapter.registrarFallbackMantido();
        adapter.registrarFallbackMantido();
        adapter.registrarFallbackMantido();

        TelemetriaTraducaoDocumento doc = lerArquivo();
        assertEquals(2, doc.alucinacoesPrevenidas());
        assertEquals(1, doc.respostasTraducaoRejeitadas());
        assertEquals(1, doc.falhasTraducaoRecuperadas());
        assertEquals(3, doc.fallbacksTraducaoMantidos());
    }

    @Test
    @DisplayName("Registro e contadores persistem juntos (única alteração lógica)")
    void registroEContadoresJuntos() throws IOException {
        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        adapter.registrarRespostaTraducaoRejeitada();
        adapter.registrarTraducao(registro("ep01.ass", "modeloA", 10, "2026-01-01T00:00:00Z"));
        TelemetriaTraducaoDocumento doc = lerArquivo();
        assertEquals(1, doc.registros().size());
        assertEquals(1, doc.respostasTraducaoRejeitadas());
    }

    @Test
    @DisplayName("Arquivo corrompido é preservado (.corrompido), não destruído")
    void arquivoCorrompidoPreservado() throws IOException {
        Path logs = Files.createDirectories(raiz.resolve("logs"));
        Files.writeString(logs.resolve("telemetria_traducao.json"), "isto nao e json {{{");

        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        adapter.carregar();

        try (Stream<Path> s = Files.list(logs)) {
            assertTrue(s.anyMatch(p -> p.getFileName().toString().startsWith("telemetria_traducao.json.corrompido")),
                "O arquivo ilegível deve ser preservado como .corrompido_<ts>");
        }
        // Estado reiniciado vazio: uma nova gravação projeta apenas o novo episódio.
        adapter.registrarTraducao(registro("ep99.ass", "modeloZ", 5, "2026-01-03T00:00:00Z"));
        TelemetriaTraducaoDocumento doc = lerArquivo();
        assertEquals(1, doc.registros().size());
        assertEquals("ep99.ass", doc.registros().get(0).nomeEpisodio());
        assertFalse(doc.registros().isEmpty());
    }

    @Test
    @DisplayName("#5: registro null no JSON não derruba o carregar() e deixa o estado usável")
    void registroNuloNaoDerrubaCarregar() throws IOException {
        Path logs = Files.createDirectories(raiz.resolve("logs"));
        Files.writeString(logs.resolve("telemetria_traducao.json"),
            "{\"schemaVersion\":\"1.1\",\"registros\":[null],\"alucinacoesPrevenidas\":0,"
            + "\"respostasTraducaoRejeitadas\":0,\"falhasTraducaoRecuperadas\":0,\"fallbacksTraducaoMantidos\":0}");

        TelemetriaTraducaoAdapter adapter = new TelemetriaTraducaoAdapter(mapper);
        assertDoesNotThrow(adapter::carregar); // antes: NPE em t.nomeEpisodio() reprovava o @PostConstruct

        adapter.registrarTraducao(registro("ep01.ass", "m", 1, "2026-01-01T00:00:00Z"));
        assertEquals(1, lerArquivo().registros().size());
    }

    @Test
    @DisplayName("#19: corrupções sucessivas geram arquivos .corrompido distintos (não sobrescreve evidência)")
    void corrupcoesSucessivasNaoSobrescrevem() throws IOException {
        Path logs = Files.createDirectories(raiz.resolve("logs"));
        Path arq = logs.resolve("telemetria_traducao.json");

        Files.writeString(arq, "corrompido A {{{");
        new TelemetriaTraducaoAdapter(mapper).carregar();
        Files.writeString(arq, "corrompido B {{{");
        new TelemetriaTraducaoAdapter(mapper).carregar();

        try (Stream<Path> s = Files.list(logs)) {
            long corrompidos = s
                .filter(p -> p.getFileName().toString().startsWith("telemetria_traducao.json.corrompido"))
                .count();
            assertEquals(2, corrompidos, "cada corrupção deve preservar um arquivo distinto");
        }
    }
}
