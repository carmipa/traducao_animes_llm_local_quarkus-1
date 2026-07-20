package org.traducao.projeto.revisaoLore.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.revisaoLore.application.RevisarLoreUseCase;
import org.traducao.projeto.revisaoLore.application.RevisarLorePtOnlyUseCase;
import org.traducao.projeto.revisaoLore.domain.ResultadoRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.StatusRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.exceptions.RevisaoLoreException;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.LogStreamService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: expõe a Revisão de Lore à interface local, enfileira o
 * trabalho com segurança e apresenta o desfecho real no console.
 * <p>INVARIANTES DO DOMÍNIO: uma revisão sempre usa contexto conhecido e a fila
 * única do pipeline; o banner reflete o status retornado pelo caso de uso.
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna HTTP 400; falha
 * assíncrona é registrada com banner vermelho e preserva a fila.
 */
@RestController
@RequestMapping("/api")
public class RevisaoLoreController {

    private static final String LINHA = "========================================================================";

    // Fila única compartilhada do pipeline: impede que a revisão de lore rode
    // em paralelo com uma tradução/correção e troque o contexto LLM global no
    // meio do outro job (ver FilaExecucaoPipeline).
    private final FilaExecucaoPipeline filaExecucao;
    private final RevisarLoreUseCase revisarLoreUseCase;
    private final RevisarLorePtOnlyUseCase revisarLorePtOnlyUseCase;
    private final GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore;
    private final LogStreamService logStreamService;

    /**
     * PROPÓSITO DE NEGÓCIO: conecta fila, revisão, catálogo de contextos e
     * canal de logs da opção 7.
     * <p>INVARIANTES DO DOMÍNIO: dependências são obrigatórias e imutáveis.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência de dependência impede a
     * inicialização do controller.
     */
    public RevisaoLoreController(
        FilaExecucaoPipeline filaExecucao,
        RevisarLoreUseCase revisarLoreUseCase,
        RevisarLorePtOnlyUseCase revisarLorePtOnlyUseCase,
        GerenciadorPromptRevisaoLore gerenciadorPromptRevisaoLore,
        LogStreamService logStreamService
    ) {
        this.filaExecucao = filaExecucao;
        this.revisarLoreUseCase = revisarLoreUseCase;
        this.revisarLorePtOnlyUseCase = revisarLorePtOnlyUseCase;
        this.gerenciadorPromptRevisaoLore = gerenciadorPromptRevisaoLore;
        this.logStreamService = logStreamService;
    }

    public record RevisaoLoreRequest(
        String diretorioOriginal,
        String diretorioTraduzido,
        String contextoId,
        boolean revisarTodasFalas
    ) {}

    public record RevisaoLoreContextoResponse(String id, String nome, String termoMetadata) {}

    /**
     * PROPÓSITO DE NEGÓCIO: solicitação da aba "Revisão de Lore PT-only" — revisa a lore
     * usando SÓ a pasta PT-BR, sem a pasta de originais em inglês.
     * <p>INVARIANTES DO DOMÍNIO: {@code aplicar=false} é dry-run; {@code usarLlm} liga a camada LLM.
     * <p>COMPORTAMENTO EM CASO DE FALHA: portador de dados puro; validação é do endpoint.
     */
    public record RevisaoLorePtOnlyRequest(
        String diretorioTraduzido,
        String contextoId,
        boolean usarLlm,
        boolean aplicar
    ) {}

    @GetMapping("/revisao-lore/contextos")
    public ResponseEntity<List<RevisaoLoreContextoResponse>> listarPromptsRevisaoLore() {
        List<RevisaoLoreContextoResponse> lista = gerenciadorPromptRevisaoLore.getProvedores().stream()
            .map(p -> new RevisaoLoreContextoResponse(
                p.getId(),
                p.getNomeExibicao(),
                limparTermoMetadata(p.getNomeExibicao())))
            .toList();
        return ResponseEntity.ok(lista);
    }

    private String limparTermoMetadata(String nomeExibicao) {
        if (nomeExibicao == null) {
            return "";
        }
        return nomeExibicao
            .replaceAll("(?i)\\s*-\\s*Revis[aã]o\\s+de\\s+Lore\\s*$", "")
            .replaceAll("(?i)\\s+Revis[aã]o\\s+de\\s+Lore\\s*$", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida a solicitação e enfileira a revisão sem
     * bloquear a requisição HTTP durante o processamento do LLM.
     * <p>INVARIANTES DO DOMÍNIO: pastas e contexto precisam ser informados; o
     * contexto precisa existir antes do enfileiramento.
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação retorna HTTP 400; exceções da
     * tarefa são convertidas em banner FALHOU no console.
     */
    @PostMapping("/revisar-lore")
    public ResponseEntity<Map<String, Object>> iniciarRevisaoLore(@RequestBody RevisaoLoreRequest req) {
        if (req.diretorioOriginal() == null || req.diretorioOriginal().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas originais em ingles nao informada."));
        }
        if (req.diretorioTraduzido() == null || req.diretorioTraduzido().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas traduzidas em portugues nao informada."));
        }
        if (req.contextoId() == null || req.contextoId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Selecione a obra/contexto no menu para carregar a lore oficial da revisao."));
        }
        if (!gerenciadorPromptRevisaoLore.existePrompt(req.contextoId())) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Prompt de revisao de lore desconhecido: \"" + req.contextoId()
                    + "\". Recarregue a pagina e selecione uma obra valida."));
        }

        Path pastaOriginal = Path.of(req.diretorioOriginal().trim());
        Path pastaTraduzida = Path.of(req.diretorioTraduzido().trim());
        boolean revisarTodas = req.revisarTodasFalas();

        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("revisao-lore");
            long inicioMs = System.currentTimeMillis();
            try {
                ResultadoRevisaoLore resultado = revisarLoreUseCase.executar(
                    pastaOriginal, pastaTraduzida, req.contextoId(), revisarTodas);
                imprimirBanner(resultado);
            } catch (RevisaoLoreException e) {
                imprimirFalha(e.getMessage());
            } catch (Exception e) {
                imprimirFalha("Falha inesperada: " + e.getMessage());
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Revisão de Lore (LLM)", inicioMs));
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Revisao de lore iniciada no servidor. Acompanhe os logs em tempo real."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida e enfileira a revisão de lore PT-only (sem a pasta de
     * originais em inglês), para o caso em que só existe a legenda PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: só a pasta PT-BR e o contexto são obrigatórios; o contexto
     * precisa existir; usa a MESMA fila única do pipeline.
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação retorna HTTP 400; exceção da tarefa vira
     * banner FALHOU no console.
     */
    @PostMapping("/revisar-lore-ptonly")
    public ResponseEntity<Map<String, Object>> iniciarRevisaoLorePtOnly(@RequestBody RevisaoLorePtOnlyRequest req) {
        if (req.diretorioTraduzido() == null || req.diretorioTraduzido().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Pasta com legendas traduzidas em portugues nao informada."));
        }
        if (req.contextoId() == null || req.contextoId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Selecione a obra/contexto no menu para carregar a lore oficial da revisao."));
        }
        if (!gerenciadorPromptRevisaoLore.existePrompt(req.contextoId())) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Prompt de revisao de lore desconhecido: \"" + req.contextoId()
                    + "\". Recarregue a pagina e selecione uma obra valida."));
        }

        Path pastaTraduzida = Path.of(req.diretorioTraduzido().trim());
        boolean usarLlm = req.usarLlm();
        boolean aplicar = req.aplicar();

        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("revisao-lore");
            long inicioMs = System.currentTimeMillis();
            try {
                RevisarLorePtOnlyUseCase.ResultadoLorePtOnly resultado = revisarLorePtOnlyUseCase.executar(
                    pastaTraduzida, req.contextoId(), usarLlm, aplicar);
                imprimirBannerPtOnly(resultado);
            } catch (Exception e) {
                imprimirFalha("Falha inesperada: " + e.getMessage());
            } finally {
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Revisão de Lore PT-only", inicioMs));
            }
        });

        return ResponseEntity.ok(Map.of(
            "mensagem", "Revisao de lore PT-only iniciada no servidor. Acompanhe os logs em tempo real."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: banner de fechamento da revisão de lore PT-only, deixando claro se
     * foi dry-run (nada gravado) ou aplicado, e as contagens reais.
     * <p>INVARIANTES DO DOMÍNIO: sempre imprime corrigidas/descartadas e o modo (dry-run/aplicado).
     * <p>COMPORTAMENTO EM CASO DE FALHA: só escreve em {@code System.out}; não lança.
     */
    private void imprimirBannerPtOnly(RevisarLorePtOnlyUseCase.ResultadoLorePtOnly r) {
        String cor = r.falasDescartadas() > 0 ? AnsiCores.YELLOW : AnsiCores.GREEN;
        String modo = r.aplicado() ? "APLICADO" : "SIMULADO (dry-run, nada gravado)";
        System.out.println("\n" + cor + LINHA + AnsiCores.RESET);
        System.out.println(cor + "  [" + modo + "] REVISAO DE LORE PT-ONLY (sem ingles)" + AnsiCores.RESET);
        System.out.println(cor + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos analisados  : " + r.arquivosAnalisados() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos alterados   : " + r.arquivosAlterados() + AnsiCores.RESET);
        System.out.println(AnsiCores.GREEN + "  • Falas corrigidas     : " + r.falasCorrigidas() + AnsiCores.RESET);
        System.out.println(AnsiCores.YELLOW + "  • Falas descartadas    : " + r.falasDescartadas() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Camada LLM ativa     : " + (r.usouLlm() ? "sim" : "nao") + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Backups              : " + r.backups().size() + AnsiCores.RESET);
        System.out.println(cor + LINHA + "\n" + AnsiCores.RESET);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fecha o job com um banner cuja cor e título refletem
     * o desfecho REAL (concluído, com pendências, cancelado, sem arquivos),
     * substituindo o "[SUCESSO]" incondicional que mentia quando havia problemas.
     *
     * <p>INVARIANTES DO DOMÍNIO: sempre imprime os contadores operacionais
     * (corrigidas, sem-resposta, descartadas, encaminhadas à Opção 6,
     * pendentes e falhas) para que o operador saiba o que ficou por resolver.
     * {@link StatusRevisaoLore#FALHOU}
     * não chega aqui — é impresso por {@link #imprimirFalha(String)}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: só escreve em {@code System.out}; não
     * lança exceção (roda dentro do finally da tarefa da fila).
     */
    private void imprimirBanner(ResultadoRevisaoLore r) {
        String cor = switch (r.status()) {
            case CONCLUIDO -> AnsiCores.GREEN;
            case CONCLUIDO_COM_PENDENCIAS, CANCELADO, SEM_ARQUIVOS -> AnsiCores.YELLOW;
            case FALHOU -> AnsiCores.RED;
        };
        System.out.println("\n" + cor + LINHA + AnsiCores.RESET);
        System.out.println(cor + "  [" + r.status().rotulo().toUpperCase() + "] REVISAO DE LORE" + AnsiCores.RESET);
        System.out.println(cor + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos analisados  : " + r.arquivosAnalisados() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos alterados   : " + r.arquivosAlterados() + AnsiCores.RESET);
        System.out.println(AnsiCores.GREEN + "  • Falas corrigidas     : " + r.falasCorrigidas() + AnsiCores.RESET);
        System.out.println(AnsiCores.YELLOW + "  • Falas sem resposta   : " + r.falasSemResposta() + AnsiCores.RESET);
        System.out.println(AnsiCores.YELLOW + "  • Falas descartadas    : " + r.falasDescartadas() + AnsiCores.RESET);
        System.out.println(AnsiCores.YELLOW + "  • Encaminhadas Opção 6 : "
            + r.falasEncaminhadasOpcao6() + AnsiCores.RESET);
        System.out.println(AnsiCores.YELLOW + "  • Falas pendentes      : " + r.falasPendentes() + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  • Falhas (arquivos)    : " + r.totalErros() + AnsiCores.RESET);
        if (r.caminhoRelatorioJson() != null) {
            System.out.println(AnsiCores.CYAN + "  • Relatorio JSON       : " + r.caminhoRelatorioJson() + AnsiCores.RESET);
        }
        System.out.println(cor + LINHA + "\n" + AnsiCores.RESET);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: sinaliza no console que a revisão de lore FALHOU por
     * completo (LLM indisponível, pastas inválidas, erro inesperado) — nenhum
     * arquivo foi processado.
     *
     * <p>INVARIANTES DO DOMÍNIO: usado apenas no caminho de exceção; deixa claro
     * que o status é {@link StatusRevisaoLore#FALHOU}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: só escreve em {@code System.out}.
     */
    private void imprimirFalha(String mensagem) {
        System.out.println("\n" + AnsiCores.RED + LINHA + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  [" + StatusRevisaoLore.FALHOU.rotulo().toUpperCase()
            + "] REVISAO DE LORE" + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + "  " + mensagem + AnsiCores.RESET);
        System.out.println(AnsiCores.RED + LINHA + "\n" + AnsiCores.RESET);
    }
}
