package org.traducao.projeto.traducaoKaraoke.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.LoreAtivaFake;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * PROPÓSITO DE NEGÓCIO: a MESMA linha da MESMA música tem de sair com a MESMA tradução em todos
 * os episódios. Este teste congela o único mecanismo que garante isso — a amostragem
 * determinística — no ponto por onde as três rotas de envio passam.
 *
 * <h2>O prejuízo que originou, medido em 2026-08-20</h2>
 * Quatro obras traduzidas no mesmo dia, contando por texto DISTINTO enviado ao LLM:
 * <pre>
 *   86 Part 1   16 de  31 divergentes (52%)  pior: 5 traduções
 *   86 Part 2    5 de  11 (45%)              pior: 4
 *   Zeta        19 de  33 (58%)              pior: 8
 *   Unicorn     73 de 131 (56%)              pior: 10 — o fragmento "dnt"
 * </pre>
 * Uma das cinco variantes de <i>"No matter how hard I wish, nothing ever changes"</i> saiu
 * <i>"Importante quanto desejar, nada nunca muda."</i> A abertura mudava de texto a cada episódio.
 *
 * <h2>Por que testar a TEMPERATURA e não o resultado</h2>
 * O resultado depende do modelo, que não roda na suíte. O que está sob controle desta fatia é o
 * parâmetro que ela ENVIA — e é ele que decide se a mesma pergunta terá a mesma resposta. Testar
 * o parâmetro é testar o que o código realmente decide.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Se alguém devolver {@code null} a qualquer um dos três pontos de envio, a temperatura volta a
 * ser a configurada (0,3, compartilhada com a Tradução Local) e a divergência entre episódios
 * volta em silêncio — não há saída visível que denuncie isso.
 */
class TemperaturaDeterministicaKaraokeTest {

    private TradutorDeLetraKaraoke tradutor;
    private LlmPortEspiao espiao;

    /**
     * Captura a temperatura de CADA chamada. Implementa o método de 4 argumentos porque é o
     * fundo do poço da porta: as sobrecargas de 1, 2 e 3 argumentos delegam até ele, então
     * espionar aqui pega qualquer rota que a fatia use hoje ou passe a usar.
     */
    static final class LlmPortEspiao implements LlmPort {
        final List<Double> temperaturas = new ArrayList<>();

        @Override
        public TraducaoLote traduzir(Lote lote) {
            temperaturas.add(null);
            return new TraducaoLote(lote.idLote(), List.of("traducao simulada"), true, null);
        }

        @Override
        public TraducaoLote traduzir(Lote lote, Double temperaturaOverride) {
            temperaturas.add(temperaturaOverride);
            return new TraducaoLote(lote.idLote(), List.of("traducao simulada"), true, null);
        }

        @Override
        public TraducaoLote traduzir(Lote lote, Double temperaturaOverride, String promptSistema) {
            return traduzir(lote, temperaturaOverride);
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return new StatusLlm(true, true, "espiao");
        }

        @Override
        public java.util.Optional<String> revisarConcordancia(String original, String traducao,
                List<String> problemas) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> corrigirTraducao(String original, String traducao,
                String motivo) {
            return java.util.Optional.empty();
        }
    }

    static final class LogSilencioso extends LogStreamService {
        @Override
        public void publicarLog(String canal, String mensagem) {
            // sem SSE na suite
        }
    }

    static final class TelemetriaSilenciosa extends TelemetriaService {
        @Override
        public void finalizarOperacao(OperacaoTelemetria operacao, Path pastaEntrada,
                String prefixo, String conteudo) {
            // a fatia nao mede telemetria por aqui
        }

        @Override
        public void registrarAlucinacaoPrevenida() {
            // silencioso
        }
    }

    @BeforeEach
    void montar() {
        tradutor = new TradutorDeLetraKaraoke();
        tradutor.llmPort = espiao = new LlmPortEspiao();
        tradutor.mascarador = new MascaradorTags();
        tradutor.logStream = new LogSilencioso();
        tradutor.telemetriaService = new TelemetriaSilenciosa();
        // O validador roda DEPOIS da resposta, mas roda: deixa-lo nulo derruba a rota com NPE
        // antes da assertiva. Lore vazia porque o que este teste mede e o parametro do ENVIO.
        tradutor.validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());
    }

    @Test
    @DisplayName("a constante é 0.0 — determinística, e não a configurada do diálogo")
    void constanteEhDeterministica() {
        assertEquals(0.0d, TradutorDeLetraKaraoke.TEMPERATURA_DETERMINISTICA,
            "temperatura diferente de zero devolve resposta diferente para a mesma pergunta");
    }

    /**
     * O caminho de TEXTO PURO é por onde 100% do 86 passa (tag só na borda). É o mais usado e o
     * mais fácil de regredir sem ninguém ver.
     */
    @Test
    @DisplayName("texto puro envia a temperatura determinística")
    void textoPuroEnviaDeterministica() {
        var semTags = org.traducao.projeto.core.texto.TextoSemTags
            .decompor("{\\blur2}Do you feel alone").orElseThrow();

        tradutor.traduzirTextoPuro(semTags, new ArrayList<>(), new AtomicInteger(), "prompt");

        assertEquals(1, espiao.temperaturas.size(), "a rota nao chamou o LLM");
        assertEquals(0.0d, espiao.temperaturas.get(0),
            "texto puro voltou a usar a temperatura configurada: divergencia entre episodios volta");
    }

    /**
     * Contra-teste que fecha a porta dos fundos: NENHUMA das chamadas pode passar {@code null},
     * porque {@code null} significa "usa a configurada" — que é 0,3 e é compartilhada com o
     * diálogo. Sem esta asserção, um ponto esquecido passaria despercebido.
     */
    @Test
    @DisplayName("nenhuma rota passa null — null significa a temperatura do diálogo")
    void nenhumaRotaPassaNull() {
        var semTags = org.traducao.projeto.core.texto.TextoSemTags
            .decompor("{\\blur2}Can you hear me now").orElseThrow();
        tradutor.traduzirTextoPuro(semTags, new ArrayList<>(), new AtomicInteger(), "prompt");

        assertFalse(espiao.temperaturas.isEmpty(), "nada foi enviado: o teste nao mediu nada");
        for (Double t : espiao.temperaturas) {
            assertNotNull(t, "uma rota passou null e caiu na temperatura configurada (0,3)");
            assertEquals(0.0d, t);
        }
    }
}
