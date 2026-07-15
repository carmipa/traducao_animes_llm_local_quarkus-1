package org.traducao.projeto.traducao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.raspagemRevisao.application.ResultadoRevisaoLegendas;
import org.traducao.projeto.raspagemRevisao.application.RevisarLegendasUseCase;
import org.traducao.projeto.traducao.domain.StatusLlm;
import org.traducao.projeto.traducao.domain.ports.MistralPort;
import org.traducao.projeto.traducao.infrastructure.contexto.GerenciadorContexto;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;

import java.nio.file.Path;
import java.util.Optional;

/**
 * PROPÓSITO DE NEGÓCIO: expõe à interface web a revisão das legendas traduzidas
 * (.ass) — via Google Translate com auditoria e via LLM local para concordância
 * PT-BR — usando cache e/ou legendas originais como referência.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
 * {@link PipelineWebSupport}; a pasta de entrada é obrigatória e validada; o modo
 * de referência e a pasta de cache são resolvidos e validados antes de
 * enfileirar; a revisão de concordância só prossegue com o LLM disponível;
 * nenhuma URL, código HTTP ou nome de campo de DTO é alterado em relação ao
 * controller monolítico original.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entrada/cache/contexto inválido retorna
 * HTTP 400; indisponibilidade do LLM e falhas do job aparecem no console SSE, sem
 * derrubar a fila.
 */
@RestController
@RequestMapping("/api")
public class RevisaoLegendasController {

    private static final Logger log = LoggerFactory.getLogger(RevisaoLegendasController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final RevisarLegendasUseCase revisarLegendasUseCase;
    private final GerenciadorContexto gerenciadorContexto;
    private final MistralPort mistralPort;

    public RevisaoLegendasController(
            PipelineWebSupport pipelineWebSupport,
            RevisarLegendasUseCase revisarLegendasUseCase,
            GerenciadorContexto gerenciadorContexto,
            MistralPort mistralPort) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.revisarLegendasUseCase = revisarLegendasUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
        this.mistralPort = mistralPort;
    }

    /**
     * 5. REVISÃO DE LEGENDAS TRADUZIDAS (.ass) via Google + auditoria
     */
    @PostMapping("/revisar-legendas")
    public ResponseEntity<RespostaPadrao> revisarLegendas(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Pasta com legendas traduzidas em português (.ass) é obrigatória."));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        Optional<Path> pathPtOpt = parseCaminhoSeguro(req.entrada(), "legendas traduzidas");
        if (pathPtOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Caminho inválido para legendas traduzidas. Informe apenas a pasta (ex.: E:\\animes\\legendas_ptbr), "
                    + "sem colar logs ou textos da interface."));
        }
        Path pathPt = pathPtOpt.get();

        final Path pathEnFinal;
        if (req.saida() != null && !req.saida().isBlank()) {
            Optional<Path> pathEnOpt = parseCaminhoSeguro(req.saida(), "legendas originais em inglês");
            if (pathEnOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Caminho inválido para legendas em inglês. Informe apenas a pasta, sem colar logs da interface."));
            }
            pathEnFinal = pathEnOpt.get();
        } else {
            pathEnFinal = null;
        }

        Optional<String> erroValidacao = revisarLegendasUseCase.validarPastaEntrada(pathPt);
        if (erroValidacao.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroValidacao.get()));
        }

        RevisarLegendasUseCase.ModoReferenciaRevisao referencia = resolverModoReferencia(req.modoReferencia());
        final Path cacheDir = resolverCacheDir(referencia, req.caminhoCache());
        final Path pathEnUso = referencia == RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE ? null : pathEnFinal;

        Optional<String> erroCache = validarCacheDirModo(referencia, cacheDir);
        if (erroCache.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroCache.get()));
        }

        pipelineWebSupport.submeterJobComRelatorio("revisao", "Revisão de Legendas Traduzidas", () -> {
            try {
                ResultadoRevisaoLegendas resultado = revisarLegendasUseCase.executar(
                    pathPt, pathEnUso, cacheDir, null,
                    RevisarLegendasUseCase.ModoRevisaoLegendas.GOOGLE, req.contextoId(), referencia);
                imprimirResultadoRevisaoLegendas("REVISÃO DE LEGENDAS TRADUZIDAS", resultado);
            } catch (Exception e) {
                log.error("Erro na revisão de legendas", e);
                System.out.println("\u001B[31m[ERRO] Revisão de legendas falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao("Revisão de legendas traduzidas iniciada no servidor."));
    }

    /**
     * 5c. REVISÃO DE CONCORDÂNCIA PT-BR NAS LEGENDAS (.ass) via LLM local
     */
    @PostMapping("/revisar-legendas-concordancia")
    public ResponseEntity<RespostaPadrao> revisarLegendasConcordancia(@RequestBody OperacaoRequest req) {
        if (req.entrada() == null || req.entrada().isBlank()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Pasta com legendas traduzidas em português (.ass) é obrigatória."));
        }

        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        Optional<Path> pathPtOpt = parseCaminhoSeguro(req.entrada(), "legendas traduzidas");
        if (pathPtOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Caminho inválido para legendas traduzidas. Informe apenas a pasta (ex.: E:\\animes\\legendas_ptbr), "
                    + "sem colar logs ou textos da interface."));
        }
        Path pathPt = pathPtOpt.get();

        final Path pathEnFinal;
        if (req.saida() != null && !req.saida().isBlank()) {
            Optional<Path> pathEnOpt = parseCaminhoSeguro(req.saida(), "legendas originais em inglês");
            if (pathEnOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(new RespostaPadrao(
                    "Caminho inválido para legendas em inglês. Informe apenas a pasta, sem colar logs da interface."));
            }
            pathEnFinal = pathEnOpt.get();
        } else {
            pathEnFinal = null;
        }

        Optional<String> erroValidacao = revisarLegendasUseCase.validarPastaEntrada(pathPt);
        if (erroValidacao.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroValidacao.get()));
        }

        RevisarLegendasUseCase.ModoReferenciaRevisao referencia = resolverModoReferencia(req.modoReferencia());
        final Path cacheDir = resolverCacheDir(referencia, req.caminhoCache());
        final Path pathEnUso = referencia == RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE ? null : pathEnFinal;

        Optional<String> erroCache = validarCacheDirModo(referencia, cacheDir);
        if (erroCache.isPresent()) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(erroCache.get()));
        }

        pipelineWebSupport.submeterJobComRelatorio("revisao", "Revisão de Concordância PT-BR (LLM)", () -> {
            try {
                StatusLlm status = mistralPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println("\u001B[31m[FAIL] LLM indisponível para revisão de concordância: "
                        + status.mensagem() + "\u001B[0m");
                    return;
                }
                ResultadoRevisaoLegendas resultado = revisarLegendasUseCase.executar(
                    pathPt,
                    pathEnUso,
                    cacheDir,
                    null,
                    RevisarLegendasUseCase.ModoRevisaoLegendas.LLM_CONCORDANCIA,
                    req.contextoId(),
                    referencia
                );
                imprimirResultadoRevisaoLegendas("REVISÃO DE CONCORDÂNCIA PT-BR (LLM)", resultado);
            } catch (Exception e) {
                log.error("Erro na revisão de concordância das legendas", e);
                System.out.println("\u001B[31m[ERRO] Revisão de concordância falhou: "
                    + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Revisão de concordância PT-BR (LLM) iniciada no servidor."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apresenta o desfecho verdadeiro da Opção 6 sem
     * transformar execução tecnicamente estável com pendências em sucesso total.
     *
     * <p>INVARIANTES DO DOMÍNIO: verde exige zero pendências; amarelo informa
     * problemas restantes e zero arquivos mantém o aviso histórico.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado nulo é tratado como falha e
     * não provoca erro no job de background.
     */
    private void imprimirResultadoRevisaoLegendas(String titulo, ResultadoRevisaoLegendas resultado) {
        if (resultado == null) {
            System.out.println(AnsiCores.RED + "[FALHA] " + titulo + " não retornou resultado."
                + AnsiCores.RESET);
            return;
        }
        if (resultado.arquivosAnalisados() == 0) {
            System.out.println(AnsiCores.YELLOW
                + "[AVISO] Revisão concluída sem arquivos .ass/.ssa para analisar."
                + AnsiCores.RESET);
            return;
        }

        boolean concluido = "CONCLUIDO".equals(resultado.status());
        String cor = concluido ? AnsiCores.GREEN : AnsiCores.YELLOW;
        String rotulo = concluido ? "[SUCESSO]" : "[ATENÇÃO]";
        System.out.println("\n" + cor + "========================================================================" + AnsiCores.RESET);
        System.out.println(cor + "  " + rotulo + " " + titulo + " — " + resultado.status() + AnsiCores.RESET);
        System.out.println(cor + "========================================================================" + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Arquivos Analisados : " + resultado.arquivosAnalisados() + AnsiCores.RESET);
        System.out.println(AnsiCores.CYAN + "  • Problemas Detectados: " + resultado.falasComProblema() + AnsiCores.RESET);
        System.out.println(AnsiCores.GREEN + "  • Falas Corrigidas    : " + resultado.falasCorrigidas() + AnsiCores.RESET);
        System.out.println((resultado.falasPendentes() > 0 ? AnsiCores.YELLOW : AnsiCores.GREEN)
            + "  • Falas Pendentes     : " + resultado.falasPendentes() + AnsiCores.RESET);
        System.out.println(cor + "========================================================================\n" + AnsiCores.RESET);
        log.info("[{}] {}: arquivos={}, corrigidas={}, pendentes={}.", resultado.status(), titulo,
            resultado.arquivosAnalisados(), resultado.falasCorrigidas(), resultado.falasPendentes());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza com segurança um caminho informado pelo
     * usuário para as pastas de legendas da revisão.
     *
     * <p>INVARIANTES DO DOMÍNIO: delega a normalização a
     * {@link PipelineWebSupport#normalizarCaminho(String)}, preservando a
     * tolerância a aspas envolventes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho inválido/ausente resulta em
     * {@link Optional#empty()}, levando o endpoint a responder HTTP 400.
     */
    private Optional<Path> parseCaminhoSeguro(String valor, String rotulo) {
        Path p = pipelineWebSupport.normalizarCaminho(valor);
        return Optional.ofNullable(p);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz a aba escolhida na Opção 6 (Ambos/Cache) para o
     * modo de referência do use case de revisão.
     * <p>INVARIANTES DO DOMÍNIO: qualquer valor diferente de "CACHE" cai em AMBOS
     * (comportamento histórico e retrocompatível).
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada nula/vazia resulta em AMBOS.
     */
    private RevisarLegendasUseCase.ModoReferenciaRevisao resolverModoReferencia(String modo) {
        return modo != null && "CACHE".equalsIgnoreCase(modo.trim())
            ? RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE
            : RevisarLegendasUseCase.ModoReferenciaRevisao.AMBOS;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: resolve a pasta de cache usada como referência no modo
     * Cache; nos demais casos mantém a pasta padrão do projeto.
     * <p>INVARIANTES DO DOMÍNIO: só o modo Cache respeita a pasta informada pelo
     * usuário; AMBOS sempre usa {@code cache}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: caminho inválido/ausente no modo Cache
     * volta ao padrão {@code cache}.
     */
    private Path resolverCacheDir(RevisarLegendasUseCase.ModoReferenciaRevisao referencia, String caminhoCache) {
        if (referencia == RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE) {
            Path escolhido = pipelineWebSupport.normalizarCaminho(caminhoCache);
            if (escolhido != null) {
                return escolhido;
            }
        }
        return Path.of("cache");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: no modo Cache, garante que a pasta informada existe, é
     * um diretório e contém ao menos um {@code .cache.json} antes de a fila iniciar
     * um job que não teria referência alguma.
     * <p>INVARIANTES DO DOMÍNIO: só valida no modo Cache; AMBOS não é afetado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve mensagem de erro (vira HTTP 400);
     * ausência de erro devolve {@link Optional#empty()}.
     */
    private Optional<String> validarCacheDirModo(
            RevisarLegendasUseCase.ModoReferenciaRevisao referencia, Path cacheDir) {
        if (referencia != RevisarLegendasUseCase.ModoReferenciaRevisao.CACHE) {
            return Optional.empty();
        }
        if (cacheDir == null || !java.nio.file.Files.isDirectory(cacheDir)) {
            return Optional.of("Pasta de cache inexistente ou não é um diretório: "
                + (cacheDir == null ? "(vazio)" : cacheDir.toString()));
        }
        try (java.util.stream.Stream<Path> walk = java.nio.file.Files.walk(cacheDir)) {
            boolean temCache = walk.filter(java.nio.file.Files::isRegularFile)
                .anyMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".cache.json"));
            if (!temCache) {
                return Optional.of("Nenhum arquivo .cache.json encontrado na pasta de cache: " + cacheDir);
            }
        } catch (java.io.IOException e) {
            return Optional.of("Falha ao ler a pasta de cache: " + e.getMessage());
        }
        return Optional.empty();
    }
}
