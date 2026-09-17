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

    // Com milissegundos (_SSS), nao so segundos: duas execucoes SEM_ARQUIVOS no MESMO segundo
    // sobrescreviam o relatorio uma da outra. O backup ja usava _SSS pelo mesmo motivo.
    private static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final ObjectMapper objectMapper;

    public RevisaoLoreLogPersistencia() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: grava o relatório JSON de UMA sessão de revisão sem nunca
     * sobrescrever o de outra sessão — o relatório é a evidência daquela execução.
     *
     * <p>INVARIANTES DO DOMÍNIO: o nome carrega o carimbo com milissegundos; se ainda assim
     * colidir (mesmo instante), um sufixo sequencial garante um caminho novo. Milissegundo +
     * catraca de existência = nome único mesmo no pior caso, sem depender do relógio ter
     * resolução suficiente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException} ao chamador, que degrada
     * para telemetria em memória; nunca sobrescreve arquivo existente.
     */
    public Path salvarRelatorioJson(Path pastaEntrada, RevisaoLoreRelatorioJson relatorio) throws IOException {
        Path pastaRelatorios = TelemetriaService.resolverPastaRelatorios(pastaEntrada);
        Files.createDirectories(pastaRelatorios);
        Path arquivo = caminhoUnico(pastaRelatorios, TIMESTAMP.format(LocalDateTime.now()));
        objectMapper.writeValue(arquivo.toFile(), relatorio);
        return arquivo.toAbsolutePath();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe um nome de arquivo que ainda não existe, para o relatório de
     * uma execução nunca sobrescrever o de outra.
     *
     * <p>INVARIANTES DO DOMÍNIO: retorna o nome base quando livre; se ocupado, acrescenta um
     * sufixo sequencial a partir de 2 até achar um livre. Não depende de o relógio ter resolução
     * suficiente — a catraca de existência fecha o buraco que os milissegundos só reduzem.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: método puro sobre o sistema de arquivos; não escreve nem
     * lança (a escrita é do chamador).
     */
    static Path caminhoUnico(Path pastaRelatorios, String timestamp) {
        Path arquivo = pastaRelatorios.resolve("revisao_lore_" + timestamp + ".json");
        for (int seq = 2; Files.exists(arquivo); seq++) {
            arquivo = pastaRelatorios.resolve("revisao_lore_" + timestamp + "_" + seq + ".json");
        }
        return arquivo;
    }
}
