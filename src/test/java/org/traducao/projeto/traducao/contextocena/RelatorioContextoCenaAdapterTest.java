package org.traducao.projeto.traducao.contextocena;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducao.domain.contextocena.RegistroExecucaoContextoCena;
import org.traducao.projeto.traducao.infrastructure.contextocena.RelatorioContextoCenaAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o adaptador do relatório A/B do contexto de cena. Prova o
 * traço que o distingue da telemetria canônica — é APPEND-ONLY: o braço A e o braço B do MESMO
 * episódio coexistem no arquivo (a telemetria deduplicaria e o B sobrescreveria o A). Prova
 * também que cada linha é um JSON compacto (JSONL) com o envelope (runId, instante) sobre a
 * medição.
 *
 * <p>INVARIANTES DO DOMÍNIO: raiz operacional isolada em {@code @TempDir}; sem rede; uma medição
 * por linha física.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: divergência de contagem de linhas, braço ou campos reprova.
 */
class RelatorioContextoCenaAdapterTest {

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

    private List<String> lerLinhas() throws IOException {
        Path arquivo = raiz.resolve("logs").resolve("contexto_cena_ab.jsonl");
        return Files.readAllLines(arquivo);
    }

    private static RegistroExecucaoContextoCena medicao(String variante, String politica,
            int traduzidasNovas, long contextoExtra) {
        return new RegistroExecucaoContextoCena("ep01.ass", variante, politica, "modelo-teste",
            "cache", 10, traduzidasNovas, 0, 0, contextoExtra, 500L, "CONCLUIDO");
    }

    @Test
    @DisplayName("Append-only: braço A e braço B do MESMO episódio coexistem (ao contrário do dedup canônico)")
    void bracosDoMesmoEpisodioCoexistem() throws IOException {
        RelatorioContextoCenaAdapter adapter = new RelatorioContextoCenaAdapter(mapper);
        adapter.registrar(medicao(RegistroExecucaoContextoCena.VARIANTE_BASELINE, "baseline", 8, 0L));
        adapter.registrar(medicao(RegistroExecucaoContextoCena.VARIANTE_CONTEXTO, "contexto-cena:v1", 8, 240L));

        List<String> linhas = lerLinhas();
        assertEquals(2, linhas.size(), "as duas execuções do mesmo episódio devem coexistir (append-only)");

        JsonNode a = mapper.readTree(linhas.get(0));
        JsonNode b = mapper.readTree(linhas.get(1));
        assertEquals("A_BASELINE", a.get("medicao").get("variante").asText());
        assertEquals("B_CONTEXTO", b.get("medicao").get("variante").asText());
        assertEquals(0L, a.get("medicao").get("caracteresContextoExtra").asLong(), "baseline sem custo de contexto");
        assertTrue(b.get("medicao").get("caracteresContextoExtra").asLong() > 0, "o braço B carrega o custo de contexto");
    }

    @Test
    @DisplayName("Cada linha é JSON compacto com envelope (runId, instante) sobre a medição")
    void linhaCarregaEnvelopeEMedicao() throws IOException {
        RelatorioContextoCenaAdapter adapter = new RelatorioContextoCenaAdapter(mapper);
        adapter.registrar(medicao(RegistroExecucaoContextoCena.VARIANTE_BASELINE, "baseline", 8, 0L));

        List<String> linhas = lerLinhas();
        assertEquals(1, linhas.size());
        JsonNode linha = mapper.readTree(linhas.get(0));
        assertFalse(linha.get("runId").asText().isBlank(), "runId carimbado");
        assertFalse(linha.get("instante").asText().isBlank(), "instante carimbado");
        JsonNode m = linha.get("medicao");
        assertEquals("ep01.ass", m.get("episodio").asText());
        assertEquals(10, m.get("linhasTraduziveis").asInt());
        assertEquals(8, m.get("traduzidasNovas").asInt());
        assertEquals("CONCLUIDO", m.get("status").asText());
    }

    @Test
    @DisplayName("Registro null é ignorado (não escreve linha, não lança)")
    void registroNuloEhIgnorado() throws IOException {
        RelatorioContextoCenaAdapter adapter = new RelatorioContextoCenaAdapter(mapper);
        adapter.registrar(null);

        Path arquivo = raiz.resolve("logs").resolve("contexto_cena_ab.jsonl");
        assertFalse(Files.exists(arquivo), "registro null não deve criar o arquivo");
    }
}
