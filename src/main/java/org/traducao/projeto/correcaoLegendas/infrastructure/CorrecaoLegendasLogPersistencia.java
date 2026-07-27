package org.traducao.projeto.correcaoLegendas.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;
import org.traducao.projeto.correcaoLegendas.domain.CorrecaoLegendasRelatorioJson;
import org.traducao.projeto.correcaoLegendas.domain.ports.RelatorioCorrecaoLegendasPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PROPÓSITO DE NEGÓCIO: grava em disco o relatório JSON da sessão de correção de legendas.
 *
 * <p>É o ADAPTADOR da {@link RelatorioCorrecaoLegendasPort}: o Jackson e o sistema de arquivos
 * vivem AQUI, e o caso de uso passa a conhecer só o contrato.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Escreve onde mandarem — não resolve caminho, não conhece convenção de pasta.</li>
 *   <li>O carimbo de tempo do NOME do arquivo é gerado aqui, e é o único do projeto: o relatório
 *       tem o seu próprio instante e não precisa casar com o da telemetria.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Propaga {@link IOException} — ver o contrato da porta para a razão.
 */
@Component
public class CorrecaoLegendasLogPersistencia implements RelatorioCorrecaoLegendasPort {

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
    @Override
    public Path salvarRelatorioJson(Path pastaRelatorios, CorrecaoLegendasRelatorioJson relatorio) throws IOException {
        Files.createDirectories(pastaRelatorios);
        String timestamp = TIMESTAMP.format(LocalDateTime.now());
        Path arquivo = pastaRelatorios.resolve("correcao_legendas_" + timestamp + ".json");
        objectMapper.writeValue(arquivo.toFile(), relatorio);
        return arquivo.toAbsolutePath();
    }
}
