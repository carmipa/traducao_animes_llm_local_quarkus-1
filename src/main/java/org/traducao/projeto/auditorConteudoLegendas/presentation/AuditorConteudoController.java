package org.traducao.projeto.auditorConteudoLegendas.presentation;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.traducao.projeto.auditorConteudoLegendas.application.AuditorConteudoUseCase;
import org.traducao.projeto.auditorConteudoLegendas.domain.AuditoriaException;
import org.traducao.projeto.auditorConteudoLegendas.domain.ModoAuditoria;
import org.traducao.projeto.auditorConteudoLegendas.domain.RelatorioAuditoriaConteudo;
import org.traducao.projeto.core.presentation.web.LogStreamService;

@Path("/api/auditoria-conteudo")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditorConteudoController {

    @Inject
    AuditorConteudoUseCase auditorConteudoUseCase;

    @Inject
    LogStreamService logStreamService;

    public record AuditoriaRequest(String modo, String caminhoOriginal, String caminhoTraduzido) {}

    /**
     * PROPÓSITO DE NEGÓCIO: expõe a Análise de Conteúdo nos três escopos das abas
     * do painel (só original, só traduzido, ambos) sobre o mesmo endpoint.
     * <p>INVARIANTES DO DOMÍNIO: o modo determina quais caminhos são obrigatórios;
     * modo ausente equivale a AMBAS (retrocompatível).
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho exigido em branco → 400 didático;
     * {@link AuditoriaException} → 400 com a mensagem de domínio; erro inesperado
     * → 500.
     */
    @POST
    public Response auditar(AuditoriaRequest request) {
        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Requisicao de auditoria ausente.")
                .build();
        }

        // Bug 7 — modo preenchido porém desconhecido é erro do cliente (não vira AMBAS).
        if (!ModoAuditoria.reconhece(request.modo())) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Modo de auditoria invalido: \"" + request.modo()
                    + "\". Use AMBAS, ORIGINAL ou TRADUZIDO.")
                .build();
        }
        ModoAuditoria modo = ModoAuditoria.porNome(request.modo());

        try {
            // Bug 11 — normaliza aspas e rejeita caminho sintaticamente inválido com 400.
            java.nio.file.Path original = parseCaminho(request.caminhoOriginal(), "original");
            java.nio.file.Path traduzido = parseCaminho(request.caminhoTraduzido(), "traduzido");
            boolean temOriginal = original != null;
            boolean temTraduzido = traduzido != null;

            String erroValidacao = switch (modo) {
                case AMBAS -> (temOriginal && temTraduzido) ? null
                    : "Caminhos original e traduzido sao obrigatorios.";
                case ORIGINAL -> temOriginal ? null
                    : "Caminho do arquivo original e obrigatorio.";
                case TRADUZIDO -> temTraduzido ? null
                    : "Caminho do arquivo traduzido e obrigatorio.";
            };
            if (erroValidacao != null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(erroValidacao).build();
            }

            logStreamService.definirCanalAtual("auditor-conteudo");
            long inicioMs = System.currentTimeMillis();
            RelatorioAuditoriaConteudo relatorio = auditorConteudoUseCase.auditar(modo, original, traduzido);
            System.out.println(org.traducao.projeto.core.util.DuracaoUtil.linhaRelatorioFinal(
                "Análise de Conteúdo de Legendas", inicioMs));
            return Response.ok(relatorio).build();
        } catch (AuditoriaException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(e.getMessage())
                .build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o caminho recebido em {@link java.nio.file.Path}
     * de forma segura, tolerando aspas coladas e rejeitando entradas inválidas com
     * mensagem didática (em vez de HTTP 500).
     * <p>INVARIANTES DO DOMÍNIO: valor ausente/em branco devolve {@code null}; aspas
     * simples/duplas envolventes são removidas antes de resolver o caminho.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho sintaticamente inválido lança
     * {@link AuditoriaException} (mapeada para HTTP 400).
     */
    private java.nio.file.Path parseCaminho(String valor, String papel) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpo = valor.trim();
        if (limpo.length() >= 2
            && ((limpo.startsWith("\"") && limpo.endsWith("\""))
                || (limpo.startsWith("'") && limpo.endsWith("'")))) {
            limpo = limpo.substring(1, limpo.length() - 1).trim();
        }
        if (limpo.isBlank()) {
            return null;
        }
        try {
            return java.nio.file.Path.of(limpo);
        } catch (java.nio.file.InvalidPathException e) {
            throw new AuditoriaException("Caminho " + papel + " invalido: " + valor);
        }
    }
}
