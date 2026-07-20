package org.traducao.projeto;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: blinda o "console dinâmico" das telas do KRONOS — a garantia
 * de que cada log de uma operação em segundo plano (tradução, karaokê, revisões...)
 * chega ao terminal do navegador em tempo real via SSE. Nasceu de um incidente real:
 * a conversão de Karaokê Simples do Unicorn (2026-07-20 18:00) publicou as linhas no
 * backend, mas o browser mostrou o console congelado porque a conexão SSE havia
 * apodrecido em ociosidade (sem heartbeat que a mantivesse viva).
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O fluxo {@code /api/logs/stream} DEVE emitir um evento {@code heartbeat}
 *       periódico mesmo sem nenhuma operação em andamento — é o que mantém a conexão
 *       viva através de timeouts de NAT/proxy e permite ao cliente detectar conexão
 *       zumbi.</li>
 *   <li>Uma operação disparada em segundo plano (aqui, {@code /api/novo-karaoke/simular})
 *       DEVE ter suas linhas entregues no canal SSE correspondente a quem está
 *       conectado ANTES da operação começar.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Ausência de heartbeat dentro da janela ⇒ primeiro teste falha (regressão que
 * reabre o congelamento). Linhas do canal {@code novo-karaoke} não chegando ao
 * stream ⇒ segundo teste falha (quebra do console dinâmico).
 */
@QuarkusTest
class SseConsoleDinamicoTest {

    @TestHTTPResource("/api/logs/stream")
    URI streamUri;

    @Test
    void streamEmiteHeartbeatMesmoSemOperacao() throws Exception {
        try (SseColetor coletor = new SseColetor(streamUri)) {
            assertTrue(coletor.conectado.await(5, TimeUnit.SECONDS),
                "conexão SSE não estabeleceu em 5s");
            assertTrue(esperar(() -> !coletor.heartbeats.isEmpty(), 8, TimeUnit.SECONDS),
                "nenhum evento 'heartbeat' recebido — sem keepalive a conexão SSE apodrece "
                    + "em ociosidade e o console para de ser dinâmico");
        }
    }

    @Test
    void operacaoEmSegundoPlanoEntregaLinhasNoCanalNovoKaraoke(@TempDir Path tmp) throws Exception {
        Path origem = Files.createDirectory(tmp.resolve("origem"));
        Files.writeString(origem.resolve("ep.ass"),
            "[Script Info]\nScriptType: v4.00+\nPlayResY: 720\n\n[Events]\n"
                + "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Ola mundo.\n",
            StandardCharsets.UTF_8);
        Path destino = tmp.resolve("destino");

        try (SseColetor coletor = new SseColetor(streamUri)) {
            assertTrue(coletor.conectado.await(5, TimeUnit.SECONDS),
                "conexão SSE não estabeleceu em 5s");

            given()
                .contentType("application/json")
                .body("{\"caminhoOrigem\":\"" + escapar(origem) + "\",\"caminhoDestino\":\"" + escapar(destino) + "\"}")
                .when().post("/api/novo-karaoke/simular")
                .then().statusCode(200);

            assertTrue(
                esperar(() -> coletor.novoKaraoke.stream().anyMatch(l -> l.contains("Karaokê Simples")),
                    10, TimeUnit.SECONDS),
                "as linhas do canal 'novo-karaoke' não chegaram pelo SSE — console não dinâmico");
        }
    }

    private static String escapar(Path caminho) {
        return caminho.toString().replace("\\", "\\\\");
    }

    private static boolean esperar(BooleanSupplier condicao, long tempo, TimeUnit unidade) throws InterruptedException {
        long limite = System.nanoTime() + unidade.toNanos(tempo);
        while (System.nanoTime() < limite) {
            if (condicao.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condicao.getAsBoolean();
    }

    /**
     * Consumidor SSE mínimo baseado no {@link HttpClient} do JDK (sem cliente JAX-RS):
     * lê o corpo linha a linha, reconstrói os frames {@code event:}/{@code data:} e
     * separa o que chega por canal. Encerrar fecha o socket e a thread leitora.
     */
    private static final class SseColetor implements AutoCloseable {
        private final HttpClient cliente = HttpClient.newHttpClient();
        private final ExecutorService leitor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "sse-coletor-teste");
            t.setDaemon(true);
            return t;
        });
        final List<String> heartbeats = new CopyOnWriteArrayList<>();
        final List<String> novoKaraoke = new CopyOnWriteArrayList<>();
        final CountDownLatch conectado = new CountDownLatch(1);

        SseColetor(URI uri) {
            HttpRequest requisicao = HttpRequest.newBuilder(uri)
                .header("Accept", "text/event-stream")
                .GET()
                .build();
            leitor.submit(() -> {
                try {
                    HttpResponse<Stream<String>> resposta =
                        cliente.send(requisicao, HttpResponse.BodyHandlers.ofLines());
                    String evento = "message";
                    for (String linha : (Iterable<String>) resposta.body()::iterator) {
                        if (linha.startsWith("event:")) {
                            evento = linha.substring(6).trim();
                        } else if (linha.startsWith("data:")) {
                            String dados = linha.substring(5).trim();
                            switch (evento) {
                                case "sistema" -> conectado.countDown();
                                case "heartbeat" -> heartbeats.add(dados);
                                case "novo-karaoke" -> novoKaraoke.add(dados);
                                default -> { }
                            }
                        } else if (linha.isEmpty()) {
                            evento = "message";
                        }
                    }
                } catch (Exception ignorado) {
                    // socket fechado no encerramento do teste — término normal
                }
            });
        }

        @Override
        public void close() {
            cliente.shutdownNow();
            leitor.shutdownNow();
        }
    }
}
