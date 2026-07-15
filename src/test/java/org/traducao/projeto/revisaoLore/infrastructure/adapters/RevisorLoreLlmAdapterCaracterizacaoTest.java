package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.infrastructure.config.RevisaoLoreLlmProperties;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a stack LLM própria da Revisão de Lore contra
 * um servidor HTTP local, garantindo paridade com o comportamento efetivo anterior
 * (payload, normalização, respostas inválidas e política de retry) sem depender do
 * LM Studio real.
 * <p>INVARIANTES DO DOMÍNIO: nenhuma rede externa; pausas de retry reduzidas para
 * determinismo.
 * <p>COMPORTAMENTO EM CASO DE FALHA: desvio de payload/normalização/retry reprova a suíte.
 */
class RevisorLoreLlmAdapterCaracterizacaoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private RevisorLoreLlmAdapter adapter(String baseUrl) {
        RevisaoLoreLlmProperties props = new RevisaoLoreLlmProperties();
        props.setBaseUrl(baseUrl);
        props.setModel("modelo-x");
        props.setPausaEntreTentativas(Duration.ofMillis(1));
        return new RevisorLoreLlmAdapter(props, mapper, new NormalizadorRespostaRevisaoLore());
    }

    private String respostaChat(String content) throws Exception {
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":"
            + mapper.writeValueAsString(content) + "}}]}";
    }

    @Test
    @DisplayName("Payload enviado ao /chat/completions: modelo, system, user, temperature 0.15 e max_tokens")
    void payloadDoChat() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(200, respostaChat("[[TAG0]]Shin[[TAG1]] chegou."));
            RevisorLoreLlmAdapter adapter = adapter(srv.baseUrl());

            String en = "[[TAG0]]Shin[[TAG1]] arrived.";
            String pt = "[[TAG0]]Shin[[TAG1]] chegou.";
            List<String> problemas = List.of("nome suspeito");
            Optional<String> r = adapter.revisar("PROMPT_SISTEMA_LORE", en, pt, problemas);

            assertTrue(r.isPresent());
            JsonNode corpo = mapper.readTree(srv.ultimoCorpoChat());
            assertEquals("modelo-x", corpo.get("model").asText());
            assertEquals(0.15, corpo.get("temperature").asDouble());
            assertEquals(2000, corpo.get("max_tokens").asInt());

            JsonNode messages = corpo.get("messages");
            assertEquals("system", messages.get(0).get("role").asText());
            assertEquals("PROMPT_SISTEMA_LORE", messages.get(0).get("content").asText());
            assertEquals("user", messages.get(1).get("role").asText());
            assertEquals(PromptRevisaoLore.montarPromptUsuario(en, pt, problemas),
                messages.get(1).get("content").asText());
        }
    }

    @Test
    @DisplayName("Normaliza a resposta (think/Markdown/prefixo) e preserva marcadores")
    void normalizaResposta() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(200, respostaChat("<think>ok</think>\nTraducao corrigida: [[TAG0]]Shin[[TAG1]] chegou."));
            RevisorLoreLlmAdapter adapter = adapter(srv.baseUrl());

            Optional<String> r = adapter.revisar("S", "[[TAG0]]Shin[[TAG1]] arrived.",
                "[[TAG0]]Shin[[TAG1]] chegou.", List.of());
            assertEquals(Optional.of("[[TAG0]]Shin[[TAG1]] chegou."), r);
        }
    }

    @Test
    @DisplayName("Resposta com choices vazio devolve vazio após esgotar tentativas")
    void choicesVazio() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(200, "{\"choices\":[]}");
            RevisorLoreLlmAdapter adapter = adapter(srv.baseUrl());
            assertTrue(adapter.revisar("S", "en", "pt", List.of()).isEmpty());
            assertEquals(2, srv.chamadasChat());
        }
    }

    @Test
    @DisplayName("message nula ou content vazio devolve vazio")
    void mensagemInvalida() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(200, "{\"choices\":[{\"message\":null}]}");
            assertTrue(adapter(srv.baseUrl()).revisar("S", "en", "pt", List.of()).isEmpty());
        }
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(200, respostaChat("   "));
            assertTrue(adapter(srv.baseUrl()).revisar("S", "en", "pt", List.of()).isEmpty());
        }
    }

    @Test
    @DisplayName("Sem linha que preserve os marcadores esperados, devolve vazio")
    void semLinhaUtilizavel() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(200, respostaChat("explicacao sem tags"));
            Optional<String> r = adapter(srv.baseUrl()).revisar("S", "[[TAG0]]x", "[[TAG0]]y", List.of("[[TAG0]]"));
            assertTrue(r.isEmpty());
        }
    }

    @Test
    @DisplayName("HTTP 400 permanente NÃO repete (uma única chamada)")
    void http400NaoRepete() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(400, "payload invalido");
            assertTrue(adapter(srv.baseUrl()).revisar("S", "en", "pt", List.of()).isEmpty());
            assertEquals(1, srv.chamadasChat());
        }
    }

    @Test
    @DisplayName("HTTP 408, 429 e 5xx repetem até o limite (duas chamadas)")
    void errosTransitoriosRepetem() throws Exception {
        for (int status : new int[]{408, 429, 500, 503}) {
            try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
                srv.enfileirarChat(status, "erro transitorio");
                assertTrue(adapter(srv.baseUrl()).revisar("S", "en", "pt", List.of()).isEmpty());
                assertEquals(2, srv.chamadasChat(), "status " + status + " deveria repetir uma vez");
            }
        }
    }

    @Test
    @DisplayName("Interrupção na pausa entre tentativas preserva o flag e impede nova tentativa")
    void interrupcaoPreservaFlagEImpedeNovaTentativa() throws Exception {
        try (ServidorLlmDeTeste srv = new ServidorLlmDeTeste()) {
            srv.enfileirarChat(500, "erro transitorio"); // transitório -> dorme antes de repetir
            RevisaoLoreLlmProperties props = new RevisaoLoreLlmProperties();
            props.setBaseUrl(srv.baseUrl());
            props.setPausaEntreTentativas(Duration.ofSeconds(30)); // pausa longa: interromper durante ela
            RevisorLoreLlmAdapter adapter = new RevisorLoreLlmAdapter(props, mapper, new NormalizadorRespostaRevisaoLore());

            final boolean[] interrompidaAoFinal = {false};
            final Optional<?>[] resultado = new Optional<?>[]{null};
            Thread worker = new Thread(() -> {
                resultado[0] = adapter.revisar("S", "en", "pt", List.of());
                interrompidaAoFinal[0] = Thread.currentThread().isInterrupted();
            });
            worker.start();
            Thread.sleep(400); // deixa a 1a chamada (500) completar e entrar na pausa
            worker.interrupt();
            worker.join(5000);

            assertFalse(worker.isAlive(), "o worker deve encerrar ao ser interrompido na pausa");
            assertEquals(1, srv.chamadasChat(), "não deve haver segunda tentativa após interrupção");
            assertTrue(interrompidaAoFinal[0], "o flag de interrupção deve ser preservado");
            assertTrue(resultado[0].isEmpty());
        }
    }
}
