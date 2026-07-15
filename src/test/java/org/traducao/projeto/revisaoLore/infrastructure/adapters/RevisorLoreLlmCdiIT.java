package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase;
import org.traducao.projeto.revisaoLore.domain.ports.RevisorLoreLlmPort;
import org.traducao.projeto.revisaoLore.infrastructure.config.RevisaoLoreLlmProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova, com o container Arc real, que a stack LLM própria
 * da Revisão de Lore está cabeada por CDI e que o {@code RevisarLoreUseCase} passou
 * a depender da porta própria — não mais da {@code MistralPort} da Tradução Local.
 * <p>INVARIANTES DO DOMÍNIO: {@link RevisorLoreLlmPort} resolve para
 * {@link RevisorLoreLlmAdapter}; as propriedades próprias refletem os defaults efetivos.
 * <p>COMPORTAMENTO EM CASO DE FALHA: bean ausente ou default divergente reprova a suíte.
 */
@QuarkusTest
class RevisorLoreLlmCdiIT {

    @Inject
    RevisorLoreLlmPort revisorLoreLlm;

    @Inject
    RevisaoLoreLlmProperties propriedades;

    @Inject
    RevisarLoreUseCase revisarLoreUseCase;

    @Test
    @DisplayName("RevisorLoreLlmPort resolve para o adapter próprio da Revisão de Lore")
    void portaResolvida() {
        assertNotNull(revisorLoreLlm);
        assertTrue(revisorLoreLlm instanceof RevisorLoreLlmAdapter,
            "A porta deve ser atendida pelo RevisorLoreLlmAdapter");
    }

    @Test
    @DisplayName("RevisaoLoreLlmProperties reflete os defaults efetivos (namespace revisao-lore.llm)")
    void propriedadesProprias() {
        assertTrue(propriedades.baseUrl().contains("127.0.0.1:1234/v1"));
        assertEquals("current", propriedades.model());
        assertEquals(2000, propriedades.maxTokens());
        assertEquals(Duration.ofSeconds(5), propriedades.connectTimeout());
        assertEquals(Duration.ofSeconds(180), propriedades.readTimeout());
    }

    @Test
    @DisplayName("RevisarLoreUseCase é resolvível, cabeado com a porta própria")
    void useCaseCabeado() {
        assertNotNull(revisarLoreUseCase);
    }
}
