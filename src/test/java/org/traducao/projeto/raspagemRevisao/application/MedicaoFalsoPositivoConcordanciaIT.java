package org.traducao.projeto.raspagemRevisao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: perguntar ao DETECTOR DE PRODUÇÃO, sobre o acervo inteiro, quantas
 * falas ele acusa e com que motivo — para separar achado real de falso positivo antes de
 * qualquer conserto.
 *
 * <p>Nasceu do log do Guilty Crown de 22/08/2026: das 3 falas acusadas na corrida inteira,
 * DUAS pareciam falso positivo à leitura. Duas em três é taxa alta demais para julgar por
 * amostra — e "parecia" não é medição. Este harness roda o detector sobre os pares EN/PT dos
 * caches, que é a mesma entrada que a tela 3.1 usa.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Usa {@link DetectorConcordanciaService} de PRODUÇÃO, com o par (original, traduzido) do
 *       cache — não reimplementa nenhum critério.</li>
 *   <li>NÃO escreve nada. Lê e imprime, com amostra por motivo para julgamento humano.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}: lê o cache real, que não existe no CI.</li>
 *   <li>Cache ausente <b>REPROVA</b>, não pula: resultado vazio por cache inacessível não pode
 *       ser lido como "o detector não acusa nada".</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Arquivo ilegível é CONTADO e reportado, nunca descartado em silêncio.
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoFalsoPositivoConcordanciaIT {

    private static final Path CACHE = Path.of(System.getProperty("kronos.cache", "cache"));
    private static final int AMOSTRAS_POR_MOTIVO = 6;

    @Test
    @DisplayName("mede quantas falas o detector de concordancia acusa, e por que motivo")
    void medeOsMotivosNoAcervo() throws IOException {
        // "NAO VERIFICADO" na mensagem e deliberado: cache ausente reprova, e a mensagem tem
        // de dizer que NAO SE MEDIU — nunca que o detector nao achou falso positivo.
        assertTrue(Files.isDirectory(CACHE),
            "cache inacessivel em " + CACHE.toAbsolutePath() + " — sem ele o resultado vazio "
                + "significaria \"nao consegui medir\", nunca \"o detector nao acusa nada\"");

        DetectorConcordanciaService detector = new DetectorConcordanciaService();
        ObjectMapper mapper = new ObjectMapper();

        List<Path> arquivos = new ArrayList<>();
        try (Stream<Path> caminhada = Files.walk(CACHE)) {
            caminhada.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".cache.json"))
                .forEach(arquivos::add);
        }
        assertFalse(arquivos.isEmpty(),
            "nenhum .cache.json sob " + CACHE.toAbsolutePath() + " — instrumento cego");

        int pares = 0;
        int ilegiveis = 0;
        int acusadas = 0;
        Map<String, Integer> porMotivo = new LinkedHashMap<>();
        Map<String, List<String>> amostraPorMotivo = new LinkedHashMap<>();

        for (Path arquivo : arquivos) {
            JsonNode raiz;
            try {
                raiz = mapper.readTree(arquivo.toFile());
            } catch (IOException e) {
                ilegiveis++;
                continue;
            }
            for (JsonNode entrada : raiz.path("entradas")) {
                String original = entrada.path("original").asText(null);
                String traduzido = entrada.path("traduzido").asText(null);
                if (original == null || traduzido == null || traduzido.isBlank()) {
                    continue;
                }
                pares++;
                ResultadoDeteccaoConcordancia r = detector.analisar(original, traduzido);
                if (r.motivos().isEmpty()) {
                    continue;
                }
                acusadas++;
                for (String motivo : r.motivos()) {
                    porMotivo.merge(motivo, 1, Integer::sum);
                    List<String> amostra =
                        amostraPorMotivo.computeIfAbsent(motivo, k -> new ArrayList<>());
                    if (amostra.size() < AMOSTRAS_POR_MOTIVO) {
                        amostra.add("EN: " + recorte(original) + "  |  PT: " + recorte(traduzido));
                    }
                }
            }
        }

        System.out.println("=== MOTIVOS DO DETECTOR DE CONCORDANCIA NO ACERVO ===");
        System.out.println("caches lidos : " + arquivos.size()
            + (ilegiveis > 0 ? "  (" + ilegiveis + " ILEGIVEIS)" : ""));
        System.out.println("pares EN/PT  : " + pares);
        System.out.println("falas ACUSADAS: " + acusadas
            + String.format("  (%.2f%%)", 100.0 * acusadas / Math.max(pares, 1)));
        System.out.println();
        porMotivo.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(e -> {
                System.out.println("### " + e.getValue() + "  " + e.getKey());
                amostraPorMotivo.getOrDefault(e.getKey(), List.of())
                    .forEach(linha -> System.out.println("      " + linha));
            });
    }

    private static String recorte(String texto) {
        String limpo = texto.replaceAll("\\{[^}]*}", "").replace("\\N", " ").trim();
        return limpo.length() <= 90 ? limpo : limpo.substring(0, 90) + "...";
    }
}
