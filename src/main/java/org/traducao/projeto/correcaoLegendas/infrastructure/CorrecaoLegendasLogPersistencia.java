package org.traducao.projeto.correcaoLegendas.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;
import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;

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

    /**
     * PROPÓSITO DE NEGÓCIO: grava o relatório JSON da sessão de correção na pasta de relatórios.
     *
     * <p>INVARIANTES DO DOMÍNIO: recebe a pasta JÁ RESOLVIDA. Antes, resolvia-a chamando um
     * estático da fatia {@code telemetria} — uma aresta entre fatias funcionais nascendo daqui,
     * só para saber uma convenção de caminho. Quem conhece a convenção é o adaptador de
     * telemetria; este persistidor só escreve onde mandarem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException} — quem chama decide se a
     * ausência do relatório interrompe a operação (hoje não interrompe).
     *
     * @param pastaRelatorios pasta de destino, já resolvida pelo chamador
     * @param relatorio conteúdo completo da sessão
     * @return caminho absoluto do arquivo gravado
     */
    public Path salvarRelatorioJson(Path pastaRelatorios, CorrecaoLegendasRelatorioJson relatorio) throws IOException {
        Files.createDirectories(pastaRelatorios);
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path arquivo = pastaRelatorios.resolve("correcao_legendas_" + timestamp + ".json");
        objectMapper.writeValue(arquivo.toFile(), relatorio);
        return arquivo.toAbsolutePath();
    }
}
