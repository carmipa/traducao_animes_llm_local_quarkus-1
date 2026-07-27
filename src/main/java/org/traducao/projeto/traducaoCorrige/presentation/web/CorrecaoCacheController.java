package org.traducao.projeto.traducaoCorrige.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.OperacaoRequest;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;
import org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase;
import org.traducao.projeto.raspagemRevisao.application.RevisarCacheUseCase;
import org.traducao.projeto.llm.domain.StatusLlm;
import org.traducao.projeto.llm.domain.LlmPort;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.traducaoCorrige.application.LimparCacheUseCase;
import org.traducao.projeto.traducaoCorrige.application.ReforcarTerminologiaCacheUseCase;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoReforcoTerminologia;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe à interface web os três modos de manutenção do
 * banco de cache de tradução — limpeza/auditoria local, preenchimento online de
 * lacunas via Google Translate e revisão gramatical via LLM local.
 *
 * <p>INVARIANTES DO DOMÍNIO: usa a MESMA fila compartilhada via
 * {@link PipelineWebSupport}; o contexto informado, quando presente, é validado
 * antes de enfileirar; a revisão via LLM só prossegue com modelo carregado;
 * nenhuma URL, código HTTP ou nome de campo de DTO é alterado em relação ao
 * controller monolítico original.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: caminho de cache ou contexto inválido
 * retorna HTTP 400; indisponibilidade do LLM e falhas do job aparecem no console
 * SSE, sem derrubar a fila.
 */
@RestController
@RequestMapping("/api")
public class CorrecaoCacheController {

    private static final Logger log = LoggerFactory.getLogger(CorrecaoCacheController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final LimparCacheUseCase limparCacheUseCase;
    private final CorrigirComGoogleUseCase corrigirComGoogleUseCase;
    private final RevisarCacheUseCase revisarCacheUseCase;
    private final GerenciadorContexto gerenciadorContexto;
    private final LlmPort llmPort;
    private final ReforcarTerminologiaCacheUseCase reforcarTerminologiaCacheUseCase;

    public CorrecaoCacheController(
            PipelineWebSupport pipelineWebSupport,
            LimparCacheUseCase limparCacheUseCase,
            CorrigirComGoogleUseCase corrigirComGoogleUseCase,
            RevisarCacheUseCase revisarCacheUseCase,
            GerenciadorContexto gerenciadorContexto,
            LlmPort llmPort,
            ReforcarTerminologiaCacheUseCase reforcarTerminologiaCacheUseCase) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.limparCacheUseCase = limparCacheUseCase;
        this.corrigirComGoogleUseCase = corrigirComGoogleUseCase;
        this.revisarCacheUseCase = revisarCacheUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
        this.llmPort = llmPort;
        this.reforcarTerminologiaCacheUseCase = reforcarTerminologiaCacheUseCase;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a limpeza segura da pasta persistente de cache.
     * <p>INVARIANTES DO DOMÍNIO: caminho e contexto informado são validados antes da fila.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna 400 para entrada inválida; falhas do job aparecem no console/status final.
     */
    @PostMapping("/corrigir-cache")
    public ResponseEntity<RespostaPadrao> limparCache(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = pipelineWebSupport.normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        pipelineWebSupport.submeterJobComRelatorio("correcao", "Limpeza e Auditoria de Cache", () -> {
            try {
                ResultadoManutencaoCache resultado = limparCacheUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("LIMPEZA E AUDITORIA DE CACHE", resultado);
            } catch (Exception e) {
                log.error("Erro ao limpar cache", e);
                System.out.println("\u001B[31m[ERRO] Limpeza do cache falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Limpeza de cache aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: ENSAIA o reforço de terminologia sobre o cache já gravado — mede o que
     * a lore mudaria no acervo sem tocar um byte. É a rota que o operador usa para decidir.
     *
     * <p>INVARIANTES DO DOMÍNIO: entra na MESMA fila serial das outras operações; nada é escrito.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 400 para caminho/contexto inválido; falhas por arquivo
     * aparecem no console sem derrubar o lote.
     */
    @PostMapping("/reforcar-terminologia-ensaio")
    public ResponseEntity<RespostaPadrao> ensaiarReforcoTerminologia(@RequestBody OperacaoRequest req) {
        return submeterReforco(req, false);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: APLICA o reforço de terminologia ao acervo, com backup e escrita
     * atômica por arquivo.
     *
     * <p>INVARIANTES DO DOMÍNIO: é uma ROTA SEPARADA do ensaio, de propósito. Escrever sobre
     * trabalho já pronto não pode ser um booleano que alguém inverte por engano num corpo de
     * requisição — a ação destrutiva tem endereço próprio e só é alcançada quando se quis chegar
     * nela.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: idêntico ao ensaio; arquivo cuja gravação falha
     * permanece intacto e não entra na contagem.
     */
    @PostMapping("/reforcar-terminologia-aplicar")
    public ResponseEntity<RespostaPadrao> aplicarReforcoTerminologia(@RequestBody OperacaoRequest req) {
        return submeterReforco(req, true);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: valida e enfileira o reforço, no ensaio ou na aplicação.
     * <p>INVARIANTES DO DOMÍNIO: mesma validação das demais rotas; o job vai para a fila serial.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve 400 antes de enfileirar.
     */
    private ResponseEntity<RespostaPadrao> submeterReforco(OperacaoRequest req, boolean aplicar) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = pipelineWebSupport.normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        String rotulo = aplicar ? "Reforço de Terminologia (APLICANDO)" : "Reforço de Terminologia (ensaio)";
        pipelineWebSupport.submeterJobComRelatorio("correcao", rotulo, () -> {
            try {
                imprimirResultadoReforco(
                    reforcarTerminologiaCacheUseCase.executar(pathCache, req.contextoId(), aplicar));
            } catch (Exception e) {
                log.error("Erro no reforço de terminologia", e);
                System.out.println("\u001B[31m[ERRO] Reforço de terminologia falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            rotulo + " aceito pela fila. O resultado por termo aparecerá no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: imprime o resultado do reforço com o contador POR TERMO, que é a razão
     * de a operação existir — é ele que separa "o modelo acertou" de "o enforcer consertou".
     * <p>INVARIANTES DO DOMÍNIO: o modo (ensaio/aplicado) aparece antes dos números, para nunca
     * se ler um ensaio como se o acervo tivesse sido alterado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: só imprime; não decide nada.
     */
    private void imprimirResultadoReforco(ResultadoReforcoTerminologia r) {
        System.out.println("\n" + AnsiCores.CYAN + "REFORÇO DE TERMINOLOGIA — "
            + (r.aplicado() ? "APLICADO NO ACERVO" : "ENSAIO (nada foi escrito)") + AnsiCores.RESET);
        System.out.println("  Status: " + r.status());
        System.out.println("  Arquivos analisados: " + r.arquivosAnalisados()
            + " | alterados: " + r.arquivosAlterados()
            + " | pulados (obra não verificável): " + r.arquivosNaoVerificaveis()
            + " | falhas: " + r.falhas());
        System.out.println("  Falas alteradas: " + r.falasAlteradas()
            + " | restaurações: " + r.totalRestauracoes());
        r.porFrequencia().forEach((termo, n) -> System.out.println("    " + n + "x  " + termo));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita o preenchimento online de lacunas do cache.
     * <p>INVARIANTES DO DOMÍNIO: somente contexto conhecido entra na fila; o uso online é explícito.
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna 400 antes da fila ou registra falha real no console do job.
     */
    @PostMapping("/corrigir-scraping")
    public ResponseEntity<RespostaPadrao> corrigirScraping(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = pipelineWebSupport.normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }
        if (req.contextoId() != null && !req.contextoId().isBlank()
            && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        pipelineWebSupport.submeterJobComRelatorio("correcao", "Correção via Google Translate", () -> {
            try {
                ResultadoManutencaoCache resultado = corrigirComGoogleUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("CORREÇÃO ONLINE VIA GOOGLE TRANSLATE", resultado);
            } catch (Exception e) {
                log.error("Erro ao executar scraping", e);
                System.out.println("\u001B[31m[ERRO] Raspagem do Google falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Correção online aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a revisão de concordância do cache via LLM local.
     * <p>INVARIANTES DO DOMÍNIO: contexto é validado e disponibilidade do modelo é checada antes da revisão.
     * <p>COMPORTAMENTO EM CASO DE FALHA: rejeita contexto inválido e registra indisponibilidade/status parcial no console.
     */
    @PostMapping("/revisar-cache")
    public ResponseEntity<RespostaPadrao> revisarCache(@RequestBody OperacaoRequest req) {
        String cacheDir = req.entrada() != null && !req.entrada().isBlank() ? req.entrada() : "cache";
        Path pathCache = pipelineWebSupport.normalizarCaminho(cacheDir);
        if (pathCache == null) {
            return ResponseEntity.badRequest().body(new RespostaPadrao("Caminho de cache inválido: " + cacheDir));
        }

        if (req.contextoId() != null && !req.contextoId().isBlank() && !gerenciadorContexto.existeContexto(req.contextoId())) {
            return ResponseEntity.badRequest().body(new RespostaPadrao(
                "Contexto desconhecido: \"" + req.contextoId() + "\"."));
        }

        pipelineWebSupport.submeterJobComRelatorio("correcao", "Revisão Gramatical do Cache (LLM)", () -> {
            try {
                StatusLlm status = llmPort.verificarDisponibilidade();
                if (!status.modeloCarregado()) {
                    System.out.println("\u001B[31m[FAIL] LLM indisponível para revisão: "
                        + status.mensagem() + "\u001B[0m");
                    return;
                }
                ResultadoManutencaoCache resultado = revisarCacheUseCase.executar(pathCache, req.contextoId());
                imprimirResultadoCache("REVISÃO GRAMATICAL DO CACHE", resultado);
            } catch (Exception e) {
                log.error("Erro na revisão gramatical do cache", e);
                System.out.println("\u001B[31m[ERRO] Revisão gramatical falhou: " + e.getMessage() + "\u001B[0m");
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Revisão local aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apresenta no console web o desfecho real dos três
     * modos de manutenção do banco de cache, incluindo falhas e cancelamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente {@code CONCLUIDO} usa banner verde;
     * qualquer outro status informa que o resultado exige atenção; a orientação
     * de avançar à Opção 6 aparece após toda execução que altera o cache.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado nulo é tratado como falha e
     * não provoca {@link NullPointerException} no job de background.
     */
    private void imprimirResultadoCache(String operacao, ResultadoManutencaoCache resultado) {
        if (resultado == null) {
            System.out.println(AnsiCores.RED + "[FALHA] " + operacao + " não retornou resultado." + AnsiCores.RESET);
            return;
        }
        String resumo = operacao + " — status=" + resultado.status()
            + ", arquivos=" + resultado.arquivosAnalisados()
            + ", alterados=" + resultado.arquivosAlterados()
            + ", corrigidos=" + resultado.itensCorrigidos()
            + ", pendentes=" + resultado.itensPendentes()
            + ", falhas=" + resultado.falhas();
        if ("CONCLUIDO".equals(resultado.status())) {
            System.out.println(AnsiCores.GREEN + "[SUCESSO] " + resumo + AnsiCores.RESET);
        } else {
            System.out.println(AnsiCores.YELLOW + "[ATENÇÃO] " + resumo + AnsiCores.RESET);
        }
        if (resultado.arquivosAlterados() > 0) {
            System.out.println(AnsiCores.CYAN
                + "[PRÓXIMO PASSO] Avance para a Opção 6. Ela sincronizará este cache mais novo no ASS antes da revisão."
                + AnsiCores.RESET);
        }
    }
}
