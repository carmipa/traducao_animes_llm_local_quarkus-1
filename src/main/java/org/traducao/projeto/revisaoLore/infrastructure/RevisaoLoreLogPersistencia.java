package org.traducao.projeto.revisaoLore.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.domain.RevisaoLoreRelatorioJson;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Persiste relatorio e log de sessao da revisao de lore exclusivamente em JSON.
 */
@Component
public class RevisaoLoreLogPersistencia {

    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ObjectMapper objectMapper;

    public RevisaoLoreLogPersistencia() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public Path salvarRelatorioJson(Path pastaEntrada, RevisaoLoreRelatorioJson relatorio) throws IOException {
        Path pastaRelatorios = TelemetriaService.resolverPastaRelatorios(pastaEntrada);
        Files.createDirectories(pastaRelatorios);
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path arquivo = pastaRelatorios.resolve("revisao_lore_" + timestamp + ".json");
        objectMapper.writeValue(arquivo.toFile(), relatorio);
        return arquivo.toAbsolutePath();
    }
}
