package org.traducao.projeto.traducao.arquitetura;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.config.ModoExecucaoStartup;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorStrategy;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.traducao.infrastructure.adapters.LlmClientAdapter;
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

    @Inject
    Instance<LlmPort> llmPort;

    @Test
    @DisplayName("ObjectMapper é resolvido sem ambiguidade impeditiva no baseline")
    void objectMapperResolvidoSemAmbiguidade() throws Exception {
        assertNotNull(objectMapper, "ObjectMapper deve ser injetável");
        assertEquals("{\"k\":1}", objectMapper.writeValueAsString(Map.of("k", 1)),
            "Serialização default esperada do ObjectMapper resolvido");
    }

    /**
     * PROPÓSITO DE NEGÓCIO (D-Obj, caracterização read-only): revela EXATAMENTE qual
     * {@link ObjectMapper} o Arc entrega hoje aos beans da Tradução Local — o produzido
     * por {@code RestClientConfig.objectMapper()} ({@code new ObjectMapper()} default)
     * ou o gerenciado pelo {@code quarkus-jackson}. É a mesma instância injetada por
     * construtor em {@code LlmClientAdapter}, {@code TelemetriaTraducaoAdapter},
     * {@code CacheTraducaoService} e {@code CacheManutencaoService} (bean único).
     *
     * <p>INVARIANTES DO DOMÍNIO: caracteriza sem alterar produção; fixa o baseline de
     * módulos registrados e das features que divergem entre o mapper default e o do
     * Quarkus ({@code FAIL_ON_UNKNOWN_PROPERTIES}, {@code WRITE_DATES_AS_TIMESTAMPS}).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: se o perfil resolvido divergir do baseline
     * documentado, a suíte reprova — sinalizando que o mapper efetivo mudou.
     */
    @Test
    @DisplayName("D-Obj: caracteriza o perfil do ObjectMapper efetivamente resolvido pelo Arc")
    void caracterizaObjectMapperResolvido() {
        var modulos = objectMapper.getRegisteredModuleIds();
        boolean falhaEmDesconhecido = objectMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        boolean datasComoTimestamp = objectMapper.isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        System.out.println("[D-OBJ] Classe do mapper resolvido: " + objectMapper.getClass().getName());
        System.out.println("[D-OBJ] Módulos registrados: " + modulos);
        System.out.println("[D-OBJ] FAIL_ON_UNKNOWN_PROPERTIES = " + falhaEmDesconhecido);
        System.out.println("[D-OBJ] WRITE_DATES_AS_TIMESTAMPS  = " + datasComoTimestamp);

        // Baseline HIPÓTESE (a confirmar na execução): o @Bean de RestClientConfig
        // é um new ObjectMapper() default e sobrepõe o @DefaultBean do quarkus-jackson.
        // Perfil default do Jackson: sem módulos e ambas as features HABILITADAS.
        assertTrue(modulos.isEmpty(),
            "Baseline: mapper default de RestClientConfig não registra módulos. Encontrado: " + modulos);
        assertTrue(falhaEmDesconhecido,
            "Baseline: FAIL_ON_UNKNOWN_PROPERTIES habilitado (default Jackson, não o do Quarkus)");
        assertTrue(datasComoTimestamp,
            "Baseline: WRITE_DATES_AS_TIMESTAMPS habilitado (default Jackson, não o do Quarkus)");
    }

    @Test
    @DisplayName("Producers das coleções de extração resolvem coleções não vazias")
    void colecoesDeExtracaoResolvidas() {
        assertNotNull(extratoresVideoPort, "List<ExtratorVideoPort> deve ser injetável");
        assertFalse(extratoresVideoPort.isEmpty(), "Esperado ao menos um ExtratorVideoPort registrado");
        assertNotNull(extratoresStrategy, "List<ExtratorStrategy> deve ser injetável");
        assertFalse(extratoresStrategy.isEmpty(), "Esperado ao menos um ExtratorStrategy registrado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: caracteriza, em runtime CDI, o bootstrap do modo TRADUZIR e o
     * PONTO DE COMPOSIÇÃO da E8d — o contrato {@link LlmPort} (peer {@code llm}) é resolvido
     * por um único bean de produção, o {@link LlmClientAdapter}, que permanece em
     * {@code traducao.infrastructure}. Assim a extração do contrato para o peer {@code llm}
     * não quebra a injeção: a fatia que possui o adapter continua fornecendo a implementação.
     *
     * <p>INVARIANTES DO DOMÍNIO: dispatcher compartilhado e observador próprio coexistem;
     * {@link LlmPort} é resolvível sem ambiguidade ({@code isResolvable()} exige bean único),
     * {@code get()} não é nulo e a implementação resolvida é {@link LlmClientAdapter}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ambiguidade, ausência de bean ou implementação
     * inesperada reprovam. A verificação da implementação usa {@code instanceof}, tolerando
     * proxy CDI (subclasse gerada), em vez de igualdade rígida de {@code getClass()}.
     */
    @Test
    @DisplayName("Bootstrap do modo TRADUZIR e ponto de composição E8d: LlmPort resolve como LlmClientAdapter (bean único)")
    void dispatcherModoTraduzirBaseline() {
        assertTrue(modoExecucaoStartup.isResolvable(),
            "ModoExecucaoStartup (dispatcher compartilhado dos demais modos CLI) deve existir");
        assertTrue(tradutorCli.isResolvable(),
            "TradutorCLI (condicional a TRADUZIR, matchIfMissing=true) é criado por CDI no ambiente de teste");
        assertTrue(traducaoStartup.isResolvable(),
            "Após D-Config, o ciclo de vida do modo TRADUZIR pertence ao observador próprio "
                + "TraducaoStartup (fatia Tradução Local), que deve ser resolvível no container");

        // Ponto de composição E8d: LlmPort (peer llm) resolve, sem ambiguidade, para o
        // único bean de produção LlmClientAdapter, que fica em traducao.infrastructure.
        assertTrue(llmPort.isResolvable(),
            "LlmPort (peer llm) deve resolver como bean ÚNICO — sem ambiguidade — no container");
        LlmPort implementacaoResolvida = llmPort.get();
        assertNotNull(implementacaoResolvida, "LlmPort resolvido não pode ser null");
        assertTrue(implementacaoResolvida instanceof LlmClientAdapter,
            "A implementação de produção de LlmPort deve ser LlmClientAdapter (instanceof tolera proxy CDI). "
                + "Encontrado: " + implementacaoResolvida.getClass().getName());
    }
}
