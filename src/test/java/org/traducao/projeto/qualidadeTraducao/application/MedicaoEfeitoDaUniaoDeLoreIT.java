package org.traducao.projeto.qualidadeTraducao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.revisaoLore.application.GerenciadorPromptRevisaoLore;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede, ANTES da união, o que as entradas que só a Revisão de Lore
 * conhece fariam na legenda se passassem a agir também na Tradução Local — quantas falas já
 * aceitas seriam reescritas, por obra e por termo, e com que texto.
 *
 * <h2>Por que existe (portão da FASE 1 do plano de fonte única de terminologia)</h2>
 * O plano declarou o risco residual com todas as letras: <i>"as 69 entradas novas passam a agir
 * na tradução; cada uma só dispara com o canônico presente no inglês em grafia exata, então o
 * risco é baixo — mas 'baixo' não é 'medido'"</i>. Este harness é o que troca a palavra pelo
 * número. Sem ele, a união seria aplicada a 68 obras confiando num raciocínio sobre o
 * enforcador, e raciocínio sobre instrumento é exatamente o que já errou por 8× neste projeto.
 *
 * <p>O caso concreto que motivou olhar: {@code Canela → Shin} (86) é homógrafo de parte do
 * corpo em português. A leitura do código diz que ela é segura, porque {@code contarCanonico}
 * é SENSÍVEL À CAIXA para canônico de uma palavra — a canela do corpo em inglês é
 * {@code shin} minúsculo e não conta. Leitura de código não é medição; quem decide é o acervo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Usa o {@link EnforcadorTermosLore} REAL, injetado. Nenhum critério de substituição é
 *       reimplementado aqui — a segunda implementação sempre diverge.</li>
 *   <li>Os dois catálogos vêm dos provedores REAIS: {@link ProvedorContexto} pelo CDI e a
 *       revisão pelo {@link GerenciadorPromptRevisaoLore}, o agregador de produção.</li>
 *   <li>Aplica SOMENTE o delta — as entradas que a revisão tem e a tradução não. O que já
 *       existe nos dois lados não é efeito da união e contaminaria o número.</li>
 *   <li>Acervo ausente sai como NÃO VERIFICADO, nunca como aprovação.</li>
 * </ul>
 *
 * <h2>Limite declarado (o universo que este harness mede, e o que ele NÃO mede)</h2>
 * O cache guarda tradução ACEITA, produzida sem estas entradas. Logo o número responde
 * <b>"quantas falas já publicadas a união reescreveria"</b> — que é a pergunta da exposição —
 * e <b>não</b> "quantas ficariam melhores". Julgar melhora é leitura humana da amostra, e é
 * para isso que a amostra sai impressa com o antes e o depois.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não reprova pelo número: ele é o objeto da medição. Reprova só quando não há acervo, catálogo
 * ou delta para medir — os três casos em que não houve medição nenhuma.
 */
@QuarkusTest
@DisplayName("MEDIÇÃO: o que a união de lore faria na legenda já traduzida")
class MedicaoEfeitoDaUniaoDeLoreIT {

    private static final Path RAIZ_CACHE = Path.of("cache");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int AMOSTRA_MAXIMA = 25;

    @Inject
    List<ProvedorContexto> catalogoTraducao;

    @Inject
    GerenciadorPromptRevisaoLore catalogoRevisao;

    @Inject
    EnforcadorTermosLore enforcador;

    @Test
    void medirEfeitoDaUniao() throws Exception {
        assertTrue(Files.isDirectory(RAIZ_CACHE),
            "NÃO VERIFICADO: pasta cache/ ausente — sem acervo não há medição");

        Map<String, ProvedorContexto> traducaoPorId = new HashMap<>();
        for (ProvedorContexto p : catalogoTraducao) {
            traducaoPorId.put(p.getId(), p);
        }
        assertTrue(!traducaoPorId.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorContexto no CDI");

        // O DELTA: por obra, o que a revisão conhece e a tradução não. É exatamente o que a
        // união acrescentaria ao lado que escreve o .ass.
        Map<String, Map<String, String>> deltaPorObra = new TreeMap<>();
        for (ProvedorPromptRevisaoLore r : catalogoRevisao.getProvedores()) {
            ProvedorContexto t = traducaoPorId.get(r.getId());
            if (t == null) {
                continue;
            }
            Map<String, String> daRevisao = r.correcoesTerminologia();
            Map<String, String> daTraducao = t.correcoesTerminologia();
            Map<String, String> delta = new LinkedHashMap<>();
            if (daRevisao != null) {
                daRevisao.forEach((formaRuim, canonico) -> {
                    if (daTraducao == null || !daTraducao.containsKey(formaRuim)) {
                        delta.put(formaRuim, canonico);
                    }
                });
            }
            if (!delta.isEmpty()) {
                deltaPorObra.put(r.getId(), delta);
            }
        }
        int entradasNoDelta = deltaPorObra.values().stream().mapToInt(Map::size).sum();
        assertTrue(entradasNoDelta > 0,
            "NÃO VERIFICADO: delta vazio — ou a união já foi feita, ou a coleta dos catálogos "
                + "falhou. Zero aqui não é 'a união é inofensiva'.");

        List<Path> caches;
        try (Stream<Path> s = Files.walk(RAIZ_CACHE)) {
            caches = s.filter(p -> p.getFileName().toString().endsWith(".cache.json")).toList();
        }
        assertTrue(!caches.isEmpty(), "NÃO VERIFICADO: nenhum .cache.json sob cache/");

        Map<String, Integer> paresPorObra = new TreeMap<>();
        Map<String, Integer> mudariamPorObra = new TreeMap<>();
        Map<String, Integer> restauracoesPorTermo = new HashMap<>();
        List<String[]> amostra = new ArrayList<>();
        int paresTotais = 0;
        int cachesComDelta = 0;
        int semContexto = 0;

        for (Path cache : caches) {
            JsonNode raiz;
            try {
                raiz = JSON.readTree(Files.readString(cache));
            } catch (Exception ilegivel) {
                continue;
            }
            String contextoId = raiz.path("proveniencia").path("contextoId").asText("");
            ProvedorContexto ctx = traducaoPorId.get(contextoId);
            if (ctx == null) {
                semContexto++;
                continue;
            }
            Map<String, String> delta = deltaPorObra.get(contextoId);
            if (delta == null) {
                continue;
            }
            cachesComDelta++;
            String obra = ctx.getNomeExibicao();

            for (JsonNode e : raiz.path("entradas")) {
                String original = e.path("original").asText("");
                String traduzido = e.path("traduzido").asText("");
                if (original.isBlank() || traduzido.isBlank()) {
                    continue;
                }
                paresTotais++;
                paresPorObra.merge(obra, 1, Integer::sum);

                EnforcadorTermosLore.Reforco r = enforcador.reforcarContando(original, traduzido, delta);
                if (r.total() == 0) {
                    continue;
                }
                mudariamPorObra.merge(obra, 1, Integer::sum);
                r.restauracoesPorTermo().forEach((canonico, quantas) ->
                    restauracoesPorTermo.merge(canonico + "  [" + obra + "]", quantas, Integer::sum));
                if (amostra.size() < AMOSTRA_MAXIMA) {
                    amostra.add(new String[] { obra, original, traduzido, r.texto() });
                }
            }
        }

        // TERCEIRO ESTADO, e ele é obrigatório aqui: obra com delta que não tem NENHUM cache não
        // foi medida. Sem esta lista, ela simplesmente não aparece na tabela e o total de
        // "0 reescritas" passa por "a união é inofensiva nesta obra" — que é o "0 não é prova"
        // acontecendo dentro da própria medição. A primeira versão deste harness tinha esse furo:
        // imprimia 6 obras de 17 e não dizia nada sobre as outras 11.
        Map<String, Integer> naoVerificadas = new TreeMap<>();
        deltaPorObra.forEach((id, delta) -> {
            ProvedorContexto ctx = traducaoPorId.get(id);
            String obra = ctx == null ? id : ctx.getNomeExibicao();
            if (!paresPorObra.containsKey(obra)) {
                naoVerificadas.put(obra, delta.size());
            }
        });

        int mudariamTotais = mudariamPorObra.values().stream().mapToInt(Integer::intValue).sum();
        int entradasNaoVerificadas = naoVerificadas.values().stream().mapToInt(Integer::intValue).sum();
        System.out.println();
        System.out.println("=== EFEITO DA UNIÃO DE LORE NA LEGENDA JÁ TRADUZIDA ===");
        System.out.println("  obras com delta ................ " + deltaPorObra.size()
            + "   (medidas: " + (deltaPorObra.size() - naoVerificadas.size())
            + " · SEM ACERVO, não verificadas: " + naoVerificadas.size() + ")");
        System.out.println("  entradas no delta .............. " + entradasNoDelta
            + "   (em obra sem acervo: " + entradasNaoVerificadas + ")");
        System.out.println("  caches lidos ................... " + caches.size()
            + "   (com delta: " + cachesComDelta + " · sem contexto: " + semContexto + ")");
        System.out.println("  pares original/tradução ........ " + paresTotais);
        System.out.println("  falas que a união REESCREVERIA . " + mudariamTotais
            + (paresTotais == 0 ? "" : String.format("   (%.3f%%)", 100.0 * mudariamTotais / paresTotais)));
        System.out.println();
        System.out.println("POR OBRA (reescritas / pares):");
        paresPorObra.entrySet().stream()
            .sorted((a, b) -> Integer.compare(mudariamPorObra.getOrDefault(b.getKey(), 0),
                mudariamPorObra.getOrDefault(a.getKey(), 0)))
            .forEach(e -> {
                int m = mudariamPorObra.getOrDefault(e.getKey(), 0);
                System.out.printf("  %-38s %6d / %6d   %6.3f%%%n",
                    e.getKey(), m, e.getValue(), 100.0 * m / e.getValue());
            });
        System.out.println();
        System.out.println("OBRAS COM DELTA E SEM ACERVO — NÃO VERIFICADAS (não é 'inofensivo'):");
        if (naoVerificadas.isEmpty()) {
            System.out.println("  (nenhuma — todas as obras com delta têm cache)");
        } else {
            naoVerificadas.forEach((obra, quantas) ->
                System.out.printf("  %-38s %3d entrada(s) do delta sem uma única fala para exercitar%n",
                    obra, quantas));
        }
        System.out.println();
        System.out.println("TERMOS QUE MAIS DISPARARIAM:");
        restauracoesPorTermo.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(30)
            .forEach(e -> System.out.printf("  %5d  %s%n", e.getValue(), e.getKey()));
        System.out.println();
        System.out.println("AMOSTRA (antes -> depois):");
        for (String[] a : amostra) {
            System.out.println("  [" + a[0] + "]");
            System.out.println("    EN....: " + corte(a[1]));
            System.out.println("    HOJE..: " + corte(a[2]));
            System.out.println("    UNIAO.: " + corte(a[3]));
        }
    }

    private static String corte(String s) {
        return s.length() > 110 ? s.substring(0, 110) + "…" : s;
    }
}
