package org.traducao.projeto.traducao.infrastructure.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.core.util.ArquivoAtomicoUtil;
import org.traducao.projeto.traducao.domain.NormalizadorNomeEpisodio;
import org.traducao.projeto.traducao.domain.TelemetriaTraducao;
import org.traducao.projeto.traducao.domain.TelemetriaTraducaoDocumento;
import org.traducao.projeto.traducao.domain.ports.TelemetriaTraducaoPort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROPÓSITO DE NEGÓCIO: única escritora do arquivo canônico próprio da telemetria
 * da Tradução Local ({@code logs/telemetria_traducao.json}). Projeta, por episódio,
 * o estado final consolidado das traduções e mantém os quatro contadores da fatia,
 * isolando a Tradução Local do módulo de telemetria (o painel apenas lê este arquivo).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Deduplicação por nome de episódio normalizado ({@link NormalizadorNomeEpisodio});
 *       o registro mais recente substitui o anterior — nunca append-only.</li>
 *   <li>Os quatro contadores iniciam em zero e acumulam SOMENTE eventos da Tradução
 *       Local após a adoção deste arquivo; jamais copiam valores do legado.</li>
 *   <li>Cada mutação persiste o documento inteiro (registros + contadores) como uma
 *       ÚNICA alteração lógica, via escrita atômica (temporário no mesmo diretório +
 *       movimentação segura).</li>
 *   <li>Sincronização de escopo JVM: as mutações são {@code synchronized}. Não há
 *       coordenação entre processos — assume-se uma única instância escrevendo o arquivo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Um arquivo existente ilegível NÃO é destruído silenciosamente: é preservado com
 * sufixo {@code .corrompido} antes de recomeçar o estado. Falha de I/O ao persistir
 * é registrada, mantendo o estado em memória coerente para a próxima escrita.
 */
@Component
public class TelemetriaTraducaoAdapter implements TelemetriaTraducaoPort {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaTraducaoAdapter.class);

    // 1.1: acrescenta pendenciasPorCausa (KPI estruturado) a cada registro; aditivo e
    // retrocompatível — arquivos 1.0 sao lidos com o campo ausente como null.
    private static final String SCHEMA_VERSION = "1.1";
    private static final String NOME_ARQUIVO = "telemetria_traducao.json";
    private static final String SUBPASTA = "logs";

    private final ObjectMapper objectMapper;
    private final Map<String, TelemetriaTraducao> banco = new LinkedHashMap<>();
    private final AtomicInteger alucinacoesPrevenidas = new AtomicInteger(0);
    private final AtomicInteger respostasTraducaoRejeitadas = new AtomicInteger(0);
    private final AtomicInteger falhasTraducaoRecuperadas = new AtomicInteger(0);
    private final AtomicInteger fallbacksTraducaoMantidos = new AtomicInteger(0);

    public TelemetriaTraducaoAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public synchronized void carregar() {
        Path arquivo = arquivo();
        if (!Files.exists(arquivo)) {
            return;
        }
        try {
            TelemetriaTraducaoDocumento doc = objectMapper.readValue(arquivo.toFile(), TelemetriaTraducaoDocumento.class);
            if (doc == null) {
                return;
            }
            if (doc.registros() != null) {
                for (TelemetriaTraducao t : doc.registros()) {
                    banco.put(NormalizadorNomeEpisodio.normalizar(t.nomeEpisodio()), t);
                }
            }
            alucinacoesPrevenidas.set(doc.alucinacoesPrevenidas());
            respostasTraducaoRejeitadas.set(doc.respostasTraducaoRejeitadas());
            falhasTraducaoRecuperadas.set(doc.falhasTraducaoRecuperadas());
            fallbacksTraducaoMantidos.set(doc.fallbacksTraducaoMantidos());
            log.info("Telemetria da Traducao Local carregada: {} episodio(s) de {}.", banco.size(), arquivo);
        } catch (IOException e) {
            preservarCorrompido(arquivo, e);
        }
    }

    @Override
    public synchronized void registrarTraducao(TelemetriaTraducao telemetria) {
        if (telemetria == null) {
            return;
        }
        banco.put(NormalizadorNomeEpisodio.normalizar(telemetria.nomeEpisodio()), telemetria);
        persistir();
    }

    @Override
    public synchronized void registrarAlucinacaoPrevenida() {
        alucinacoesPrevenidas.incrementAndGet();
        persistir();
    }

    @Override
    public synchronized void registrarRespostaTraducaoRejeitada() {
        respostasTraducaoRejeitadas.incrementAndGet();
        persistir();
    }

    @Override
    public synchronized void registrarFalhaTraducaoRecuperada() {
        falhasTraducaoRecuperadas.incrementAndGet();
        persistir();
    }

    @Override
    public synchronized void registrarFallbackMantido() {
        fallbacksTraducaoMantidos.incrementAndGet();
        persistir();
    }

    private void persistir() {
        try {
            Path pasta = pasta();
            Files.createDirectories(pasta);
            Path arquivo = pasta.resolve(NOME_ARQUIVO);
            TelemetriaTraducaoDocumento doc = new TelemetriaTraducaoDocumento(
                SCHEMA_VERSION,
                new ArrayList<>(banco.values()),
                alucinacoesPrevenidas.get(),
                respostasTraducaoRejeitadas.get(),
                falhasTraducaoRecuperadas.get(),
                fallbacksTraducaoMantidos.get());
            Path temporario = pasta.resolve(NOME_ARQUIVO + ".tmp");
            objectMapper.writeValue(temporario.toFile(), doc);
            ArquivoAtomicoUtil.substituirAtomico(temporario, arquivo);
        } catch (IOException e) {
            log.error("Falha ao persistir a telemetria da Traducao Local: {}", e.getMessage(), e);
        }
    }

    private void preservarCorrompido(Path arquivo, IOException causa) {
        Path corrompido = arquivo.resolveSibling(NOME_ARQUIVO + ".corrompido");
        try {
            Files.move(arquivo, corrompido, StandardCopyOption.REPLACE_EXISTING);
            log.error("Telemetria da Traducao Local ilegivel; preservada como {} e reiniciado estado vazio. Causa: {}",
                corrompido, causa.getMessage());
        } catch (IOException e) {
            log.error("Telemetria da Traducao Local ilegivel e NAO foi possivel preserva-la ({}). "
                + "Estado iniciado vazio; o arquivo original NAO sera sobrescrito ate nova escrita bem-sucedida. Causa: {}",
                e.getMessage(), causa.getMessage());
        }
    }

    private static Path pasta() {
        return DiretorioBaseKronos.resolver(SUBPASTA);
    }

    private static Path arquivo() {
        return pasta().resolve(NOME_ARQUIVO);
    }
}
