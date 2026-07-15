package org.traducao.projeto.traducao.presentation.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.telemetria.TelemetriaResumo;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducao.infrastructure.config.TradutorProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * PROPÓSITO DE NEGÓCIO: expõe à interface web a telemetria acumulada do pipeline
 * — resumo consolidado para o painel, exportação segura do arquivo para download
 * e a publicação do dataset público sanitizado no repositório Git dedicado.
 *
 * <p>INVARIANTES DO DOMÍNIO: nenhuma URL, código HTTP ou nome de campo de DTO é
 * alterado em relação ao controller monolítico original; a exportação usa o
 * arquivo canônico e a publicação delega ao serviço de dataset já sanitizado.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: exportação sem arquivo retorna 404 e falha
 * de leitura retorna 500; falha na publicação do dataset retorna 500 com a
 * mensagem do erro no corpo padrão.
 */
@RestController
@RequestMapping("/api")
public class TelemetriaController {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaController.class);

    private final TelemetriaService telemetriaService;
    private final org.traducao.projeto.telemetria.TelemetriaDatasetService telemetriaDatasetService;
    private final TradutorProperties propriedades;

    public TelemetriaController(
            TelemetriaService telemetriaService,
            org.traducao.projeto.telemetria.TelemetriaDatasetService telemetriaDatasetService,
            TradutorProperties propriedades) {
        this.telemetriaService = telemetriaService;
        this.telemetriaDatasetService = telemetriaDatasetService;
        this.propriedades = propriedades;
    }

    /**
     * Retorna estatísticas acumuladas do TelemetriaService.
     * O TelemetriaService em si não tem getters (não é um DTO), por isso
     * o resumo é montado explicitamente em {@link TelemetriaResumo}.
     */
    @GetMapping("/telemetria")
    public ResponseEntity<TelemetriaResumo> obterTelemetria() {
        Path diretorioCache = Path.of(propriedades.diretorioCache() != null && !propriedades.diretorioCache().isBlank()
                ? propriedades.diretorioCache() : "cache");
        return ResponseEntity.ok(telemetriaService.gerarResumo(diretorioCache));
    }

    /**
     * Exportação segura do arquivo de telemetria para download (Higienizado)
     */
    @GetMapping("/telemetria/exportar")
    public ResponseEntity<byte[]> exportarTelemetria() {
        try {
            Path arquivoTelemetria = TelemetriaService.resolverArquivoTelemetriaCanonico();
            if (!Files.exists(arquivoTelemetria)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(arquivoTelemetria);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"kronos_telemetria_segura.json\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .contentLength(fileContent.length)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("Erro ao exportar telemetria para download", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Publica a telemetria sanitizada como dataset público no repositório Git
     * dedicado ({@code kronos-anime-translation-telemetry-dataset}): snapshot em
     * {@code metrics/}, commit e push. Síncrono — o push leva poucos segundos
     * e o resultado volta na própria resposta para o painel exibir.
     */
    @PostMapping("/telemetria/publicar-dataset")
    public ResponseEntity<?> publicarDatasetTelemetria() {
        try {
            var resultado = telemetriaDatasetService.publicar();
            log.info("Publicação do dataset de telemetria: {}", resultado.mensagem());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Falha ao publicar o dataset de telemetria", e);
            return ResponseEntity.internalServerError()
                .body(new RespostaPadrao("Falha ao publicar o dataset: " + e.getMessage()));
        }
    }
}
