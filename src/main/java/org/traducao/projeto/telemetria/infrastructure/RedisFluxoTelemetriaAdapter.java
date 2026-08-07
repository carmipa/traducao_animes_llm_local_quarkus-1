package org.traducao.projeto.telemetria.infrastructure;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.stream.StreamCommands;
import io.quarkus.redis.datasource.stream.XAddArgs;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.telemetria.FatiaTelemetria;
import org.traducao.projeto.telemetria.FluxoTelemetriaPort;
import org.traducao.projeto.telemetria.StatusFluxoTelemetria;

import java.util.HashMap;
import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: publica os eventos de telemetria num Redis Stream, para
 * a interface acompanhar a execução ao vivo e para que o histórico possa ser
 * relido por posição depois de um F5.
 *
 * <h2>A degradação é a funcionalidade principal</h2>
 * O KRONOS roda na máquina de quem usa e precisa funcionar por inteiro sem Redis
 * nenhum — é assim que ele funciona hoje e é assim que tem de continuar. Este
 * adaptador existe sob essa condição: <b>toda</b> chamada é embrulhada, nenhuma
 * falha sobe, e o pipeline nunca espera por ele.
 *
 * <p>Vale lembrar por que o fluxo NÃO substitui o arquivo: o
 * {@code telemetria_execucoes.jsonl} é append em disco e sobrevive a
 * {@code kill -9}; o Redis com AOF perde os últimos milissegundos. Para
 * telemetria que vira dataset público, perder evento é perder dado. O fluxo
 * acrescenta granularidade e releitura, não substitui durabilidade.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li>Erro de publicação é registrado em DEBUG, não em WARN. Com o Redis fora
 *       do ar, cada fala geraria uma linha de aviso e o log viraria ruído — o
 *       estado do fluxo se lê no card da tela, não no log da tradução.</li>
 *   <li>O fluxo tem teto de tamanho. Sem isso, uma tradução longa encheria a
 *       memória do contêiner do Redis silenciosamente.</li>
 *   <li>Falha de conexão NUNCA vira exceção para o chamador.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link #publicar} descarta o evento;
 * {@link #status} devolve desconectado com o motivo. Nada propaga.
 */
@ApplicationScoped
public class RedisFluxoTelemetriaAdapter implements FluxoTelemetriaPort {

    private static final Logger log = LoggerFactory.getLogger(RedisFluxoTelemetriaAdapter.class);

    /**
     * Prefixo dos fluxos. Um stream POR FATIA ({@code kronos:telemetria:auditoria},
     * {@code kronos:telemetria:cache}, …), e não um fluxo único.
     *
     * <p>Separar não é organização: é o que permite cada aba do painel reler só o
     * que lhe interessa e publicar o próprio dataset. Num fluxo único, a aba de
     * karaokê teria de varrer 2.930 eventos de auditoria para achar os 214 dela.
     *
     * <p>O prefixo {@code kronos:} existe para conviver com outro uso do mesmo
     * Redis — medido em 06/08/2026, esta máquina tinha um servidor de outro
     * projeto com chaves {@code spn:*} escutando na mesma porta.
     */
    static final String PREFIXO_FLUXO = "kronos:telemetria:";

    /**
     * Teto de eventos guardados. Uma execução do 86 gerou 6.805 lotes; 50 mil
     * cobre com folga várias execuções e ainda cabe no teto de 512 MB do
     * contêiner. Sem teto, o crescimento é silencioso até a memória acabar.
     */
    private static final long TETO_EVENTOS = 50_000L;

    private final RedisDataSource redis;

    public RedisFluxoTelemetriaAdapter(RedisDataSource redis) {
        this.redis = redis;
    }

    @Override
    public void publicar(String fatia, String tipo, Map<String, String> campos) {
        try {
            Map<String, String> carga = new HashMap<>();
            if (campos != null) {
                campos.forEach((k, v) -> {
                    if (k != null && v != null) {
                        carga.put(k, v);
                    }
                });
            }
            carga.put("tipo", tipo == null ? "desconhecido" : tipo);

            comandos().xadd(chaveDe(fatia), new XAddArgs().maxlen(TETO_EVENTOS), carga);
        } catch (RuntimeException e) {
            // DEBUG, não WARN: ver invariante sobre ruído de log.
            log.debug("Evento de telemetria descartado (fluxo indisponivel): {}", e.getMessage());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: soma os eventos de TODAS as fatias conhecidas, para o
     * card do painel mostrar um total que corresponda ao que existe.
     *
     * <p>INVARIANTES DO DOMÍNIO: uma fatia sem nenhum evento ainda não tem stream
     * no Redis, e {@code XLEN} de chave inexistente devolve zero — não é erro.
     * Basta UMA resposta para o fluxo ser considerado conectado; se a primeira
     * consulta já falha, é indisponibilidade e não vazio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: desconectado com motivo legível.
     */
    @Override
    public StatusFluxoTelemetria status() {
        try {
            long total = 0;
            for (String fatia : fatiasConhecidas()) {
                total += comandos().xlen(chaveDe(fatia));
            }
            return new StatusFluxoTelemetria(true, fatiasConhecidas().size() + " fluxos", total);
        } catch (RuntimeException e) {
            return StatusFluxoTelemetria.desconectado(motivoLegivel(e));
        }
    }

    /** As fatias do inventário mais o destino de tipo não mapeado. */
    private static java.util.Set<String> fatiasConhecidas() {
        java.util.Set<String> fatias =
            new java.util.TreeSet<>(FatiaTelemetria.inventario().values());
        fatias.add(FatiaTelemetria.OUTROS);
        return fatias;
    }

    /**
     * Monta a chave do stream. Fatia em branco cai em {@code outros} — nunca numa
     * chave degenerada como {@code kronos:telemetria:}, que juntaria num balaio
     * tudo que perdeu a classificação sem ninguém notar.
     */
    private static String chaveDe(String fatia) {
        String limpa = (fatia == null || fatia.isBlank())
            ? FatiaTelemetria.OUTROS
            : fatia.trim().toLowerCase(java.util.Locale.ROOT);
        return PREFIXO_FLUXO + limpa;
    }

    private StreamCommands<String, String, String> comandos() {
        return redis.stream(String.class);
    }

    /**
     * Extrai algo curto e utilizável da exceção. A mensagem crua de conexão
     * recusada vem com pilha e classe interna do cliente, e isso não cabe — nem
     * ajuda — num card de painel.
     */
    private static String motivoLegivel(RuntimeException e) {
        Throwable raiz = e;
        while (raiz.getCause() != null) {
            raiz = raiz.getCause();
        }
        String tipo = raiz.getClass().getSimpleName();
        String msg = raiz.getMessage();

        // "TimeoutException" num card de painel nao diz nada a quem opera. As
        // duas causas reais no dia a dia sao o Redis fora do ar e o nome do
        // servico nao resolvendo fora do compose — e as duas tem a mesma acao.
        if (tipo.contains("Timeout")) {
            return "sem resposta do fluxo (Redis fora do ar?)";
        }
        if (tipo.contains("UnknownHost") || (msg != null && msg.contains("failed to resolve"))) {
            return "servidor de fluxo nao encontrado";
        }
        if (tipo.contains("Connect") || (msg != null && msg.contains("Connection refused"))) {
            return "conexao recusada pelo fluxo";
        }
        return (msg == null || msg.isBlank()) ? tipo : msg;
    }
}
