package org.traducao.projeto.remuxer.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.remuxer.application.RemuxarLoteUseCase;
import org.traducao.projeto.remuxer.domain.RelatorioRemux;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.traducao.presentation.web.RemuxRequest;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: expõe o Remuxer (mkvmerge) à interface web, agendando um
 * único lote de remux que combina vídeos e legendas traduzidas com política
 * explícita para as legendas originais.
 *
 * <p>Fronteira arquitetural: este endpoint pertence ao módulo {@code remuxer}
 * (Opção 12) e reside na sua camada de apresentação própria. Não importa nenhuma
 * regra funcional da Tradução Local (Opção 4): usa o use case do próprio módulo e
 * a fila técnica neutra {@code core}. As dependências {@link PipelineWebSupport},
 * {@link RespostaPadrao}, {@link RemuxRequest} e {@link AnsiCores} são <b>glue
 * técnico de apresentação</b> (fila única, transporte HTTP, cores de console)
 * hoje em {@code traducao.presentation}; é dívida técnica temporária reservada
 * para saneamento na FASE E — não é acoplamento funcional.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
 * {@link PipelineWebSupport} e consulta a MESMA {@link FilaExecucaoPipeline} para
 * recusar concorrência; as pastas existem antes da aceitação; o offset fica na
 * faixa operacional de ±86.400.000 ms; a rota {@code POST /api/remuxar}, os
 * status (200/400/409) e os campos de DTO são contrato público preservado
 * exatamente como antes da movimentação.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna HTTP 400, fila
 * ocupada retorna HTTP 409 e falha do lote aparece no status final do console.
 */
@RestController
@RequestMapping("/api")
public class RemuxerController {

    private static final Logger log = LoggerFactory.getLogger(RemuxerController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final RemuxarLoteUseCase remuxarLoteUseCase;
    // Fila única compartilhada por todos os módulos (ver FilaExecucaoPipeline):
    // consultada aqui para recusar o Remuxer quando já há trabalho em andamento.
    private final FilaExecucaoPipeline filaExecucao;

    public RemuxerController(
            PipelineWebSupport pipelineWebSupport,
            RemuxarLoteUseCase remuxarLoteUseCase,
            FilaExecucaoPipeline filaExecucao) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.remuxarLoteUseCase = remuxarLoteUseCase;
        this.filaExecucao = filaExecucao;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida e agenda um único lote de remux com política
     * explícita para legendas originais.
     *
     * <p>INVARIANTES DO DOMÍNIO: pastas existem antes da aceitação; offset fica em
     * faixa operacional; nenhuma segunda operação entra enquanto a fila está
     * ocupada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada inválida retorna 400, fila ocupada
     * retorna 409 e falha do lote aparece no status final do console.
     */
    @PostMapping("/remuxar")
    public synchronized ResponseEntity<RespostaPadrao> remuxar(@RequestBody RemuxRequest req) {
        if (req == null || req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de vídeos de entrada obrigatória."));
        }
        Path pathVideos = pipelineWebSupport.normalizarCaminho(req.entrada());
        if (pathVideos == null || !Files.isDirectory(pathVideos)) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Pasta de vídeos inválida: " + req.entrada()));
        }
        Path pathLegendas = req.saida() == null || req.saida().isBlank()
            ? localizarPastaLegendasAutomatica(pathVideos)
            : pipelineWebSupport.normalizarCaminho(req.saida());
        if (pathLegendas == null || !Files.isDirectory(pathLegendas)) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Pasta de legendas inválida. Informe-a ou crie uma subpasta como 'legendas pt' dentro da pasta de vídeos."));
        }
        long sincronismoMs = req.syncOffsetMs() == null ? 0 : req.syncOffsetMs();
        if (sincronismoMs < -86_400_000L || sincronismoMs > 86_400_000L) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Sincronismo fora do limite seguro de ±86.400.000 ms (24 horas)."));
        }
        if (filaExecucao.ocupada()) {
            return ResponseEntity.status(409).body(new RespostaPadrao(
                "O pipeline já possui uma operação em andamento ou aguardando. Aguarde a conclusão antes de iniciar o Remuxer."));
        }
        boolean preservarOriginais = Boolean.TRUE.equals(req.preservarLegendasOriginais());
        pipelineWebSupport.submeterJobComRelatorio("remuxer", "Remuxer (mkvmerge)", () -> {
            try {
                RelatorioRemux relatorio = remuxarLoteUseCase.executar(
                    pathVideos, pathLegendas, sincronismoMs, preservarOriginais);
                String status = relatorio.getStatusFinal();
                String resumo = "status=" + status
                    + ", sucessos=" + relatorio.getMkvProcessadosSucesso()
                    + ", pendências=" + relatorio.getTotalPendencias()
                    + ", falhas=" + relatorio.getTotalErros()
                    + ", semLegenda=" + relatorio.getVideosSemLegenda()
                    + ", ambíguos=" + relatorio.getPareamentosAmbiguos()
                    + ", existentesPreservados=" + relatorio.getSaidasJaExistentes();
                String linhaSeparadora = "========================================================================";
                if ("CONCLUIDO".equals(status)) {
                    System.out.println("\n" + AnsiCores.GREEN + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.GREEN + "  [SUCESSO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.GREEN + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.info("[SUCESSO] Remuxer de videos finalizado: {}", resumo);
                } else if ("CONCLUIDO_COM_PENDENCIAS".equals(status) || "SEM_ARQUIVOS".equals(status)) {
                    System.out.println("\n" + AnsiCores.YELLOW + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.YELLOW + "  [ATENÇÃO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.YELLOW + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.warn("[ATENÇÃO] Remuxer finalizado: {}", resumo);
                } else {
                    System.out.println("\n" + AnsiCores.RED + linhaSeparadora + AnsiCores.RESET);
                    System.out.println(AnsiCores.RED + "  [FALHA/CANCELADO] REMUXER FINALIZADO! (" + resumo + ")" + AnsiCores.RESET);
                    System.out.println(AnsiCores.RED + linhaSeparadora + AnsiCores.RESET + "\n");
                    log.error("[FALHA/CANCELADO] Remuxer finalizado: {}", resumo);
                }
            } catch (Exception e) {
                log.error("Erro no remuxer em background", e);
                System.out.println("\u001B[31m[ERRO] Falha no Remuxer: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Remuxer aceito pela fila. O resultado real, arquivo por arquivo, aparecerá no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encontra automaticamente a pasta local de legendas ao
     * lado dos vídeos usando nomes adotados no pipeline do Paulo.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente subdiretórios diretos são considerados e
     * a comparação ignora caixa, espaço, hífen e underscore.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de leitura ou ausência devolve
     * {@code null}, fazendo o endpoint pedir um caminho explícito.
     */
    private Path localizarPastaLegendasAutomatica(Path pastaVideos) {
        Set<String> nomesAceitos = Set.of("legendaspt", "legendasptbr", "legendasportugues");
        try (Stream<Path> stream = Files.list(pastaVideos)) {
            return stream.filter(Files::isDirectory)
                .filter(p -> nomesAceitos.contains(p.getFileName().toString().toLowerCase()
                    .replace("-", "").replace("_", "").replace(" ", "")))
                .sorted()
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            log.warn("Não foi possível procurar pasta automática de legendas em {}: {}", pastaVideos, e.getMessage());
            return null;
        }
    }
}
