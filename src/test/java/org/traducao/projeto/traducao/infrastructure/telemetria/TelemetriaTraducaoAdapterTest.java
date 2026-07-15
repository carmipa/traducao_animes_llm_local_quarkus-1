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
            List.of(), "Anime", "Temporada Única", registradoEm, "lore", "CONCLUIDO");
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
        assertEquals("1.0", doc.schemaVersion());
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

        assertTrue(Files.exists(logs.resolve("telemetria_traducao.json.corrompido")),
            "O arquivo ilegível deve ser preservado como .corrompido");
        // Estado reiniciado vazio: uma nova gravação projeta apenas o novo episódio.
        adapter.registrarTraducao(registro("ep99.ass", "modeloZ", 5, "2026-01-03T00:00:00Z"));
        TelemetriaTraducaoDocumento doc = lerArquivo();
        assertEquals(1, doc.registros().size());
        assertEquals("ep99.ass", doc.registros().get(0).nomeEpisodio());
        assertFalse(doc.registros().isEmpty());
    }
}
