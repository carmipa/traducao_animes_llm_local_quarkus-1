package org.traducao.projeto.traducao.presentation.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.SseEventSink;

/**
 * Endpoint SSE nativo do Quarkus (substitui SseEmitter do Spring MVC).
 */
@Path("/api/logs")
public class LogStreamResource {

    @Inject
    LogStreamService logStreamService;

    @GET
    @Path("/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void stream(@Context SseEventSink sink) {
        logStreamService.registrar(sink);
    }
}
