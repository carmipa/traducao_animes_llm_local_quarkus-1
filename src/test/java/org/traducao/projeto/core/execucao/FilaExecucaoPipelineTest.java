package org.traducao.projeto.core.execucao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobre o contrato de execução da fila única do pipeline: submissão, execução
 * síncrona com propagação de exceção, sinal de ocupação e cancelamento. É a
 * invariante que garante que UI, MCP e CLI compartilhem a MESMA política de
 * execução sequencial (um job pesado por vez).
 */
class FilaExecucaoPipelineTest {

    private FilaExecucaoPipeline fila;

    @BeforeEach
    void iniciar() {
        fila = new FilaExecucaoPipeline();
    }

    @AfterEach
    void encerrar() {
        fila.encerrar();
    }

    @Test
    void submeterExecutaATarefa() throws Exception {
        AtomicBoolean executou = new AtomicBoolean(false);

        Future<?> future = fila.submeter(() -> executou.set(true));
        future.get(5, TimeUnit.SECONDS);

        assertTrue(executou.get());
    }

    @Test
    void executarEAguardarRetornaOResultado() throws Exception {
        String resultado = fila.executarEAguardar(() -> "ok");

        assertEquals("ok", resultado);
    }

    @Test
    void executarEAguardarPropagaAExcecaoDaTarefa() {
        IllegalStateException erro = assertThrows(
            IllegalStateException.class,
            () -> fila.executarEAguardar(() -> {
                throw new IllegalStateException("falha na tarefa");
            }));

        assertEquals("falha na tarefa", erro.getMessage());
    }

    @Test
    void ocupadaRefleteTarefaEmExecucao() throws Exception {
        CountDownLatch comecou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);

        assertFalse(fila.ocupada(), "fila recém-criada não deve estar ocupada");

        Future<?> future = fila.submeter(() -> {
            comecou.countDown();
            aguardar(liberar);
        });

        assertTrue(comecou.await(5, TimeUnit.SECONDS));
        assertTrue(fila.ocupada(), "fila deve estar ocupada com a tarefa em execução");

        liberar.countDown();
        future.get(5, TimeUnit.SECONDS);

        assertFalse(fila.ocupada(), "fila deve ficar livre após a tarefa concluir");
    }

    @Test
    void pararCancelaTarefaEmExecucaoERetornaContagem() throws Exception {
        CountDownLatch comecou = new CountDownLatch(1);

        fila.submeter(() -> {
            comecou.countDown();
            aguardar(new CountDownLatch(1)); // nunca liberado: só sai por interrupção
        });
        assertTrue(comecou.await(5, TimeUnit.SECONDS));

        int canceladas = fila.parar();

        assertTrue(canceladas >= 1, "parar() deve cancelar a tarefa em execução");
    }

    private static void aguardar(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
