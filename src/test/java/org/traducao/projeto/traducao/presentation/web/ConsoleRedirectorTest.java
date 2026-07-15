package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de regressão para o bug pós-migração Spring Boot -> Quarkus: o
 * console web parou de exibir logs de sucesso/alerta porque
 * {@link ConsoleRedirector} (um bean cujo construtor chamava
 * {@code System.setOut}) nunca era instanciado pelo CDI/ARC, já que nada o
 * injetava em lugar nenhum. Sem o redirecionamento ativo, nada que os use
 * cases imprimem com {@code System.out.println} chega ao
 * {@code LogStreamService} (SSE) nem ao espelho em arquivo.
 * <p>
 * Este teste falha sem o fix (em {@code @Observes StartupEvent}) e passa com
 * ele, pois depende exclusivamente do bean ter sido ativado automaticamente
 * na subida do Quarkus — nenhuma injeção explícita de
 * {@code ConsoleRedirector} é feita aqui.
 */
@QuarkusTest
class ConsoleRedirectorTest {

    // Destino redirecionado sob a suíte (ver DiretorioBaseKronos), evitando
    // leitura do logs/console-web.log real.
    private static final Path ARQUIVO_LOG = DiretorioBaseKronos.resolver("logs", "console-web.log");

    @Test
    void systemOutEstaRedirecionadoParaOArquivoDeLogDoConsoleWeb() throws IOException {
        String marcador = "marcador-console-redirector-" + UUID.randomUUID();

        System.out.println(marcador);

        String conteudo = Files.readString(ARQUIVO_LOG, StandardCharsets.UTF_8);
        assertTrue(conteudo.contains(marcador),
            "System.out.println deveria ter sido espelhado em logs/console-web.log pelo ConsoleRedirector");
    }
}
