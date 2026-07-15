package org.traducao.projeto.revisaoLore.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.revisaoLore.domain.EntradaAuditoriaRevisaoLore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Cache append-only para mineração posterior das decisões da revisão de lore.
 */
@Component
public class RevisaoLoreAuditoriaCache {

    private static final Logger log = LoggerFactory.getLogger(RevisaoLoreAuditoriaCache.class);
    private static final Path ARQUIVO_CANONICO =
        DiretorioBaseKronos.resolver("cache", "auditoria", "revisao_lore_correcoes.jsonl");

    private final ObjectMapper objectMapper;

    public RevisaoLoreAuditoriaCache(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized void registrar(EntradaAuditoriaRevisaoLore entrada) {
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
            log.warn("Falha ao registrar auditoria de revisão de lore em {}: {}",
                ARQUIVO_CANONICO, e.getMessage());
        }
    }

    public Path caminhoCanonico() {
        return ARQUIVO_CANONICO.toAbsolutePath();
    }
}
