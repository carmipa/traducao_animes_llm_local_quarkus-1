package org.traducao.projeto.qualidadeTraducao.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: responde, com número, a pergunta que o próprio código deixou em
 * aberto — quantas falas do acervo o {@code temLocutorInventado} recusa por ter um
 * complemento entre o verbo de elocução e os dois-pontos, e quantas dessas recusas são
 * tradução CORRETA.
 *
 * <h2>Por que este harness existe</h2>
 * {@code ConectivoDeDiscursoNaoEhLocutorInventadoTest#oLimiteQuePermanece} congela o limite
 * atual e diz, com todas as letras, o que falta antes de alargá-lo: <i>"alargar exige medir
 * antes quantas falas do acervo o padrão passaria a poupar e quantas narrações inventadas
 * escapariam junto"</i>. Enquanto essa medição não existir, mexer no padrão é chute — e o
 * custo conhecido é o episódio 06 do 86, que saiu PARCIAL em 14/08 e DE NOVO em 15/08, com a
 * fala publicada em inglês na legenda final.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Pergunta ao validador REAL ({@link ValidadorTraducaoService}, injetado). Não
 *       reimplementa o critério: critério reimplementado diverge do de produção, e foi assim
 *       que 14 medições saíram erradas numa única sessão.</li>
 *   <li>Acervo ausente NÃO é aprovação: sem cache para ler, o teste falha com a mensagem de
 *       "não verifiquei" em vez de passar em silêncio sobre zero arquivo.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Não reprova por causa do NÚMERO — ele é o que se quer descobrir. Reprova apenas se não
 * houver acervo para medir. O resultado sai no stdout do teste, para virar decisão.
 */
@QuarkusTest
@DisplayName("MEDIÇÃO: quanto o limite do locutor inventado custa no acervo")
class MedicaoLocutorInventadoNoAcervoIT {

    private static final Path RAIZ_CACHE = Path.of("cache");
    private static final ObjectMapper JSON = new ObjectMapper();

    @Inject
    ValidadorTraducaoService validador;

    @Test
    void medirRecusasPorLocutorInventado() throws Exception {
        if (!Files.isDirectory(RAIZ_CACHE)) {
            assertTrue(false, "NÃO VERIFICADO: pasta cache/ ausente — sem acervo não há medição, "
                + "e passar aqui seria aprovar por cegueira");
        }

        List<Path> caches;
        try (Stream<Path> s = Files.walk(RAIZ_CACHE)) {
            caches = s.filter(p -> p.getFileName().toString().endsWith(".cache.json")).toList();
        }
        assertTrue(!caches.isEmpty(), "NÃO VERIFICADO: nenhum .cache.json sob cache/");

        int pares = 0;
        List<String[]> recusadas = new ArrayList<>();

        for (Path cache : caches) {
            JsonNode raiz;
            try {
                raiz = JSON.readTree(Files.readString(cache));
            } catch (Exception ilegivel) {
                continue; // cache corrompido é problema de outra guarda
            }
            JsonNode entradas = raiz.path("entradas");
            if (!entradas.isArray()) {
                continue;
            }
            for (JsonNode e : entradas) {
                String original = e.path("original").asText("");
                String traduzido = e.path("traduzido").asText("");
                if (original.isBlank() || traduzido.isBlank()) {
                    continue;
                }
                pares++;
                try {
                    validador.validarPar(original, traduzido);
                } catch (RuntimeException recusa) {
                    String motivo = String.valueOf(recusa.getMessage());
                    if (motivo.contains("Locutor/narração inventado")) {
                        recusadas.add(new String[] { original, traduzido });
                    }
                }
            }
        }

        // Classificação das recusas pela EVIDÊNCIA objetiva, não por julgamento: a tradução
        // abre discurso citado (aspas depois dos dois-pontos) E o original também traz um
        // trecho entre aspas? Se sim, os dois-pontos são a pontuação que o português exige
        // onde o inglês usa vírgula — não há falante inventado. Se não, é candidato legítimo
        // a invenção e continua tendo de ser recusado.
        int comAspasDosDoisLados = 0;
        Map<String, Integer> amostra = new LinkedHashMap<>();
        for (String[] par : recusadas) {
            String original = par[0];
            String traduzido = par[1];
            int doisPontos = traduzido.indexOf(':');
            String depois = doisPontos >= 0 ? traduzido.substring(doisPontos + 1).trim() : "";
            boolean traduzidoCita = depois.startsWith("\"") || depois.startsWith("“") || depois.startsWith("«");
            boolean originalCita = original.contains("\"") || original.contains("“") || original.contains("«");
            if (traduzidoCita && originalCita) {
                comAspasDosDoisLados++;
                if (amostra.size() < 25) {
                    amostra.put(original, amostra.size());
                    System.out.println("  CITADO  EN: " + original);
                    System.out.println("          PT: " + traduzido);
                }
            }
        }

        System.out.println();
        System.out.println("=== MEDIÇÃO: locutor inventado no acervo ===");
        System.out.println("  caches lidos ................... " + caches.size());
        System.out.println("  pares original/tradução ........ " + pares);
        System.out.println("  recusados por locutor inventado  " + recusadas.size());
        System.out.println("  destes, discurso CITADO nos dois lados: " + comAspasDosDoisLados);
        System.out.println("  destes, sem aspas dos dois lados ....... "
            + (recusadas.size() - comAspasDosDoisLados) + "  <- candidatos a invenção real");
        System.out.println();
        System.out.println("AMOSTRA DOS QUE NÃO SÃO CITAÇÃO (os que a regra deve continuar recusando):");
        int mostrados = 0;
        for (String[] par : recusadas) {
            int dp = par[1].indexOf(':');
            String depois = dp >= 0 ? par[1].substring(dp + 1).trim() : "";
            boolean cita = depois.startsWith("\"") || depois.startsWith("“") || depois.startsWith("«");
            boolean origCita = par[0].contains("\"") || par[0].contains("“") || par[0].contains("«");
            if (!(cita && origCita)) {
                System.out.println("  EN: " + par[0]);
                System.out.println("  PT: " + par[1]);
                if (++mostrados >= 20) {
                    break;
                }
            }
        }
    }
}
