package org.traducao.projeto.traducao.presentation.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.io.GuardaCaminhoEntrada;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.presentation.web.OperacaoRequest;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.Lote;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.TraducaoLote;
import org.traducao.projeto.traducao.infrastructure.AvisoSonoroSistema;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que a Tradução Local anuncie o FIM do lote por um canal
 * próprio em TODOS os desfechos — inclusive nos que morrem em dois segundos.
 *
 * <h2>O prejuízo que originou</h2>
 * Pedido de Paulo em 2026-08-14: o lote leva de meia hora a duas, e quem dispara vai fazer
 * outra coisa enquanto espera. O risco não é o lote demorar; é ele TERMINAR sem ninguém
 * ficar sabendo. E o pior desfecho não é a falha no meio — é a saída antecipada: com o LM
 * Studio fora do ar o job devolve o console em dois segundos, e quem saiu de perto continua
 * esperando por horas um trabalho que nunca começou.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O aviso sai pelo canal {@code <canal>-finalizada}, nunca por casamento de texto no
 *       banner do relatório: rótulo é apresentação, e trocar "CONCLUÍDO" por outra palavra
 *       emudeceria o aviso sem quebrar nada visível.</li>
 *   <li>Terminar bem e terminar mal avisam IGUAL — muda só o desfecho no corpo do evento.
 *       Se apenas o sucesso avisasse, o fracasso seria indistinguível de "ainda rodando",
 *       que é precisamente o silêncio que este mecanismo existe para acabar.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Falha se nenhum evento chegar ao canal em 10s ou se o canal/desfecho divergir. Não afirma
 * nada sobre SOM: som é decisão do navegador e vive em {@code traducao.js}; aqui se prova o
 * sinal que o navegador precisa receber para poder tocá-lo.
 */
class AvisoFimDeLoteTest {

    /** Captura o par (canal, mensagem), que é o contrato real com o frontend. */
    private static final class LogStreamEspiao extends LogStreamService {
        private final List<String[]> publicacoes = new ArrayList<>();
        private final CountDownLatch chegouOFim = new CountDownLatch(1);

        @Override
        public void publicarLog(String canal, String mensagem) {
            synchronized (publicacoes) {
                publicacoes.add(new String[] { canal, mensagem });
            }
            if (canal.endsWith(TraducaoController.SUFIXO_CANAL_FIM)) {
                chegouOFim.countDown();
            }
        }

        String[] eventoDeFim() {
            synchronized (publicacoes) {
                return publicacoes.stream()
                    .filter(p -> p[0].endsWith(TraducaoController.SUFIXO_CANAL_FIM))
                    .findFirst()
                    .orElse(null);
            }
        }
    }

    /** LLM fora do ar: o desfecho mais perigoso, porque devolve o console quase na hora. */
    private static final class LlmForaDoAr implements LlmPort {
        @Override
        public TraducaoLote traduzir(Lote lote) {
            throw new UnsupportedOperationException("nao deve ser alcancado com o LLM offline");
        }

        @Override
        public StatusLlm verificarDisponibilidade() {
            return new StatusLlm(false, false, "LM Studio nao respondeu (caso-controle do teste)");
        }

        @Override
        public java.util.Optional<String> revisarConcordancia(String original, String traduzido, List<String> termos) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Optional<String> corrigirTraducao(String original, String traduzido, String problema) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Dublê mudo: a suíte não pode disparar PowerShell nem fazer barulho na máquina de quem
     * roda os testes. Responde INDISPONIVEL, que é justamente o caso em que o contrato manda
     * o navegador assumir o aviso — o mais interessante de congelar.
     */
    private static final class AvisoSonoroMudo extends AvisoSonoroSistema {
        @Override
        public Resultado tocar(int toques) {
            return Resultado.INDISPONIVEL;
        }
    }

    private static final class ContextoQualquerExiste extends GerenciadorContexto {
        ContextoQualquerExiste() {
            super(List.of());
        }

        @Override
        public boolean existeContexto(String id) {
            return true;
        }
    }

    @Test
    void loteAbortadoPorLlmOfflineTambemAnunciaOFim(@TempDir Path entrada) throws Exception {
        LogStreamEspiao espiao = new LogStreamEspiao();
        TraducaoController controller = new TraducaoController(
            new PipelineWebSupport(new FilaExecucaoPipeline(), espiao),
            null,
            new LlmForaDoAr(),
            new ContextoQualquerExiste(),
            null,
            null,
            null,
            new GuardaCaminhoEntrada(),
            espiao,
            new AvisoSonoroMudo());

        controller.traduzir(new OperacaoRequest(
            entrada.toString(), null, "gundam_unicorn", null, false, null, null, false));

        assertTrue(espiao.chegouOFim.await(10, TimeUnit.SECONDS),
            "o lote morreu no LLM offline e NAO anunciou o fim: quem esperava ficaria esperando para sempre");

        String[] evento = espiao.eventoDeFim();
        assertEquals("traducao" + TraducaoController.SUFIXO_CANAL_FIM, evento[0],
            "canal errado: o frontend escuta o canal da tela, e nome diferente e' silencio");
        // O corpo carrega DUAS informacoes: o desfecho e o veredito do som. Sem a segunda, o
        // frontend nao teria como saber se deve assumir o aviso — e tocaria por cima da
        // maquina no caso normal, ou ficaria calado justamente quando ela nao conseguiu.
        assertEquals("ENCERRADO SEM RELATÓRIO"
                + TraducaoController.SEPARADOR_EVENTO
                + AvisoSonoroSistema.Resultado.INDISPONIVEL,
            evento[1],
            "o corpo do evento tem de trazer o desfecho E o veredito do som da maquina");
    }
}
