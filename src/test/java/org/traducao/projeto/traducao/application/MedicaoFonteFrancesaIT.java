package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: responde, ANTES de alguém gastar uma hora traduzindo, quantas falas em
 * FRANCÊS o pipeline classificaria como "já está em português" e portanto jamais enviaria ao LLM.
 *
 * <h2>A suspeita que originou a medição</h2>
 * {@code jaNoIdiomaAlvo} decide por duas vias, e as duas foram calibradas contra fonte INGLESA:
 * <ul>
 *   <li>{@code SINAL_PORTUGUES} — lista de stopwords "sem colisão com inglês". Sem colisão com
 *       inglês não é sem colisão com francês: {@code à} e {@code nos} são francês corrente.</li>
 *   <li>Ramo por diacríticos — exige dois acentos do conjunto {@code ãõçáéíóúâêôà} em palavra
 *       minúscula. O francês usa {@code é}, {@code è}, {@code ê}, {@code à}, {@code ç} e
 *       {@code ô} o tempo todo.</li>
 * </ul>
 * Uma fala classificada assim é PRESERVADA como está: fica em francês no arquivo final, sem
 * sequer virar pendência. É o pior desfecho possível — silencioso.
 *
 * <h2>Invariantes do domínio</h2>
 * Roda o detector de PRODUÇÃO, sem reimplementar o critério. PULA quando as legendas não estão
 * em disco. Não congela número: imprime o observado.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Só reprova se o detector lançar — o que já seria defeito por si.
 */
@DisplayName("fonte francesa: quantas falas o pipeline daria por já traduzidas")
class MedicaoFonteFrancesaIT {

    private static final Path PASTA_FR = Path.of("C:", "animes", "Memories (1995)", "legendas_extraidas_fr");
    private static final Path PASTA_EN = Path.of("C:", "animes", "Memories (1995)", "legendas_extraidas_ass");

    @Test
    @DisplayName("mede o falso 'já no idioma-alvo' em francês, com o inglês como caso-controle")
    void medirFalsoJaTraduzido() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(PASTA_FR), "pasta FR ausente — NÃO VERIFICADO");

        var detector = new DetectorIdiomaFonteService();
        StringBuilder r = new StringBuilder("\n=== fonte francesa: falso 'ja traduzido' ===\n");

        int totalFr = 0;
        int puladasFr = 0;
        List<String> exemplos = new ArrayList<>();

        for (Path arquivo : listarAss(PASTA_FR)) {
            int total = 0;
            int puladas = 0;
            for (String fala : falasDe(arquivo)) {
                total++;
                if (detector.jaNoIdiomaAlvo(fala, "pt-br")) {
                    puladas++;
                    if (exemplos.size() < 10) {
                        exemplos.add(fala.length() > 78 ? fala.substring(0, 78) : fala);
                    }
                }
            }
            totalFr += total;
            puladasFr += puladas;
            r.append(String.format("  FR  %-30s  falas=%-5d  PULADAS=%-5d  (%.1f%%)%n",
                curto(arquivo), total, puladas, pct(puladas, total)));
        }

        // CASO-CONTROLE: a mesma medição na faixa INGLESA, para separar "o detector é frouxo" de
        // "o detector é cego para francês". Se o inglês também pulasse muito, o problema seria
        // outro.
        int totalEn = 0;
        int puladasEn = 0;
        if (Files.isDirectory(PASTA_EN)) {
            for (Path arquivo : listarAss(PASTA_EN)) {
                for (String fala : falasDe(arquivo)) {
                    totalEn++;
                    if (detector.jaNoIdiomaAlvo(fala, "pt-br")) {
                        puladasEn++;
                    }
                }
            }
            r.append(String.format("  EN  %-30s  falas=%-5d  PULADAS=%-5d  (%.1f%%)  <- controle%n",
                "(três filmes)", totalEn, puladasEn, pct(puladasEn, totalEn)));
        }

        r.append(String.format("%n  TOTAL FR: %d de %d falas nunca chegariam ao LLM (%.1f%%)%n",
            puladasFr, totalFr, pct(puladasFr, totalFr)));
        r.append("  Exemplos do que ficaria em francês no arquivo final:\n");
        exemplos.forEach(e -> r.append("     ").append(e).append('\n'));

        // CONTRA-CONTROLE, e é o que protege o lado oposto: português de verdade, em escala, tem
        // de CONTINUAR sendo reconhecido. Uma guarda contra francês que derrubasse a detecção de
        // PT trocaria um dano por outro — fala boa voltaria ao LLM, com custo e risco de eco.
        int totalPt = 0;
        int reconhecidasPt = 0;
        for (Path arquivo : legendasPtDoAcervo()) {
            for (String fala : falasDe(arquivo)) {
                totalPt++;
                if (detector.jaNoIdiomaAlvo(fala, "pt-br")) {
                    reconhecidasPt++;
                }
            }
        }
        if (totalPt > 0) {
            r.append(String.format("%n  PT do acervo: %d de %d reconhecidas como português (%.1f%%)"
                + "  <- contra-controle, quanto MAIOR melhor%n",
                reconhecidasPt, totalPt, pct(reconhecidasPt, totalPt)));
        } else {
            r.append("\n  PT do acervo: NÃO VERIFICADO (nenhuma legenda traduzida encontrada)\n");
        }
        System.out.println(r);

        assertTrue(totalFr > 0, "nenhuma fala lida — NÃO VERIFICADO");
    }

    /**
     * Amostra de legendas JÁ TRADUZIDAS do acervo, para o contra-controle. Limitada a 12 arquivos
     * porque o ponto é medir a taxa, não varrer o disco inteiro.
     */
    private static List<Path> legendasPtDoAcervo() throws IOException {
        Path acervo = Path.of("C:", "animes");
        if (!Files.isDirectory(acervo)) {
            return List.of();
        }
        try (Stream<Path> s = Files.walk(acervo, 3)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".ass"))
                .filter(p -> {
                    Path pai = p.getParent();
                    return pai != null && pai.getFileName().toString().startsWith("traducao_ptbr");
                })
                .limit(12)
                .toList();
        }
    }

    private static List<Path> listarAss(Path pasta) throws IOException {
        try (Stream<Path> s = Files.list(pasta)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".ass")).sorted().toList();
        }
    }

    private static List<String> falasDe(Path arquivo) throws IOException {
        List<String> falas = new ArrayList<>();
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            if (!linha.startsWith("Dialogue:")) {
                continue;
            }
            String[] campos = linha.split(",", 10);
            if (campos.length == 10 && !campos[9].isBlank()) {
                falas.add(campos[9]);
            }
        }
        return falas;
    }

    private static double pct(int parte, int todo) {
        return todo == 0 ? 0.0 : parte * 100.0 / todo;
    }

    private static String curto(Path arquivo) {
        String n = arquivo.getFileName().toString();
        return n.length() <= 28 ? n : n.substring(n.length() - 28);
    }
}
