package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: as ferramentas que varrem o ACERVO e reescrevem legenda também têm de
 * perguntar se a linha é música — a mesma exigência que as telas cumprem, aplicada ao caminho
 * que nenhuma catraca cobria.
 *
 * <h2>O prejuízo que originou, e ele foi GRAVADO (19/08/2026)</h2>
 * O {@code AplicarAcentosNoAcervoIT} rodou sobre as 232 legendas traduzidas e reescreveu 699
 * falas. <b>103 delas eram ROMAJI:</b>
 *
 * <pre>
 *   "aa kizutsukeau mae ni"  ->  "aa kizutsukeau mãe ni"    (102 Song JP + 1 OP Roma)
 * </pre>
 *
 * O {@code mae} do romaji é 前 ("antes"), não a palavra portuguesa. O normalizador de acentos
 * está CERTO para diálogo — a lista dele tem quatro formas que são romaji válido
 * ({@code ate}, {@code mae}, {@code nao}, {@code sao}), medidas contra as 129.745 formas do
 * dicionário {@code ja_ROMAJI} — e não tem como saber sozinho onde está pisando.
 *
 * <p>A cicatriz já existia desde 14/08/2026, com a instrução escrita: <i>pular estilo musical</i>.
 * As três telas da etapa 3 perguntam à {@code PoliticaEstiloMusical} antes de escrever, e as três
 * têm catraca. <b>Esta família não tinha nenhuma</b> — ela não passa por tela, não passa por
 * controller, e por isso escapou de todas as guardas existentes. O dano foi desfeito do snapshot
 * byte a byte; esta catraca é o que impede a próxima.
 *
 * <h2>O critério, e por que é este</h2>
 * Ferramenta de acervo = arquivo de teste que (1) lê a raiz do acervo por
 * {@code kronos.acervo}, (2) grava com {@code Files.write}/{@code Files.copy} e (3) mexe em
 * linha {@code Dialogue:}. Os três juntos: quem só escreve RELATÓRIO não casa o terceiro, e quem
 * monta fixture em {@code @TempDir} não casa o primeiro.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Toda ferramenta que casa os três critérios está no inventário nominal, com o que ela faz
 *       escrito — e consulta {@code estiloIgnorado}, o dono da regra.</li>
 *   <li>Ferramenta NOVA reprova até ser classificada. Nascer sem veto é o defeito.</li>
 *   <li>Ferramenta que SUMIU do alcance também reprova: ou deixou de escrever, e sai do
 *       inventário, ou mudou de forma e a varredura ficou cega.</li>
 *   <li><b>Alvo vazio é instrumento CEGO, não aprovação.</b></li>
 * </ul>
 *
 * <h2>Limite declarado</h2>
 * Prova que a CHAMADA existe, não que ela funciona — a mutação {@code false &&} passaria aqui. O
 * que prova o comportamento é a corrida no acervo com a conferência por estilo, e ela está no
 * commit que consertou o dano.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia o arquivo exato e o que falta. Nunca lança por conta própria.
 */
class CatracaFerramentaDeAcervoVetaMusicaTest {

    private static final Path RAIZ = Path.of("src", "test", "java");

    /** (1) lê o acervo real. */
    private static final String LE_O_ACERVO = "kronos.acervo";
    /** (2) grava de verdade. */
    private static final String[] GRAVA = {"Files.write", "Files.copy"};
    /** (3) mexe na linha de fala, e não só em relatório. */
    private static final String MEXE_EM_FALA = "\"Dialogue:\"";
    /** Como se pergunta ao dono da regra. */
    private static final String VETO_DE_MUSICA = "estiloIgnorado";

    /** As ferramentas de acervo conhecidas, com o que cada uma faz. */
    private static final Map<String, String> FERRAMENTAS_CONHECIDAS = new LinkedHashMap<>();

    static {
        FERRAMENTAS_CONHECIDAS.put("AplicarAcentosNoAcervoIT.java",
            "REESCREVE o .ass do acervo repondo acentos. Ganhou o veto em 19/08/2026, depois de "
            + "gravar 103 linhas de romaji. Injeta PoliticaEstiloMusical pelo CDI.");
        FERRAMENTAS_CONHECIDAS.put("MedicaoDivergenciaPadraoMusicalIT.java",
            "LE o acervo e grava RELATORIO; toca em Dialogue: so para classificar. Ja consulta o "
            + "juiz de musica porque a divergencia entre os dois donos do criterio E o assunto dela.");
    }

    /**
     * A própria catraca se exclui, e isso não é conveniência: o arquivo dela cita as três marcas
     * — {@code kronos.acervo}, a gravação e a linha de fala — na documentação e no caso-controle.
     * Na primeira execução ela ACUSOU A SI MESMA. Exclusão nominal, de um arquivo só: excluir
     * {@code Catraca*} inteiro abriria brecha para a próxima nascer sem ser vista.
     */
    private static final String EU_MESMA = "CatracaFerramentaDeAcervoVetaMusicaTest.java";

    private static Set<String> ferramentasDeAcervo(Path raiz) throws IOException {
        Set<String> achadas = new TreeSet<>();
        try (Stream<Path> caminhos = Files.walk(raiz)) {
            for (Path arquivo : caminhos.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !p.getFileName().toString().equals(EU_MESMA)).toList()) {
                String fonte = Files.readString(arquivo, StandardCharsets.UTF_8);
                boolean grava = false;
                for (String assinatura : GRAVA) {
                    grava = grava || fonte.contains(assinatura);
                }
                if (fonte.contains(LE_O_ACERVO) && grava && fonte.contains(MEXE_EM_FALA)) {
                    achadas.add(arquivo.getFileName().toString());
                }
            }
        }
        return achadas;
    }

    @Test
    @DisplayName("toda ferramenta que reescreve legenda do acervo pergunta se a linha e musica")
    void ferramentaDeAcervoRespondeAoVetoDeMusica() throws IOException {
        assertTrue(Files.isDirectory(RAIZ), "NAO VERIFICADO: " + RAIZ.toAbsolutePath() + " nao existe");

        Set<String> achadas = ferramentasDeAcervo(RAIZ);
        assertFalse(achadas.isEmpty(),
            "instrumento CEGO: nao achou NENHUMA ferramenta de acervo, e existe pelo menos uma. "
                + "Se a forma mudou, corrija os criterios — nao apague a catraca.");

        List<String> novas = new ArrayList<>(achadas);
        novas.removeAll(FERRAMENTAS_CONHECIDAS.keySet());
        assertTrue(novas.isEmpty(), () -> """
            Ferramenta de acervo NOVA, sem classificacao: %s

            Ela le o acervo real, grava, e mexe em linha Dialogue:. Em 19/08/2026 uma ferramenta
            assim reescreveu 103 linhas de romaji — "mae" (前) virou "mãe" — porque era a unica
            que varria o acervo inteiro sem perguntar se a linha e musica.

            Consulte a PoliticaEstiloMusical antes de escrever, e declare aqui o que ela faz.
            """.formatted(novas));

        List<String> sumiram = new ArrayList<>(FERRAMENTAS_CONHECIDAS.keySet());
        sumiram.removeAll(achadas);
        assertTrue(sumiram.isEmpty(),
            "ferramenta conhecida sumiu da varredura: " + sumiram + ". Se deixou de escrever no "
                + "acervo, tire do inventario. Se so mudou de forma, corrija os criterios — nao "
                + "afrouxe a busca.");

        List<String> semVeto = new ArrayList<>();
        for (String nome : achadas) {
            if (!fonteDe(nome).contains(VETO_DE_MUSICA)) {
                semVeto.add(nome);
            }
        }
        assertTrue(semVeto.isEmpty(),
            "ferramenta de acervo SEM o veto de musica: " + semVeto + ". Sem consultar "
                + VETO_DE_MUSICA + ", o romaji volta a ser tratado como portugues — e o "
                + "normalizador, que esta certo para dialogo, escreve 'mãe' onde o japones diz "
                + "'antes'.");
    }

    private static String fonteDe(String nomeDoArquivo) throws IOException {
        try (Stream<Path> caminhos = Files.walk(RAIZ)) {
            Path achado = caminhos.filter(p -> p.getFileName().toString().equals(nomeDoArquivo))
                .findFirst().orElse(null);
            return achado == null ? "" : Files.readString(achado, StandardCharsets.UTF_8);
        }
    }

    /**
     * CASO-CONTROLE: a varredura só vale se separar os três casos — a ferramenta de acervo, a que
     * só escreve relatório e a que monta fixture em pasta temporária. Sem isto, um critério
     * errado deixaria a catraca verde para sempre.
     */
    @Test
    @DisplayName("instrumento calibrado: pega a ferramenta de acervo, ignora relatorio e fixture")
    void instrumentoDiscrimina(@TempDir Path temp) throws IOException {
        Files.writeString(temp.resolve("FerramentaDeAcervoIT.java"), """
            class FerramentaDeAcervoIT {
                Path raiz = Path.of(System.getProperty("kronos.acervo", "/acervo"));
                void aplicar(String linha) {
                    if (linha.startsWith("Dialogue:")) { Files.write(alvo, novas); }
                }
            }
            """);
        Files.writeString(temp.resolve("SoRelatorioIT.java"), """
            class SoRelatorioIT {
                Path raiz = Path.of(System.getProperty("kronos.acervo", "/acervo"));
                void medir() { Files.write(Path.of("relatorios", "saida.txt"), texto); }
            }
            """);
        Files.writeString(temp.resolve("SoFixtureTest.java"), """
            class SoFixtureTest {
                void montar(Path dir) {
                    Files.write(dir.resolve("ep.ass"), List.of("Dialogue: 0,..."));
                }
            }
            """);

        Set<String> achadas = ferramentasDeAcervo(temp);

        assertEquals(Set.of("FerramentaDeAcervoIT.java"), achadas,
            "a varredura tinha de achar EXATAMENTE a ferramenta de acervo. Achou: " + achadas);
    }
}
