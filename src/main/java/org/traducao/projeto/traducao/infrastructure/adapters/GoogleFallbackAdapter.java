package org.traducao.projeto.traducao.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoOnlinePort;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: implementação PRÓPRIA da fatia Tradução Local para a recuperação
 * online de último recurso via Google Translate. É deliberadamente independente do módulo
 * manual {@code raspagemCorrecao} (cuja fronteira arquitetural é proibida à Tradução Local):
 * mascara as tags/quebras de forma amigável ao tradutor externo, chama a API pública e
 * restaura a formatação, recusando a resposta se qualquer marcador for corrompido.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Blocos de tag {@code {...}} viram {@code [Tn]} e quebras {@code \N} viram {@code [B]}
 *       antes do envio; ambos são restaurados na volta.</li>
 *   <li>Se o Google perder/mutilar qualquer marcador, ou devolver texto igual/vazio, o
 *       resultado é {@link Optional#empty()} — nunca uma legenda com formatação quebrada.</li>
 *   <li>Só depende do JDK ({@code java.net.http}) e do Jackson; não conhece cache, LLM,
 *       legenda nem outras fatias.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Erro de rede, HTTP != 200, corpo inesperado, marcador residual/mutilado ou tradução
 * idêntica devolvem {@link Optional#empty()}. O transporte HTTP fica isolado em
 * {@link #executarGet(String)} ({@code protected}) para os testes substituírem sem rede.
 */
@Component
public class GoogleFallbackAdapter implements FallbackTraducaoOnlinePort {

    private static final Logger log = LoggerFactory.getLogger(GoogleFallbackAdapter.class);

    private static final Pattern PADRAO_TAG = Pattern.compile("\\{[^}]+\\}");
    // Marcador residual APENAS nas formas que este adapter emite ([T<n>], [B], [Bn], [Bh]),
    // tolerando mutilação de colchete/espaço — sem confundir '(b)'/'(t)' de conteúdo (alternativas
    // de múltipla escolha) com marcador. A perda/duplicação real é pega pela contagem exata.
    private static final Pattern PADRAO_MARCADOR_RESIDUAL = Pattern.compile(
        "(?i)[\\[(]\\s*t\\s*\\d+\\s*[\\])]"       // [T<n>]: T exige dígito
        + "|[\\[(]\\s*b\\s*[nh]\\s*[\\])]"         // [Bn]/[Bh]: B com sufixo n/h
        + "|\\[\\s*b\\s*\\]");                      // [B] puro: exige colchete reto (não pega "(b)")
    /** Quebras estruturais ASS verificadas por contagem exata na volta do Google. */
    private static final List<String> QUEBRAS = List.of("\\N", "\\n", "\\h");

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public GoogleFallbackAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
        this.httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz uma fala pendente preservando tags e quebras.
     *
     * <p>INVARIANTES DO DOMÍNIO: a saída, quando presente, contém exatamente as mesmas
     * tags/quebras do original; qualquer corrupção de marcador reprova.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@link Optional#empty()} em rede, resposta
     * inválida, marcador residual ou tradução igual ao original; não lança.
     */
    @Override
    public Optional<String> traduzir(String original) {
        if (original == null || original.isBlank()) {
            return Optional.empty();
        }

        List<String> tags = new ArrayList<>();
        Matcher matcher = PADRAO_TAG.matcher(original);
        StringBuilder sb = new StringBuilder();
        int ultimoFim = 0;
        while (matcher.find()) {
            sb.append(original, ultimoFim, matcher.start());
            tags.add(matcher.group());
            sb.append(" [T").append(tags.size() - 1).append("] ");
            ultimoFim = matcher.end();
        }
        sb.append(original, ultimoFim, original.length());
        String mascarado = sb.toString();

        // Todas as quebras estruturais ASS viram marcadores distintos e restauráveis.
        mascarado = mascarado
            .replace("\\N", " [B] ")
            .replace("\\n", " [Bn] ")
            .replace("\\h", " [Bh] ");
        mascarado = mascarado.replaceAll("\\s+", " ").strip();
        if (mascarado.isEmpty()) {
            return Optional.empty();
        }

        String corpo;
        try {
            String query = URLEncoder.encode(mascarado, StandardCharsets.UTF_8);
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=pt&dt=t&q=" + query;
            RespostaHttp resposta = executarGet(url);
            if (resposta.statusCode() != 200) {
                log.warn("Fallback Google: HTTP {} — fala mantida pendente.", resposta.statusCode());
                return Optional.empty();
            }
            corpo = resposta.corpo();
        } catch (Exception e) {
            log.warn("Fallback Google: falha de comunicação ({}) — fala mantida pendente.", e.getMessage());
            return Optional.empty();
        }

        String traduzido;
        try {
            JsonNode root = mapper.readTree(corpo);
            JsonNode segmentos = root.get(0);
            StringBuilder acc = new StringBuilder();
            if (segmentos != null && segmentos.isArray()) {
                for (JsonNode segmento : segmentos) {
                    JsonNode texto = segmento.get(0);
                    if (texto != null && !texto.isNull()) {
                        acc.append(texto.asText());
                    }
                }
            }
            traduzido = acc.toString();
        } catch (Exception e) {
            log.warn("Fallback Google: resposta em formato inesperado ({}) — fala mantida pendente.", e.getMessage());
            return Optional.empty();
        }

        if (traduzido.isBlank()) {
            return Optional.empty();
        }

        // Restaura as quebras (marcadores mais específicos primeiro) e depois as tags.
        traduzido = restaurarQuebra(traduzido, "bh", "\\h");
        traduzido = restaurarQuebra(traduzido, "bn", "\\n");
        traduzido = restaurarQuebra(traduzido, "b", "\\N");
        for (int i = 0; i < tags.size(); i++) {
            traduzido = traduzido.replaceAll("(?i)\\s*\\[t" + i + "\\]\\s*", Matcher.quoteReplacement(tags.get(i)));
        }
        traduzido = traduzido.replace("\\ N", "\\N").replace("\\ n", "\\n").replace("\\ h", "\\h");

        if (PADRAO_MARCADOR_RESIDUAL.matcher(traduzido).find()) {
            log.warn("Fallback Google: marcadores de tag/quebra mutilados — fala mantida pendente: {}", traduzido);
            return Optional.empty();
        }
        // Contagem EXATA de cada tag e quebra: pega tanto perda quanto DUPLICAÇÃO de marcador
        // pelo Google (um simples contains aceitaria uma tag restaurada em dobro).
        for (String tag : tags) {
            if (contarOcorrencias(traduzido, tag) != contarOcorrencias(original, tag)) {
                log.warn("Fallback Google: contagem da tag ASS {} divergente — fala mantida pendente.", tag);
                return Optional.empty();
            }
        }
        for (String quebra : QUEBRAS) {
            if (contarOcorrencias(traduzido, quebra) != contarOcorrencias(original, quebra)) {
                log.warn("Fallback Google: contagem da quebra {} divergente — fala mantida pendente.", quebra);
                return Optional.empty();
            }
        }
        if (traduzido.equals(original)) {
            return Optional.empty();
        }
        return Optional.of(traduzido);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: devolve uma quebra estrutural ASS ({@code \N}, {@code \n},
     * {@code \h}) ao lugar do seu marcador ({@code [B]}, {@code [Bn]}, {@code [Bh]}).
     *
     * <p>INVARIANTES DO DOMÍNIO: casa o marcador ignorando caixa e espaços ao redor; o
     * código de quebra é inserido literalmente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: marcador ausente é no-op — a divergência de
     * contagem a jusante mantém a fala pendente.
     */
    private static String restaurarQuebra(String texto, String marcador, String codigo) {
        return texto.replaceAll("(?i)\\s*\\[" + marcador + "\\]\\s*", Matcher.quoteReplacement(codigo));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: conta ocorrências literais de {@code alvo} em {@code texto} para
     * a verificação de integridade de tags/quebras.
     *
     * <p>INVARIANTES DO DOMÍNIO: contagem por sobreposição zero (avança pelo comprimento do
     * alvo); alvo vazio devolve 0.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: não lança; entradas sem ocorrência devolvem 0.
     */
    private static int contarOcorrencias(String texto, String alvo) {
        if (alvo.isEmpty()) {
            return 0;
        }
        int total = 0;
        int idx = 0;
        while ((idx = texto.indexOf(alvo, idx)) >= 0) {
            total++;
            idx += alvo.length();
        }
        return total;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: transporte HTTP cru (status + corpo), isolado para os testes
     * substituírem sem rede — separando a política de mascaramento/restauração da chamada
     * de rede propriamente dita.
     *
     * <p>INVARIANTES DO DOMÍNIO: apenas executa o GET; não interpreta o corpo.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga a exceção de I/O para o chamador tratar
     * como falha transitória (fala mantida pendente).
     */
    protected RespostaHttp executarGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<String> resposta = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new RespostaHttp(resposta.statusCode(), resposta.body());
    }

    /** Resposta HTTP mínima (status + corpo) usada como seam de transporte nos testes. */
    protected record RespostaHttp(int statusCode, String corpo) {}
}
