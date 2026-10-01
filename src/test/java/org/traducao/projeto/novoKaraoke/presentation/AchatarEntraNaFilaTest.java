package org.traducao.projeto.novoKaraoke.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.io.GuardaCaminhoEntrada;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.novoKaraoke.application.ConversorKaraokeUseCase;
import org.traducao.projeto.novoKaraoke.domain.ResultadoConversaoKaraoke;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO (F2, 24/09/2026): a GRAVAÇÃO do Passo 2 entra na fila do pipeline, a mesma
 * do Passo 1. Fora dela, a tela única dizia "Passada concluída" no mesmo segundo do clique — ela
 * espera a fila esvaziar, e a fila estava vazia — enquanto o achatamento só rodava 8 s depois; e
 * nada impedia achatar a tradução ainda sendo escrita.
 *
 * <p>A1 — a SIMULAÇÃO (read-only) continua fora da fila, como a do Passo 1: não pode ficar presa
 * atrás de uma tradução de horas.
 */
class AchatarEntraNaFilaTest {

    /** Fila que só CONTA o que recebeu, sem executar — prova o roteamento, não o trabalho. */
    private static final class FilaQueConta extends FilaExecucaoPipeline {
        final List<Runnable> recebidas = new ArrayList<>();

        @Override
        public Future<?> submeter(Runnable tarefa) {
            recebidas.add(tarefa);
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ConversorQueConta extends ConversorKaraokeUseCase {
        int aplicacoes;
        int simulacoes;

        @Override
        public List<ResultadoConversaoKaraoke> aplicar(Path origem, Path destino) {
            aplicacoes++;
            return List.of();
        }

        @Override
        public List<ResultadoConversaoKaraoke> simular(Path origem, Path destino) {
            simulacoes++;
            return List.of();
        }
    }

    private static NovoKaraokeController controller(FilaQueConta fila, ConversorQueConta conversor) {
        NovoKaraokeController c = new NovoKaraokeController();
        c.filaExecucao = fila;
        c.conversor = conversor;
        c.guardaCaminho = new GuardaCaminhoEntrada();
        c.logStream = new LogStreamService() {
            @Override
            public void publicarLog(String canal, String mensagem) {
            }
        };
        return c;
    }

    @Test
    @DisplayName("aplicar (grava) entra na fila do pipeline e so roda quando a fila chega nele")
    void aplicarEntraNaFila(@TempDir Path pasta) throws Exception {
        Path origem = Files.createDirectories(pasta.resolve("traducao_ptbr-karaoke-ptbr"));
        FilaQueConta fila = new FilaQueConta();
        ConversorQueConta conversor = new ConversorQueConta();

        var resposta = controller(fila, conversor).aplicar(new NovoKaraokeRequest(origem.toString(), ""));

        assertEquals(200, resposta.getStatus());
        assertEquals(1, fila.recebidas.size(), "a gravacao tinha de ser ENFILEIRADA");
        assertEquals(0, conversor.aplicacoes, "nada pode rodar fora da fila, antes da vez dela");
        fila.recebidas.getFirst().run();
        assertEquals(1, conversor.aplicacoes, "a tarefa enfileirada e a que grava");
    }

    @Test
    @DisplayName("A1: simular (read-only) NAO entra na fila")
    void simularNaoEntraNaFila(@TempDir Path pasta) throws Exception {
        Path origem = Files.createDirectories(pasta.resolve("traducao_ptbr-karaoke-ptbr"));
        FilaQueConta fila = new FilaQueConta();
        ConversorQueConta conversor = new ConversorQueConta();

        controller(fila, conversor).simular(new NovoKaraokeRequest(origem.toString(), ""));

        assertTrue(fila.recebidas.isEmpty(), "simulacao read-only nao pode esperar atras de uma traducao de horas");
    }
}
