package org.traducao.projeto.traducao.presentation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a análise de mídia (Opção 1) à interface web,
 * enfileirando o processamento pesado em segundo plano e publicando o relatório
 * estruturado no canal SSE {@code analise-relatorio} para renderização no
 * navegador.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
 * {@link PipelineWebSupport}; caminhos são normalizados antes do uso; nenhuma
 * URL, código HTTP, nome de campo de DTO ou canal SSE é alterado em relação ao
 * controller monolítico original.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco retorna HTTP 400; falhas
 * do job de background são registradas no log e no console SSE, sem derrubar a
 * fila.
 */
@RestController
@RequestMapping("/api")
public class AnaliseMidiaController {

    private static final Logger log = LoggerFactory.getLogger(AnaliseMidiaController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final AnalisarMidiaUseCase analisarMidiaUseCase;
    private final LogStreamService logStreamService;
    private final ObjectMapper objectMapper;

    public AnaliseMidiaController(
            PipelineWebSupport pipelineWebSupport,
            AnalisarMidiaUseCase analisarMidiaUseCase,
            LogStreamService logStreamService,
            ObjectMapper objectMapper) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.analisarMidiaUseCase = analisarMidiaUseCase;
        this.logStreamService = logStreamService;
        this.objectMapper = objectMapper;
    }

    /**
     * 1. ANÁLISE DE MÍDIA
     */
    @PostMapping("/analisar")
    public ResponseEntity<RespostaPadrao> analisar(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de entrada obrigatório."));
        }

        pipelineWebSupport.submeterJobComRelatorio("analise", "Análise de Mídia", () -> {
            try {
                Path pathEntrada = pipelineWebSupport.normalizarCaminho(req.entrada());
                if (pathEntrada == null) {
                    System.out.println("\u001B[31m[FAIL] Caminho de entrada inválido: " + req.entrada() + "\u001B[0m");
                    log.error("Caminho de entrada inválido informado para análise: {}", req.entrada());
                    return;
                }
                Path pathSaida = pipelineWebSupport.normalizarCaminho(req.saida());
                ResultadoAnaliseLote resultadoLote = analisarMidiaUseCase.executar(pathEntrada, pathSaida);
                publicarResultadoAnalise(resultadoLote);
                System.out.println("\n\u001B[32m========================================================================\u001B[0m");
                System.out.println("\u001B[32m  🎉 [SUCESSO] ANÁLISE DE MÍDIA FINALIZADA COM SUCESSO!\u001B[0m");
                System.out.println("\u001B[32m========================================================================\n\u001B[0m");
                log.info("[SUCESSO] Análise de mídia finalizada.");
            } catch (Exception e) {
                log.error("Erro na análise de mídia em background", e);
                System.out.println("\u001B[31m[ERRO] Falha na análise: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Análise de mídia iniciada no servidor."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: publica o resultado ESTRUTURADO (JSON) da análise no
     * canal SSE {@code analise-relatorio}; o navegador renderiza cartões/tabelas
     * a partir dele. A análise não grava mais relatório em disco — a exportação
     * TXT é manual.
     *
     * <p>INVARIANTES DO DOMÍNIO: publica exatamente no canal
     * {@code analise-relatorio}; o payload é o JSON serializado do lote.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de serialização é registrado no log
     * e não propaga exceção para o job de background.
     */
    private void publicarResultadoAnalise(ResultadoAnaliseLote lote) {
        try {
            String json = objectMapper.writeValueAsString(lote);
            logStreamService.publicarLog("analise-relatorio", json);
        } catch (Exception e) {
            log.error("Erro ao serializar o resultado da análise para o navegador: {}", e.getMessage());
        }
    }
}
