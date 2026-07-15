package org.traducao.projeto.traducao.presentation.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.springframework.stereotype.Service;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gerencia conexoes SSE (JAX-RS) e despacha logs em tempo real para clientes web.
 */
@Service
@ApplicationScoped
public class LogStreamService {

    // Resolvido via DiretorioBaseKronos: logs/console-web.log em produção,
    // redirecionado para árvore descartável sob a suíte de testes.
    private static final Path ARQUIVO_LOG = DiretorioBaseKronos.resolver("logs", "console-web.log");
    private static final char CHAR_ESC = (char) 27;
    private static final char CHAR_COLCHETE = (char) 91;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final List<SseEventSink> conexoes = new CopyOnWriteArrayList<>();

    @Inject
    Sse sse;

    // Cada operação em segundo plano roda em sua própria thread do executor e
    // chama definirCanalAtual() como primeiro passo (ver ApiController). Usar
    // ThreadLocal em vez de um campo único compartilhado evita que operações
    // concorrentes (ex.: uma tradução em andamento + uma análise disparada
    // em paralelo) "roubem" o canal SSE uma da outra e misturem os consoles.
    private final ThreadLocal<String> canalAtual = ThreadLocal.withInitial(() -> "console");

    public void registrar(SseEventSink sink) {
        conexoes.add(sink);
        enviar(sink, "sistema", "Conexao de fluxo de logs estabelecida.");
    }

    public void definirCanalAtual(String canal) {
        this.canalAtual.set(canal);
    }

    public void publicarLog(String mensagem) {
        publicarLog(canalAtual.get(), mensagem);
    }

    public void publicarLog(String canal, String mensagem) {
        OutboundSseEvent evento = sse.newEventBuilder()
            .name(canal)
            .data(mensagem)
            .build();
        for (SseEventSink sink : conexoes) {
            if (sink.isClosed()) {
                conexoes.remove(sink);
                continue;
            }
            try {
                sink.send(evento);
            } catch (Exception e) {
                conexoes.remove(sink);
            }
        }
        persistirEmArquivo(canal, mensagem);
    }

    private void enviar(SseEventSink sink, String canal, String dados) {
        try {
            sink.send(sse.newEventBuilder().name(canal).data(dados).build());
        } catch (Exception e) {
            conexoes.remove(sink);
        }
    }

    private void persistirEmArquivo(String canal, String mensagem) {
        String linhaLimpa = removerCodigosAnsi(mensagem);
        if (linhaLimpa.isBlank()) {
            return;
        }
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        String linha = "%s [%s] %s%n".formatted(timestamp, canal, linhaLimpa);
        try {
            Files.createDirectories(ARQUIVO_LOG.getParent());
            Files.writeString(ARQUIVO_LOG, linha, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Evita recursao via ConsoleRedirector.
        }
    }

    private String removerCodigosAnsi(String texto) {
        StringBuilder limpo = new StringBuilder(texto.length());
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
            if (c == CHAR_ESC && i + 1 < texto.length() && texto.charAt(i + 1) == CHAR_COLCHETE) {
                int j = i + 2;
                while (j < texto.length() && (Character.isDigit(texto.charAt(j)) || texto.charAt(j) == ';')) {
                    j++;
                }
                if (j < texto.length() && texto.charAt(j) == 'm') {
                    i = j + 1;
                    continue;
                }
            }
            limpo.append(c);
            i++;
        }
        return limpo.toString();
    }
}
