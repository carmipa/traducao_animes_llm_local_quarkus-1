package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.traducao.projeto.core.presentation.ui.AnsiCores;
import org.traducao.projeto.legenda.domain.PoliticaEstiloMusical;
import org.traducao.projeto.legenda.infrastructure.EscritorLegendaAss;
import org.traducao.projeto.legenda.infrastructure.LeitorLegendaAss;
import org.traducao.projeto.revisaoConcordancia.domain.ResultadoConcordancia;
import org.traducao.projeto.telemetria.OperacaoTelemetria;
import org.traducao.projeto.telemetria.TelemetriaService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a REGRA DE CORES do console desta tela — a mesma da 3.1 e da 3.2,
 * aplicada à 3.3 por ordem de Paulo. O operador precisa distinguir, na hora e sem abrir
 * relatório, o arquivo que foi gravado do que ficou por resolver e do que não tinha nada.
 *
 * <pre>
 *   VERDE     [Revisado]  arquivo escrito, com a contagem de falas corrigidas
 *   AMARELO   [Pendente]  nada escrito, mas há falas que mudariam (a simulação)
 *   DIM       [OK]        reservado ao arquivo que realmente não tinha nada
 *   VERMELHO  [Erro]      só erro
 * </pre>
 *
 * <h2>O prejuízo que originou a regra</h2>
 * Medido na 3.2 em 18/08/2026: com uma linha por fala, 10.013 das 10.563 linhas do console
 * (94,8%) não diziam nada ao operador, e "arquivo limpo" saía IGUAL a "arquivo com 90
 * pendências" — a diferença só aparecia no relatório final, horas depois. Aqui a 3.3 estava um
 * passo atrás: não imprimia <b>nada</b> por arquivo, e erro de leitura/escrita ia só para o
 * {@code log.warn}, invisível na tela.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Uma linha por ARQUIVO, nunca por fala — esta fatia percorre 380.697 falas do acervo.</li>
 *   <li>Cada estado tem cor E rótulo próprios; dois estados diferentes nunca saem iguais.</li>
 *   <li>O caso vermelho é exercitado com uma falha REAL (backup impossível), não simulada com
 *       dublê — e ele prova também que os contadores não sobem quando nada foi gravado.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer estado que saia com a cor ou o rótulo de outro reprova.
 */
class ConsoleDaRevisaoConcordanciaTest {

    private final RevisarConcordanciaUseCase useCase = new RevisarConcordanciaUseCase(
        new LeitorLegendaAss(), new EscritorLegendaAss(), new CorretorConcordanciaGeneroService(),
        new TelemetriaMuda(), new PoliticaEstiloMusical(List.of()));

    static class TelemetriaMuda extends TelemetriaService {
        @Override
        public synchronized void registrarOperacao(OperacaoTelemetria op) {
            // nada: o teste é do console, não da telemetria
        }
    }

    private static final String CABECALHO = """
        [Script Info]
        ScriptType: v4.00+

        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1

        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        """;

    private void escreverAss(Path arquivo, String... falas) throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (String f : falas) {
            sb.append("Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,").append(f).append("\n");
        }
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Roda o caso de uso capturando o console, e devolve o que saiu na tela. */
    private String consoleDe(Path pasta, boolean aplicar) {
        PrintStream original = System.out;
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(capturado, true, StandardCharsets.UTF_8));
            useCase.revisarPasta(pasta, aplicar);
        } finally {
            System.setOut(original);
        }
        return capturado.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("VERDE [Revisado] no arquivo gravado, com a contagem de falas corrigidas")
    void verdeNoArquivoGravado(@TempDir Path dir) throws IOException {
        escreverAss(dir.resolve("ep01_PT-BR.ass"), "Vi o menina no parque.");

        String console = consoleDe(dir, true);

        assertTrue(console.contains(AnsiCores.GREEN + "  [Revisado] ep01_PT-BR.ass (1 fala(s) corrigida(s))"),
            "faltou a linha verde do arquivo gravado. Console:\n" + console);
        assertTrue(console.contains(AnsiCores.CYAN + "  Backup anterior: "),
            "o backup precisa aparecer na tela junto do arquivo gravado. Console:\n" + console);
    }

    @Test
    @DisplayName("AMARELO [Pendente] na simulação: há o que corrigir e nada foi gravado")
    void amareloNaSimulacao(@TempDir Path dir) throws IOException {
        Path arquivo = dir.resolve("ep02_PT-BR.ass");
        escreverAss(arquivo, "Vi o menina no parque.");
        String antes = Files.readString(arquivo, StandardCharsets.UTF_8);

        String console = consoleDe(dir, false);

        assertTrue(console.contains(AnsiCores.YELLOW + "  [Pendente] ep02_PT-BR.ass (1 fala(s) mudariam"),
            "simulação com fala a corrigir tem de sair AMARELA. Console:\n" + console);
        assertFalse(console.contains("[Revisado]"), "dry-run não pode dizer Revisado. Console:\n" + console);
        assertEquals(antes, Files.readString(arquivo, StandardCharsets.UTF_8),
            "o dry-run escreveu no arquivo");
    }

    @Test
    @DisplayName("DIM [OK] só para o arquivo que realmente não tinha nada")
    void cinzaNoArquivoConforme(@TempDir Path dir) throws IOException {
        escreverAss(dir.resolve("ep03_PT-BR.ass"), "A menina está cansada.");

        String console = consoleDe(dir, true);

        assertTrue(console.contains(AnsiCores.DIM + "  [OK]       ep03_PT-BR.ass (concordancia conforme)"),
            "arquivo conforme tem de sair em cinza. Console:\n" + console);
        assertFalse(console.contains("[Pendente]"),
            "arquivo sem nada NÃO é pendente — é justamente a confusão que a regra desfaz. Console:\n" + console);
    }

    /**
     * O caso VERMELHO, com falha real: um ARQUIVO chamado {@code backup_revisao_concordancia}
     * ocupando o nome da pasta de backup faz {@code Files.createDirectories} falhar, e o caso de
     * uso não pode gravar por cima do original sem backup.
     *
     * <p>Prova duas coisas de uma vez: a linha vermelha aparece na tela (antes o erro ia só para
     * o {@code log.warn}, invisível ao operador) e os contadores NÃO sobem — o arquivo continua
     * errado no disco, e um banner dizendo "1 fala corrigida" seria estado-alvo vendido como
     * estado-atual.
     */
    @Test
    @DisplayName("VERMELHO [Erro] quando o arquivo não pôde ser gravado — e os contadores não sobem")
    void vermelhoNaFalhaDeGravacao(@TempDir Path dir) throws IOException {
        Path arquivo = dir.resolve("ep04_PT-BR.ass");
        escreverAss(arquivo, "Vi o menina no parque.");
        String antes = Files.readString(arquivo, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("backup_revisao_concordancia"), "ocupa o nome da pasta");

        PrintStream original = System.out;
        ByteArrayOutputStream capturado = new ByteArrayOutputStream();
        ResultadoConcordancia resultado;
        try {
            System.setOut(new PrintStream(capturado, true, StandardCharsets.UTF_8));
            resultado = useCase.revisarPasta(dir, true);
        } finally {
            System.setOut(original);
        }
        String console = capturado.toString(StandardCharsets.UTF_8);

        assertTrue(console.contains(AnsiCores.RED + "  [Erro]     ep04_PT-BR.ass"),
            "falha de gravação tem de aparecer VERMELHA na tela. Console:\n" + console);
        assertEquals(0, resultado.falasCorrigidas(),
            "nada foi gravado: contar como corrigida é vender estado-alvo como estado-atual");
        assertEquals(0, resultado.arquivosAlterados(), "nenhum arquivo foi alterado de verdade");
        assertEquals(antes, Files.readString(arquivo, StandardCharsets.UTF_8),
            "o original foi mexido mesmo sem backup");
    }
}
