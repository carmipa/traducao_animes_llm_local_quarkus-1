package org.traducao.projeto.traducao.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;
import org.traducao.projeto.traducao.application.RecuperarPendenciaFallbackService;
import org.traducao.projeto.traducao.domain.fallback.ProvedorFallback;
import org.traducao.projeto.traducao.domain.fallback.ResultadoFallback;
import org.traducao.projeto.traducao.domain.fallback.StatusFallback;
import org.traducao.projeto.traducao.domain.ports.FallbackTraducaoMaquinaPort;
import org.traducao.projeto.traducao.infrastructure.config.FallbackOnlineProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: subfase F4 — mede, sobre as falas que o LLM local REALMENTE não
 * conseguiu traduzir, se o LibreTranslate local (CPU) paga o próprio custo operacional frente
 * ao Google. É o GATE da decisão: sem este número, adotar um provedor novo seria escolha por
 * preferência, não por evidência. Não altera produção — apenas lê os caches e chama os
 * provedores.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>NÃO roda na suíte normal: exige {@code -Dkronos.benchmark=true}. Faz rede e depende de
 *       um container local, coisas que uma suíte de CI não pode assumir.</li>
 *   <li>Estritamente READ-ONLY sobre {@code cache/}: lê os originais das entradas cuja tradução
 *       ficou vazia e nunca grava nada de volta.</li>
 *   <li>Mede as etapas SEPARADAMENTE (respondeu / preservou tags / passou na guarda), porque a
 *       taxa agregada esconde a causa — foi assim que o braço B do motor de contexto de cena
 *       pareceu funcionar enquanto esvaziava 22,6% do diálogo.</li>
 *   <li>A amostra é limitada e o limite é IMPRESSO: o Google é endpoint público e centenas de
 *       requisições seguidas tomam 429, o que mediria o rate limit em vez do tradutor.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Container fora do ar aparece como {@link StatusFallback#PROVEDOR_INDISPONIVEL} na coluna do
 * LibreTranslate — o benchmark continua e reporta, em vez de abortar. Ausência de caches faz o
 * teste terminar cedo avisando; não lança.
 */
@EnabledIfSystemProperty(named = "kronos.benchmark", matches = "true")
class BenchmarkFallbackProvedoresIT {

    /** Falas por provedor. Mantido baixo: o endpoint do Google responde 429 em rajada. */
    private static final int AMOSTRA = 80;
    private static final String LIBRE_URL = System.getProperty("kronos.libre.url", "http://localhost:5000");

    @Test
    @DisplayName("F4: LibreTranslate (CPU) vs Google sobre pendências reais")
    void compararProvedores() throws Exception {
        List<String> pendentes = lerPendentesDosCaches(Path.of(System.getProperty("kronos.benchmark.cache", "cache")));
        if (pendentes.isEmpty()) {
            System.out.println("[BENCHMARK] nenhuma fala pendente encontrada — nada a medir.");
            return;
        }
        List<String> amostra = pendentes.stream().distinct().limit(AMOSTRA).toList();
        System.out.printf("%n[BENCHMARK] corpus total=%d falas pendentes; amostra medida=%d (limite explícito)%n",
            pendentes.size(), amostra.size());

        Medicao google = medir("GOOGLE", amostra, new GoogleFallbackAdapter(new ObjectMapper()));
        Medicao libre = medir("LIBRETRANSLATE", amostra, new LibreAdapterBenchmark());

        imprimirTabela(amostra.size(), google, libre);
        imprimirComplementaridade(google, libre);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: responde a ÚNICA pergunta que decide se o LibreTranslate paga o
     * custo de manter um container — quantas falas ele recupera que o Google NÃO recupera. Numa
     * cadeia onde o Google vem primeiro, a taxa isolada do segundo provedor é irrelevante: o que
     * importa é o ganho INCREMENTAL sobre o resíduo que sobrou.
     * <p>INVARIANTES DO DOMÍNIO: compara os conjuntos de falas aprovadas por cada provedor após
     * a guarda; "só Libre" é o valor incremental real.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjuntos vazios imprimem zero; não lança.
     */
    private void imprimirComplementaridade(Medicao g, Medicao l) {
        Set<String> soGoogle = new LinkedHashSet<>(g.aprovadas);
        soGoogle.removeAll(l.aprovadas);
        Set<String> soLibre = new LinkedHashSet<>(l.aprovadas);
        soLibre.removeAll(g.aprovadas);
        Set<String> ambos = new LinkedHashSet<>(g.aprovadas);
        ambos.retainAll(l.aprovadas);
        Set<String> nenhum = new LinkedHashSet<>(g.avaliadas);
        nenhum.removeAll(g.aprovadas);
        nenhum.removeAll(l.aprovadas);

        System.out.println("========== COMPLEMENTARIDADE (cadeia Google → Libre) ==========");
        System.out.printf("  recuperadas por AMBOS.................: %d%n", ambos.size());
        System.out.printf("  só pelo GOOGLE.......................: %d%n", soGoogle.size());
        System.out.printf("  só pelo LIBRE (GANHO INCREMENTAL)....: %d%n", soLibre.size());
        System.out.printf("  nenhum dos dois recuperou............: %d%n", nenhum.size());
        System.out.println("  --> o Libre só se paga pelo tamanho de 'GANHO INCREMENTAL'.");
        if (!soLibre.isEmpty()) {
            System.out.println("  exemplos do ganho incremental:");
            soLibre.stream().limit(5).forEach(f ->
                System.out.println("    - " + f.substring(0, Math.min(90, f.length()))));
        }
        System.out.println("==============================================================\n");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: roda a amostra por um provedor e separa o desfecho em ETAPAS, para
     * distinguir "o provedor não respondeu" de "respondeu e nós recusamos".
     * <p>INVARIANTES DO DOMÍNIO: usa a MESMA guarda de produção; o tempo é medido só na chamada.
     * <p>COMPORTAMENTO EM CASO DE FALHA: exceção de rede vira causa contabilizada, não aborta.
     */
    private Medicao medir(String rotulo, List<String> amostra, FallbackTraducaoMaquinaPort porta) {
        System.out.printf("[BENCHMARK] medindo %s...%n", rotulo);
        Map<StatusFallback, Integer> causas = new EnumMap<>(StatusFallback.class);
        Map<String, String> aprovadasPelaPorta = new LinkedHashMap<>();
        long inicio = System.currentTimeMillis();

        for (String original : amostra) {
            ResultadoFallback r;
            try {
                r = porta.traduzir(original);
            } catch (RuntimeException e) {
                r = ResultadoFallback.recusada(porta.provedor(),
                    StatusFallback.PROVEDOR_INDISPONIVEL, e.getMessage());
            }
            causas.merge(r.status(), 1, Integer::sum);
            if (r.recuperou()) {
                aprovadasPelaPorta.put(original, r.traducao());
            }
        }
        long tempoMs = System.currentTimeMillis() - inicio;

        // Guarda de produção, sem lore ativa: mede o piso (só siglas/identificadores obrigam).
        RecuperarPendenciaFallbackService servico = new RecuperarPendenciaFallbackService(
            new FallbackOnlineProperties(true), portaFixa(aprovadasPelaPorta, porta.provedor()), loreVazia(),
            new org.traducao.projeto.traducao.application.VerificadorIdentificadorNumerico());
        Set<String> aprovadasGuarda = servico.recuperar(new LinkedHashSet<>(aprovadasPelaPorta.keySet()))
            .recuperadas().keySet();
        int passouGuarda = aprovadasGuarda.size();

        return new Medicao(rotulo, causas, aprovadasPelaPorta.size(), passouGuarda, tempoMs,
            new LinkedHashSet<>(aprovadasGuarda), new LinkedHashSet<>(amostra));
    }

    private void imprimirTabela(int amostra, Medicao g, Medicao l) {
        System.out.println("\n================ F4 — BENCHMARK DE PROVEDORES ================");
        System.out.printf("%-34s %12s %12s%n", "métrica (amostra=" + amostra + ")", "GOOGLE", "LIBRE");
        System.out.printf("%-34s %12d %12d%n", "1. respondeu com tradução", g.respondeu, l.respondeu);
        System.out.printf("%-34s %12d %12d%n", "2. tags/quebras preservadas (idem)", g.respondeu, l.respondeu);
        System.out.printf("%-34s %12d %12d%n", "3. passou na guarda de termos", g.passouGuarda, l.passouGuarda);
        System.out.printf("%-34s %11.1f%% %11.1f%%", "4. recuperação final",
            100.0 * g.passouGuarda / amostra, 100.0 * l.passouGuarda / amostra);
        System.out.println();
        System.out.printf("%-34s %11dms %11dms%n", "5. tempo total", g.tempoMs, l.tempoMs);
        System.out.printf("%-34s %11.0fms %11.0fms%n", "6. tempo por fala",
            (double) g.tempoMs / amostra, (double) l.tempoMs / amostra);
        System.out.println("\n-- causas por provedor --");
        System.out.println("  GOOGLE: " + g.causas);
        System.out.println("  LIBRE : " + l.causas);
        System.out.println("==============================================================\n");
    }

    /** Falas cuja tradução ficou vazia no cache — o resíduo real que o LLM não resolveu. */
    private List<String> lerPendentesDosCaches(Path raiz) throws IOException {
        List<String> pendentes = new ArrayList<>();
        if (!Files.isDirectory(raiz)) {
            return pendentes;
        }
        ObjectMapper mapper = new ObjectMapper();
        try (Stream<Path> arquivos = Files.walk(raiz)) {
            for (Path arquivo : arquivos.filter(p -> p.toString().endsWith(".cache.json")).toList()) {
                try {
                    JsonNode raizJson = mapper.readTree(arquivo.toFile());
                    JsonNode entradas = raizJson.isArray() ? raizJson : raizJson.get("entradas");
                    if (entradas == null || !entradas.isArray()) {
                        continue;
                    }
                    for (JsonNode entrada : entradas) {
                        JsonNode traduzido = entrada.get("traduzido");
                        JsonNode original = entrada.get("original");
                        if (original != null && traduzido != null
                                && traduzido.asText().isBlank() && !original.asText().isBlank()) {
                            pendentes.add(original.asText());
                        }
                    }
                } catch (IOException e) {
                    // cache ilegível não interrompe a medição
                }
            }
        }
        return pendentes;
    }

    /** Porta que devolve o que o provedor já produziu — isola a etapa de guarda da de rede. */
    private static FallbackTraducaoMaquinaPort portaFixa(Map<String, String> prontas, ProvedorFallback p) {
        return new FallbackTraducaoMaquinaPort() {
            @Override
            public ResultadoFallback traduzir(String original) {
                String t = prontas.get(original);
                return t == null
                    ? ResultadoFallback.recusada(p, StatusFallback.RESPOSTA_VAZIA, "ausente")
                    : ResultadoFallback.recuperada(t, p);
            }

            @Override
            public ProvedorFallback provedor() {
                return p;
            }
        };
    }

    private static LoreAtivaPort loreVazia() {
        return new LoreAtivaPort() {
            @Override
            public Set<String> termosProtegidosAtivos() {
                return Set.of();
            }

            @Override
            public String obterLoreAtiva() {
                return "";
            }
        };
    }

    private record Medicao(String rotulo, Map<StatusFallback, Integer> causas,
                           int respondeu, int passouGuarda, long tempoMs,
                           Set<String> aprovadas, Set<String> avaliadas) {}

    /**
     * PROPÓSITO DE NEGÓCIO: cliente mínimo do LibreTranslate para o benchmark, reaproveitando o
     * MESMO mascaramento/restauração/contagem do {@link GoogleFallbackAdapter} — só o transporte
     * muda. Sem isso a comparação seria injusta: um provedor seria medido com blindagem de tags
     * e o outro sem. Vive no teste porque o adaptador de produção só nasce se F4 aprovar.
     * <p>INVARIANTES DO DOMÍNIO: idioma de destino {@code pt}, confirmado por {@code GET
     * /languages} no container (a documentação citava {@code pb}, que NÃO existe na instância).
     * <p>COMPORTAMENTO EM CASO DE FALHA: erro de rede vira {@link StatusFallback#PROVEDOR_INDISPONIVEL}.
     */
    private static final class LibreAdapterBenchmark extends GoogleFallbackAdapter {

        private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();
        private final ObjectMapper mapper = new ObjectMapper();

        LibreAdapterBenchmark() {
            super(new ObjectMapper());
        }

        @Override
        public ProvedorFallback provedor() {
            return ProvedorFallback.LIBRETRANSLATE;
        }

        /**
         * PROPÓSITO DE NEGÓCIO: substitui o transporte do Google pelo POST do LibreTranslate,
         * devolvendo a resposta no MESMO envelope que a classe-mãe já sabe interpretar.
         * <p>INVARIANTES DO DOMÍNIO: a classe-mãe envia o texto já mascarado na querystring
         * {@code q=}; aqui ele é extraído e reenviado como JSON.
         * <p>COMPORTAMENTO EM CASO DE FALHA: propaga para o {@code catch} da classe-mãe.
         */
        @Override
        protected RespostaHttp executarGet(String url) throws Exception {
            String q = java.net.URLDecoder.decode(
                url.substring(url.indexOf("&q=") + 3), StandardCharsets.UTF_8);
            String corpo = mapper.writeValueAsString(Map.of(
                "q", q, "source", "en", "target", "pt", "format", "text"));
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(LIBRE_URL + "/translate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                return new RespostaHttp(resp.statusCode(), resp.body());
            }
            // Reembala no formato que a classe-mãe espera do Google: [[[traducao,...]]]
            String texto = mapper.readTree(resp.body()).path("translatedText").asText("");
            String comoGoogle = mapper.writeValueAsString(List.of(List.of(List.of(texto, q))));
            return new RespostaHttp(200, comoGoogle);
        }
    }
}
