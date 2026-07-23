package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.StatusArquivoTraducao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
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

    /**
     * PROPÓSITO DE NEGÓCIO: dublê da porta do LLM que informa qual modelo de fato respondeu.
     * <p>INVARIANTES DO DOMÍNIO: só {@code modeloAtivo()} importa aqui; os demais métodos não são
     * exercitados pela montagem do registro.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; devolve o valor fixo recebido.
     */
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

    /**
     * PROPÓSITO DE NEGÓCIO: o registro precisa dizer QUAL modelo produziu a tradução, senão não
     * existe comparação entre execuções — a mesma obra numa máquina potente e num notebook com
     * GPU pequena só é comparável se cada registro souber qual LLM rodou.
     *
     * <p>CASO REAL: em 2026-07-23 os 155 registros traziam {@code modeloLlm="current"}. O valor
     * vinha da configuração, e {@code "current"} é proposital no {@code application.yml} —
     * pedir o id exato faz o LM Studio recarregar o modelo. A configuração literalmente não sabe
     * o nome; só a porta sabe. A detecção já existia em {@code LlmClientAdapter}, mas o
     * {@code setModel} não chegava ao montador porque {@link LlmProperties} é
     * {@code @ConfigurationProperties} e não é instância compartilhada.
     *
     * <p>INVARIANTES DO DOMÍNIO: a porta VENCE a configuração; sem porta, cai na configuração.
     * <p>COMPORTAMENTO EM CASO DE FALHA: voltar a gravar o valor configurado inutiliza o campo e
     * torna impossível comparar máquinas e modelos.
     */
    @Test
    void modeloVemDaPortaNaoDaConfiguracao() {
        MontadorTelemetriaTraducao comPorta = new MontadorTelemetriaTraducao(
            llmComModelo("current"), resolvedorComTemporada("T1"), portaComModelo("qwen2.5-14b-instruct-q4"));

        TelemetriaTraducao t = comPorta.montar(Path.of("A", "ep.ass"), 1, 1, 0, 1L,
            List.of(), "A", "Lore", StatusArquivoTraducao.CONCLUIDO, List.of());

        assertEquals("qwen2.5-14b-instruct-q4", t.modeloLlm(),
            "o modelo tem de ser o que a porta resolveu, nunca o 'current' da configuracao");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sem modelo resolvido (nenhuma verificação de disponibilidade ainda),
     * gravar a configuração é melhor que gravar vazio — preserva ao menos o rastro da execução.
     * <p>INVARIANTES DO DOMÍNIO: porta ausente ou sem resposta degrada para a configuração.
     * <p>COMPORTAMENTO EM CASO DE FALHA: campo vazio apaga o rastro da execução.
     */
    @Test
    void semModeloResolvidoCaiNaConfiguracao() {
        MontadorTelemetriaTraducao semPorta = new MontadorTelemetriaTraducao(
            llmComModelo("modelo-do-yml"), resolvedorComTemporada("T1"), portaComModelo(null));

        TelemetriaTraducao t = semPorta.montar(Path.of("A", "ep.ass"), 1, 1, 0, 1L,
            List.of(), "A", "Lore", StatusArquivoTraducao.CONCLUIDO, List.of());

        assertEquals("modelo-do-yml", t.modeloLlm());
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
            new MontadorTelemetriaTraducao(llmComModelo("modelo-montador"), resolvedorComTemporada("Temporada 7"), null);

        TelemetriaTraducao t = montador.montar(
            Path.of("MeuAnime", "legendas_originais", "ep03.ass"),
            10, 4, 6, 1234L, List.of("aviso A"), "MeuAnime", "MinhaLore",
            StatusArquivoTraducao.CONCLUIDO, List.of());

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
            new MontadorTelemetriaTraducao(llmComModelo("m"), resolvedorComTemporada("Temporada Única"), null);
        List<String> avisosOriginais = new ArrayList<>(List.of("aviso 1"));

        TelemetriaTraducao t = montador.montar(
            Path.of("ep.ass"), 1, 1, 0, 1L, avisosOriginais, "Anime", "Lore",
            StatusArquivoTraducao.PARCIAL, List.of());

        avisosOriginais.add("aviso 2 posterior");
        assertEquals(List.of("aviso 1"), t.errosOcorridos(),
            "mutação posterior da lista original não pode alterar a telemetria");
        assertThrows(UnsupportedOperationException.class, () -> t.errosOcorridos().add("x"),
            "a lista de avisos da telemetria deve ser imutável");
    }
}
