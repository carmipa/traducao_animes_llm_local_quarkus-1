package org.traducao.projeto.legendasExtracao.infrastructure.config;

import jakarta.enterprise.inject.Instance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorStrategy;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;

/**
 * PROPÓSITO DE NEGÓCIO: reúne, dentro da própria fatia de Extração de Legendas,
 * a composição CDI dos adaptadores de vídeo e das strategies de formato. Entrega
 * a {@code ExtrairLegendaUseCase} a coleção completa de implementações
 * registradas, para que a escolha do extrator (por contêiner) e da strategy (por
 * formato) seja feita em tempo de execução sem a fatia conhecer as classes
 * concretas. A composição pertence à fatia dona da extração — não à Tradução Local.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cada coleção agrega TODAS as implementações CDI disponíveis do respectivo
 *       tipo, via {@link Instance}, preservando a ordem natural de descoberta do
 *       container — nenhuma ordenação, prioridade ou sorting é imposto.</li>
 *   <li>As listas são novas instâncias mutáveis desacopladas do {@link Instance}.</li>
 *   <li>A semântica é idêntica à anterior (Spring DI integrado ao Quarkus): apenas
 *       o LOCAL da composição mudou para a fatia proprietária.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se nenhuma implementação de um dos tipos estiver registrada, a coleção
 * correspondente é retornada vazia (nunca nula); o consumidor decide como tratar
 * a ausência de extrator/strategy compatível.
 */
@Configuration
public class ExtracaoBeansConfig {

    /**
     * PROPÓSITO DE NEGÓCIO: expõe todos os adaptadores de vídeo capazes de
     * identificar e extrair faixas de legenda de contêineres suportados.
     * <p>INVARIANTES DO DOMÍNIO: agrega todas as implementações CDI de
     * {@link ExtratorVideoPort} sem ordenação imposta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem implementações registradas, devolve
     * lista vazia (não nula).
     */
    @Bean
    public List<ExtratorVideoPort> todosExtratoresVideoPort(Instance<ExtratorVideoPort> instancias) {
        List<ExtratorVideoPort> list = new ArrayList<>();
        instancias.forEach(list::add);
        return list;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: expõe todas as strategies de seleção de faixa por
     * formato de legenda (ASS/SRT/PGS).
     * <p>INVARIANTES DO DOMÍNIO: agrega todas as implementações CDI de
     * {@link ExtratorStrategy} sem ordenação imposta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem implementações registradas, devolve
     * lista vazia (não nula).
     */
    @Bean
    public List<ExtratorStrategy> todosExtratoresStrategy(Instance<ExtratorStrategy> instancias) {
        List<ExtratorStrategy> list = new ArrayList<>();
        instancias.forEach(list::add);
        return list;
    }
}
