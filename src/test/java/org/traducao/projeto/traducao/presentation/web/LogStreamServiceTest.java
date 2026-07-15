package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.io.DiretorioBaseKronos;
import org.traducao.projeto.core.presentation.web.LogStreamService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sem nenhum SSE client conectado, {@code publicarLog} ainda deve persistir a
 * linha em {@code logs/console-web.log} (espelho em disco usado por
 * {@link ConsoleRedirector} e pelos consoles web). Isso é o que prova que o
 * pipeline de publicação/persistência em arquivo funciona independente de
 * haver navegador conectado via SSE.
 */
@QuarkusTest
class LogStreamServiceTest {

    // Mesmo destino redirecionado que o LogStreamService usa sob a suíte
    // (DiretorioBaseKronos), para não ler/reescrever o logs/console-web.log real.
    private static final Path ARQUIVO_LOG = DiretorioBaseKronos.resolver("logs", "console-web.log");
    private static final char ESC = (char) 27;

    @Inject
    LogStreamService logStreamService;

    @Test
    void publicarLogPersisteNoArquivoComAnsiRemovido() throws IOException {
        String marcador = "marcador-teste-" + UUID.randomUUID();
        String mensagemComAnsi = ESC + "[32m[ OK ] " + marcador + ESC + "[0m";

        try {
            logStreamService.publicarLog("console-teste", mensagemComAnsi);

            String conteudo = Files.readString(ARQUIVO_LOG, StandardCharsets.UTF_8);
            assertTrue(conteudo.contains("[console-teste] [ OK ] " + marcador),
                "Linha persistida deveria conter o canal e a mensagem sem códigos ANSI");
            assertFalse(conteudo.contains("[32m" + marcador),
                "Códigos ANSI não deveriam sobreviver à persistência em arquivo");
        } finally {
            removerLinhasDoMarcador(marcador);
        }
    }

    private void removerLinhasDoMarcador(String marcador) throws IOException {
        if (!Files.exists(ARQUIVO_LOG)) {
            return;
        }
        List<String> linhasMantidas = Files.readAllLines(ARQUIVO_LOG, StandardCharsets.UTF_8).stream()
            .filter(linha -> !linha.contains(marcador))
            .toList();
        Files.write(ARQUIVO_LOG, linhasMantidas, StandardCharsets.UTF_8);
    }
}
