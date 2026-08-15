package org.traducao.projeto.lore;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;
import org.traducao.projeto.lore.domain.ProvedorPromptRevisaoLore;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede o quanto os DOIS catálogos de lore da mesma obra já divergiram.
 *
 * <h2>Por que existe</h2>
 * Cada obra tem dois provedores com um mapa {@code correcoesTerminologia()}: um para a
 * Tradução Local ({@link ProvedorContexto}) e outro para a Revisão de Lore
 * ({@link ProvedorPromptRevisaoLore}). São a MESMA verdade — a forma errada que o modelo
 * produz e a grafia canônica que a obra exige — escrita em dois lugares.
 *
 * <p>Levantado em 2026-08-15 quando Paulo apontou que <i>"nós temos uma pasta de lore"</i>,
 * depois de "Spearhead" sair como {@code Esquadroe de Ponta} na legenda final. No 86 os dois
 * mapas já estavam diferentes: a Revisão conhecia {@code Canela → Shin},
 * {@code Para RAID → Para-RAID} e {@code Jugernaut → Juggernaut}, que a Tradução não tinha.
 * Quem traduz não enxerga o que quem revisa aprendeu, e vice-versa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Lê os provedores REAIS pelo CDI. Nenhum catálogo é copiado para cá.</li>
 *   <li>Compara por {@code getId()}: é o mesmo identificador de obra nos dois lados.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não reprova pela divergência — ela é o objeto da medição, e congelá-la como catraca exige
 * decisão sobre qual lado é a fonte de verdade. Reprova apenas se um dos lados vier vazio,
 * porque aí não houve medição.
 */
@QuarkusTest
@DisplayName("MEDIÇÃO: divergência entre os dois catálogos de lore da mesma obra")
class MedicaoDivergenciaEntreCatalogosDeLoreIT {

    // Instance<>, e não List<>: é como a produção coleta os provedores de revisão
    // (GerenciadorPromptRevisaoLore). Com List<> o Arc não resolve e o teste nem sobe.
    @Inject
    java.util.List<ProvedorContexto> catalogoTraducao;

    @Inject
    jakarta.enterprise.inject.Instance<ProvedorPromptRevisaoLore> instRevisao;

    @Test
    void medirDivergencia() {
        java.util.List<ProvedorContexto> daTraducao = catalogoTraducao;
        java.util.List<ProvedorPromptRevisaoLore> daRevisao = instRevisao.stream().toList();
        assertTrue(!daTraducao.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorContexto no CDI");
        assertTrue(!daRevisao.isEmpty(), "NÃO VERIFICADO: nenhum ProvedorPromptRevisaoLore no CDI");

        Map<String, Map<String, String>> mapaTraducao = new TreeMap<>();
        for (ProvedorContexto p : daTraducao) {
            Map<String, String> m = p.correcoesTerminologia();
            mapaTraducao.put(p.getId(), m == null ? Map.of() : m);
        }
        Map<String, Map<String, String>> mapaRevisao = new TreeMap<>();
        Map<String, String> nomePorId = new TreeMap<>();
        for (ProvedorPromptRevisaoLore p : daRevisao) {
            Map<String, String> m = p.correcoesTerminologia();
            mapaRevisao.put(p.getId(), m == null ? Map.of() : m);
            nomePorId.put(p.getId(), p.getNomeExibicao());
        }

        Set<String> obrasNosDois = new TreeSet<>(mapaTraducao.keySet());
        obrasNosDois.retainAll(mapaRevisao.keySet());

        int soNaTraducao = 0;
        int soNaRevisao = 0;
        int conflitantes = 0;
        int obrasDivergentes = 0;

        System.out.println();
        System.out.println("=== DIVERGÊNCIA ENTRE OS DOIS CATÁLOGOS DE LORE ===");
        System.out.println("  obras com provedor de tradução ..... " + mapaTraducao.size());
        System.out.println("  obras com provedor de revisão ...... " + mapaRevisao.size());
        System.out.println("  obras nos DOIS ..................... " + obrasNosDois.size());
        System.out.println();

        for (String id : obrasNosDois) {
            Map<String, String> t = mapaTraducao.get(id);
            Map<String, String> r = mapaRevisao.get(id);
            if (t.isEmpty() && r.isEmpty()) {
                continue;
            }
            Set<String> soT = new TreeSet<>(t.keySet());
            soT.removeAll(r.keySet());
            Set<String> soR = new TreeSet<>(r.keySet());
            soR.removeAll(t.keySet());
            Set<String> comuns = new TreeSet<>(t.keySet());
            comuns.retainAll(r.keySet());
            Set<String> conflito = new TreeSet<>();
            for (String k : comuns) {
                if (!java.util.Objects.equals(t.get(k), r.get(k))) {
                    conflito.add(k + ": tradução=\"" + t.get(k) + "\" × revisão=\"" + r.get(k) + "\"");
                }
            }
            if (soT.isEmpty() && soR.isEmpty() && conflito.isEmpty()) {
                continue;
            }
            obrasDivergentes++;
            soNaTraducao += soT.size();
            soNaRevisao += soR.size();
            conflitantes += conflito.size();
            System.out.println("  " + id + "  (" + nomePorId.getOrDefault(id, "?") + ")");
            System.out.println("     tradução " + t.size() + " entradas · revisão " + r.size());
            if (!soT.isEmpty()) {
                System.out.println("     só na TRADUÇÃO: " + soT);
            }
            if (!soR.isEmpty()) {
                System.out.println("     só na REVISÃO.: " + soR);
            }
            for (String c : conflito) {
                System.out.println("     CONFLITO: " + c);
            }
        }

        System.out.println();
        System.out.println("TOTAIS");
        System.out.println("  obras divergentes .............. " + obrasDivergentes + " de " + obrasNosDois.size());
        System.out.println("  entradas só na tradução ........ " + soNaTraducao);
        System.out.println("  entradas só na revisão ......... " + soNaRevisao);
        System.out.println("  mesma chave, canônico DIFERENTE  " + conflitantes);
    }
}
