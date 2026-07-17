package org.traducao.projeto.traducao.application;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: monta o registro de telemetria de uma tradução — o instantâneo de
 * métricas (modelo, falas traduzíveis/novas/reaproveitadas, tempo, avisos, obra/temporada,
 * status) que alimenta o painel —, isolando essa montagem da orquestração de
 * {@link ProcessarArquivoUseCase} (FASE F, R7).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O modelo registrado é o efetivamente ativo ({@code llmProperties.model()}) e a
 *       temporada é derivada do nome da obra pela mesma regra do cache.</li>
 *   <li>A lista de avisos é copiada de forma imutável para o registro não refletir mutações
 *       posteriores do chamador.</li>
 *   <li>O carimbo de tempo é o instante da montagem, em ISO-8601.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Montagem pura de um record de domínio, sem I/O nem exceção: os valores recebidos são
 * transcritos como estão para a telemetria.
 */
@Component
public class MontadorTelemetriaTraducao {

    private final LlmProperties llmPropriedades;
    private final ResolvedorCacheTraducao resolvedorCache;

    public MontadorTelemetriaTraducao(LlmProperties llmPropriedades, ResolvedorCacheTraducao resolvedorCache) {
        this.llmPropriedades = llmPropriedades;
        this.resolvedorCache = resolvedorCache;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: constrói o registro de telemetria da execução a partir das
     * métricas apuradas pelo fluxo de tradução.
     *
     * <p>INVARIANTES DO DOMÍNIO: transcreve as métricas fielmente; deriva modelo, temporada
     * e carimbo de tempo; copia a lista de avisos de forma imutável.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro; nunca lança.
     */
    public TelemetriaTraducao montar(
            Path arquivoEntrada,
            int eventosTraduziveis,
            int traducoesNovasValidas,
            int cacheReaproveitadas,
            long tempoTotalMs,
            List<String> avisos,
            String animeNome,
            String loreNome,
            StatusArquivoTraducao status) {
        return new TelemetriaTraducao(
            arquivoEntrada.getFileName().toString(),
            llmPropriedades.model(),
            eventosTraduziveis,
            traducoesNovasValidas,
            cacheReaproveitadas,
            tempoTotalMs,
            List.copyOf(avisos),
            animeNome,
            resolvedorCache.temporadaAPartirDoNome(animeNome),
            Instant.now().toString(),
            loreNome,
            status.name()
        );
    }
}
