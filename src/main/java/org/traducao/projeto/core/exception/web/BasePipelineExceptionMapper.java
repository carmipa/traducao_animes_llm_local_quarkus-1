package org.traducao.projeto.core.exception.web;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.exception.BasePipelineException;

/**
 * Converte qualquer exceção de domínio do pipeline (uma por pacote, todas
 * estendendo {@link BasePipelineException}) em uma resposta JSON estruturada
 * e rastreável, em vez de cada endpoint precisar capturar e formatar erro
 * manualmente. O {@code errorId} permite cruzar a resposta HTTP com a
 * entrada correspondente no log do servidor.
 */
@Provider
public class BasePipelineExceptionMapper implements ExceptionMapper<BasePipelineException> {

    private static final Logger log = LoggerFactory.getLogger(BasePipelineExceptionMapper.class);

    public record ErroResponse(String erro, String errorId, String timestamp) {}

    @Override
    public Response toResponse(BasePipelineException exception) {
        log.error("[{}] {}", exception.getErrorId(), exception.getMessage(), exception);
        ErroResponse corpo = new ErroResponse(
            exception.getMessage(),
            exception.getErrorId(),
            exception.getTimestamp().toString()
        );
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(corpo)
            .build();
    }
}
