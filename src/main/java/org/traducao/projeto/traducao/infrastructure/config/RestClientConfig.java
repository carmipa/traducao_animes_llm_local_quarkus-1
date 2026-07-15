package org.traducao.projeto.traducao.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.Instance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

/**
 * PROPÓSITO DE NEGÓCIO: fornece os beans técnicos de suporte à injeção CDI/Quarkus
 * usados internamente pela Tradução Local — o {@link ObjectMapper} de serialização
 * e a coleção agregada de provedores de contexto de tradução ({@link ProvedorContexto}).
 * A composição dos extratores de vídeo/strategies deixou de residir aqui e pertence
 * à fatia proprietária (ver {@code legendasExtracao.infrastructure.config.ExtracaoBeansConfig}).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code todosProvedoresContexto} agrega TODAS as implementações CDI de
 *       {@link ProvedorContexto} via {@link Instance}, sem ordenação imposta.</li>
 *   <li>O {@link ObjectMapper} é criado com configuração default (sem módulos
 *       ou features customizadas).</li>
 *   <li>Esta config não conhece classes de outras fatias funcionais.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem provedores de contexto registrados, {@code todosProvedoresContexto} devolve
 * lista vazia (nunca nula); a serialização default do {@link ObjectMapper} propaga
 * as exceções de Jackson ao chamador.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public List<ProvedorContexto> todosProvedoresContexto(Instance<ProvedorContexto> instancias) {
        List<ProvedorContexto> list = new ArrayList<>();
        instancias.forEach(list::add);
        return list;
    }
}
