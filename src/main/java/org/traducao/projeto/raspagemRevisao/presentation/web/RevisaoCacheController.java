package org.traducao.projeto.raspagemRevisao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.OperacaoRequest;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe à interface web a revisão gramatical do CACHE via LLM local.
 *
 * <p>A rota {@code /api/revisar-cache} morava no {@code CorrecaoCacheController} de
 * {@code traducaoCorrige}, e era metade da perna de volta do ciclo
 * {@code raspagemRevisao ⇄ traducaoCorrige}: um controller de uma fatia injetando o caso de uso de
 * outra.
 *
 * <h2>Por que não entrou no RevisaoLegendasController</h2>
 * A fatia já tinha controller, mas ele trata de arquivos {@code .ass}: valida pasta de legendas em
 * português, modo de referência, cache auxiliar, e publica no canal {@code "revisao"}. Esta rota
 * trata de arquivos {@code .cache.json}, valida só cache e contexto, e publica no canal
 * {@code "correcao"}. São operações distintas sobre artefatos distintos que apenas moram na mesma
 * fatia. "Um controller por fatia" NÃO é regra do contrato — a fatia gold tem quatro; a regra é
 * "controller não importa {@code application} de outra fatia", e era essa que estava furada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>URL, método, corpo do DTO e código HTTP IDÊNTICOS aos de antes. O prefixo {@code /api} é o
 *       mesmo em todos os controllers, então trocar a classe que hospeda a rota não muda o
 *       endereço.</li>
 *   <li>O canal de job continua {@code "correcao"}, NÃO {@code "revisao"}. O canal é o console SSE
 *       que o painel de Correção escuta — não é o nome da fatia. Alinhá-lo ao controller vizinho
 *       "por coerência" mandaria o job para o console errado sem quebrar nenhum HTTP: o operador
 *       clicaria no painel de Correção e veria silêncio.</li>
 *   <li>O portão de disponibilidade do LLM foi preservado e roda DENTRO do job, não antes de
 *       enfileirar — é o único ponto que impede a revisão de varrer o acervo com o modelo
 *       descarregado. A rota irmã de raspagem não tem esse portão, e não deve ganhar um: ela não
 *       usa LLM.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Caminho ou contexto inválido devolve HTTP 400 antes de enfileirar; LLM indisponível e falha do
 * job aparecem no console SSE sem derrubar a fila.
 */
@RestController
@RequestMapping("/api")
public class RevisaoCacheController {

    private static final Logger log = LoggerFactory.getLogger(RevisaoCacheController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final RevisarCacheUseCase revisarCacheUseCase;
    private final GerenciadorContexto gerenciadorContexto;
    private final LlmPort llmPort;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe a rota com a fila, o caso de uso, os contextos e o LLM.
     * <p>INVARIANTES DO DOMÍNIO: só depende do {@code application} da PRÓPRIA fatia, do
     * {@code core} e de peers.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a subida do contexto web.
     */
    public RevisaoCacheController(
            PipelineWebSupport pipelineWebSupport,
            RevisarCacheUseCase revisarCacheUseCase,
            GerenciadorContexto gerenciadorContexto,
            LlmPort llmPort) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.revisarCacheUseCase = revisarCacheUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
        this.llmPort = llmPort;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a revisão de concordância do cache via LLM local.
     * <p>INVARIANTES DO DOMÍNIO: contexto é validado e disponibilidade do modelo é checada antes da revisão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: rejeita contexto inválido e registra indisponibilidade/status parcial no console.
     */
    @PostMapping("/revisar-cache")
    public ResponseEntity<RespostaPadrao> revisarCache(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = pipelineWebSupport.normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }

        if (req.contextoId() != null && !req.contextoId().isBlank() && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        pipelineWebSupport.submeterJobComRelatorio("correcao", "Revisão Gramatical do Cache (LLM)", () -> {
            try {
                StatusLlm status = llmPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println(AnsiCores.RED + "[FAIL] LLM indisponível para revisão: "
                        + status.mensagem() + AnsiCores.RESET);
                    return;
                }
                ResultadoManutencaoCache resultado = revisarCacheUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("REVISÃO GRAMATICAL DO CACHE", resultado);
            } catch (Exception e) {
                log.error("Erro na revisão gramatical do cache", e);
                System.out.println(AnsiCores.RED + "[ERRO] Revisão gramatical falhou: "
                    + e.getMessage() + AnsiCores.RESET);
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Revisão local aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apresenta no console web o desfecho real da operação, incluindo falhas
     * e cancelamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente {@code CONCLUIDO} usa banner verde; qualquer outro status
     * informa que o resultado exige atenção; a orientação de avançar à Opção 6 aparece após toda
     * execução que altera o cache — sem ela o operador não sabe que precisa sincronizar o ASS.
     *
     * <p>É CÓPIA CONSCIENTE do helper homônimo das outras duas fatias que imprimem este mesmo
     * resumo. A alternativa era pôr o formatador em {@code core}, e ele consome um tipo de domínio
     * de {@code traducaoCorrige} — o kernel passaria a conhecer uma fatia, que o contrato proíbe.
     * Vinte linhas de apresentação duplicadas custam menos que essa inversão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado nulo é tratado como falha e não provoca
     * {@link NullPointerException} no job de background.
     */
    private void imprimirResultadoCache(String operacao, ResultadoManutencaoCache resultado) {
        if (resultado == null) {
            System.out.println(AnsiCores.RED + "[FALHA] " + operacao + " não retornou resultado." + AnsiCores.RESET);
            return;
        }
        String resumo = operacao + " — status=" + resultado.status()
            + ", arquivos=" + resultado.arquivosAnalisados()
            + ", alterados=" + resultado.arquivosAlterados()
            + ", corrigidos=" + resultado.itensCorrigidos()
            + ", pendentes=" + resultado.itensPendentes()
            + ", falhas=" + resultado.falhas();
        if ("CONCLUIDO".equals(resultado.status())) {
            System.out.println(AnsiCores.GREEN + "[SUCESSO] " + resumo + AnsiCores.RESET);
        } else {
            System.out.println(AnsiCores.YELLOW + "[ATENÇÃO] " + resumo + AnsiCores.RESET);
        }
        if (resultado.arquivosAlterados() > 0) {
            System.out.println(AnsiCores.CYAN
                + "[PRÓXIMO PASSO] Avance para a Opção 6. Ela sincronizará este cache mais novo no ASS antes da revisão."
                + AnsiCores.RESET);
        }
    }
}
