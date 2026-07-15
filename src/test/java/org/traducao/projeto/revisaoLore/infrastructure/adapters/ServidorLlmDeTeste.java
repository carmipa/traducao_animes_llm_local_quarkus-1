package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PROPÓSITO DE NEGÓCIO: servidor HTTP local determinístico que emula o endpoint
 * OpenAI-compatible do LLM (LM Studio) para caracterizar a stack de Revisão de
 * Lore sem depender de rede externa ou do LM Studio real.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Roteia {@code /v1/chat/completions}, {@code /v1/models} e {@code /api/v0/models}.</li>
 *   <li>Conta chamadas ao chat e captura cada corpo recebido, para asserção de payload e retry.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem resposta de chat enfileirada, repete a última configurada; encerra a porta ao fechar.
 */
final class ServidorLlmDeTeste implements AutoCloseable {

    record Resposta(int status, String corpo) {}

    private final HttpServer server;
    private final Deque<Resposta> respostasChat = new ArrayDeque<>();
    private volatile Resposta respostaModelsV0 = new Resposta(404, "");
    private volatile Resposta respostaModels = new Resposta(404, "");
    private final List<String> corposChat = new CopyOnWriteArrayList<>();
    private final AtomicInteger chamadasChat = new AtomicInteger(0);

    ServidorLlmDeTeste() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.server.createContext("/v1/chat/completions", this::tratarChat);
        this.server.createContext("/v1/models", ex -> responder(ex, respostaModels));
        this.server.createContext("/api/v0/models", ex -> responder(ex, respostaModelsV0));
        this.server.setExecutor(null);
        this.server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    void enfileirarChat(int status, String corpo) {
        respostasChat.addLast(new Resposta(status, corpo));
    }

    void definirModelsV0(int status, String corpo) {
        this.respostaModelsV0 = new Resposta(status, corpo);
    }

    void definirModels(int status, String corpo) {
        this.respostaModels = new Resposta(status, corpo);
    }

    int chamadasChat() {
        return chamadasChat.get();
    }

    String ultimoCorpoChat() {
        return corposChat.isEmpty() ? null : corposChat.get(corposChat.size() - 1);
    }

    private void tratarChat(HttpExchange ex) throws IOException {
        chamadasChat.incrementAndGet();
        corposChat.add(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        Resposta r = respostasChat.isEmpty()
            ? new Resposta(200, "{\"choices\":[]}")
            : (respostasChat.size() == 1 ? respostasChat.peekFirst() : respostasChat.pollFirst());
        responder(ex, r);
    }

    private void responder(HttpExchange ex, Resposta r) throws IOException {
        byte[] corpo = r.corpo().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(r.status(), corpo.length == 0 ? -1 : corpo.length);
        if (corpo.length > 0) {
            try (OutputStream os = ex.getResponseBody()) {
                os.write(corpo);
            }
        } else {
            ex.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
