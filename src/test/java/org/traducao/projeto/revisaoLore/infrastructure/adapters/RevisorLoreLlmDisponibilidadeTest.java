package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLoreLlm;
import org.traducao.projeto.revisaoLore.infrastructure.config.RevisaoLoreLlmProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a verificação de disponibilidade do LLM da
 * Revisão de Lore — API estendida {@code /api/v0/models}, preferência pelo modelo
 * configurado, fallback para {@code /v1/models}, catálogo vazio, modelo ausente e
 * servidor inacessível — sem depender do LM Studio real.
 * <p>INVARIANTES DO DOMÍNIO: nenhuma rede externa; servidor HTTP local determinístico.
 * <p>COMPORTAMENTO EM CASO DE FALHA: divergência de status reprova a suíte.
 */
class RevisorLoreLlmDisponibilidadeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private RevisaoLoreLlmProperties props(String baseUrl, String model) {
        RevisaoLoreLlmProperties p = new RevisaoLoreLlmProperties();
        p.setBaseUrl(baseUrl);
        p.setModel(model);
        p.setConnectTimeout(Duration.ofSeconds(1));
        return p;
    }

    private RevisorLoreLlmAdapter adapter(RevisaoLoreLlmProperties props) {
        return new RevisorLoreLlmAdapter(props, mapper, new NormalizadorRespostaRevisaoLore());
    }

    @Test
    @DisplayName("/api/v0/models com state=loaded: online + modelo carregado")
    void v0ModeloCarregado() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.definirModelsV0(200, "{\"data\":[{\"id\":\"mistral-nemo\",\"state\":\"loaded\"}]}");
            RevisaoLoreLlmProperties props = props(srv.baseUrl(), "current");
            StatusRevisaoLoreLlm status = adapter(props).verificarDisponibilidade();
            assertTrue(status.servidorOnline());
            assertTrue(status.modeloCarregado());
            assertEquals("mistral-nemo", props.model());
        }
    }

    @Test
    @DisplayName("Vários carregados: prefere o modelo configurado")
    void v0PrefereConfigurado() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.definirModelsV0(200,
                "{\"data\":[{\"id\":\"model-a\",\"state\":\"loaded\"},{\"id\":\"model-b\",\"state\":\"loaded\"}]}");
            RevisaoLoreLlmProperties props = props(srv.baseUrl(), "model-b");
            StatusRevisaoLoreLlm status = adapter(props).verificarDisponibilidade();
            assertTrue(status.modeloCarregado());
            assertEquals("model-b", props.model());
        }
    }

    @Test
    @DisplayName("Sem API estendida: fallback para /v1/models com o modelo configurado no catálogo")
    void fallbackCatalogo() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.definirModelsV0(404, "");
            srv.definirModels(200, "{\"data\":[{\"id\":\"mistral-x\"}]}");
            RevisaoLoreLlmProperties props = props(srv.baseUrl(), "mistral-x");
            StatusRevisaoLoreLlm status = adapter(props).verificarDisponibilidade();
            assertTrue(status.servidorOnline());
            assertTrue(status.modeloCarregado());
            assertEquals("mistral-x", props.model());
        }
    }

    @Test
    @DisplayName("Catálogo vazio: online, mas sem modelo carregado")
    void catalogoVazio() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.definirModelsV0(404, "");
            srv.definirModels(200, "{\"data\":[]}");
            StatusRevisaoLoreLlm status = adapter(props(srv.baseUrl(), "qualquer")).verificarDisponibilidade();
            assertTrue(status.servidorOnline());
            assertFalse(status.modeloCarregado());
        }
    }

    @Test
    @DisplayName("Modelo configurado ausente do catálogo: online, não carregado")
    void modeloConfiguradoAusente() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.definirModelsV0(404, "");
            srv.definirModels(200, "{\"data\":[{\"id\":\"outro-modelo\"}]}");
            StatusRevisaoLoreLlm status = adapter(props(srv.baseUrl(), "inexistente-xyz")).verificarDisponibilidade();
            assertTrue(status.servidorOnline());
            assertFalse(status.modeloCarregado());
        }
    }

    @Test
    @DisplayName("Servidor indisponível: offline e sem modelo")
    void servidorIndisponivel() throws Exception {
        String baseUrlFechada;
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            baseUrlFechada = srv.baseUrl(); // captura a porta e fecha em seguida
        }
        StatusRevisaoLoreLlm status = adapter(props(baseUrlFechada, "current")).verificarDisponibilidade();
        assertFalse(status.servidorOnline());
        assertFalse(status.modeloCarregado());
    }
}
