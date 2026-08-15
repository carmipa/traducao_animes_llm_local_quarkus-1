package org.traducao.projeto.qualidadeTraducao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ProvedorContexto;
import org.traducao.projeto.core.texto.FronteiraTermoAss;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede o tamanho real do buraco que o mapa de terminologia tapa forma a
 * forma — quantas falas do acervo trazem um termo de {@code termosProtegidos()} no INGLÊS e o
 * perderam na tradução.
 *
 * <h2>Por que existe</h2>
 * Ordem de Paulo em 2026-08-15, depois de "Spearhead" sair como {@code Esquadroe de Ponta}:
 * <i>"o protetor de lore tem de funcionar"</i>. Hoje a proteção real é
 * {@code correcoesTerminologia()}, um mapa forma-errada → forma-canônica alimentado por
 * medição depois de cada rodada. Isso é gato e rato: das 5 entradas de Spearhead cadastradas
 * em 05/08, ZERO ocorreu na rodada de 15/08 — o modelo inventou três formas novas.
 *
 * <p>Antes de propor mecanismo é preciso o número. Esta medição diz, por obra e por termo,
 * quantas falas perderam o termo. É o denominador que decide se vale trocar o mapa por uma
 * restauração geral, e o alvo contra o qual qualquer mecanismo novo terá de ser comparado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A lista de termos vem dos {@link ProvedorContexto} REAIS, injetados. Nenhuma lista é
 *       copiada para cá — cópia diverge, e é a origem documentada de medição errada.</li>
 *   <li>O casamento do termo no ORIGINAL é por palavra inteira e sensível a maiúscula para
 *       termo de uma palavra, igual ao {@code contarCanonico} do enforcador: é o mesmo
 *       critério que separa {@code Spearhead} (esquadrão) de {@code spearhead} (arma).</li>
 *   <li>Acervo ausente sai como NÃO VERIFICADO, nunca como aprovação.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não reprova pelo número — ele é o objeto da medição. Reprova só sem acervo para ler.
 */
@QuarkusTest
@DisplayName("MEDIÇÃO: termo de lore que o inglês tinha e a tradução perdeu")
class MedicaoTermoDeLorePerdidoIT {

    private static final Path RAIZ_CACHE = Path.of("cache");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    List<ProvedorContexto> provedores;

    /**
     * A MESMA fronteira que o enforcador usa, e não uma equivalente escrita à mão.
     *
     * <p>A primeira versão desta medição montou {@code (?<![\p{L}\p{N}])termo} por conta
     * própria e inflou o resultado: em ASS a quebra de linha é {@code \N}, e o {@code N} é
     * LETRA — então {@code "usou a\NLegion para..."} contava como termo PERDIDO com o termo
     * ali, escrito. É a mesma cicatriz que originou {@link FronteiraTermoAss}: 435 campos do
     * acervo com nome composto partido, por cópia incompleta da regra de fronteira.
     */
    private static Pattern padrao(String termo) {
        boolean multi = termo.trim().indexOf(' ') >= 0;
        int flags = multi ? (Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE) : 0;
        return Pattern.compile(
            FronteiraTermoAss.INICIO + FronteiraTermoAss.corpo(termo) + FronteiraTermoAss.FIM,
            flags);
    }

    @Test
    void medirTermosDeLorePerdidos() throws Exception {
        assertTrue(Files.isDirectory(RAIZ_CACHE),
            "NÃO VERIFICADO: pasta cache/ ausente — sem acervo não há medição");

        Map<String, ProvedorContexto> porId = new HashMap<>();
        for (ProvedorContexto p : provedores) {
            porId.put(p.getId(), p);
        }

        // Padrões compilados uma vez por termo: são 500+ caches × dezenas de termos.
        Map<String, Map<String, Pattern>> padroesPorContexto = new HashMap<>();

        List<Path> caches;
        try (Stream<Path> s = Files.walk(RAIZ_CACHE)) {
            caches = s.filter(p -> p.getFileName().toString().endsWith(".cache.json")).toList();
        }
        assertTrue(!caches.isEmpty(), "NÃO VERIFICADO: nenhum .cache.json sob cache/");

        Map<String, Integer> paresPorObra = new TreeMap<>();
        Map<String, Integer> perdasPorObra = new TreeMap<>();
        Map<String, Integer> perdasPorTermo = new HashMap<>();
        List<String[]> amostra = new ArrayList<>();
        int semContexto = 0;
        int paresTotais = 0;

        for (Path cache : caches) {
            JsonNode raiz;
            try {
                raiz = JSON.readTree(Files.readString(cache));
            } catch (Exception ilegivel) {
                continue;
            }
            String contextoId = raiz.path("proveniencia").path("contextoId").asText("");
            ProvedorContexto ctx = porId.get(contextoId);
            if (ctx == null) {
                semContexto++;
                continue;
            }
            Set<String> termos = ctx.termosProtegidos();
            if (termos == null || termos.isEmpty()) {
                continue;
            }
            Map<String, Pattern> padroes = padroesPorContexto.computeIfAbsent(contextoId, id -> {
                Map<String, Pattern> m = new LinkedHashMap<>();
                for (String t : termos) {
                    if (t != null && !t.isBlank()) {
                        m.put(t, padrao(t));
                    }
                }
                return m;
            });

            String obra = ctx.getNomeExibicao();
            for (JsonNode e : raiz.path("entradas")) {
                String original = e.path("original").asText("");
                String traduzido = e.path("traduzido").asText("");
                if (original.isBlank() || traduzido.isBlank()) {
                    continue;
                }
                paresTotais++;
                paresPorObra.merge(obra, 1, Integer::sum);
                for (Map.Entry<String, Pattern> par : padroes.entrySet()) {
                    if (!par.getValue().matcher(original).find()) {
                        continue;
                    }
                    if (par.getValue().matcher(traduzido).find()) {
                        continue; // sobreviveu
                    }
                    perdasPorObra.merge(obra, 1, Integer::sum);
                    perdasPorTermo.merge(par.getKey() + "  [" + obra + "]", 1, Integer::sum);
                    if (amostra.size() < 30) {
                        amostra.add(new String[] { par.getKey(), original, traduzido });
                    }
                }
            }
        }

        int perdasTotais = perdasPorObra.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println();
        System.out.println("=== TERMO DE LORE PERDIDO NA TRADUÇÃO ===");
        System.out.println("  caches lidos ............... " + caches.size()
            + "   (sem contexto reconhecido: " + semContexto + ")");
        System.out.println("  pares original/tradução .... " + paresTotais);
        System.out.println("  perdas (termo × fala) ...... " + perdasTotais);
        System.out.println();
        System.out.println("POR OBRA (perdas / pares):");
        paresPorObra.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<String, Integer> e) ->
                -perdasPorObra.getOrDefault(e.getKey(), 0)))
            .limit(15)
            .forEach(e -> {
                int perdas = perdasPorObra.getOrDefault(e.getKey(), 0);
                double pct = 100.0 * perdas / e.getValue();
                System.out.printf("  %-34s %6d / %6d   %5.2f%%%n", e.getKey(), perdas, e.getValue(), pct);
            });
        System.out.println();
        System.out.println("TERMOS MAIS PERDIDOS:");
        perdasPorTermo.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(25)
            .forEach(e -> System.out.printf("  %5d  %s%n", e.getValue(), e.getKey()));
        System.out.println();
        System.out.println("AMOSTRA:");
        for (String[] a : amostra) {
            System.out.println("  [" + a[0] + "]");
            System.out.println("    EN: " + (a[1].length() > 100 ? a[1].substring(0, 100) : a[1]));
            System.out.println("    PT: " + (a[2].length() > 100 ? a[2].substring(0, 100) : a[2]));
        }
    }
}
