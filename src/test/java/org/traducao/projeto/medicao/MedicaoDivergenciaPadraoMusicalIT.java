package org.traducao.projeto.medicao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.traducao.projeto.legenda.application.DetectorEfeitoKaraokeService;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: medir, contra o acervo real, em quantas linhas os DOIS proprietários
 * da pergunta "este estilo é musical?" respondem DIFERENTE. Congela o número antes de qualquer
 * convergência, para que unificar os padrões seja uma mudança com antes e depois — e não uma
 * reclassificação em massa que ninguém dimensionou.
 *
 * <h2>O prejuízo que originou</h2>
 * A mesma pergunta tem duas implementações vivas:
 * <ul>
 *   <li>{@link PoliticaEstiloMusical} — proprietário DECLARADO desde a fase E3c, usa
 *       {@code \b} e conhece {@code romaji};</li>
 *   <li>{@link DetectorEfeitoKaraokeService} — usa lookaround de LETRA e conhece
 *       {@code lyrics?}, mas não {@code romaji}.</li>
 * </ul>
 * A {@code CatracaRegraDuplicadaEntreFatiasTest} já DECLAROU essa duplicação em 2026-08-03
 * ("o padrão largo já alcançava OP_S2, mas eEstiloDeMusica usava o de \b") — mas contar a
 * cópia não faz as duas convergirem, e a divergência seguiu viva.
 *
 * <p>Medido em 07/08/2026 no acervo: <b>ED_Romaji1..5, OP2, ED2, OP_S2, ED_S2</b> a Política
 * diz "não é música" e o Detector diz "é"; em <b>"Hey World Romaji"</b> e
 * <b>"RISE LIGHT RISE Romaji"</b> acontece o inverso. O {@code \b} não alcança
 * {@code ED_Romaji1} porque {@code _} é caractere de palavra e o dígito fecha o token — a
 * MESMA razão pela qual {@code OPL2} precisou de entrada nominal no {@code application.yml}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Este harness NÃO corrige nada e NÃO altera comportamento: só conta e imprime.</li>
 *   <li>Consulta os objetos de PRODUÇÃO, nunca uma reimplementação do padrão — reimplementar
 *       aqui seria criar a TERCEIRA cópia da regra que este mesmo arquivo existe para medir.</li>
 *   <li>Travado por {@code -Dkronos.medicao=true}: lê o acervo real, que não existe no CI.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Acervo ausente faz o teste falhar com a mensagem do caminho — resultado vazio por acervo
 * inacessível NÃO pode ser lido como "os dois padrões concordam".
 */
@EnabledIfSystemProperty(named = "kronos.medicao", matches = "true")
class MedicaoDivergenciaPadraoMusicalIT {

    private static final Path ACERVO =
        Path.of(System.getProperty("kronos.acervo", "C:\\animes"));

    private static final Set<String> PASTAS_DE_ENTRADA =
        Set.of("legendas_eng", "legendas_originais", "legendas_extraidas_ass");

    /**
     * PROPÓSITO DE NEGÓCIO: percorre o acervo, pergunta o mesmo estilo aos dois proprietários
     * e agrupa os desacordos por estilo, com a contagem de linhas de cada um.
     */
    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) — esta medição compara DOIS donos do critério
     * musical, e um zero só significa "eles concordam" se os dois tiverem sido vistos respondendo.
     *
     * <p>Dois objetos que devolvem sempre a mesma coisa concordam perfeitamente e não dizem nada.
     * O controle exige que cada um separe um estilo claramente musical de um claramente de
     * diálogo — senão a divergência medida é a de dois relógios parados.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: imprime o que falhou e devolve {@code false}.
     */
    private static boolean instrumentoCalibrado(PoliticaEstiloMusical politica,
                                               DetectorEfeitoKaraokeService detector) {
        boolean politicaVeMusica = politica.estiloIgnorado("Song JP");
        boolean politicaVeDialogo = !politica.estiloIgnorado("Default");
        boolean detectorVeMusica = detector.podeSerCamadaMusical("Song JP", "");
        boolean detectorVeDialogo = !detector.podeSerCamadaMusical("Default", "");

        if (politicaVeMusica && politicaVeDialogo && detectorVeMusica && detectorVeDialogo) {
            System.out.println("  controle: os DOIS donos separam 'Song JP' de 'Default' — "
                + "a divergencia medida abaixo e real, e nao dois relogios parados");
            return true;
        }
        System.out.printf("INSTRUMENTO REPROVADO NO CONTROLE — politica(musica)=%s "
            + "politica(dialogo)=%s detector(musica)=%s detector(dialogo)=%s. "
            + "Nenhum numero abaixo vale.%n",
            politicaVeMusica, politicaVeDialogo, detectorVeMusica, detectorVeDialogo);
        return false;
    }

    @Test
    @DisplayName("mede a divergencia entre os dois proprietarios do criterio musical")
    void medeDivergencia() throws IOException {
        assertTrue(Files.isDirectory(ACERVO),
            "NAO VERIFICADO: acervo inacessivel em " + ACERVO + " — sem ele o resultado vazio "
                + "significaria \"nao consegui medir\", nunca \"os dois padroes concordam\"");

        // A lista de estilos-ignorados NAO entra aqui de proposito: ela e configuracao do
        // operador, e o que se mede e a divergencia entre os dois CODIGOS.
        PoliticaEstiloMusical politica = new PoliticaEstiloMusical(List.of());
        DetectorEfeitoKaraokeService detector = new DetectorEfeitoKaraokeService();

        // O controle roda DEPOIS de os dois donos existirem — sao variaveis locais, e uma guarda
        // no topo do metodo nao teria a quem perguntar.
        if (!instrumentoCalibrado(politica, detector)) {
            return;
        }

        Map<String, long[]> porEstilo = new TreeMap<>();   // [linhas, politica, detector]
        long total = 0;

        // ALCANCE PELO DONO UNICO: honra -Dkronos.medicao.obra e declara NAO VERIFICADO
        // quando o filtro nao casa. A varredura propria daqui ignorava o filtro, entao pedir
        // uma obra e receber o acervo inteiro saia com a mesma cara de medicao dirigida.
        List<Path> obras = AlcanceDaMedicao.obras();
        if (obras.isEmpty()) {
            System.out.println("NAO VERIFICADO: nenhuma obra casou com o alcance — 'os dois "
                + "padroes concordam' aqui seria cegueira, e nao concordancia.");
            return;
        }
        List<Path> deTodasAsObras = new ArrayList<>();
        for (Path obra : obras) {
            try (Stream<Path> arquivos = Files.walk(obra)) {
                arquivos.filter(p -> p.toString().endsWith(".ass"))
                    .filter(p -> p.getParent() != null
                        && PASTAS_DE_ENTRADA.contains(p.getParent().getFileName().toString()))
                    .forEach(deTodasAsObras::add);
            }
        }
        {
            for (Path arquivo : deTodasAsObras) {
                for (String linha : Files.readAllLines(arquivo)) {
                    if (!linha.startsWith("Dialogue:")) {
                        continue;
                    }
                    String[] campos = linha.split(",", 10);
                    if (campos.length < 4) {
                        continue;
                    }
                    String estilo = campos[3];
                    total++;
                    boolean daPolitica = politica.estiloIgnorado(estilo);
                    boolean doDetector = detector.podeSerCamadaMusical(estilo, "");
                    if (daPolitica != doDetector) {
                        long[] acc = porEstilo.computeIfAbsent(estilo, k -> new long[3]);
                        acc[0]++;
                        acc[1] = daPolitica ? 1 : 0;
                        acc[2] = doDetector ? 1 : 0;
                    }
                }
            }
        }

        long linhasEmDesacordo = porEstilo.values().stream().mapToLong(a -> a[0]).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("=== DIVERGENCIA ENTRE OS DOIS PROPRIETARIOS DO CRITERIO MUSICAL ===\n");
        sb.append("falas analisadas: ").append(total).append('\n');
        sb.append("linhas em desacordo: ").append(linhasEmDesacordo).append('\n');
        sb.append("estilos em desacordo: ").append(porEstilo.size()).append("\n\n");
        sb.append(String.format("%-28s %8s  %-9s %-9s%n", "estilo", "linhas", "Politica", "Detector"));
        porEstilo.entrySet().stream()
            .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
            .forEach(e -> sb.append(String.format("%-28s %8d  %-9s %-9s%n",
                e.getKey(), e.getValue()[0],
                e.getValue()[1] == 1 ? "musical" : "nao",
                e.getValue()[2] == 1 ? "musical" : "nao")));

        Path saida = Path.of("build", "medicao-divergencia-padrao-musical.txt");
        Files.createDirectories(saida.getParent());
        Files.writeString(saida, sb.toString());
        System.out.println(sb);

        assertFalse(total == 0,
            "zero falas analisadas — o instrumento nao encontrou nenhuma pasta de entrada em "
                + ACERVO + ", entao nao mediu nada");
    }
}
