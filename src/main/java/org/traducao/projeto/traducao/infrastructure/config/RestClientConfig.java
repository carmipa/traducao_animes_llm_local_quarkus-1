package org.traducao.projeto.traducao.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * PROPÓSITO DE NEGÓCIO: fornece o bean técnico {@link ObjectMapper} de serialização
 * usado internamente pela Tradução Local. A agregação dos provedores de contexto
 * ({@code todosProvedoresContexto}) foi movida na E7b para o peer proprietário
 * ({@code contexto.infrastructure.config.ContextoBeansConfig}); a composição dos
 * extratores de vídeo/strategies pertence a {@code legendasExtracao.infrastructure.config.ExtracaoBeansConfig}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O {@link ObjectMapper} é criado com configuração default (sem módulos
 *       ou features customizadas).</li>
 *   <li>Esta config não conhece classes de outras fatias funcionais nem do peer contexto.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * A serialização default do {@link ObjectMapper} propaga as exceções de Jackson ao chamador.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
