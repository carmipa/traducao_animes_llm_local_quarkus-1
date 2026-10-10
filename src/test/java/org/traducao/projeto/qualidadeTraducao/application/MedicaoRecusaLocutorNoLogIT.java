package org.traducao.projeto.qualidadeTraducao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: dá o denominador que o limite declarado do locutor inventado exige antes
 * de mudar ("a medição que autoriza precisa vir junto"): toda recusa por locutor inventado que o
 * console real já registrou, passada de novo pelo validador de HOJE. Rodado antes e depois de uma
 * mudança na regra, a diferença por identidade é exatamente o que a mudança passou a aceitar.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Pergunta ao validador REAL ({@link ValidadorTraducaoService#validarPar}); não reimplementa.</li>
 *   <li>Só leitura: {@code logs/console-web.log} ao lado da raiz de cache dada por
 *       {@code -Dkronos.medicao.cache}; escreve só em {@code build/}.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Log ausente ou zero recusas lidas REPROVAM: zero por instrumento cego não é "nenhuma recusa".
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
@DisplayName("MEDIÇÃO: recusas reais por locutor inventado contra o validador de hoje")
class MedicaoRecusaLocutorNoLogIT {

    private static final Pattern RECUSA = Pattern.compile(
        "Locutor/narra\\S{1,4}o inventado, ausente no original: \"(.*)\" \\(original: \"(.*)\"\\)");

    @Inject
    ValidadorTraducaoService validador;

    @Test
    void reavaliarRecusasDoLog() throws Exception {
        Path log = LeitorAcervoCache.raizPadrao().toAbsolutePath().getParent().resolve("logs").resolve("console-web.log");
        assertTrue(Files.isRegularFile(log), "NÃO VERIFICADO: log ausente em " + log);

        Map<String, String> pares = new LinkedHashMap<>();
        try (BufferedReader r = Files.newBufferedReader(log, StandardCharsets.UTF_8)) {
            String linha;
            while ((linha = r.readLine()) != null) {
                if (!linha.contains("inventado, ausente no original")) {
                    continue;
                }
                Matcher m = RECUSA.matcher(linha);
                if (m.find()) {
                    pares.putIfAbsent(m.group(2) + " || " + m.group(1), m.group(2));
                }
            }
        }
        assertFalse(pares.isEmpty(), "NÃO VERIFICADO: nenhuma recusa lida em " + log);

        List<String> aceitas = new ArrayList<>();
        List<String> recusadas = new ArrayList<>();
        for (Map.Entry<String, String> e : pares.entrySet()) {
            String traduzido = e.getKey().substring(e.getValue().length() + 4);
            try {
                validador.validarPar(e.getValue(), traduzido);
                aceitas.add(e.getKey());
            } catch (RuntimeException recusa) {
                recusadas.add(e.getKey() + "   <- " + recusa.getMessage().split(":")[0]);
            }
        }
        StringBuilder sb = new StringBuilder("=== RECUSAS POR LOCUTOR INVENTADO NO LOG x VALIDADOR DE HOJE ===\n");
        sb.append("log: ").append(log).append("\npares distintos: ").append(pares.size())
            .append(" | aceitos hoje: ").append(aceitas.size())
            .append(" | ainda recusados: ").append(recusadas.size()).append("\n\n--- ACEITOS HOJE ---\n");
        aceitas.forEach(a -> sb.append(a).append('\n'));
        sb.append("\n--- AINDA RECUSADOS ---\n");
        recusadas.forEach(a -> sb.append(a).append('\n'));
        Path saida = Path.of("build", "medicao-recusa-locutor-no-log.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, sb.toString());
        System.out.println(sb);
    }
}
