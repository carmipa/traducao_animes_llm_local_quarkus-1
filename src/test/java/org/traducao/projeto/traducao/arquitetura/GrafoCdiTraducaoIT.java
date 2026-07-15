package org.traducao.projeto.traducao.arquitetura;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.config.ModoExecucaoStartup;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorStrategy;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.traducao.presentation.TradutorCLI;
import org.traducao.projeto.traducao.presentation.bootstrap.TraducaoStartup;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: Camada B (runtime CDI) do harness de fitness da FASE D.
 * Sobe o container Arc com {@code @QuarkusTest} e caracteriza — sem alterar
 * produção — o grafo de injeção que a análise estática não alcança: o
 * {@link ObjectMapper} efetivamente resolvido, as coleções agregadas de extração
 * e o dispatcher do modo CLI. Fixa o baseline homologado antes das subfases D.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Existe exatamente um {@link ObjectMapper} injetável (sem ambiguidade
 *       impeditiva no baseline).</li>
 *   <li>Os producers de {@code List<ExtratorVideoPort>} e {@code List<ExtratorStrategy>}
 *       resolvem coleções não vazias (mesmo contrato consumido por
 *       {@code ExtrairLegendaUseCase}).</li>
 *   <li>O dispatcher compartilhado {@link ModoExecucaoStartup} é sempre resolvível.
 *       {@link TradutorCLI} — condicional a {@code app.modo=TRADUZIR} com
 *       {@code matchIfMissing=true} — é ativado no ambiente de teste. Após D-Config,
 *       o ciclo de vida do modo TRADUZIR pertence ao observador próprio
 *       {@link org.traducao.projeto.traducao.presentation.bootstrap.TraducaoStartup},
 *       também resolvível — o dispatcher não conhece mais a Tradução Local.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Ambiguidade de bean, coleção não resolvida ou dispatcher ausente reprovam o
 * teste na subida do container ou na asserção, sinalizando desvio do baseline
 * antes de qualquer refatoração da FASE D.
 */
@QuarkusTest
class GrafoCdiTraducaoIT {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    List<ExtratorVideoPort> extratoresVideoPort;

    @Inject
    List<ExtratorStrategy> extratoresStrategy;

    @Inject
    Instance<TradutorCLI> tradutorCli;

    @Inject
    Instance<ModoExecucaoStartup> modoExecucaoStartup;

    @Inject
    Instance<TraducaoStartup> traducaoStartup;

    @Test
    @DisplayName("ObjectMapper é resolvido sem ambiguidade impeditiva no baseline")
    void objectMapperResolvidoSemAmbiguidade() throws Exception {
        assertNotNull(objectMapper, "ObjectMapper deve ser injetável");
        assertEquals("{\"k\":1}", objectMapper.writeValueAsString(Map.of("k", 1)),
            "Serialização default esperada do ObjectMapper resolvido");
    }

    @Test
    @DisplayName("Producers das coleções de extração resolvem coleções não vazias")
    void colecoesDeExtracaoResolvidas() {
        assertNotNull(extratoresVideoPort, "List<ExtratorVideoPort> deve ser injetável");
        assertFalse(extratoresVideoPort.isEmpty(), "Esperado ao menos um ExtratorVideoPort registrado");
        assertNotNull(extratoresStrategy, "List<ExtratorStrategy> deve ser injetável");
        assertFalse(extratoresStrategy.isEmpty(), "Esperado ao menos um ExtratorStrategy registrado");
    }

    @Test
    @DisplayName("Bootstrap do modo TRADUZIR: dispatcher compartilhado e observador próprio coexistem")
    void dispatcherModoTraduzirBaseline() {
        assertTrue(modoExecucaoStartup.isResolvable(),
            "ModoExecucaoStartup (dispatcher compartilhado dos demais modos CLI) deve existir");
        assertTrue(tradutorCli.isResolvable(),
            "TradutorCLI (condicional a TRADUZIR, matchIfMissing=true) é criado por CDI no ambiente de teste");
        assertTrue(traducaoStartup.isResolvable(),
            "Após D-Config, o ciclo de vida do modo TRADUZIR pertence ao observador próprio "
                + "TraducaoStartup (fatia Tradução Local), que deve ser resolvível no container");
    }
}
