package org.traducao.projeto.medicao;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.qualidadeTraducao.application.NormalizadorAcentosComuns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: repõe nos arquivos JÁ TRADUZIDOS os acentos que o mapa do
 * pipeline passou a corrigir — as legendas em disco não se beneficiam de uma
 * correção que só existe daqui para a frente.
 *
 * <h2>O prejuízo que originou</h2>
 * Auditado o acervo em 07/08/2026: <b>232 episódios, 241.084 falas, 855 com a
 * forma sem acento</b>. O {@code NormalizadorAcentosComuns} foi ampliado no mesmo
 * dia e cobre a maioria delas, mas o arquivo em disco continua como saiu.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Usa o normalizador DE PRODUÇÃO.</b> Reimplementar a regra aqui criaria
 *       uma segunda fonte de verdade, e a divergência entre as duas só
 *       apareceria quando já tivesse corrompido arquivo.</li>
 *   <li>Só o campo de TEXTO da linha {@code Dialogue:} é tocado. Tempo, estilo,
 *       camada e margens ficam byte a byte como estavam — a coluna 10 é separada
 *       com limite, e uma vírgula dentro da fala não parte o resto.</li>
 *   <li>A contagem de linhas {@code Dialogue:} tem de ser IDÊNTICA antes e
 *       depois. Correção de acento que muda a quantidade de falas é outra coisa
 *       acontecendo.</li>
 *   <li><b>Snapshot antes de escrever</b>, em {@code backups/}. O acervo não está
 *       sob git — sem cópia, um erro aqui é irreversível.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem a segunda trava, roda em ENSAIO e não
 * grava nada. Arquivo ilegível é contado e pulado; nunca é reescrito pela metade.
 *
 * <h2>Como executar</h2>
 * <pre>
 *   ENSAIO (não grava, conta o que faria):
 *     .\gradlew.bat test --tests "*AplicarAcentosNoAcervoIT*" ^
 *       "-Dkronos.medicao=true" "-Dkronos.acervo=C:\animes"
 *
 *   APLICAR de verdade:
 *     ... acrescentando "-Dkronos.aplicar.acentos=SIM-ESCREVER-NO-ACERVO"
 * </pre>
 */
@QuarkusTest
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class AplicarAcentosNoAcervoIT {

    /**
     * O DONO da regra "isto e musica?", resolvido pelo CDI — a mesma instancia que as tres telas
     * da etapa 3 consultam. Uma lista propria aqui divergiria em silencio no dia em que um estilo
     * novo entrasse no acervo, e o sinal apareceria numa legenda estragada. Foi por NAO perguntar
     * a ele que esta ferramenta reescreveu 103 linhas de romaji em 19/08/2026.
     */
    @Inject
    PoliticaEstiloMusical politicaEstiloMusical;

    /** Segunda trava, separada de propósito: medir é barato, escrever é irreversível. */
    private static final String CHAVE_ESCRITA = "kronos.aplicar.acentos";
    private static final String AUTORIZACAO = "SIM-ESCREVER-NO-ACERVO";

    private static final DateTimeFormatter CARIMBO =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final NormalizadorAcentosComuns normalizador = new NormalizadorAcentosComuns();

    @Test
    @DisplayName("repoe os acentos nas legendas ja traduzidas do acervo")
    void aplicar() throws IOException {
        Path acervo = Path.of(System.getProperty("kronos.acervo", "C:\\animes"));
        assertTrue(Files.isDirectory(acervo), () -> "acervo ausente em " + acervo);

        boolean escrever = AUTORIZACAO.equals(System.getProperty(CHAVE_ESCRITA));
        Path snapshot = Path.of("backups", "acentos_acervo_" + LocalDateTime.now().format(CARIMBO));

        System.out.printf("%n=== ACENTOS NO ACERVO — modo %s ===%n  raiz: %s%n",
            escrever ? "!! ESCRITA !!" : "ENSAIO (nada e gravado)", acervo);

        List<Path> arquivos = localizarTraduzidos(acervo);
        Map<String, Integer> porObra = new TreeMap<>();
        int falasCorrigidas = 0;
        int arquivosAlterados = 0;
        int ilegiveis = 0;

        for (Path arquivo : arquivos) {
            List<String> linhas;
            try {
                linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
            } catch (IOException e) {
                ilegiveis++;
                continue;
            }

            int antes = contarDialogos(linhas);
            List<String> saida = new ArrayList<>(linhas.size());
            int corrigidasNoArquivo = 0;

            for (String linha : linhas) {
                if (!linha.startsWith("Dialogue:")) {
                    saida.add(linha);
                    continue;
                }
                // Limite 10: a fala é a última coluna e pode conter vírgula. Sem o
                // limite, uma fala com vírgula seria partida e o resto da linha
                // viraria coluna de metadado.
                String[] col = linha.split(",", 10);
                if (col.length < 10) {
                    saida.add(linha);
                    continue;
                }
                // ESTILO MUSICAL É VETO — e este veto nasceu de dano GRAVADO, em 19/08/2026.
                // Sem ele, esta ferramenta reescreveu 103 linhas de ROMAJI no acervo:
                // "aa kizutsukeau mae ni" virou "...mãe ni". O `mae` do romaji é 前 ("antes"),
                // não a palavra portuguesa — e o normalizador, que está CERTO para diálogo, não
                // tem como saber disso. Foram 102 linhas `Song JP` e 1 `OP Roma`, desfeitas do
                // snapshot byte a byte.
                //
                // A regra já existia e tinha dono: as três telas da etapa 3 perguntam à
                // PoliticaEstiloMusical antes de escrever. Esta ferramenta era a única que
                // varria o acervo inteiro sem perguntar — e a lista de acentos da fatia
                // qualidadeTraducao tem 4 formas que são romaji válido (ate, mae, nao, sao),
                // medidas contra as 129.745 formas do dicionário ja_ROMAJI.
                if (politicaEstiloMusical.estiloIgnorado(col[3])) {
                    saida.add(linha);
                    continue;
                }
                String corrigido = normalizador.normalizar(col[9]);
                if (corrigido == null || corrigido.equals(col[9])) {
                    saida.add(linha);
                    continue;
                }
                col[9] = corrigido;
                saida.add(String.join(",", col));
                corrigidasNoArquivo++;
            }

            if (corrigidasNoArquivo == 0) {
                continue;
            }

            int depois = contarDialogos(saida);
            assertEquals(antes, depois, () -> "a contagem de falas MUDOU em " + arquivo
                + " — correcao de acento nao pode criar nem apagar linha");

            falasCorrigidas += corrigidasNoArquivo;
            arquivosAlterados++;
            porObra.merge(obraDe(acervo, arquivo), corrigidasNoArquivo, Integer::sum);

            if (escrever) {
                copiarParaSnapshot(acervo, arquivo, snapshot);
                Files.write(arquivo, saida, StandardCharsets.UTF_8);
            }
        }

        System.out.printf("  arquivos varridos........ %d%n", arquivos.size());
        System.out.printf("  arquivos ilegiveis....... %d%n", ilegiveis);
        System.out.printf("  arquivos com correcao.... %d%n", arquivosAlterados);
        System.out.printf("  FALAS corrigidas......... %d%n", falasCorrigidas);
        if (escrever) {
            System.out.printf("  snapshot................. %s%n", snapshot.toAbsolutePath());
        }
        System.out.println();
        porObra.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> System.out.printf("     %-52s %5d%n",
                e.getKey().length() > 50 ? e.getKey().substring(0, 50) : e.getKey(), e.getValue()));

        assertTrue(!arquivos.isEmpty(), """
            NENHUM ARQUIVO TRADUZIDO ENCONTRADO.

            Instrumento cego devolve zero igual a acervo limpo. Confira -Dkronos.acervo.
            """);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: os arquivos entregues que esta ferramenta vai <b>reescrever</b>.
     *
     * <h2>Por que delega ao {@link AlcanceDaMedicao}</h2>
     * A varredura própria daqui ignorava {@code -Dkronos.medicao.obra}: pedir "aplique só no
     * 0080" reescrevia o acervo inteiro. Numa ferramenta que ENSAIA isso é perda de tempo; numa
     * que ESCREVE, é a diferença entre um erro de seis arquivos e um de 222.
     *
     * <p>O dono único também recusa {@code .parcial} — que não é entrega — e declara NÃO
     * VERIFICADO quando o filtro não casa com nada, em vez de devolver lista vazia em silêncio.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: propaga {@link IOException}; lista vazia significa "não
     * medi", e quem chama já trata isso com a mensagem de instrumento cego logo acima.
     */
    private static List<Path> localizarTraduzidos(Path acervo) throws IOException {
        List<Path> arquivos = new java.util.ArrayList<>();
        for (Path pasta : AlcanceDaMedicao.pastasDeTraducao()) {
            arquivos.addAll(AlcanceDaMedicao.arquivosEntregues(pasta));
        }
        return arquivos;
    }

    private static int contarDialogos(List<String> linhas) {
        return (int) linhas.stream().filter(l -> l.startsWith("Dialogue:")).count();
    }

    private static String obraDe(Path acervo, Path arquivo) {
        Path rel = acervo.relativize(arquivo);
        return rel.getNameCount() > 0 ? rel.getName(0).toString() : "?";
    }

    /**
     * Copia o arquivo para o snapshot preservando a árvore relativa. O acervo NÃO
     * está sob git — sem esta cópia, um erro aqui não tem volta.
     */
    private static void copiarParaSnapshot(Path acervo, Path arquivo, Path snapshot) throws IOException {
        Path destino = snapshot.resolve(acervo.relativize(arquivo));
        Files.createDirectories(destino.getParent());
        Files.copy(arquivo, destino, StandardCopyOption.REPLACE_EXISTING);
    }
}
