package org.traducao.projeto.contexto.infrastructure.config;

import jakarta.enterprise.inject.Instance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: reúne, para o módulo compartilhado {@code contexto}, todas as
 * implementações CDI de {@link ProvedorContexto} numa única lista consumível pelo
 * {@code GerenciadorContexto}. Extraído de {@code traducao.infrastructure.config.RestClientConfig}
 * na E7b para que a agregação dos provedores pertença ao próprio peer, e não à fatia
 * de tradução (mesmo padrão de {@code legendasExtracao.infrastructure.config.ExtracaoBeansConfig}).
 *
 * <p>INVARIANTES DO DOMÍNIO: agrega TODAS as implementações descobertas via
 * {@link Instance}, na ordem de iteração fornecida pelo container, sem impor ordenação
 * própria (a ordenação por nome de exibição é responsabilidade do {@code GerenciadorContexto}).
 * Não conhece classes de nenhuma fatia funcional.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: quando não há nenhum provedor registrado,
 * {@link #todosProvedoresContexto(Instance)} devolve uma lista vazia (nunca nula).
 */
@Configuration
public class ContextoBeansConfig {

    /**
     * PROPÓSITO DE NEGÓCIO: expõe todas as implementações CDI de {@link ProvedorContexto}
     * ao {@code GerenciadorContexto}.
     * <p>INVARIANTES DO DOMÍNIO: agrega todas as instâncias descobertas, na ordem do
     * container, sem ordenação própria.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem implementações, devolve lista vazia (não nula).
     */
    @Bean
    public List<ProvedorContexto> todosProvedoresContexto(Instance<ProvedorContexto> instancias) {
        List<ProvedorContexto> list = new ArrayList<>();
        instancias.forEach(list::add);
        return list;
    }
}
