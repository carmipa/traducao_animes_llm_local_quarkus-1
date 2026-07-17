package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * PROPÓSITO DE NEGÓCIO: fixa por regressão a montagem do registro de telemetria pelo
 * {@link MontadorTelemetriaTraducao} (FASE F, R7), garantindo que cada métrica caia no campo
 * certo, que modelo/temporada/timestamp sejam derivados e que os avisos sejam imutáveis.
 *
 * <p>INVARIANTES DO DOMÍNIO: o modelo vem de {@link LlmProperties} e a temporada de
 * {@link ResolvedorCacheTraducao} (aqui controlado por subclasse), provando o roteamento; sem
 * rede, LM Studio, sleep ou dependência temporal além do carimbo ISO-8601 gerado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: campo trocado, timestamp inválido ou lista mutável reprova.
 */
class MontadorTelemetriaTraducaoTest {

    private static LlmProperties llmComModelo(String modelo) {
        return new LlmProperties("http://127.0.0.1:1234/v1", modelo, 0.3, 2048,
            Duration.ofSeconds(5), Duration.ofSeconds(30));
    }

    private static ResolvedorCacheTraducao resolvedorComTemporada(String temporada) {
        return new ResolvedorCacheTraducao(null, null, null, null, null) {
            @Override
            public String temporadaAPartirDoNome(String animeNome) {
                return temporada;
            }
        };
    }

    /**
     * PROPÓSITO DE NEGÓCIO: prova que cada argumento e cada valor derivado aparecem no campo
     * exato do registro de telemetria.
     * <p>INVARIANTES DO DOMÍNIO: nome do arquivo, modelo ativo, contagens, duração, anime,
     * temporada, lore, status e timestamp ISO-8601 são transcritos fielmente.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer campo divergente ou timestamp inválido reprova.
     */
    @Test
    void montaRegistroComCadaCampoNoLugarCerto() {
        MontadorTelemetriaTraducao montador =
            new MontadorTelemetriaTraducao(llmComModelo("modelo-montador"), resolvedorComTemporada("Temporada 7"));

        TelemetriaTraducao t = montador.montar(
            Path.of("MeuAnime", "legendas_originais", "ep03.ass"),
            10, 4, 6, 1234L, List.of("aviso A"), "MeuAnime", "MinhaLore",
            StatusArquivoTraducao.CONCLUIDO);

        assertEquals("ep03.ass", t.nomeEpisodio());
        assertEquals("modelo-montador", t.modeloLlm());
        assertEquals(10, t.totalLinhas());
        assertEquals(4, t.falasTraduzidas());
        assertEquals(6, t.falasDoCache());
        assertEquals(1234L, t.tempoTotalMs());
        assertEquals("MeuAnime", t.animeNome());
        assertEquals("Temporada 7", t.temporada());
        assertEquals("MinhaLore", t.loreNome());
        assertEquals("CONCLUIDO", t.statusFinal());
        assertEquals(List.of("aviso A"), t.errosOcorridos());
        assertDoesNotThrow(() -> Instant.parse(t.registradoEm()),
            "registradoEm deve ser um timestamp ISO-8601 válido");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os avisos gravados na telemetria são um instantâneo imutável — não
     * refletem mutações posteriores da lista original do chamador.
     * <p>INVARIANTES DO DOMÍNIO: {@code List.copyOf} produz cópia imutável; alterar a lista
     * original depois não altera a telemetria.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista mutável ou reflexo de mutação posterior reprova.
     */
    @Test
    void avisosSaoCopiadosDeFormaImutavel() {
        MontadorTelemetriaTraducao montador =
            new MontadorTelemetriaTraducao(llmComModelo("m"), resolvedorComTemporada("Temporada Única"));
        List<String> avisosOriginais = new ArrayList<>(List.of("aviso 1"));

        TelemetriaTraducao t = montador.montar(
            Path.of("ep.ass"), 1, 1, 0, 1L, avisosOriginais, "Anime", "Lore",
            StatusArquivoTraducao.PARCIAL);

        avisosOriginais.add("aviso 2 posterior");
        assertEquals(List.of("aviso 1"), t.errosOcorridos(),
            "mutação posterior da lista original não pode alterar a telemetria");
        assertThrows(UnsupportedOperationException.class, () -> t.errosOcorridos().add("x"),
            "a lista de avisos da telemetria deve ser imutável");
    }
}
