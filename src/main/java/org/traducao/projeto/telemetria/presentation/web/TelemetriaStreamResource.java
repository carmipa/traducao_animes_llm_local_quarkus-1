package org.traducao.projeto.telemetria.presentation.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;
import org.traducao.projeto.telemetria.TelemetriaService;

/**
 * PROPÓSITO DE NEGÓCIO: canal Server-Sent Events (SSE) reativo que transmite a
 * telemetria acumulada da KRONOS ao painel web em tempo real, conforme os
 * episódios são processados.
 *
 * <p>Fronteira arquitetural: pertence ao módulo {@code telemetria}, dono da
 * funcionalidade, e reside na sua camada de apresentação própria. Depende apenas
 * do {@link TelemetriaService} do próprio módulo — sem qualquer dependência
 * funcional da Tradução Local (Opção 4).
 *
 * <p>INVARIANTES DO DOMÍNIO: a rota {@code GET /api/telemetria/stream} e o tipo
 * {@code text/event-stream} são contrato público preservado exatamente como antes
 * da movimentação; a rota é distinta das rotas Spring MVC para evitar colisão de
 * endpoints (JAX-RS/SSE nativo do Quarkus).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: o registro do {@code SseEventSink} é delegado
 * ao serviço de telemetria; o encerramento/erro da conexão é gerido pelo runtime
 * SSE, sem afetar o processamento em andamento.
 */
@Path("/api/telemetria/stream")
public class TelemetriaStreamResource {

    @Inject
    TelemetriaService telemetriaService;

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(@Context SseEventSink sink) {
        telemetriaService.registrarSink(sink);
    }
}
