package org.traducao.projeto.traducao.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.traducao.application.ProcessarArquivoUseCase;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;
import org.traducao.projeto.traducao.presentation.ui.ConsoleUILogger;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza o tratamento que o {@link TradutorCLI} dá a uma
 * {@code AlucinacaoDetectadaException} lançada ao processar um arquivo — hoje herdado
 * do {@code catch (TradutorException)}. Trava o contrato antes da E8b: mensagem de
 * falha, contagem, lista de arquivos com falha e continuidade do lote.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Uma falha por alucinação num arquivo é contabilizada como falha e não aborta
 *       o processamento dos demais (continuidade do lote).</li>
 *   <li>A mensagem exibida usa o formato do ramo crítico ("[ FAIL ] Falha em X: msg"),
 *       preservando a mensagem original da exceção.</li>
 *   <li>O relatório final reflete 0 sucessos, N falhas e lista os arquivos com falha.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Após o reparenting da E8b, {@code AlucinacaoDetectadaException} deixa de ser
 * {@code TradutorException}; o multi-catch {@code (TradutorException | AlucinacaoDetectadaException)}
 * preserva EXATAMENTE este comportamento e este teste deve continuar verde.
 */
@DisplayName("E8b caracterização: TradutorCLI trata alucinação como falha crítica e continua o lote")
class TradutorCLIAlucinacaoCaracterizacaoTest {

    /** Captura as linhas enviadas ao console para inspeção do relatório/mensagens. */
    private static final class UiLoggerCaptor extends ConsoleUILogger {
        final List<String> linhas = new java.util.ArrayList<>();
        @Override public void log(String mensagem) { linhas.add(mensagem); }
    }

    /** LLM disponível — só precisa passar pela verificação inicial do CLI. */
    private static final class LlmDisponivel implements LlmPort {
        @Override public org.traducao.projeto.llm.domain.TraducaoLote traduzir(
            org.traducao.projeto.llm.domain.Lote lote) { return null; }
        @Override public StatusLlm verificarDisponibilidade() { return new StatusLlm(true, true, "modelo carregado"); }
        @Override public Optional<String> revisarConcordancia(String a, String b, List<String> c) { return Optional.empty(); }
        @Override public Optional<String> corrigirTraducao(String a, String b, String c) { return Optional.empty(); }
    }

    /** Caso de uso que sempre alucina — conta quantos arquivos foram efetivamente processados. */
    private static final class ProcessarArquivoAlucina extends ProcessarArquivoUseCase {
        final AtomicInteger chamadas = new AtomicInteger();
        ProcessarArquivoAlucina() {
            super(null, null, null, null, null, null, null, null, null, null,
                  null, null, null, null, null, null, null, null, null, null);
        }
        @Override
        public Path processar(Path arquivoEntrada) {
            chamadas.incrementAndGet();
            throw new AlucinacaoDetectadaException("alucinacao em " + arquivoEntrada.getFileName());
        }
    }

    @Test
    @DisplayName("dois arquivos que alucinam: ambos contam como falha, mensagem preservada, lote continua")
    void alucinacaoEmArquivoContaComoFalhaESegueOLote(@TempDir Path entrada) throws Exception {
        Files.writeString(entrada.resolve("ep1.ass"), "conteudo");
        Files.writeString(entrada.resolve("ep2.ass"), "conteudo");

        UiLoggerCaptor uiLogger = new UiLoggerCaptor();
        ProcessarArquivoAlucina processar = new ProcessarArquivoAlucina();
        TradutorProperties props = new TradutorProperties(
            entrada.toString(), entrada.toString(), entrada.toString(), 20, List.of(), "en", "pt-BR");
        PastasExecucao pastas = new PastasExecucao();

        TradutorCLI cli = new TradutorCLI(processar, uiLogger, props, pastas, new LlmDisponivel());
        cli.executar();

        // Continuidade do lote: os DOIS arquivos foram processados apesar da falha no primeiro.
        assertEquals(2, processar.chamadas.get(), "o lote deve continuar após a falha do primeiro arquivo");

        // Mensagem preservada no formato do ramo crítico "[ FAIL ] Falha em X: msg".
        long falhasFormatoCritico = uiLogger.linhas.stream()
            .filter(l -> l.startsWith("[ FAIL ] Falha em ") && l.contains("alucinacao em "))
            .count();
        assertEquals(2, falhasFormatoCritico, "cada arquivo alucinado deve exibir a falha crítica com a mensagem original");

        // Relatório final: 0 sucessos, 2 falhas, e a lista de arquivos com falha.
        assertTrue(uiLogger.linhas.stream().anyMatch(l -> l.contains("0 sucesso(s), 2 falha(s) de 2 arquivo(s)")),
            "relatório deve contabilizar 2 falhas e 0 sucessos");
        assertTrue(uiLogger.linhas.stream().anyMatch(l ->
                l.startsWith("[ WARN ] Arquivos com falha") && l.contains("ep1.ass") && l.contains("ep2.ass")),
            "relatório deve listar os dois arquivos com falha");
    }
}
