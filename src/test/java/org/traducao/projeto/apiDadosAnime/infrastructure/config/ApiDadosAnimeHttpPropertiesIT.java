package org.traducao.projeto.apiDadosAnime.infrastructure.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: gate/caracterização da subfase E4a — prova, no binding REAL
 * do {@code application.yml}, que os timeouts efetivos hoje usados pelos adapters de
 * {@code apiDadosAnime} (via {@code LlmProperties}) são {@code 5s/180s}, e que a nova
 * config própria ({@code ApiDadosAnimeHttpProperties}) resolve os MESMOS valores —
 * comprovando paridade antes de neutralizar/mover o cliente HTTP.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Valores efetivos ATUAIS de {@code LlmProperties}: connect {@code 5s}, read {@code 180s}.</li>
 *   <li>Nova config {@code ApiDadosAnimeHttpProperties}: os MESMOS pares.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência reprova o teste — sinal de gate (não prosseguir com a migração).
 */
@QuarkusTest
class ApiDadosAnimeHttpPropertiesIT {

    @Inject
    LlmProperties llmProperties;

    @Inject
    ApiDadosAnimeHttpProperties apiHttp;

    @Test
    @DisplayName("gate: timeouts efetivos atuais de LlmProperties == 5s/180s")
    void timeoutsEfetivosAtuais() {
        assertEquals(Duration.ofSeconds(5), llmProperties.connectTimeout(), "connect efetivo atual");
        assertEquals(Duration.ofSeconds(180), llmProperties.readTimeout(), "read efetivo atual");
    }

    @Test
    @DisplayName("paridade: ApiDadosAnimeHttpProperties resolve os mesmos 5s/180s")
    void paridadeComConfigNova() {
        assertEquals(llmProperties.connectTimeout(), apiHttp.connectTimeout(), "paridade connect");
        assertEquals(llmProperties.readTimeout(), apiHttp.readTimeout(), "paridade read");
        assertEquals(Duration.ofSeconds(5), apiHttp.connectTimeout());
        assertEquals(Duration.ofSeconds(180), apiHttp.readTimeout());
    }
}
