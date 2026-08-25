package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: as ferramentas de {@code ferramentas/} carregam guardas que nasceram de
 * prejuízo. Esta catraca impede que a próxima reescrita as apague sem perceber.
 *
 * <h2>Por que uma guarda sobre SCRIPT, e não só um comentário dentro dele</h2>
 * Os dois scripts existem porque as versões anteriores — escritas na hora, no rascunho — deram
 * resultado errado com cara de certo em 25/08/2026:
 *
 * <ul>
 *   <li>O mutador rodou CINCO mutações e devolveu "nenhum teste reprovou" nas cinco, inclusive na
 *       que desliga a remoção do caractere invisível. Faltava {@code --rerun-tasks}: o Gradle
 *       servia o XML da rodada verde anterior.</li>
 *   <li>Ele decidia pelo CÓDIGO DE SAÍDA, e {@code rc=1} significa "teste reprovou" E "o cmd não
 *       achou o executável" — dois estados com o mesmo sinal, dentro do instrumento cuja função é
 *       justamente pegar esse defeito.</li>
 *   <li>O conferidor escolhia a baseline pelo NOME do backup e acusou 6 linhas de {@code Romanji}
 *       alteradas que eram a diferença entre o estado danificado e o já consertado.</li>
 * </ul>
 *
 * <p>Comentário explica; catraca impede. Quem reescrever o script sem a guarda vê vermelho.
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia a ferramenta, a guarda ausente e o prejuízo que ela evita.
 */
class CatracaFerramentaDeMedicaoTest {

    private static final Path MUTADOR = Path.of("ferramentas", "mutar.py");
    private static final Path CONFERIDOR = Path.of("ferramentas", "conferir-acervo.py");

    private record Guarda(String trecho, String porque) {}

    private static final List<Guarda> DO_MUTADOR = List.of(
        new Guarda("--rerun-tasks",
            "sem ele o Gradle serve o XML da rodada anterior e cinco mutacoes 'passam' de uma vez"),
        new Guarda("ET.parse",
            "o veredito tem de sair do XML; rc=1 significa 'teste reprovou' E 'cmd nao achou o "
                + "executavel', e os dois nao podem sair iguais"),
        new Guarda("os.path.join(os.getcwd()",
            "o caminho do wrapper tem de ser ABSOLUTO: este shell nao procura na pasta atual"),
        new Guarda("os.remove(x)",
            "o XML e apagado antes de cada rodada, para 'sem XML' significar 'nao rodou'"),
        new Guarda("NAO VERIFICADO",
            "alvo que casa zero ou duas vezes nao e mutacao aplicada, e tem de ser declarado"));

    private static final List<Guarda> DO_CONFERIDOR = List.of(
        new Guarda("hashlib.sha256(bruto).hexdigest() == shaEsperado",
            "a baseline e escolhida pelo SHA e nunca pelo nome — 'antes-do-desfazer-...' ordena "
                + "depois de '2026...' e inventou um achado de dano em romaji"),
        new Guarda("MUSICAL",
            "o relatorio tem de separar linha de MUSICA das outras: e o dano que este projeto ja "
                + "pagou tres vezes"),
        new Guarda("semBaseline",
            "arquivo sem baseline localizavel e DECLARADO — 'nao consegui comparar' e diferente "
                + "de 'nao mudou'"),
        new Guarda("dialogue",
            "a contagem de linhas antes e depois tem de ser comparada: correcao de texto nao pode "
                + "criar nem apagar fala"));

    private static void conferir(Path ferramenta, List<Guarda> guardas) throws IOException {
        assertTrue(Files.isRegularFile(ferramenta),
            "a ferramenta sumiu: " + ferramenta + ". Ela carrega guardas que nasceram de prejuizo "
                + "medido; apagar o arquivo apaga a memoria junto.");
        String fonte = Files.readString(ferramenta, StandardCharsets.UTF_8);
        for (Guarda g : guardas) {
            assertTrue(fonte.contains(g.trecho()),
                String.format("%s perdeu a guarda '%s'.%n  Por que ela existe: %s",
                    ferramenta, g.trecho(), g.porque()));
        }
    }

    @Test
    @DisplayName("o mutador mantem as guardas que o fizeram parar de mentir")
    void mutador() throws IOException {
        conferir(MUTADOR, DO_MUTADOR);
    }

    @Test
    @DisplayName("o conferidor de acervo mantem a baseline por SHA e a separacao de musica")
    void conferidor() throws IOException {
        conferir(CONFERIDOR, DO_CONFERIDOR);
    }

    /**
     * O CASO-CONTROLE DA PRÓPRIA CATRACA: ela precisa ser vista REPROVANDO uma ferramenta sem as
     * guardas. Uma catraca que só olha o arquivo são pode estar aprovando por não enxergar nada.
     */
    @Test
    @DisplayName("CONTROLE: a catraca REPROVA uma ferramenta sem as guardas")
    void aCatracaEnxerga(@org.junit.jupiter.api.io.TempDir Path tmp) throws IOException {
        Path falsa = tmp.resolve("mutar-sem-guarda.py");
        Files.writeString(falsa, "subprocess.run('gradlew.bat test')\n", StandardCharsets.UTF_8);
        boolean reprovou = false;
        try {
            conferir(falsa, DO_MUTADOR);
        } catch (AssertionError esperado) {
            reprovou = true;
        }
        assertTrue(reprovou, "a catraca aprovou uma ferramenta que nao tem guarda nenhuma");

        Path ausente = tmp.resolve("nao-existe.py");
        boolean reprovouAusente = false;
        try {
            conferir(ausente, DO_MUTADOR);
        } catch (AssertionError esperado) {
            reprovouAusente = true;
        }
        assertTrue(reprovouAusente, "a catraca aprovou uma ferramenta que nem existe");
        assertFalse(Files.exists(ausente), "");
    }
}
