package org.traducao.projeto.revisaoLore.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.revisaoLore.domain.EntradaAuditoriaRevisaoLore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RevisaoLoreAuditoriaCacheTest {

    @Test
    void registraEntradaJsonlAppendOnly() throws IOException {
        RevisaoLoreAuditoriaCache cache = new RevisaoLoreAuditoriaCache(new ObjectMapper());
        String marcador = "teste-auditoria-" + UUID.randomUUID();
        Path arquivo = cache.caminhoCanonico();

        try {
            cache.registrar(new EntradaAuditoriaRevisaoLore(
                "2026-07-05T22:31:02Z",
                "gundam_08ms",
                "Mobile Suit Gundam: The 08th MS Team - Revisao de Lore",
                "apenas_sinalizadas",
                marcador + ".ass",
                12,
                10,
                353,
                "CORRIGIDA",
                List.of("Termo de faccao/organizacao traduzivel permaneceu em ingles: \"federation\""),
                "Let the Federation have Odessa and the Earth!",
                "Deixe a Federation ficar com Odessa e a Terra!",
                "Deixe a Federação ficar com Odessa e a Terra!",
                "Deixe a Federação ficar com Odessa e a Terra!",
                null
            ));

            String conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
            assertTrue(conteudo.contains(marcador));
            assertTrue(conteudo.contains("\"resultado\":\"CORRIGIDA\""));
        } finally {
            removerLinhasDoMarcador(arquivo, marcador);
        }
    }

    private void removerLinhasDoMarcador(Path arquivo, String marcador) throws IOException {
        if (!Files.exists(arquivo)) {
            return;
        }
        List<String> linhasMantidas = Files.readAllLines(arquivo, StandardCharsets.UTF_8).stream()
            .filter(linha -> !linha.contains(marcador))
            .toList();
        Files.write(arquivo, linhasMantidas, StandardCharsets.UTF_8);
    }
}
