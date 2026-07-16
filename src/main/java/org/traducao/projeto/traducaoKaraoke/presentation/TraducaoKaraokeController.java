package org.traducao.projeto.traducaoKaraoke.presentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.presentation.web.LogStreamService;
import org.traducao.projeto.traducaoKaraoke.application.TraduzirKaraokeUseCase;
import org.traducao.projeto.traducaoKaraoke.domain.TraducaoKaraokeException;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Endpoints do módulo Tradução de Karaokê. A simulação só lê arquivos e roda
 * async fora da fila (mesmo padrão do Karaokê Simples); a APLICAÇÃO chama o
 * LLM e muda o contexto de lore ativo — estado global —, então
 * obrigatoriamente entra na {@link FilaExecucaoPipeline}.
 */
@Path("/api/traducao-karaoke")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TraducaoKaraokeController {

    private static final Logger log = LoggerFactory.getLogger(TraducaoKaraokeController.class);

    @Inject
    TraduzirKaraokeUseCase useCase;

    @Inject
    LogStreamService logStream;

    @Inject
    FilaExecucaoPipeline filaExecucao;

    @Inject
    GerenciadorContexto gerenciadorContexto;

    @POST
    @Path("/simular")
    public Response simular(TraducaoKaraokeRequest request) {
        String erro = validar(request, false);
        if (erro != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", erro)).build();
        }
        CompletableFuture.runAsync(() -> {
            try {
                useCase.simular(Paths.get(request.caminhoOrigem()), request.contextoId());
            } catch (TraducaoKaraokeException e) {
                logStream.publicarLog(TraduzirKaraokeUseCase.CANAL_LOG, "[ERRO] " + e.getMessage());
            } catch (Exception e) {
                log.error("Erro na simulação da tradução de karaokê", e);
                logStream.publicarLog(TraduzirKaraokeUseCase.CANAL_LOG,
                    "[ERRO FATAL] Falha durante a simulação: " + e.getMessage());
            }
        });
        return Response.ok(Map.of("mensagem",
            "Simulação da tradução de karaokê iniciada. Acompanhe a classificação linha a linha no console abaixo.")).build();
    }

    @POST
    @Path("/aplicar")
    public Response aplicar(TraducaoKaraokeRequest request) {
        String erro = validar(request, true);
        if (erro != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", erro)).build();
        }
        filaExecucao.submeter(() -> {
            logStream.definirCanalAtual(TraduzirKaraokeUseCase.CANAL_LOG);
            try {
                useCase.aplicar(Paths.get(request.caminhoOrigem()), request.contextoId());
            } catch (TraducaoKaraokeException e) {
                logStream.publicarLog(TraduzirKaraokeUseCase.CANAL_LOG, "[ERRO] " + e.getMessage());
            } catch (Exception e) {
                log.error("Erro na tradução de karaokê", e);
                logStream.publicarLog(TraduzirKaraokeUseCase.CANAL_LOG,
                    "[ERRO FATAL] Falha durante a tradução: " + e.getMessage());
            }
        });
        return Response.ok(Map.of("mensagem",
            "Tradução de karaokê enviada para a fila do pipeline. Acompanhe o progresso no console abaixo.")).build();
    }

    private String validar(TraducaoKaraokeRequest request, boolean exigeContextoValido) {
        if (request == null || request.caminhoOrigem() == null || request.caminhoOrigem().trim().isEmpty()) {
            return "Informe a pasta com as legendas que deseja traduzir.";
        }
        if (exigeContextoValido && request.contextoId() != null && !request.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(request.contextoId())) {
            return "Contexto de tradução desconhecido: \"" + request.contextoId()
                + "\". Recarregue a página e selecione uma obra válida.";
        }
        return null;
    }
}
