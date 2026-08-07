package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.traducao.projeto.core.presentation.web.OperacaoRequest;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;
import org.traducao.projeto.raspagemCorrecao.presentation.web.CorrecaoRaspagemController;
import org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoCacheController;
import org.traducao.projeto.raspagemRevisao.presentation.web.RevisaoLegendasController;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducaoCorrige.presentation.web.CorrecaoCacheController;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza que a FASE C2 (mover os controllers de correção
 * e revisão para suas fatias proprietárias) preservou integralmente a validação
 * síncrona de contexto — um {@code contextoId} inexistente continua retornando
 * HTTP 400, sem enfileirar trabalho e sem iniciar processamento assíncrono.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Um {@code GerenciadorContexto} sem provedores reprova qualquer id
 *       ({@code existeContexto} → false), simulando um contexto desconhecido.</li>
 *   <li>O {@code PipelineWebSupport} é espionado: se {@code submeterJobComRelatorio}
 *       for chamado, houve enfileiramento/processamento — o que reprova o teste.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer resposta diferente de 400 ou qualquer enfileiramento reprova a suíte,
 * sinalizando regressão da validação síncrona.
 */
class ContextoInvalidoC2CaracterizacaoTest {

    private static final String CONTEXTO_INEXISTENTE = "contexto-que-nao-existe-xyz";

    /** GerenciadorContexto vazio: nenhum id é reconhecido. */
    private static GerenciadorContexto gerenciadorSemContextos() {
        return new GerenciadorContexto(List.of());
    }

    /** Espião do kernel Web: registra se algum job foi submetido à fila. */
    private static PipelineWebSupport pipelineEspiao(AtomicBoolean enfileirou) {
        return new PipelineWebSupport(null, null) {
            @Override
            public Path normalizarCaminho(String valor) {
                return Path.of("cache"); // caminho válido, para chegar à checagem de contexto
            }

            @Override
            public void submeterJobComRelatorio(String canal, String nomeOperacao, Runnable corpo) {
                enfileirou.set(true);
            }
        };
    }

    private static OperacaoRequest requisicaoComContextoInvalido() {
        return new OperacaoRequest(
            Path.of("animes", "entrada").toString(),
            null, CONTEXTO_INEXISTENTE, null, null, null, null, null);
    }

    @Test
    @DisplayName("CorrecaoCacheController./corrigir-cache: contexto inválido → 400 e NÃO enfileira")
    void corrigirCacheContextoInvalido() {
        AtomicBoolean enfileirou = new AtomicBoolean(false);
        CorrecaoCacheController controller = new CorrecaoCacheController(
            pipelineEspiao(enfileirou), null, gerenciadorSemContextos(), null);

        ResponseEntity<RespostaPadrao> resposta = controller.limparCache(requisicaoComContextoInvalido());

        assertEquals(400, resposta.getStatusCode().value(), "Contexto inválido deve retornar HTTP 400");
        assertTrue(resposta.getBody().mensagem().contains("Contexto desconhecido"),
            "A mensagem deve indicar contexto desconhecido");
        assertFalse(enfileirou.get(), "Contexto inválido NÃO pode enfileirar/processar em segundo plano");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: as duas rotas do reforço de terminologia entram na MESMA catraca. A
     * de APLICAR reescreve o acervo em lote, então deixá-la de fora desta caracterização seria
     * deixar sem rede justamente a mais destrutiva.
     */
    @Test
    @DisplayName("CorrecaoCacheController./reforcar-terminologia-*: contexto inválido → 400 e NÃO enfileira")
    void reforcarTerminologiaContextoInvalido() {
        AtomicBoolean enfileirouEnsaio = new AtomicBoolean(false);
        ResponseEntity<RespostaPadrao> ensaio = new CorrecaoCacheController(
            pipelineEspiao(enfileirouEnsaio), null, gerenciadorSemContextos(), null)
            .ensaiarReforcoTerminologia(requisicaoComContextoInvalido());

        AtomicBoolean enfileirouAplicar = new AtomicBoolean(false);
        ResponseEntity<RespostaPadrao> aplicar = new CorrecaoCacheController(
            pipelineEspiao(enfileirouAplicar), null, gerenciadorSemContextos(), null)
            .aplicarReforcoTerminologia(requisicaoComContextoInvalido());

        assertEquals(400, ensaio.getStatusCode().value());
        assertEquals(400, aplicar.getStatusCode().value());
        assertFalse(enfileirouEnsaio.get(), "Contexto inválido NÃO pode enfileirar o ensaio");
        assertFalse(enfileirouAplicar.get(),
            "Contexto inválido NÃO pode enfileirar a APLICAÇÃO — esta escreve no acervo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: a rota saiu do {@code CorrecaoCacheController} para a fatia dona na
     * FASE 3, pelo mesmo motivo que moveu as da FASE C2 — e a garantia tem de sobreviver à mudança
     * de endereço de classe, que é exatamente o que esta suíte existe para provar.
     */
    @Test
    @DisplayName("RevisaoCacheController./revisar-cache: contexto inválido → 400 e NÃO enfileira")
    void revisarCacheContextoInvalido() {
        AtomicBoolean enfileirou = new AtomicBoolean(false);
        RevisaoCacheController controller = new RevisaoCacheController(
            pipelineEspiao(enfileirou), null, gerenciadorSemContextos(), null);

        ResponseEntity<RespostaPadrao> resposta = controller.revisarCache(requisicaoComContextoInvalido());

        assertEquals(400, resposta.getStatusCode().value(), "Contexto inválido deve retornar HTTP 400");
        assertTrue(resposta.getBody().mensagem().contains("Contexto desconhecido"),
            "A mensagem deve indicar contexto desconhecido");
        assertFalse(enfileirou.get(), "Contexto inválido NÃO pode enfileirar/processar em segundo plano");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha uma LACUNA que já existia antes desta fase — a rota do scraping
     * nunca esteve nesta caracterização, embora tenha a mesma validação síncrona das irmãs e chame
     * a rede. Mover a rota sem cobri-la teria repetido o buraco no endereço novo.
     */
    @Test
    @DisplayName("CorrecaoRaspagemController./corrigir-scraping: contexto inválido → 400 e NÃO enfileira")
    void corrigirScrapingContextoInvalido() {
        AtomicBoolean enfileirou = new AtomicBoolean(false);
        CorrecaoRaspagemController controller = new CorrecaoRaspagemController(
            pipelineEspiao(enfileirou), null, gerenciadorSemContextos());

        ResponseEntity<RespostaPadrao> resposta = controller.corrigirScraping(requisicaoComContextoInvalido());

        assertEquals(400, resposta.getStatusCode().value(), "Contexto inválido deve retornar HTTP 400");
        assertTrue(resposta.getBody().mensagem().contains("Contexto desconhecido"),
            "A mensagem deve indicar contexto desconhecido");
        assertFalse(enfileirou.get(),
            "Contexto inválido NÃO pode enfileirar — esta rota chega ao Google Translate");
    }

    @Test
    @DisplayName("RevisaoLegendasController./revisar-legendas: contexto inválido → 400 e NÃO enfileira")
    void revisarLegendasContextoInvalido() {
        AtomicBoolean enfileirou = new AtomicBoolean(false);
        RevisaoLegendasController controller = new RevisaoLegendasController(
            pipelineEspiao(enfileirou), null, gerenciadorSemContextos(), null);

        ResponseEntity<RespostaPadrao> resposta = controller.revisarLegendas(requisicaoComContextoInvalido());

        assertEquals(400, resposta.getStatusCode().value(), "Contexto inválido deve retornar HTTP 400");
        assertTrue(resposta.getBody().mensagem().contains("Contexto desconhecido"),
            "A mensagem deve indicar contexto desconhecido");
        assertFalse(enfileirou.get(), "Contexto inválido NÃO pode enfileirar/processar em segundo plano");
    }
}
