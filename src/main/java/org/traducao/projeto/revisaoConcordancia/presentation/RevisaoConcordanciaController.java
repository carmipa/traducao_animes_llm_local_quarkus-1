package org.traducao.projeto.revisaoConcordancia.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.revisaoConcordancia.application.RevisarConcordanciaUseCase;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;

import java.nio.file.Path;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a Revisão de Concordância à interface local — corrige gênero
 * inequívoco numa pasta de legendas PT-BR, sem inglês, sem cache e sem LLM. Enfileira o trabalho
 * na fila única do pipeline e reporta o desfecho real no console.
 * <p>INVARIANTES DO DOMÍNIO: só a pasta PT-BR é obrigatória; usa a MESMA fila do pipeline para não
 * rodar em paralelo com tradução/revisão.
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna HTTP 400; falha assíncrona vira
 * banner vermelho no console.
 */
@RestController
@RequestMapping("/api")
public class RevisaoConcordanciaController {

    private static final String LINHA = "========================================================================";

    private final FilaExecucaoPipeline filaExecucao;
    private final RevisarConcordanciaUseCase revisarConcordanciaUseCase;
    private final LogStreamService logStreamService;

    public RevisaoConcordanciaController(
        FilaExecucaoPipeline filaExecucao,
        RevisarConcordanciaUseCase revisarConcordanciaUseCase,
        LogStreamService logStreamService
    ) {
        this.filaExecucao = filaExecucao;
        this.revisarConcordanciaUseCase = revisarConcordanciaUseCase;
        this.logStreamService = logStreamService;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: solicitação da Revisão de Concordância — a pasta PT-BR e se deve
     * gravar ({@code aplicar}) ou só simular.
     * <p>INVARIANTES DO DOMÍNIO: {@code aplicar=false} é dry-run.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; validação é do endpoint.
     */
    public record RevisaoConcordanciaRequest(String diretorioTraduzido, boolean aplicar) {}

    /**
     * PROPÓSITO DE NEGÓCIO: valida e enfileira a revisão de concordância de uma pasta PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: a pasta PT-BR é obrigatória; usa a fila única do pipeline.
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação retorna HTTP 400; exceção da tarefa vira banner
     * FALHOU no console.
     */
    @PostMapping("/revisar-concordancia")
    public ResponseEntity<Map<String, Object>> iniciarRevisaoConcordancia(@RequestBody RevisaoConcordanciaRequest req) {
        if (req.diretorioTraduzido() == null || req.diretorioTraduzido().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas traduzidas em portugues nao informada."));
        }

        Path pastaTraduzida = Path.of(req.diretorioTraduzido().trim());
        boolean aplicar = req.aplicar();

        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("revisao-concordancia");
            long inicioMs = System.currentTimeMillis();
            try {
                ResultadoConcordancia resultado =
                    revisarConcordanciaUseCase.revisarPasta(pastaTraduzida, aplicar);
                imprimirBanner(resultado);
            } catch (Exception e) {
                imprimirFalha("Falha inesperada: " + e.getMessage());
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Revisão de Concordância", inicioMs));
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Revisao de concordancia iniciada no servidor. Acompanhe os logs em tempo real."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: banner de fechamento deixando claro dry-run vs aplicado e as contagens.
     * <p>INVARIANTES DO DOMÍNIO: sempre imprime arquivos/falas e o modo.
     * <p>COMPORTAMENTO EM CASO DE FALHA: só escreve em {@code System.out}; não lança.
     */
    private void imprimirBanner(ResultadoConcordancia r) {
        String cor = AnsiCores.GREEN;
        String modo = r.aplicado() ? "APLICADO" : "SIMULADO (dry-run, nada gravado)";
        System.out.println("\n" + cor + LINHA + AnsiCores.RESET);
        System.out.println(cor + "  [" + modo + "] REVISAO DE CONCORDANCIA (genero PT-BR)" + AnsiCores.RESET);
        System.out.println(cor + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos analisados  : " + r.arquivosAnalisados() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos alterados   : " + r.arquivosAlterados() + AnsiCores.RESET);
        System.out.println(AnsiCores.GREEN + "  • Falas corrigidas     : " + r.falasCorrigidas() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Backups              : " + r.backups().size() + AnsiCores.RESET);
        System.out.println(cor + LINHA + "\n" + AnsiCores.RESET);
    }

    private void imprimirFalha(String mensagem) {
        System.out.println("\n" + AnsiCores.RED + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  [FALHOU] REVISAO DE CONCORDANCIA" + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  " + mensagem + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + LINHA + "\n" + AnsiCores.RESET);
    }
}
