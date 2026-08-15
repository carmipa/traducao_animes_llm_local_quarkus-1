package org.traducao.projeto.correcaoLegendas.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.correcaoLegendas.application.CorrigirLegendasUseCase;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.core.util.DuracaoUtil;
import org.traducao.projeto.lore.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.presentation.web.LogStreamService;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CorrecaoLegendasController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CorrecaoLegendasController.class);

    private final CorrigirLegendasUseCase corrigirLegendasUseCase;
    private final GerenciadorContexto gerenciadorContexto;
    private final LogStreamService logStreamService;
    private final FilaExecucaoPipeline filaExecucao;
    private final org.traducao.projeto.core.io.GuardaCaminhoEntrada guardaCaminho;

    public CorrecaoLegendasController(
        CorrigirLegendasUseCase corrigirLegendasUseCase,
        GerenciadorContexto gerenciadorContexto,
        LogStreamService logStreamService,
        FilaExecucaoPipeline filaExecucao,
        org.traducao.projeto.core.io.GuardaCaminhoEntrada guardaCaminho
    ) {
        this.corrigirLegendasUseCase = corrigirLegendasUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
        this.logStreamService = logStreamService;
        this.filaExecucao = filaExecucao;
        this.guardaCaminho = guardaCaminho;
    }

    @PostMapping("/correcao-legendas")
    public ResponseEntity<Map<String, Object>> iniciarCorrecaoLegendas(@RequestBody Map<String, String> payload) {
        return executarCorrecaoLegendas(payload);
    }

    @PostMapping("/cura-tags")
    public ResponseEntity<Map<String, Object>> iniciarCuraTagsLegado(@RequestBody Map<String, String> payload) {
        return executarCorrecaoLegendas(payload);
    }

    private ResponseEntity<Map<String, Object>> executarCorrecaoLegendas(Map<String, String> payload) {
        String diretorioOriginal = payload.get("diretorioOriginal");
        String diretorioTraduzido = payload.get("diretorioTraduzido");
        if (diretorioOriginal == null || diretorioOriginal.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Pasta com as legendas originais/referência não informada."));
        }
        if (diretorioTraduzido == null || diretorioTraduzido.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Pasta com as legendas traduzidas (PT-BR) não informada."));
        }

        String contextoId = payload.get("contextoId");
        if (contextoId != null && !contextoId.isBlank() && !gerenciadorContexto.existeContexto(contextoId)) {
            return ResponseEntity.badRequest().body(Map.of(
                "erro", "Contexto de tradução desconhecido: \"" + contextoId + "\". Recarregue a página e selecione um contexto válido."));
        }

        // ANTES da fila. Esta rota foi a que MEDIU o pior caso em 11/08/2026: com uma
        // pasta inexistente ela respondia 202, criava relatorios/<nome-da-pasta-que-nao-
        // existe>/, gravava o JSON de relatorio e registrava na telemetria canonica
        // {"arquivosProcessados": 1, "itensCorrigidos": 0} — que o
        // ConsolidadorTelemetriaPorFatia le. Ali "0 corrigidos porque a pasta nao existe"
        // ficou identico a "0 corrigidos porque estava tudo certo".
        var recusa = guardaCaminho
            .conferirDiretorio("Pasta com as legendas originais/referência", diretorioOriginal)
            .or(() -> guardaCaminho
                .conferirDiretorio("Pasta com as legendas traduzidas (PT-BR)", diretorioTraduzido));
        if (recusa.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("erro", recusa.get().mensagem()));
        }

        Path pastaOriginal;
        Path pastaTraduzida;
        try {
            pastaOriginal = Paths.get(diretorioOriginal.trim());
            pastaTraduzida = Paths.get(diretorioTraduzido.trim());
        } catch (InvalidPathException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Caminho de pasta inválido: " + e.getMessage()));
        }

        // Toda correção entra na fila única do pipeline, mesmo a 100%
        // estrutural (sem contextoId/LLM): com contextoId ela muta o contexto
        // de tradução global e disputa a GPU; e em qualquer modo roda em
        // segundo plano publicando logs no canal SSE da aba — dois jobs em
        // paralelo misturariam consoles e estado.
        if (filaExecucao.ocupada()) {
            return ResponseEntity.status(409).body(Map.of(
                "erro", "Outra operação do pipeline está em andamento. "
                    + "Aguarde a conclusão antes de iniciar a correção de legendas."));
        }

        final Path fOriginal = pastaOriginal;
        final Path fTraduzida = pastaTraduzida;
        final String fContextoId = contextoId;

        filaExecucao.submeter(() -> {
            logStreamService.definirCanalAtual("correcao-legendas");
            long inicioMs = System.currentTimeMillis();
            try {
                corrigirLegendasUseCase.corrigirPasta(fOriginal, fTraduzida, fContextoId);
                System.out.println(DuracaoUtil.linhaRelatorioFinal("Correção de Karaoke/Legendas (LLM)", inicioMs));
            } catch (Exception e) {
                log.error("Falha ao executar correção de legendas em background", e);
                System.out.println("\u001B[31m[FAIL] Erro na correção de legendas: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.accepted().body(Map.of(
            "mensagem", "Correção de legendas enviada para a fila de execução em segundo plano."
        ));
    }
}
