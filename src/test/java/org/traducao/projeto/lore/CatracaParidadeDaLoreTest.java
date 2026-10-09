package org.traducao.projeto.lore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;
import org.traducao.projeto.lore.infrastructure.CatalogoLoreSqlite;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: congela o que hoje está igual entre as obras POR CÓPIA — e que, por ser
 * cópia, pode divergir numa obra só sem ninguém ver.
 *
 * <h2>O que foi medido em 2026-10-09</h2>
 * <pre>
 *   núcleo UC .............. 52 pares de terminologia, idênticos, em exatamente 23 obras Gundam
 *   prompt-base tradução ... 40 linhas presentes nos 69 prompts (78% dos bytes de prompt)
 *   prompt-base revisão .... 30 linhas presentes nos 69 prompts
 * </pre>
 * Cada obra tem o seu arquivo, e melhorar uma regra do núcleo ou uma linha do prompt-base exige
 * editar 23 ou 69 arquivos. Esquecer um não quebra nada à vista: aquela obra passa a traduzir com a
 * regra velha. Esta catraca transforma o esquecimento em reprovação.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Cada obra da lista do núcleo contém TODOS os 52 pares, com o mesmo canônico. Par a mais,
 *       próprio da obra, é livre.</li>
 *   <li>Cada prompt contém cada linha-base, inteira, como linha. Linha a mais, própria da obra, é
 *       livre.</li>
 *   <li>Os gabaritos ({@code nucleo-uc.tsv}, {@code prompt-base-*.txt}) foram extraídos da lore no
 *       dia da migração e não são regravados por teste: mudar o núcleo ou o prompt-base de
 *       propósito é mudar as obras E o gabarito no mesmo commit.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista obra a obra o que falta ou mudou. Gabarito ausente ou vazio reprova como NÃO VERIFICADO.
 */
@DisplayName("CATRACA: o que é cópia entre obras continua igual em todas")
class CatracaParidadeDaLoreTest {

    private final CatalogoLoreSqlite catalogo = new CatalogoLoreSqlite();

    @Test
    @DisplayName("as 23 obras UC têm os 52 pares do núcleo, iguais")
    void nucleoUcIgualNasObras() {
        Nucleo nucleo = nucleo();
        Map<String, Map<String, String>> porObra = new LinkedHashMap<>();
        catalogo.obras().forEach(p -> porObra.put(p.getId(), p.correcoesTerminologia()));

        List<String> faltas = new ArrayList<>();
        for (String id : nucleo.obras()) {
            Map<String, String> mapa = porObra.get(id);
            if (mapa == null) {
                faltas.add(id + ": a obra sumiu da lore");
                continue;
            }
            faltas.addAll(divergenciasDoNucleo(id, mapa, nucleo.pares()));
        }
        assertTrue(faltas.isEmpty(), () -> faltas.size() + " divergência(s) do núcleo UC:\n  " + String.join("\n  ", faltas));
    }

    @Test
    @DisplayName("todo prompt de tradução e de revisão tem as linhas do prompt-base")
    void promptBaseEmTodosOsPrompts() {
        List<String> faltas = new ArrayList<>();
        faltas.addAll(divergenciasDoPromptBase("tradução", linhas("/lore/prompt-base-traducao.txt"),
            catalogo.obras().stream().collect(Collectors.toMap(ProvedorContexto::getId, ProvedorContexto::obterPromptSistema))));
        faltas.addAll(divergenciasDoPromptBase("revisão", linhas("/lore/prompt-base-revisao.txt"),
            catalogo.obrasRevisao().stream().collect(Collectors.toMap(ProvedorPromptRevisaoLore::getId,
                ProvedorPromptRevisaoLore::obterPromptSistema))));
        assertTrue(faltas.isEmpty(), () -> faltas.size() + " prompt(s) sem linha do prompt-base:\n  "
            + String.join("\n  ", faltas.subList(0, Math.min(20, faltas.size()))));
    }

    /**
     * Calibração com resposta conhecida: o par trocado e o par ausente reprovam; o par a mais passa.
     * A linha-base alterada em um caractere reprova; a linha a mais passa.
     */
    @Test
    @DisplayName("calibração: a catraca separa a divergência do acréscimo legítimo")
    void calibracao() {
        Map<String, String> nucleo = Map.of("Traje Móvel", "Mobile Suit", "Armadura Móvel", "Mobile Armor");
        assertEquals(List.of(), divergenciasDoNucleo("x", Map.of("Traje Móvel", "Mobile Suit",
            "Armadura Móvel", "Mobile Armor", "Próprio da obra", "Own"), nucleo));
        assertEquals(1, divergenciasDoNucleo("x", Map.of("Traje Móvel", "Mobile Suit"), nucleo).size());
        assertEquals(1, divergenciasDoNucleo("x", Map.of("Traje Móvel", "Mobile Suits",
            "Armadura Móvel", "Mobile Armor"), nucleo).size());

        List<String> base = List.of("- Preserve sentido.", "- Use português natural.");
        assertEquals(List.of(), divergenciasDoPromptBase("t", base,
            Map.of("x", "Topo\n- Preserve sentido.\nLinha própria\n- Use português natural.")));
        assertEquals(1, divergenciasDoPromptBase("t", base,
            Map.of("x", "- Preserve sentido\n- Use português natural.")).size());
    }

    static List<String> divergenciasDoNucleo(String id, Map<String, String> mapa, Map<String, String> pares) {
        List<String> d = new ArrayList<>();
        pares.forEach((forma, canonico) -> {
            String atual = mapa.get(forma);
            if (atual == null) {
                d.add(id + ": falta \"" + forma + "\" -> \"" + canonico + "\"");
            } else if (!atual.equals(canonico)) {
                d.add(id + ": \"" + forma + "\" aponta para \"" + atual + "\", o núcleo diz \"" + canonico + "\"");
            }
        });
        return d;
    }

    static List<String> divergenciasDoPromptBase(String lado, List<String> base, Map<String, String> prompts) {
        List<String> d = new ArrayList<>();
        prompts.forEach((id, prompt) -> {
            Set<String> linhas = Set.copyOf(List.of(prompt.split("\n", -1)));
            for (String l : base) {
                if (!linhas.contains(l)) {
                    d.add(lado + " " + id + ": falta a linha-base \"" + (l.length() > 80 ? l.substring(0, 80) + "…" : l) + "\"");
                }
            }
        });
        return d;
    }

    record Nucleo(List<String> obras, Map<String, String> pares) {
    }

    private static Nucleo nucleo() {
        List<String> obras = new ArrayList<>();
        Map<String, String> pares = new LinkedHashMap<>();
        for (String l : linhas("/lore/nucleo-uc.tsv")) {
            if (l.startsWith("obras=")) {
                obras.addAll(List.of(l.substring("obras=".length()).split(",")));
            } else {
                String[] c = l.split("\t", -1);
                assertEquals(2, c.length, () -> "linha malformada no nucleo-uc.tsv: " + l);
                pares.put(c[0], c[1]);
            }
        }
        assertTrue(obras.size() > 0 && pares.size() > 0, "NÃO VERIFICADO: nucleo-uc.tsv sem obras ou sem pares");
        return new Nucleo(obras, pares);
    }

    private static List<String> linhas(String recurso) {
        Function<String, String> ler = r -> {
            try (InputStream in = CatracaParidadeDaLoreTest.class.getResourceAsStream(r)) {
                assertTrue(in != null, "NÃO VERIFICADO: gabarito ausente " + r);
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        };
        List<String> saida = ler.apply(recurso).lines().filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
        assertTrue(!saida.isEmpty(), "NÃO VERIFICADO: gabarito vazio " + recurso);
        return saida;
    }
}
