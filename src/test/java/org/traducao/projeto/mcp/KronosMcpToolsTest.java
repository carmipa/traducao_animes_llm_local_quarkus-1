package org.traducao.projeto.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garante que a porta MCP siga a MESMA política de execução da UI: a análise
 * roda pela {@link FilaExecucaoPipeline} (não direto), e uma solicitação com a
 * fila ocupada é recusada de forma estruturada em vez de rodar em paralelo com
 * outro job. Usa um fake do use case — sem ffprobe real.
 */
class KronosMcpToolsTest {

    private FilaExecucaoPipeline fila;

    // A fila usa thread daemon; não é preciso encerrá-la explicitamente aqui
    // (encerrar() é package-private e o daemon não impede o fim da JVM).
    @BeforeEach
    void iniciar() {
        fila = new FilaExecucaoPipeline();
    }

    @Test
    void caminhoVazioRetornaErroSemTocarNaFila() {
        FakeAnalise fake = new FakeAnalise(loteVazio(), null);
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        assertTrue(mcp.analisarMidia("   ").startsWith("ERRO"));
        assertEquals(0, fake.chamadas.get());
    }

    @Test
    void caminhoNuloRetornaErroSemTocarNaFila() {
        FakeAnalise fake = new FakeAnalise(loteVazio(), null);
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        assertTrue(mcp.analisarMidia(null).startsWith("ERRO"));
        assertEquals(0, fake.chamadas.get());
    }

    @Test
    void caminhoSintaticamenteInvalidoRetornaErroDeCaminhoInvalido() {
        FakeAnalise fake = new FakeAnalise(loteVazio(), null);
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        // Caractere NUL torna o caminho sintaticamente invalido (InvalidPathException)
        // tanto no Windows quanto em sistemas POSIX. Construido via (char) 0 para
        // evitar ambiguidade de escape no fonte.
        String caminhoInvalido = "caminho" + (char) 0 + "invalido";
        String resposta = mcp.analisarMidia(caminhoInvalido);

        assertTrue(resposta.startsWith("ERRO: caminho invalido"), resposta);
        assertEquals(0, fake.chamadas.get(), "caminho invalido não deve acionar a fila");
    }

    @Test
    void caminhoInexistenteRetornaErro() {
        FakeAnalise fake = new FakeAnalise(loteVazio(), null);
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        String resposta = mcp.analisarMidia("Z:/pasta/que/nao/existe/kronos-xyz");

        assertTrue(resposta.startsWith("ERRO"), resposta);
        assertEquals(0, fake.chamadas.get());
    }

    @Test
    void analiseRodaPelaFilaERetornaResumo(@TempDir Path dir) {
        FakeAnalise fake = new FakeAnalise(loteVazio(), null);
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        String resposta = mcp.analisarMidia(dir.toString());

        assertTrue(resposta.contains("Auditoria concluida"), resposta);
        assertEquals(1, fake.chamadas.get(), "a análise deve ter sido executada uma vez pela fila");
    }

    @Test
    void filaOcupadaRecusaSemExecutar(@TempDir Path dir) throws Exception {
        FakeAnalise fake = new FakeAnalise(loteVazio(), null);
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        CountDownLatch comecou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        fila.submeter(() -> {
            comecou.countDown();
            try {
                liberar.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(comecou.await(5, TimeUnit.SECONDS));

        String resposta = mcp.analisarMidia(dir.toString());

        assertTrue(resposta.startsWith("OCUPADO"), resposta);
        assertEquals(0, fake.chamadas.get(), "não deve rodar em paralelo com o job em execução");

        liberar.countDown();
    }

    @Test
    void falhaDaAnaliseViraErro(@TempDir Path dir) {
        FakeAnalise fake = new FakeAnalise(null, new IllegalStateException("ffprobe explodiu"));
        KronosMcpTools mcp = new KronosMcpTools(fake, fila);

        String resposta = mcp.analisarMidia(dir.toString());

        assertTrue(resposta.startsWith("ERRO"), resposta);
        assertTrue(resposta.contains("ffprobe explodiu"), resposta);
    }

    private static ResultadoAnaliseLote loteVazio() {
        return new ResultadoAnaliseLote(List.of(), List.of());
    }

    /** Fake do use case (sem ffprobe/telemetria reais): conta chamadas e devolve o resultado/erro combinado. */
    private static final class FakeAnalise extends AnalisarMidiaUseCase {
        final AtomicInteger chamadas = new AtomicInteger();
        private final ResultadoAnaliseLote resultado;
        private final RuntimeException erro;

        FakeAnalise(ResultadoAnaliseLote resultado, RuntimeException erro) {
            super(null, null, null, null, null, null);
            this.resultado = resultado;
            this.erro = erro;
        }

        @Override
        public ResultadoAnaliseLote executar(Path entrada, Path saidaEfetiva) {
            chamadas.incrementAndGet();
            if (erro != null) {
                throw erro;
            }
            return resultado;
        }
    }
}
