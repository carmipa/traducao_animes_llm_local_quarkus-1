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
import org.traducao.projeto.core.presentation.web.LogStreamService;

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

    /**
     * Nome da pasta de saída quando o operador não informa nenhuma. Fica IRMÃ da pasta de
     * entrada, ao lado de {@code legendas_extraidas_ass} e {@code traducao_ptbr} — a convenção
     * do acervo é uma pasta por etapa, todas no mesmo nível dentro da pasta da obra.
     */
    static final String PASTA_SIMPLIFICADA_PADRAO = "legenda-simplificada";

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
        java.nio.file.Path origem = Paths.get(request.caminhoOrigem());
        java.nio.file.Path destino = resolverDestino(origem, request.caminhoDestino());
        CompletableFuture.runAsync(() -> {
            try {
                if (gravar) {
                    conversor.aplicar(origem, destino);
                } else {
                    conversor.simular(origem, destino);
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

    /**
     * PROPÓSITO DE NEGÓCIO: resolve a pasta de destino quando o operador não informa nenhuma.
     * A convenção do acervo é uma pasta por etapa, TODAS irmãs dentro da pasta da obra
     * ({@code legendas_extraidas_ass}, {@code traducao_ptbr}, ...), então a simplificação de
     * karaokê ganha {@code legenda-simplificada} no mesmo nível — e não dentro da entrada, que
     * faria a próxima execução varrer a própria saída.
     *
     * <p>INVARIANTES DO DOMÍNIO: destino informado pelo operador vence sempre e é usado como
     * está. O padrão é IRMÃO da pasta de entrada; entrada em raiz (sem pai) cai dentro dela
     * mesma, que é o comportamento degradado seguro por não haver nível acima.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não cria nada e não toca disco — quem cria a pasta é o
     * use case, na hora de gravar. Em simulação nada é criado.
     *
     * @param origem pasta das legendas a simplificar
     * @param informado o que veio da tela; nulo ou em branco aciona o padrão
     */
    static java.nio.file.Path resolverDestino(java.nio.file.Path origem, String informado) {
        if (informado != null && !informado.trim().isEmpty()) {
            return Paths.get(informado.trim());
        }
        java.nio.file.Path pai = origem.toAbsolutePath().normalize().getParent();
        return (pai != null ? pai : origem).resolve(PASTA_SIMPLIFICADA_PADRAO);
    }

    private String validar(NovoKaraokeRequest request) {
        if (request == null || request.caminhoOrigem() == null || request.caminhoOrigem().trim().isEmpty()) {
            return "Informe a pasta das legendas de origem.";
        }
        return null;
    }
}
