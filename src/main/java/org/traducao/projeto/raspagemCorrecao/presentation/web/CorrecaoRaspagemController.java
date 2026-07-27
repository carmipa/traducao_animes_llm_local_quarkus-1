package org.traducao.projeto.raspagemCorrecao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.contexto.infrastructure.GerenciadorContexto;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.core.presentation.web.OperacaoRequest;
import org.traducao.projeto.core.presentation.web.PipelineWebSupport;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;
import org.traducao.projeto.raspagemCorrecao.application.CorrigirComGoogleUseCase;
import org.traducao.projeto.traducaoCorrige.domain.ResultadoManutencaoCache;

import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe à interface web o preenchimento online de lacunas do cache via
 * Google Translate — a operação desta fatia.
 *
 * <p>É o PRIMEIRO controller de {@code raspagemCorrecao}. A rota {@code /api/corrigir-scraping}
 * morava no {@code CorrecaoCacheController} de {@code traducaoCorrige}, e era metade da perna de
 * volta do ciclo {@code raspagemCorrecao ⇄ traducaoCorrige}: um controller de uma fatia injetando
 * o caso de uso de outra. Era a única violação do padrão em 21 controllers do projeto.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A URL, o método, o corpo do DTO e o código HTTP são IDÊNTICOS aos de antes. O prefixo
 *       {@code /api} é o mesmo em todos os controllers, então trocar a classe que hospeda a rota
 *       não muda o endereço — o frontend não sabe que isto aconteceu, e não deve saber.</li>
 *   <li>O canal de job continua sendo {@code "correcao"}. Ele NÃO é o nome da fatia: é o console
 *       SSE que o painel de Correção escuta. Renomear para {@code "raspagem"} "por coerência" faria
 *       o job sumir do console sem quebrar nenhum HTTP — o operador clicaria e veria silêncio.</li>
 *   <li>Usa a MESMA fila serial das demais operações, via {@link PipelineWebSupport}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Caminho ou contexto inválido devolve HTTP 400 antes de enfileirar; falha durante o job aparece no
 * console SSE sem derrubar a fila.
 */
@RestController
@RequestMapping("/api")
public class CorrecaoRaspagemController {

    private static final Logger log = LoggerFactory.getLogger(CorrecaoRaspagemController.class);

    private final PipelineWebSupport pipelineWebSupport;
    private final CorrigirComGoogleUseCase corrigirComGoogleUseCase;
    private final GerenciadorContexto gerenciadorContexto;

    /**
     * PROPÓSITO DE NEGÓCIO: compõe a rota com a fila, o caso de uso e o registro de contextos.
     * <p>INVARIANTES DO DOMÍNIO: só depende do {@code application} da PRÓPRIA fatia, do
     * {@code core} e de peers — que é a regra que este controller existe para restaurar.
     * <p>COMPORTAMENTO EM CASO DE FALHA: dependência ausente impede a subida do contexto web.
     */
    public CorrecaoRaspagemController(
            PipelineWebSupport pipelineWebSupport,
            CorrigirComGoogleUseCase corrigirComGoogleUseCase,
            GerenciadorContexto gerenciadorContexto) {
        this.pipelineWebSupport = pipelineWebSupport;
        this.corrigirComGoogleUseCase = corrigirComGoogleUseCase;
        this.gerenciadorContexto = gerenciadorContexto;
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
                System.out.println(AnsiCores.RED + "[ERRO] Raspagem do Google falhou: "
                    + e.getMessage() + AnsiCores.RESET);
            }
        });

        return ResponseEntity.ok(new RespostaPadrao(
            "Correção online aceita pela fila. A conclusão e o status real aparecerão no console."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: apresenta no console web o desfecho real da operação, incluindo falhas
     * e cancelamento.
     *
     * <p>INVARIANTES DO DOMÍNIO: somente {@code CONCLUIDO} usa banner verde; qualquer outro status
     * informa que o resultado exige atenção; a orientação de avançar à Opção 6 aparece após toda
     * execução que altera o cache — sem ela o operador não sabe que precisa sincronizar o ASS.
     *
     * <p>É CÓPIA CONSCIENTE do helper homônimo das outras duas fatias que imprimem este mesmo
     * resumo. A alternativa era pôr o formatador em {@code core}, e ele consome um tipo de domínio
     * de {@code traducaoCorrige} — o kernel passaria a conhecer uma fatia, que o contrato proíbe.
     * Vinte linhas de apresentação duplicadas custam menos que essa inversão.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: resultado nulo é tratado como falha e não provoca
     * {@link NullPointerException} no job de background.
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
