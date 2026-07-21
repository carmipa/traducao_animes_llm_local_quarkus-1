package org.traducao.projeto.traducao.infrastructure.contextocena;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.traducao.domain.contextocena.RegistroExecucaoContextoCena;
import org.traducao.projeto.traducao.domain.ports.RelatorioContextoCenaPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * PROPÓSITO DE NEGÓCIO: única escritora do relatório A/B da correção de gênero por contexto de
 * cena ({@code logs/contexto_cena_ab.jsonl}). Cada execução de episódio acrescenta UMA linha
 * JSON (JSONL) — append-only, ao contrário da telemetria canônica que deduplica por episódio.
 * É o que permite o braço A (flag desligada) e o braço B (flag ligada) do MESMO episódio
 * coexistirem no arquivo para comparação posterior. Carimba o envelope de execução (runId do
 * processo + instante) sobre a medição pura recebida da aplicação.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Append-only: cada {@link #registrar} adiciona uma linha ao final; nunca deduplica,
 *       nunca sobrescreve linha anterior.</li>
 *   <li>Uma medição por linha, JSON compacto (uma linha física), independentemente de o
 *       {@code ObjectMapper} global estar configurado com indentação — a cópia local desliga
 *       {@code INDENT_OUTPUT} para garantir o formato JSONL.</li>
 *   <li>{@code runId} é estável dentro do processo (uma invocação da CLI): as execuções do
 *       mesmo processo compartilham o id; braços A e B rodados em invocações distintas recebem
 *       ids distintos e são pareados por {@code episodio}.</li>
 *   <li>Sincronização de escopo JVM: a escrita é {@code synchronized}. Não há coordenação entre
 *       processos — assume-se uma única instância acrescentando ao arquivo por vez.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Observabilidade nunca derruba a tradução: qualquer {@link IOException} ao serializar/gravar é
 * registrada em log e absorvida — o episódio segue seu curso. Uma linha malformada nunca é
 * escrita pela metade porque a serialização ocorre inteira em memória antes do append.
 */
@Component
public class RelatorioContextoCenaAdapter implements RelatorioContextoCenaPort {

    private static final Logger log = LoggerFactory.getLogger(RelatorioContextoCenaAdapter.class);

    private static final String NOME_ARQUIVO = "contexto_cena_ab.jsonl";
    private static final String SUBPASTA = "logs";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /** Id do processo (uma invocação da CLI): estável em toda a vida da JVM. */
    private static final String RUN_ID = UUID.randomUUID().toString().substring(0, 8);

    private final ObjectMapper escritorJsonl;

    public RelatorioContextoCenaAdapter(ObjectMapper objectMapper) {
        // Cópia com INDENT_OUTPUT desligado: garante uma medição por linha física (JSONL),
        // mesmo que o mapper global esteja indentando em algum ambiente.
        this.escritorJsonl = objectMapper.copy().disable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public synchronized void registrar(RegistroExecucaoContextoCena registro) {
        if (registro == null) {
            return;
        }
        LinhaRelatorio linha = new LinhaRelatorio(RUN_ID, LocalDateTime.now().format(TS), registro);
        try {
            String json = escritorJsonl.writeValueAsString(linha) + System.lineSeparator();
            Path pasta = DiretorioBaseKronos.resolver(SUBPASTA);
            Files.createDirectories(pasta);
            Files.writeString(pasta.resolve(NOME_ARQUIVO), json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Falha ao acrescentar linha no relatorio A/B do contexto de cena: {}", e.getMessage(), e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: envelope de uma linha do JSONL — carimbo de execução (runId,
     * instante) sobre a medição pura do domínio. Existe só na infraestrutura, onde Jackson é
     * permitido; o domínio ({@link RegistroExecucaoContextoCena}) permanece puro.
     * <p>INVARIANTES DO DOMÍNIO: portador de dados; a serialização produz um objeto JSON único.
     * <p>COMPORTAMENTO EM CASO DE FALHA: não valida.
     */
    private record LinhaRelatorio(String runId, String instante, RegistroExecucaoContextoCena medicao) {
    }
}
