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
 * A montagem não realiza I/O próprio, mas NÃO é infalível: {@code arquivoEntrada}, {@code avisos},
 * {@code animeNome} e {@code status} são pré-condições do chamador. Argumentos nulos ou dados
 * inválidos propagam a exceção Java correspondente em vez de serem mascarados — por exemplo,
 * {@code arquivoEntrada} nulo dispara {@link NullPointerException} em {@code getFileName()},
 * {@code status} nulo em {@code status.name()} e {@link java.util.List#copyOf(java.util.Collection)}
 * rejeita lista ou elemento nulo. Nenhuma falha é ocultada silenciosamente.
 */
@Component
public class MontadorTelemetriaTraducao {

    private final LlmProperties llmPropriedades;
    private final ResolvedorCacheTraducao resolvedorCache;

    /**
     * PROPÓSITO DE NEGÓCIO: injeta as fontes derivadas do registro — o modelo ativo e a regra de
     * temporada — para que a montagem apenas transcreva métricas e derive esses dois campos.
     *
     * <p>INVARIANTES DO DOMÍNIO: guarda as referências recebidas; não as substitui nem cria
     * implementação própria.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida os argumentos; a injeção CDI garante os beans.
     *
     * @param llmPropriedades configuração de onde vem o modelo efetivamente ativo
     * @param resolvedorCache resolvedor que deriva a temporada a partir do nome da obra
     */
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
     * <p>COMPORTAMENTO EM CASO DE FALHA: não faz I/O próprio; argumentos nulos ou dados inválidos
     * propagam a exceção Java correspondente ({@link NullPointerException} em
     * {@code arquivoEntrada}/{@code status}; {@code List.copyOf} rejeita {@code avisos} nulo ou com
     * elemento nulo). Nenhuma falha é ocultada.
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
