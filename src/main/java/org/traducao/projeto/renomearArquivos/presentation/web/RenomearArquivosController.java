package org.traducao.projeto.renomearArquivos.presentation.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.renomearArquivos.application.OperacaoRenomeacaoEmAndamentoException;
import org.traducao.projeto.renomearArquivos.application.RenomeadorUseCase;

import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Supplier;

/**
 * PROPÓSITO DE NEGÓCIO: expõe simulação, aplicação e reversão da opção 13 com
 * resposta somente depois que o status real da operação estiver disponível.
 *
 * <p>INVARIANTES DO DOMÍNIO: entradas inválidas retornam 400, concorrência na
 * mesma pasta retorna 409 e nenhuma resposta antecipada anuncia falso sucesso.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: erros esperados viram JSON didático; falhas
 * inesperadas são registradas e retornam HTTP 500 sem expor stack trace.
 */
@Path("/api/renomear-arquivos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RenomearArquivosController {
    private static final Logger log = LoggerFactory.getLogger(RenomearArquivosController.class);

    @Inject
    RenomeadorUseCase renomeadorUseCase;

    /**
     * PROPÓSITO DE NEGÓCIO: entrega ao painel o dry-run completo e seu status real.
     *
     * <p>INVARIANTES DO DOMÍNIO: simulação nunca altera mídia.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: delega validação e mapeamento HTTP ao
     * executor comum do controller.
     */
    @POST
    @Path("/simular")
    public Response simular(RenomearArquivosRequest request) {
        return executar(request, () -> renomeadorUseCase.simularComResultado(
            Paths.get(request.caminhoOrigem()), request.nomePadrao(), request.temporada()));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aplica a renomeação e mantém a requisição aberta até
     * o lote concluir, permitindo que a tela bloqueie novos cliques.
     *
     * <p>INVARIANTES DO DOMÍNIO: a resposta contém contagens finais, não apenas
     * aceitação em fila.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve o status parcial produzido pelo
     * caso de uso ou erro HTTP estruturado.
     */
    @POST
    @Path("/aplicar")
    public Response aplicar(RenomearArquivosRequest request) {
        return executar(request, () -> renomeadorUseCase.aplicarRenomeacao(
            Paths.get(request.caminhoOrigem()), request.nomePadrao(), request.temporada()));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: retoma ou conclui a reversão da última aplicação da
     * pasta selecionada.
     *
     * <p>INVARIANTES DO DOMÍNIO: nome padrão e temporada não são necessários para
     * localizar o manifesto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: manifesto ausente é um resultado de
     * negócio; manifesto inválido é reportado sem mover arquivos.
     */
    @POST
    @Path("/reverter")
    public Response reverter(RenomearArquivosRequest request) {
        return executar(request, () -> renomeadorUseCase.reverterRenomeacao(
            Paths.get(request.caminhoOrigem())));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: centraliza validação de contrato e tradução de falhas
     * para HTTP consistente consumido pelo JavaScript.
     *
     * <p>INVARIANTES DO DOMÍNIO: toda resposta de erro possui JSON com campo
     * {@code error}; requisição nula ou caminho vazio nunca chega ao caso de uso.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: validação retorna 400, pasta ocupada 409
     * e exceção inesperada 500.
     */
    private Response executar(RenomearArquivosRequest request, Supplier<Object> operacao) {
        if (request == null || request.caminhoOrigem() == null || request.caminhoOrigem().isBlank()) {
            return erro(Response.Status.BAD_REQUEST, "Caminho de origem não fornecido.");
        }
        try {
            return Response.ok(operacao.get()).build();
        } catch (OperacaoRenomeacaoEmAndamentoException e) {
            return erro(Response.Status.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            return erro(Response.Status.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Falha inesperada na opção 13", e);
            return erro(Response.Status.INTERNAL_SERVER_ERROR,
                "Falha inesperada na opção 13. Consulte o console do servidor.");
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cria o corpo de erro uniforme esperado pela tela.
     *
     * <p>INVARIANTES DO DOMÍNIO: mensagem nunca é nula.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mensagem ausente recebe descrição
     * genérica segura.
     */
    private Response erro(Response.Status status, String mensagem) {
        return Response.status(status)
            .entity(Map.of("error", mensagem == null ? "Falha não detalhada." : mensagem))
            .build();
    }
}
