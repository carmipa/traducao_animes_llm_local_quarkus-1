package org.traducao.projeto.core.presentation.web;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia conexoes SSE (JAX-RS) e despacha logs em tempo real para clientes web.
 * <p>
 * Mantém um <b>heartbeat</b> periódico (evento SSE {@code heartbeat}) enquanto houver
 * cliente conectado: sem esse keepalive a conexão SSE apodrece em ociosidade (timeout
 * de NAT/proxy/servidor) e vira "zumbi" — o navegador acha que está conectado, mas o
 * servidor já não tem o sink, então os logs da próxima operação (ex.: uma conversão de
 * karaokê de poucos milissegundos) escoam para uma lista vazia e o console trava. O
 * heartbeat evita o apodrecimento e, do lado do cliente, arma o watchdog de reconexão.
 */
@Service
@ApplicationScoped
public class LogStreamService {

    // Intervalo entre heartbeats SSE. 15s cabe folgado abaixo dos timeouts de
    // ociosidade típicos (30–60s de proxies/NAT). Configurável para os testes
    // reduzirem a janela (%test o baixa para 1s).
    private static final String CHAVE_INTERVALO_HEARTBEAT = "kronos.sse.heartbeat-intervalo-segundos";
    private static final String INTERVALO_HEARTBEAT_PADRAO = "15";
    private static final String CANAL_HEARTBEAT = "heartbeat";

    // Resolvido via DiretorioBaseKronos: logs/console-web.log em produção,
    // redirecionado para árvore descartável sob a suíte de testes.
    private static final Path ARQUIVO_LOG = DiretorioBaseKronos.resolver("logs", "console-web.log");
    private static final char CHAR_ESC = (char) 27;
    private static final char CHAR_COLCHETE = (char) 91;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final List<SseEventSink> conexoes = new CopyOnWriteArrayList<>();

    @Inject
    Sse sse;

    // Injetado pelo ARC no build (não via ServiceLoader em runtime): ler a config
    // por ConfigProvider.getConfig() dentro do @PostConstruct quebrava a partida do
    // Quarkus no ParidadeBindingVazioIT (SmallRye/QuarkusConfigFactory sob outro
    // classloader). A injeção resolve o valor com segurança antes do @PostConstruct.
    @ConfigProperty(name = CHAVE_INTERVALO_HEARTBEAT, defaultValue = INTERVALO_HEARTBEAT_PADRAO)
    int heartbeatIntervaloSegundos;

    // Cada operação em segundo plano roda em sua própria thread do executor e
    // chama definirCanalAtual() como primeiro passo (ver ApiController). Usar
    // ThreadLocal em vez de um campo único compartilhado evita que operações
    // concorrentes (ex.: uma tradução em andamento + uma análise disparada
    // em paralelo) "roubem" o canal SSE uma da outra e misturem os consoles.
    private final ThreadLocal<String> canalAtual = ThreadLocal.withInitial(() -> "console");

    // Agendador do heartbeat SSE (thread única daemon). Iniciado no @PostConstruct
    // e desligado no @PreDestroy; nunca bloqueia o encerramento da aplicação.
    private ScheduledExecutorService agendadorHeartbeat;

    /**
     * PROPÓSITO DE NEGÓCIO: liga o keepalive do fluxo de logs — o batimento que mantém
     * a conexão SSE de cada cliente viva enquanto ele estiver com uma tela aberta,
     * preservando o console dinâmico entre operações.
     *
     * <p>INVARIANTES DO DOMÍNIO: intervalo lido de {@value #CHAVE_INTERVALO_HEARTBEAT}
     * (padrão {@value #INTERVALO_HEARTBEAT_PADRAO}s), nunca inferior a 1s; roda em uma
     * única thread daemon dedicada, sem competir com o executor do pipeline.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: valor de configuração ausente cai no intervalo
     * padrão (via {@code defaultValue}); a falha ao enviar para um sink apenas o remove
     * (ver {@link #enviarHeartbeat()}), sem derrubar o agendador.
     */
    @PostConstruct
    void iniciarHeartbeat() {
        int intervalo = Math.max(1, heartbeatIntervaloSegundos);
        agendadorHeartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });
        agendadorHeartbeat.scheduleAtFixedRate(
            this::enviarHeartbeat, intervalo, intervalo, TimeUnit.SECONDS);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: desliga o batimento ao encerrar a aplicação.
     * <p>INVARIANTES DO DOMÍNIO: idempotente e não-bloqueante.
     * <p>COMPORTAMENTO EM CASO DE FALHA: sem agendador criado, nada a fazer.
     */
    @PreDestroy
    void encerrarHeartbeat() {
        if (agendadorHeartbeat != null) {
            agendadorHeartbeat.shutdownNow();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: emite um evento SSE {@code heartbeat} para todos os clientes
     * conectados, mantendo a conexão viva e permitindo ao navegador detectar (via
     * watchdog) uma conexão zumbi para reconectar.
     *
     * <p>INVARIANTES DO DOMÍNIO: NÃO passa por {@link #publicarLog(String, String)} — o
     * heartbeat não é log, não vai para o canal de nenhum console nem para o arquivo
     * {@code console-web.log}; sem cliente conectado, não faz nada.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: sink fechado ou que rejeite o envio é removido
     * da lista de conexões (mesma poda de {@link #publicarLog(String, String)}).
     */
    private void enviarHeartbeat() {
        if (conexoes.isEmpty()) {
            return;
        }
        OutboundSseEvent batimento = sse.newEventBuilder()
            .name(CANAL_HEARTBEAT)
            .data(String.valueOf(System.currentTimeMillis()))
            .build();
        for (SseEventSink sink : conexoes) {
            if (sink.isClosed()) {
                conexoes.remove(sink);
                continue;
            }
            try {
                sink.send(batimento);
            } catch (Exception e) {
                conexoes.remove(sink);
            }
        }
    }

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
