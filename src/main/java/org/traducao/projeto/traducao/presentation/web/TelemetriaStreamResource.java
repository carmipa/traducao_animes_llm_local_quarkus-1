package org.traducao.projeto.traducao.presentation.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;
import org.traducao.projeto.telemetria.TelemetriaService;

/**
 * Endpoint Server-Sent Events (SSE) reativo para streaming da telemetria da KRONOS em tempo real.
 * Rota mapeada especificamente para evitar colisões com o Controller Spring.
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
