package org.traducao.projeto.auditoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.cachetraducao.domain.CacheDocumento;
import org.traducao.projeto.raspagemRevisao.application.CorretorDeterministicoConcordanciaService;
import org.traducao.projeto.raspagemRevisao.application.DetectorConcordanciaService;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: mede quanto o detector de concordância deixa de ver por causa da quebra
 * {@code \N} do ASS. Numa legenda, "Minha mãe" partida em duas linhas vira {@code Minha\Nmãe} — e
 * o {@code N} da quebra COLA na palavra seguinte, matando a fronteira {@code \b} de que todas as
 * regras dependem. O defeito não é corrigido errado: ele fica invisível.
 *
 * <h2>Por que rodar o detector de produção, e não uma regex própria</h2>
 * O critério de "isto é um defeito de concordância" já existe e é grande — léxico de gênero,
 * parentesco, agressividade, pronomes cruzados. Reimplementá-lo aqui produziria uma segunda
 * verdade que divergiria da primeira, que é precisamente o erro que a regra de medição do projeto
 * proíbe. Este harness só faz uma coisa: chama {@code analisar} DUAS vezes na mesma fala — como
 * ela está e com {@code \N} trocado por espaço — e conta os motivos que só aparecem na segunda.
 *
 * <p>A diferença entre as duas chamadas é, por definição, o que a quebra de linha escondeu.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Mede sobre o cache, que é onde inglês e português ficam pareados pelo próprio pipeline —
 *       sem pareamento por índice, que já produziu 77,7% onde o real era 0,5%.</li>
 *   <li>Controle positivo e negativo antes de qualquer contagem: o harness precisa ser visto
 *       enxergando o caso doente E calando no são.</li>
 *   <li>Número é PISO: só conta o que o detector atual saberia acusar se enxergasse. Defeito de
 *       classe que ele não cobre continua fora.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem pasta de cache, PULA por {@link Assumptions} — declarado como NÃO VERIFICADO, nunca como
 * ausência de defeito.
 */
@DisplayName("medição: o que a quebra \\N esconde do detector de concordância")
class MedicaoCegueiraQuebraLinhaIT {

    private static final Path CACHE = Path.of("cache");

    private record Achado(String arquivo, int indice, String en, String pt, List<String> motivos) {
    }

    @Test
    @DisplayName("conta as falas cujo defeito só aparece com a quebra normalizada")
    void quantoAQuebraEsconde() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(CACHE), "pasta de cache ausente — NÃO VERIFICADO");

        DetectorConcordanciaService detector = new DetectorConcordanciaService();
        CorretorDeterministicoConcordanciaService corretor =
            new CorretorDeterministicoConcordanciaService();

        // A PRIMEIRA VERSÃO DESTE HARNESS MEDIA O ALVO ERRADO, e o controle o derrubou: supus que
        // o DETECTOR fosse cego à quebra, e ele não é — `removerTagsAss` troca \N por espaço antes
        // de analisar. Quem era cego é o CORRETOR, que recebia o texto cru. O desfecho era o pior
        // dos dois mundos: acusar o defeito e não conseguir consertá-lo.
        assertFalse(motivos(detector, "My father is waiting.", "Minha\\Nmãe está esperando.").isEmpty(),
            "premissa caiu: o detector precisa ACUSAR o caso com quebra — é o que torna a "
                + "cegueira do corretor um desperdício, e não um empate");

        // CONTROLE POSITIVO do corretor, nas duas formas. A de cima já funcionava; a de baixo é a
        // que a fronteira do ASS passou a alcançar.
        assertTrue(corretor.corrigir("My father is waiting.", "Minha mãe está esperando.").isPresent(),
            "instrumento cego: o caso SEM quebra tem de ser corrigível");
        assertTrue(corretor.corrigir("My father is waiting.", "Minha\\Nmãe está esperando.").isPresent(),
            "REGRESSÃO: a fronteira do ASS parou de alcançar o defeito colado à quebra");
        // E a quebra tem de sobreviver à correção — juntar as linhas estoura a caixa na tela.
        assertTrue(corretor.corrigir("My father is waiting.", "Minha\\Nmãe está esperando.")
                .orElse("").contains("\\N"),
            "a correção comeu a quebra de linha");
        // CONTROLE NEGATIVO: fala correta não pode ser reescrita por causa da quebra.
        assertTrue(corretor.corrigir("My father is waiting.", "Meu\\Npai está esperando.").isEmpty(),
            "alarme falso: fala CORRETA foi reescrita");

        ObjectMapper mapper = new ObjectMapper();
        List<Achado> achados = new ArrayList<>();
        Map<String, Integer> porMotivo = new TreeMap<>();
        int falas = 0;
        int comQuebraColada = 0;
        int corrigivelIdeal = 0;
        int corrigivelReal = 0;

        List<Path> arquivos;
        try (var s = Files.walk(CACHE)) {
            arquivos = s.filter(p -> p.toString().endsWith(".cache.json")).sorted().toList();
        }

        for (Path arquivo : arquivos) {
            CacheDocumento doc;
            try {
                doc = mapper.readValue(arquivo.toFile(), CacheDocumento.class);
            } catch (Exception e) {
                continue;
            }
            if (doc.entradas() == null) {
                continue;
            }
            for (var entrada : doc.entradas()) {
                String pt = entrada.traduzido();
                String en = entrada.original();
                if (pt == null || pt.isBlank()) {
                    continue;
                }
                falas++;
                if (!pt.contains("\\N")) {
                    continue;
                }
                comQuebraColada++;
                // O corrigível IDEAL é o que sai quando a quebra não atrapalha; o corrigível REAL
                // é o que o corretor alcança no texto como ele vem do disco. A diferença entre os
                // dois é a cegueira que ainda resta.
                boolean idealCorrige = corretor.corrigir(en, pt.replace("\\N", " ")).isPresent();
                boolean realCorrige = corretor.corrigir(en, pt).isPresent();
                if (idealCorrige) {
                    corrigivelIdeal++;
                }
                if (realCorrige) {
                    corrigivelReal++;
                }
                if (idealCorrige && !realCorrige) {
                    List<String> motivos = motivos(detector, en, pt);
                    achados.add(new Achado(arquivo.getFileName().toString(), entrada.indice(), en, pt, motivos));
                    motivos.forEach(m -> porMotivo.merge(m, 1, Integer::sum));
                }
            }
        }

        System.out.printf("%n=== QUEBRA \\N × CORRETOR DETERMINÍSTICO — sobre o cache real ===%n");
        System.out.printf("arquivos de cache             : %d%n", arquivos.size());
        System.out.printf("falas traduzidas              : %d%n", falas);
        System.out.printf("falas com \\N                  : %d%n", comQuebraColada);
        System.out.printf("corrigíveis se \\N não atrapalha: %d%n", corrigivelIdeal);
        System.out.printf("corrigíveis no texto como está : %d%n", corrigivelReal);
        System.out.printf("CEGUEIRA RESIDUAL              : %d%n", achados.size());
        if (!porMotivo.isEmpty()) {
            System.out.printf("%nPor motivo (o que o detector acusa e o corretor não alcança):%n");
            porMotivo.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("   %4d  %s%n", e.getValue(), e.getKey()));
        }

        System.out.printf("%nAmostra (até 20):%n");
        Map<String, Integer> porArquivo = new LinkedHashMap<>();
        achados.stream().limit(20).forEach(a -> {
            porArquivo.merge(a.arquivo(), 1, Integer::sum);
            System.out.printf("   %s #%d%n      EN: %s%n      PT: %s%n      -> %s%n",
                a.arquivo(), a.indice(), a.en(), a.pt(), String.join(" | ", a.motivos()));
        });
    }

    /** Os motivos que o detector de produção acusa para o par, ou lista vazia. */
    private static List<String> motivos(DetectorConcordanciaService detector, String en, String pt) {
        ResultadoDeteccaoConcordancia r = detector.analisar(en == null ? "" : en, pt);
        return r.motivos() == null ? List.of() : List.copyOf(r.motivos());
    }

}
