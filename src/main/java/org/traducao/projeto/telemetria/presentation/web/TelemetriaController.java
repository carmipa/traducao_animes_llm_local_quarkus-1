package org.traducao.projeto.telemetria.presentation.web;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.traducao.projeto.core.presentation.web.RespostaPadrao;
import org.traducao.projeto.telemetria.TelemetriaResumo;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: expõe à interface web a telemetria acumulada do pipeline
 * — resumo consolidado para o painel, exportação segura do arquivo para download
 * e a publicação do dataset público sanitizado no repositório Git dedicado.
 *
 * <p>INVARIANTES DO DOMÍNIO: nenhuma URL, código HTTP ou nome de campo de DTO é
 * alterado em relação ao controller monolítico original; a pasta de cache é lida
 * diretamente da configuração {@code tradutor.diretorio-cache} (mesma chave e
 * default {@code cache} usados antes por {@code TradutorProperties.diretorioCache()},
 * preservando o fallback local para valor nulo/em branco); a exportação usa o
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

    @ConfigProperty(name = "tradutor.diretorio-cache", defaultValue = "cache")
    String diretorioCache;

    private final TelemetriaService telemetriaService;
    private final org.traducao.projeto.telemetria.TelemetriaDatasetService telemetriaDatasetService;
    private final org.traducao.projeto.telemetria.ConsolidadorTelemetriaPorFatia consolidador;

    public TelemetriaController(
            TelemetriaService telemetriaService,
            org.traducao.projeto.telemetria.TelemetriaDatasetService telemetriaDatasetService,
            org.traducao.projeto.telemetria.ConsolidadorTelemetriaPorFatia consolidador) {
        this.telemetriaService = telemetriaService;
        this.telemetriaDatasetService = telemetriaDatasetService;
        this.consolidador = consolidador;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: reúne a telemetria espalhada em {@code relatorios/}
     * num arquivo por fatia, sanitizada — é o que enche as abas do painel e o que
     * torna cada dataset publicável.
     *
     * <p>Medido em 06/08/2026: <b>6.488 das 6.601 operações</b> do acervo viviam
     * em pastas de relatório que o publicador nunca varreu, e só 85 chegaram ao
     * repositório público.
     *
     * <p>INVARIANTES DO DOMÍNIO: não toca nos arquivos de origem, e é idempotente
     * — rodar duas vezes não duplica.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 500 com a mensagem apenas em falha de
     * escrita do destino; arquivo de origem ilegível é pulado e contabilizado no
     * campo {@code arquivosPulados} da resposta.
     */
    @PostMapping("/telemetria/consolidar-fatias")
    public ResponseEntity<Map<String, Object>> consolidarFatias() {
        try {
            var r = consolidador.consolidar();
            return ResponseEntity.ok(Map.of(
                "arquivosLidos", r.arquivosLidos(),
                "arquivosPulados", r.arquivosPulados(),
                "operacoesLidas", r.operacoesLidas(),
                "operacoesDeTeste", r.operacoesDeTeste(),
                "operacoesGravadas", r.operacoesGravadas(),
                "porFatia", r.porFatia()));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).body(Map.of("erro", String.valueOf(e.getMessage())));
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: lista as fatias consolidadas com a contagem de cada
     * uma, para o painel montar as abas sem adivinhar nomes.
     *
     * <p>INVARIANTES DO DOMÍNIO: fatia sem arquivo consolidado NÃO aparece — aba
     * vazia não contribui com ninguém. Lista vazia significa "ainda não
     * consolidou", e a interface diz isso em vez de mostrar abas fantasma.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: 200 com lista vazia; ausência de
     * consolidação é estado legítimo, não erro.
     */
    @GetMapping("/telemetria/fatias")
    public ResponseEntity<Map<String, Object>> listarFatias() {
        return ResponseEntity.ok(Map.of("fatias", lerFatias(null)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: entrega as operações de UMA fatia, que é o conteúdo
     * de uma aba.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: fatia inexistente devolve 404, distinto
     * de fatia existente e vazia — que devolve 200 com lista vazia.
     */
    @GetMapping("/telemetria/fatias/{fatia}")
    public ResponseEntity<Map<String, Object>> obterFatia(@PathVariable String fatia) {
        List<Map<String, Object>> achadas = lerFatias(fatia);
        if (achadas.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "erro", "Fatia sem consolidado: " + fatia,
                "motivo", "NAO_CONSOLIDADA"));
        }
        return ResponseEntity.ok(achadas.get(0));
    }

    /**
     * Lê os consolidados de {@code logs/}. Quando {@code apenas} é informado,
     * devolve só aquela fatia.
     */
    private List<Map<String, Object>> lerFatias(String apenas) {
        List<Map<String, Object>> saida = new java.util.ArrayList<>();
        Path pasta = org.traducao.projeto.core.io.DiretorioBaseKronos.resolver("logs");
        if (!java.nio.file.Files.isDirectory(pasta)) {
            return saida;
        }
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        try (var fluxo = java.nio.file.Files.list(pasta)) {
            for (Path p : fluxo.filter(f -> f.getFileName().toString().startsWith("telemetria_fatia_")).toList()) {
                String nome = p.getFileName().toString()
                    .replace("telemetria_fatia_", "").replace(".json", "");
                if (apenas != null && !apenas.equalsIgnoreCase(nome)) {
                    continue;
                }
                try {
                    com.fasterxml.jackson.databind.JsonNode raiz = om.readTree(p.toFile());
                    com.fasterxml.jackson.databind.JsonNode ops = raiz.get("operacoes");
                    saida.add(Map.of(
                        "fatia", nome,
                        "total", ops == null ? 0 : ops.size(),
                        "operacoes", apenas == null ? List.of() : om.convertValue(ops, List.class)));
                } catch (java.io.IOException e) {
                    // Consolidado ilegível não pode derrubar as outras abas.
                    saida.add(Map.of("fatia", nome, "total", 0, "operacoes", List.of(),
                        "erro", "consolidado ilegivel"));
                }
            }
        } catch (java.io.IOException e) {
            return saida;
        }
        saida.sort((a, b) -> Integer.compare((Integer) b.get("total"), (Integer) a.get("total")));
        return saida;
    }

    /**
     * Retorna estatísticas acumuladas do TelemetriaService.
     * O TelemetriaService em si não tem getters (não é um DTO), por isso
     * o resumo é montado explicitamente em {@link TelemetriaResumo}.
     */
    @GetMapping("/telemetria")
    public ResponseEntity<TelemetriaResumo> obterTelemetria() {
        Path pastaCache = Path.of(diretorioCache != null && !diretorioCache.isBlank()
                ? diretorioCache : "cache");
        return ResponseEntity.ok(telemetriaService.gerarResumo(pastaCache));
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
