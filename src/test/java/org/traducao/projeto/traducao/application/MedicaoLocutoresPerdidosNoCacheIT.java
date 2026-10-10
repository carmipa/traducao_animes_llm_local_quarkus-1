package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.medicao.LeitorAcervoCache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o CUSTO, no cache real, de recusar a entrada que perdeu a separação dos
 * locutores (regra A8): quantas falas de vários locutores o acervo já tem, quantas saíram tortas e,
 * portanto, quantas voltariam ao modelo na próxima execução da 2.1.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Pergunta ao dono da regra ({@link FalaDeLocutores#locutoresPerdidos}); não reimplementa.</li>
 *   <li>Só leitura do cache dado por {@code -Dkronos.medicao.cache}; escreve só em {@code build/}.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cache ausente ou vazio REPROVA — zero por instrumento cego não é "nenhuma fala torta".
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
@DisplayName("MEDIÇÃO: falas de locutores com a separação perdida no cache real")
class MedicaoLocutoresPerdidosNoCacheIT {

    @Test
    void medir() throws Exception {
        Path raiz = LeitorAcervoCache.raizPadrao();
        assertTrue(Files.isDirectory(raiz), "NÃO VERIFICADO: cache ausente em " + raiz);
        LeitorAcervoCache.Acervo acervo = LeitorAcervoCache.ler(raiz);
        assertFalse(acervo.falas().isEmpty(), "NÃO VERIFICADO: nenhuma entrada lida");

        assertTrue(FalaDeLocutores.locutoresPerdidos("- Why did you dodge?\\N- Yes.", "Por que você\\Ndesviou? - Sim.") != null,
            "calibracao: o caso doente do Reconguista tem de ser acusado");
        assertTrue(FalaDeLocutores.locutoresPerdidos("- Go over there.\\N- Sorry.", "- Vá até lá.\\N- Desculpe.") == null,
            "calibracao: o caso sao tem de passar");

        int deLocutores = 0;
        Map<String, int[]> porObra = new TreeMap<>();
        List<String> amostra = new ArrayList<>();
        for (LeitorAcervoCache.FalaDoAcervo fala : acervo.falas()) {
            if (FalaDeLocutores.decompor(fala.original()).isEmpty()) {
                continue;
            }
            deLocutores++;
            int[] c = porObra.computeIfAbsent(fala.obra(), k -> new int[2]);
            c[0]++;
            if (FalaDeLocutores.locutoresPerdidos(fala.original(), fala.traduzido()) != null) {
                c[1]++;
                if (amostra.size() < 40) {
                    amostra.add(fala.obra() + " | " + fala.original() + "  =>  " + fala.traduzido());
                }
            }
        }
        StringBuilder sb = new StringBuilder("=== FALAS DE LOCUTORES NO CACHE REAL ===\n");
        sb.append("cache: ").append(raiz).append(" | entradas: ").append(acervo.falas().size())
            .append(" | de locutores: ").append(deLocutores).append("\n\n");
        int tortas = 0;
        for (Map.Entry<String, int[]> e : porObra.entrySet()) {
            tortas += e.getValue()[1];
            sb.append(String.format("%-60s %6d de locutores, %6d tortas%n", e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
        sb.append("\nTOTAL tortas: ").append(tortas).append(" de ").append(deLocutores).append("\n\n--- amostra ---\n");
        amostra.forEach(a -> sb.append(a).append('\n'));
        Path saida = Path.of("build", "medicao-locutores-perdidos-no-cache.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, sb.toString());
        System.out.println(sb);
    }
}
