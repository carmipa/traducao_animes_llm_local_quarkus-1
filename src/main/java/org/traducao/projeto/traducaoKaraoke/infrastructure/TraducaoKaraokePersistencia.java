package org.traducao.projeto.traducaoKaraoke.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.traducao.projeto.telemetria.TelemetriaService;
import org.traducao.projeto.traducaoKaraoke.domain.ResultadoTraducaoKaraoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manifesto de auditoria da tradução de karaokê: registra, por execução, o
 * que foi preservado/traduzido em cada arquivo. Fica em
 * {@code logs/traducao-karaoke/manifestos} — junto com os originais intocados
 * na pasta de origem e o cache JSON editável, é a trilha completa para
 * auditar (ou refazer) qualquer tradução de letra.
 */
@ApplicationScoped
public class TraducaoKaraokePersistencia {

    private static final Path PASTA_MANIFESTOS =
        TelemetriaService.resolverPastaArtefatosOperacionais("traducao-karaoke").resolve("manifestos");
    private static final DateTimeFormatter FORMATO_CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Inject
    ObjectMapper objectMapper;

    public Path salvarManifesto(
        Path pastaOrigem,
        Path pastaDestino,
        List<ResultadoTraducaoKaraoke> resultados,
        long duracaoMs
    ) throws IOException {
        Files.createDirectories(PASTA_MANIFESTOS);

        Map<String, Object> manifesto = new LinkedHashMap<>();
        manifesto.put("executadoEm", LocalDateTime.now().toString());
        manifesto.put("pastaOrigem", pastaOrigem.toAbsolutePath().toString());
        manifesto.put("pastaDestino", pastaDestino.toAbsolutePath().toString());
        manifesto.put("duracaoMs", duracaoMs);

        List<Map<String, Object>> arquivos = resultados.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("arquivo", r.arquivo());
            item.put("arquivoDestino", r.arquivoDestino());
            item.put("eventosTotais", r.eventosTotais());
            item.put("efeitosKfxPreservados", r.efeitosKfxPreservados());
            item.put("preservadasOriginalJapones", r.preservadasOriginalJapones());
            item.put("jaEmPortugues", r.jaEmPortugues());
            item.put("paraTraduzir", r.paraTraduzir());
            item.put("reaproveitadasCache", r.reaproveitadasCache());
            item.put("traduzidas", r.traduzidas());
            item.put("mantidasSemTraducao", r.mantidasSemTraducao());
            item.put("avisos", r.avisos());
            return item;
        }).toList();
        manifesto.put("arquivos", arquivos);

        Path destino = PASTA_MANIFESTOS.resolve(
            "kronos_traducao_karaoke_" + LocalDateTime.now().format(FORMATO_CARIMBO) + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(destino.toFile(), manifesto);
        return destino;
    }
}
