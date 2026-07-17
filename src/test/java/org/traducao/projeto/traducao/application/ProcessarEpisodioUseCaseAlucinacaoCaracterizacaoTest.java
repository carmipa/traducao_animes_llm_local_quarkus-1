package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.domain.Lote;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.TraducaoLote;
import org.traducao.projeto.qualidadeTraducao.application.ValidadorTraducaoService;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.traducao.domain.ports.LlmPort;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o comportamento REALMENTE observável do
 * {@link ProcessarEpisodioUseCase} quando o LLM devolve uma fala isolada que o
 * validador rejeita como alucinação — travando o contrato antes da E8b para que a
 * extração de {@code AlucinacaoDetectadaException} para o peer {@code qualidadeTraducao}
 * não altere o fluxo em silêncio.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A resposta rejeitada dispara nova tentativa da fala isolada (retry com
 *       temperatura variável), não a propagação imediata da exceção.</li>
 *   <li>Cada rejeição é contabilizada em {@code registrarRespostaTraducaoRejeitada}.</li>
 *   <li>Esgotadas as tentativas, o fallback mantém a fala ORIGINAL — sem sucesso
 *       falso: {@code registrarFalhaTraducaoRecuperada} nunca é chamado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Este teste NÃO afirma que a exceção alcança o catch externo de
 * {@code traduzirEValidar}: no fluxo real ela é absorvida pela divisão/retry/fallback.
 * O multi-catch externo introduzido na E8b é preservação defensiva do contrato antes
 * coberto pela hierarquia de {@code TradutorException}, e este teste deve permanecer
 * verde antes e depois do reparenting.
 */
@DisplayName("E8b caracterização: fala alucinada é rejeitada, retentada e sofre fallback seguro (sem sucesso falso)")
class ProcessarEpisodioUseCaseAlucinacaoCaracterizacaoTest {

    /** Fake de telemetria com contadores — o único observável do fluxo de rejeição/recuperação. */
    private static final class TelemetriaFake implements TelemetriaTraducaoPort {
        final AtomicInteger rejeitadas = new AtomicInteger();
        final AtomicInteger recuperadas = new AtomicInteger();

        @Override public void registrarTraducao(TelemetriaTraducao telemetria) { }
        @Override public void registrarAlucinacaoPrevenida() { }
        @Override public void registrarRespostaTraducaoRejeitada() { rejeitadas.incrementAndGet(); }
        @Override public void registrarFalhaTraducaoRecuperada() { recuperadas.incrementAndGet(); }
        @Override public void registrarFallbackMantido() { }
    }

    /** Fake de LLM: sempre devolve UMA linha "traduzida" com sucesso; a rejeição vem do validador. */
    private static final class LlmSempreUmaLinha implements LlmPort {
        @Override public TraducaoLote traduzir(Lote lote) {
            return new TraducaoLote(lote.idLote(), List.of("resposta-do-llm"), true, null);
        }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(true, true, "ok"); }
        @Override public Optional<String> revisarConcordancia(String a, String b, List<String> c) { return Optional.empty(); }
        @Override public Optional<String> corrigirTraducao(String a, String b, String c) { return Optional.empty(); }
    }

    @Test
    @DisplayName("fala alucinada: 3 rejeições contabilizadas, fallback mantém original, sem recuperação")
    void falaAlucinadaSofreRejeicaoRetentativaEFallbackSeguro() throws InterruptedException, ExecutionException {
        TelemetriaFake telemetria = new TelemetriaFake();
        // Validador que sempre rejeita a fala como alucinação (simula o LLM alucinando).
        ValidadorTraducaoService validadorAlucina = new ValidadorTraducaoService() {
            @Override
            public void validarFala(String textoTraduzido) {
                throw new AlucinacaoDetectadaException("fala alucinada: " + textoTraduzido);
            }
        };

        ProcessarEpisodioUseCase useCase = new ProcessarEpisodioUseCase(
            new LlmSempreUmaLinha(), validadorAlucina, new ConsoleUILogger(), telemetria);

        List<TraducaoLote> resultado = useCase.processarEpisodio(List.of(new Lote(1, List.of("KEEPME"))), null);

        // Fallback seguro: mantém a fala ORIGINAL, sem propagar a exceção nem inventar tradução.
        assertEquals(1, resultado.size());
        assertEquals(List.of("KEEPME"), resultado.getFirst().linhasTraduzidas(),
            "o fallback deve manter a fala original, não a resposta alucinada do LLM");

        // 1 tentativa inicial + 2 extras = 3 respostas rejeitadas contabilizadas.
        assertEquals(3, telemetria.rejeitadas.get(), "cada rejeição deve ser contabilizada");
        // Sem sucesso falso: nenhuma recuperação foi registrada.
        assertEquals(0, telemetria.recuperadas.get(),
            "manter o original NÃO é recuperação — não pode registrar falha recuperada");
        assertTrue(resultado.getFirst().sucesso(),
            "o lote encerra sem abortar o episódio (marcador transitório convertido em parcial adiante)");
    }
}
