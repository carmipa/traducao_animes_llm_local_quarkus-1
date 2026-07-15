package org.traducao.projeto.traducao.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.Instance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.legendasExtracao.application.strategy.ExtratorStrategy;

/**
 * Beans de agregacao e suporte para injecao CDI/Quarkus.
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

    @Bean
    public List<ExtratorVideoPort> todosExtratoresVideoPort(Instance<ExtratorVideoPort> instancias) {
        List<ExtratorVideoPort> list = new ArrayList<>();
        instancias.forEach(list::add);
        return list;
    }

    @Bean
    public List<ExtratorStrategy> todosExtratoresStrategy(Instance<ExtratorStrategy> instancias) {
        List<ExtratorStrategy> list = new ArrayList<>();
        instancias.forEach(list::add);
        return list;
    }
}
