package org.traducao.projeto.trocaTipoLegenda.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.trocaTipoLegenda.domain.EntradaAuditoriaTrocaFonte;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Cache append-only para gravação de auditoria histórica e granular de cada alteração de fonte aplicada.
 */
@Component
public class TrocaTipoLegendaAuditoriaCache {

    private static final Logger log = LoggerFactory.getLogger(TrocaTipoLegendaAuditoriaCache.class);
    private static final Path ARQUIVO_CANONICO =
        DiretorioBaseKronos.resolver("cache", "auditoria", "troca_tipo_legenda_correcoes.jsonl");

    private final ObjectMapper objectMapper;

    public TrocaTipoLegendaAuditoriaCache(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void registrar(EntradaAuditoriaTrocaFonte entrada) {
        if (entrada == null) {
            return;
        }
        try {
            Files.createDirectories(ARQUIVO_CANONICO.getParent());
            String linha = objectMapper.writeValueAsString(entrada) + System.lineSeparator();
            Files.writeString(
                ARQUIVO_CANONICO,
                linha,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.warn("Falha ao registrar auditoria de troca de fontes em {}: {}",
                ARQUIVO_CANONICO, e.getMessage());
        }
    }

    public Path caminhoCanonico() {
        return ARQUIVO_CANONICO.toAbsolutePath();
    }
}
