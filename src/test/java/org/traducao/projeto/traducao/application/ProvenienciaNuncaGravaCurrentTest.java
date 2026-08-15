package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.ProvenienciaCache;
import org.traducao.projeto.lore.domain.SnapshotContexto;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o carimbo de proveniência registra o modelo que
 * REALMENTE respondeu — o invariante que a {@link ProvenienciaCache} promete
 * ("qualquer troca de lore/modelo/idioma muda o carimbo") e que, até 2026-07-29, valia
 * para lore e idioma mas NÃO para modelo.
 *
 * <p>O defeito: {@code ResolvedorCacheTraducao} lia {@code llmPropriedades.model()}, e
 * {@code LlmProperties} não é instância compartilhada entre beans — o {@code setModel}
 * do adaptador muda a cópia dele, não esta. Resultado medido no acervo: <b>218 dos 279
 * caches carimbados {@code "current"}</b>. Trocar de LLM não invalidava nada: metade de
 * um episódio podia sair com a voz de um modelo e metade com a de outro, sem aviso.
 *
 * <p>O terceiro caso é o que impede a regressão de verdade: telemetria e cache lendo o
 * modelo por caminhos diferentes foi a origem do problema — o commit {@code 5824d42}
 * corrigiu a telemetria e não tocou o cache.
 */
class ProvenienciaNuncaGravaCurrentTest {

    private static final String MODELO_REAL = "mistralai/mistral-nemo-instruct-2407";

    /** Porta que devolve um modelo fixo, como o adaptador faz após detectar o carregado. */
    private static LlmPort portaComModelo(String modelo) {
        return new LlmPort() {
            @Override public TraducaoLote traduzir(Lote lote) { return null; }
            @Override public StatusLlm verificarDisponibilidade() { return null; }
            @Override public String modeloAtivo() { return modelo; }
            @Override public java.util.Optional<String> revisarConcordancia(String o, String t, List<String> p) {
                return java.util.Optional.empty();
            }
            @Override public java.util.Optional<String> corrigirTraducao(String o, String t, String m) {
                return java.util.Optional.empty();
            }
        };
    }

    private static LlmProperties configuracaoCom(String modelo) {
        LlmProperties p = new LlmProperties();
        p.setModel(modelo);
        return p;
    }

    private static ResolvedorCacheTraducao resolvedor(LlmPort porta, LlmProperties config) {
        return new ResolvedorCacheTraducao(null, null, porta, config, new TradutorProperties());
    }

    private static SnapshotContexto contexto() {
        return new SnapshotContexto("gundam_unicorn", "Mobile Suit Gundam Unicorn",
            "prompt de sistema", "lore", java.util.Set.of(), java.util.Map.of(), java.util.Set.of());
    }

    @Test
    @DisplayName("a PORTA vence a configuração: grava o modelo real, não o \"current\" do yml")
    void portaVenceConfiguracao() {
        // Reproduz o estado real: application.yml traz "current" de propósito e literalmente
        // não sabe o nome do modelo carregado no LM Studio.
        ResolvedorCacheTraducao r = resolvedor(portaComModelo(MODELO_REAL), configuracaoCom("current"));

        ProvenienciaCache prov = r.provenienciaDe(contexto());

        assertEquals(MODELO_REAL, prov.modeloLlm());
        assertNotEquals("current", prov.modeloLlm(),
            "gravar \"current\" faz trocar de LLM NÃO invalidar o cache — o invariante inteiro cai");
    }

    @Test
    @DisplayName("porta sem modelo resolvido cai na configuração — documenta o fallback")
    void fallbackParaConfiguracaoQuandoPortaNaoSabe() {
        // modeloAtivo() devolve null enquanto nenhuma verificação de disponibilidade ocorreu.
        // Gravar a configuração é pior que o ideal, mas melhor que gravar vazio: perderíamos
        // até o rastro de que houve execução.
        ResolvedorCacheTraducao r = resolvedor(portaComModelo(null), configuracaoCom("current"));

        assertEquals("current", r.provenienciaDe(contexto()).modeloLlm());
    }

    @Test
    @DisplayName("cache e telemetria leem o MESMO modelo na mesma execução")
    void cacheETelemetriaConcordam() {
        // A regressão real não é "gravou errado", é "as duas leituras divergiram" — foi assim
        // que o cache ficou com "current" enquanto a telemetria já registrava o modelo real.
        LlmPort porta = portaComModelo(MODELO_REAL);
        LlmProperties config = configuracaoCom("current");
        ResolvedorCacheTraducao resolvedorCache = resolvedor(porta, config);

        String doCache = resolvedorCache.provenienciaDe(contexto()).modeloLlm();
        String daTelemetria = new MontadorTelemetriaTraducao(config, resolvedorCache, porta)
            .montar(java.nio.file.Path.of("ep01.ass"), 10, 10, 0, 1000L,
                List.of(), "Unicorn", "Mobile Suit Gundam Unicorn",
                org.traducao.projeto.traducao.domain.StatusArquivoTraducao.CONCLUIDO, List.of())
            .modeloLlm();

        assertEquals(daTelemetria, doCache,
            "cache e telemetria precisam atribuir a mesma tradução ao mesmo modelo");
    }
}
