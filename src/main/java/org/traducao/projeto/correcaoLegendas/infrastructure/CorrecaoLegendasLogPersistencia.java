package org.traducao.projeto.correcaoLegendas.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;
import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CorrecaoLegendasLogPersistencia {

    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;

    public CorrecaoLegendasLogPersistencia() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path salvarRelatorioJson(Path pastaEntrada, CorrecaoLegendasRelatorioJson relatorio) throws IOException {
        Path pastaRelatorios = TelemetriaService.resolverPastaRelatorios(pastaEntrada);
        Files.createDirectories(pastaRelatorios);
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path arquivo = pastaRelatorios.resolve("correcao_legendas_" + timestamp + ".json");
        objectMapper.writeValue(arquivo.toFile(), relatorio);
        return arquivo.toAbsolutePath();
    }
}
