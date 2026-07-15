package org.traducao.projeto.novoKaraoke.presentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.novoKaraoke.application.ConversorKaraokeUseCase;
import org.traducao.projeto.novoKaraoke.domain.NovoKaraokeException;
import org.traducao.projeto.traducao.presentation.web.LogStreamService;

import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Endpoints do módulo Karaokê Simples. Operação puramente local (sem LLM,
 * sem estado global do pipeline), por isso roda async fora da fila — mesmo
 * padrão do módulo de Renomear Arquivos.
 */
@Path("/api/novo-karaoke")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NovoKaraokeController {

    private static final Logger log = LoggerFactory.getLogger(NovoKaraokeController.class);

    @Inject
    ConversorKaraokeUseCase conversor;

    @Inject
    LogStreamService logStream;

    @POST
    @Path("/simular")
    public Response simular(NovoKaraokeRequest request) {
        return executar(request, false);
    }

    @POST
    @Path("/aplicar")
    public Response aplicar(NovoKaraokeRequest request) {
        return executar(request, true);
    }

    private Response executar(NovoKaraokeRequest request, boolean gravar) {
        String erroValidacao = validar(request);
        if (erroValidacao != null) {
            return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", erroValidacao)).build();
        }
        CompletableFuture.runAsync(() -> {
            try {
                if (gravar) {
                    conversor.aplicar(Paths.get(request.caminhoOrigem()), Paths.get(request.caminhoDestino()));
                } else {
                    conversor.simular(Paths.get(request.caminhoOrigem()), Paths.get(request.caminhoDestino()));
                }
            } catch (NovoKaraokeException e) {
                logStream.publicarLog(ConversorKaraokeUseCase.CANAL_LOG, "[ERRO] " + e.getMessage());
            } catch (Exception e) {
                log.error("Erro na conversão de karaokê", e);
                logStream.publicarLog(ConversorKaraokeUseCase.CANAL_LOG,
                    "[ERRO FATAL] Falha durante a conversão: " + e.getMessage());
            }
        });
        return Response.ok(Map.of("mensagem",
            (gravar ? "Conversão" : "Simulação") + " de karaokê iniciada. Acompanhe o progresso no console abaixo.")).build();
    }

    private String validar(NovoKaraokeRequest request) {
        if (request == null || request.caminhoOrigem() == null || request.caminhoOrigem().trim().isEmpty()) {
            return "Informe a pasta das legendas de origem.";
        }
        if (request.caminhoDestino() == null || request.caminhoDestino().trim().isEmpty()) {
            return "Informe a pasta de destino das legendas convertidas.";
        }
        return null;
    }
}
