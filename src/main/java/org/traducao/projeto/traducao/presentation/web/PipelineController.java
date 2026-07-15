package org.traducao.projeto.traducao.presentation.web;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.config.LlmProperties;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: expõe os endpoints de estado e controle do pipeline
 * local à interface web — heartbeat, parada cooperativa da fila, estado da fila,
 * status ao vivo do servidor LLM e a lista de contextos de tradução disponíveis.
 *
 * <p>INVARIANTES DO DOMÍNIO: compartilha a MESMA {@link FilaExecucaoPipeline}
 * (bean CDI) dos demais controllers; a parada é cooperativa e preserva o
 * progresso já salvo; nenhuma URL, código HTTP ou nome de campo de DTO é alterado
 * em relação ao controller monolítico original.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: a consulta de status do LLM nunca propaga
 * exceção — falhas viram uma resposta {@code online=false} com a mensagem do
 * erro; os demais endpoints são consultas simples sem caminho de falha explícito.
 */
@RestController
@RequestMapping("/api")
public class PipelineController {

    private static final Logger log = LoggerFactory.getLogger(PipelineController.class);

    // Fila única compartilhada por todos os módulos (ver FilaExecucaoPipeline):
    // garante execução sequencial em segundo plano e impede que outro endpoint
    // troque o contexto/modelo LLM no meio de um job em andamento.
    private final FilaExecucaoPipeline filaExecucao;
    private final MistralPort mistralPort;
    private final GerenciadorContexto gerenciadorContexto;
    private final LlmProperties llmProperties;

    public PipelineController(
            FilaExecucaoPipeline filaExecucao,
            MistralPort mistralPort,
            GerenciadorContexto gerenciadorContexto,
            LlmProperties llmProperties) {
        this.filaExecucao = filaExecucao;
        this.mistralPort = mistralPort;
        this.gerenciadorContexto = gerenciadorContexto;
        this.llmProperties = llmProperties;
    }

    /**
     * Endpoint para consulta de status geral (heartbeat)
     */
    @GetMapping("/status")
    public ResponseEntity<RespostaPadrao> status() {
        return ResponseEntity.ok(new RespostaPadrao("online"));
    }

    /**
     * Para o trabalho em execução na fila do pipeline e descarta os
     * enfileirados. A parada é cooperativa: o job interrompido encerra no
     * próximo ponto seguro (entre falas/arquivos), preservando o progresso já
     * salvo — cache de tradução e arquivos concluídos não se perdem.
     */
    @PostMapping("/pipeline/parar")
    public ResponseEntity<RespostaPadrao> pararPipeline() {
        if (!filaExecucao.ocupada()) {
            return ResponseEntity.ok(new RespostaPadrao("Nenhum trabalho em execução no pipeline."));
        }
        int canceladas = filaExecucao.parar();
        log.info("Pipeline interrompido pelo usuário ({} tarefa(s) cancelada(s)).", canceladas);
        System.out.println(AnsiCores.YELLOW
            + "[STOP] Interrupção solicitada pelo usuário — o trabalho atual encerra no próximo ponto seguro."
            + AnsiCores.RESET);
        return ResponseEntity.ok(new RespostaPadrao(
            "Parada solicitada (" + canceladas + " tarefa(s) cancelada(s)). "
                + "O trabalho atual encerra no próximo ponto seguro, preservando o progresso já salvo."));
    }

    /**
     * Estado da fila do pipeline — usado pela UI no modal do "Sair" para
     * avisar quando ainda há trabalho em execução.
     */
    @GetMapping("/pipeline/status")
    public ResponseEntity<RespostaPadrao> statusPipeline() {
        return ResponseEntity.ok(new RespostaPadrao(filaExecucao.ocupada() ? "ocupada" : "livre"));
    }

    /**
     * Status ao vivo do servidor LLM local (LM Studio) para o card do painel
     * inicial: informa se está online, se há modelo carregado em memória e qual
     * é ({@link MistralPort#verificarDisponibilidade()} já adota o modelo ativo
     * em {@link LlmProperties#model()} quando o detecta).
     */
    @GetMapping("/llm/status")
    public ResponseEntity<LlmStatusResponse> statusLlm() {
        try {
            StatusLlm status = mistralPort.verificarDisponibilidade();
            String modelo = status.modeloCarregado() ? llmProperties.model() : null;
            return ResponseEntity.ok(new LlmStatusResponse(
                status.servidorOnline(), status.modeloCarregado(), modelo, status.mensagem()));
        } catch (Exception e) {
            log.warn("Falha ao consultar o status do LLM: {}", e.getMessage());
            return ResponseEntity.ok(new LlmStatusResponse(false, false, null,
                "Falha ao consultar o servidor LLM: " + e.getMessage()));
        }
    }

    /**
     * Lista os contextos de tradução disponíveis (animes).
     */
    @GetMapping("/contextos")
    public ResponseEntity<List<ContextoResponse>> listarContextos() {
        String idPadrao = gerenciadorContexto.getIdContextoPadrao();
        List<ContextoResponse> lista = gerenciadorContexto.getProvedores().stream()
                .map(p -> new ContextoResponse(p.getId(), p.getNomeExibicao(), p.getId().equals(idPadrao)))
                .toList();
        return ResponseEntity.ok(lista);
    }
}
