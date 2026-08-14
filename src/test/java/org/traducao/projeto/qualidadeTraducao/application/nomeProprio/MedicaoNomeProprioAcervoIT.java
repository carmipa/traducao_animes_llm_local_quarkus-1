package org.traducao.projeto.qualidadeTraducao.application.nomeProprio;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.core.texto.dicionarioOrtografia.CorretorOrtograficoLegenda;
import org.traducao.projeto.core.texto.dicionarioOrtografia.VeredictoPalavra;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede o VOLUME que o detector produziria numa obra real antes de ele ser
 * usado para julgar qualquer coisa — a pergunta é se ele aponta três nomes ou três mil.
 *
 * <h2>Por que esta medição precede o uso</h2>
 * A versão anterior desta ideia foi removida do projeto por gerar 323 falso positivo em 560
 * pendências. O erro de método não foi a heurística: foi confiar nela sem medir o volume primeiro.
 * Este teste roda o código de PRODUÇÃO — extrator e dicionário reais, nada reimplementado — sobre
 * as legendas em disco e imprime o que encontrou.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>PULA quando a pasta ou o hunspell não estão disponíveis. Nunca falha por ausência de
 *       ambiente, e nunca afirma "medido" sem ter medido.</li>
 *   <li>Não afirma qual é o número certo — imprime o observado. Congelar um número aqui seria
 *       catraca sobre acervo que muda, o que é alarme falso garantido.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Só reprova se o extrator devolver algo estruturalmente impossível — candidata vazia ou com
 * espaço —, que indicaria defeito no próprio extrator.
 */
@DisplayName("nome próprio: medição de volume em acervo real")
class MedicaoNomeProprioAcervoIT {

    private static final Path PASTA = Path.of("C:", "animes", "Memories (1995)", "legendas_extraidas_ass");

    @Test
    @DisplayName("mede quantas candidatas viram DESCONHECIDA nas legendas do Memories")
    void medirVolumeNoAcervo() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(PASTA), "pasta ausente — NÃO VERIFICADO: " + PASTA);

        List<Path> arquivos;
        try (Stream<Path> s = Files.list(PASTA)) {
            arquivos = s.filter(p -> p.getFileName().toString().endsWith(".ass")).sorted().toList();
        }
        Assumptions.assumeFalse(arquivos.isEmpty(), "nenhum .ass — NÃO VERIFICADO");

        var dicionario = new CorretorOrtograficoLegenda();
        StringBuilder relatorio = new StringBuilder("\n=== nome próprio: volume no acervo ===\n");
        Set<String> desconhecidasGerais = new TreeSet<>();

        for (Path arquivo : arquivos) {
            Set<String> candidatas = candidatasDoArquivo(arquivo);
            for (String c : candidatas) {
                assertFalse(c.isBlank(), "extrator devolveu candidata em branco");
                assertFalse(c.contains(" "), "extrator devolveu candidata com espaço: [" + c + "]");
            }
            if (candidatas.isEmpty()) {
                relatorio.append(String.format("%-34s  sem candidatas%n", nomeCurto(arquivo)));
                continue;
            }

            Map<String, VeredictoPalavra> vereditos = dicionario.classificar(candidatas);
            Assumptions.assumeFalse(vereditos.isEmpty(), "hunspell ausente — NÃO VERIFICADO");

            Set<String> desconhecidas = new TreeSet<>();
            long ingles = 0;
            for (Map.Entry<String, VeredictoPalavra> v : vereditos.entrySet()) {
                if (v.getValue() == VeredictoPalavra.DESCONHECIDA) {
                    desconhecidas.add(v.getKey());
                } else if (v.getValue() == VeredictoPalavra.RESIDUO_INGLES) {
                    ingles++;
                }
            }
            desconhecidasGerais.addAll(desconhecidas);
            relatorio.append(String.format("%-34s  candidatas=%-4d  inglês=%-4d  DESCONHECIDA=%d%n",
                nomeCurto(arquivo), candidatas.size(), ingles, desconhecidas.size()));
            relatorio.append("      ").append(amostra(desconhecidas)).append('\n');
        }

        relatorio.append("--- união de DESCONHECIDA nos três filmes: ")
            .append(desconhecidasGerais.size()).append(" ---\n")
            .append(String.join(", ", desconhecidasGerais)).append('\n');
        System.out.println(relatorio);

        assertTrue(true, "medição informativa: o número observado vai no relatório, não em assert");
    }

    /** Lê os textos de Dialogue e junta as candidatas de todas as falas. */
    private static Set<String> candidatasDoArquivo(Path arquivo) throws IOException {
        Set<String> todas = new LinkedHashSet<>();
        for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
            if (!linha.startsWith("Dialogue:")) {
                continue;
            }
            String[] campos = linha.split(",", 10);
            if (campos.length < 10) {
                continue;
            }
            todas.addAll(ExtratorCandidatosNomeProprio.candidatas(campos[9]));
        }
        return todas;
    }

    private static String amostra(Set<String> palavras) {
        return palavras.stream().limit(14).reduce((a, b) -> a + ", " + b).orElse("(nenhuma)");
    }

    private static String nomeCurto(Path arquivo) {
        String n = arquivo.getFileName().toString();
        return n.length() <= 32 ? n : n.substring(0, 32);
    }
}
