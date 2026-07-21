package org.traducao.projeto.trocaTipoLegenda.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.trocaTipoLegenda.application.AchatarEstilosUseCase;
import org.traducao.projeto.trocaTipoLegenda.application.TrocaTipoLegendaUseCase;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoGeralAuditoria;
import org.traducao.projeto.trocaTipoLegenda.domain.ResultadoTrocaFonte;
import org.traducao.projeto.trocaTipoLegenda.domain.exceptions.TrocaTipoLegendaException;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TrocaTipoLegendaController {

    private final FilaExecucaoPipeline filaExecucao;
    private final TrocaTipoLegendaUseCase useCase;
    private final AchatarEstilosUseCase achatarUseCase;
    private final LogStreamService logStreamService;

    public TrocaTipoLegendaController(
        FilaExecucaoPipeline filaExecucao,
        TrocaTipoLegendaUseCase useCase,
        AchatarEstilosUseCase achatarUseCase,
        LogStreamService logStreamService
    ) {
        this.filaExecucao = filaExecucao;
        this.useCase = useCase;
        this.achatarUseCase = achatarUseCase;
        this.logStreamService = logStreamService;
    }

    public record TrocaLegendaRequest(String diretorioLegendas) {}

    @PostMapping("/troca-legenda/escanear")
    public ResponseEntity<?> escanearFontes(@RequestBody TrocaLegendaRequest req) {
        if (req.diretorioLegendas() == null || req.diretorioLegendas().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com as legendas originais/traduzidas não informada."));
        }

        Path diretorio = Path.of(req.diretorioLegendas().trim());

        try {
            // Executamos de forma síncrona na thread pool do pipeline para garantir
            // que nenhuma outra operação pesada esteja rodando em paralelo e para
            // retornar o resultado de escaneamento imediatamente.
            ResultadoGeralAuditoria resultado = filaExecucao.executarEAguardar(() ->
                useCase.escanear(diretorio)
            );
            return ResponseEntity.ok(resultado);
        } catch (TrocaTipoLegendaException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "erro", "Erro interno ao escanear fontes: " + e.getMessage()));
        }
    }

    @PostMapping("/troca-legenda/aplicar")
    public ResponseEntity<Map<String, Object>> aplicarTrocaFontes(@RequestBody TrocaLegendaRequest req) {
        if (req.diretorioLegendas() == null || req.diretorioLegendas().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com as legendas originais/traduzidas não informada."));
        }

        Path diretorio = Path.of(req.diretorioLegendas().trim());

        // Submete à fila única em segundo plano (assíncrono) para gravação física
        // e backups, logando em tempo real no console do SSE.
        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("troca-tipo-legenda");
            long inicioMs = System.currentTimeMillis();
            try {
                ResultadoTrocaFonte resultado = useCase.aplicar(diretorio);
                System.out.println("\n[32m========================================================================[0m");
                System.out.println("[32m  [SUCESSO] TROCA DE FONTES CONCLUÍDA COM SUCESSO![0m");
                System.out.println("[32m========================================================================[0m");
            } catch (Exception e) {
                System.out.println("\n[31m========================================================================[0m");
                System.out.println("[31m  [FAIL] ERRO FATAL AO APLICAR TROCA DE FONTES[0m");
                System.out.println("[31m  • Erro: " + e.getMessage() + "[0m");
                System.out.println("[31m========================================================================\n[0m");
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Troca Tipo Legenda (fontes)", inicioMs));
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Processo de substituição de fontes iniciado em segundo plano."
        ));
    }

    @PostMapping("/troca-legenda/achatar-estilos")
    public ResponseEntity<Map<String, Object>> achatarEstilosDecorativos(@RequestBody TrocaLegendaRequest req) {
        if (req.diretorioLegendas() == null || req.diretorioLegendas().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com as legendas não informada."));
        }

        Path diretorio = Path.of(req.diretorioLegendas().trim());

        // Mesma fila única/segundo plano da troca de fontes: grava in-place com
        // backup e loga no console SSE do canal troca-tipo-legenda em tempo real.
        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("troca-tipo-legenda");
            long inicioMs = System.currentTimeMillis();
            try {
                achatarUseCase.aplicar(diretorio);
            } catch (Exception e) {
                System.out.println("\n[31m========================================================================[0m");
                System.out.println("[31m  [FAIL] ERRO FATAL AO ACHATAR ESTILOS DECORATIVOS[0m");
                System.out.println("[31m  • Erro: " + e.getMessage() + "[0m");
                System.out.println("[31m========================================================================\n[0m");
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Achatar Estilos (in-place)", inicioMs));
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Achatamento de estilos decorativos iniciado em segundo plano."
        ));
    }
}
