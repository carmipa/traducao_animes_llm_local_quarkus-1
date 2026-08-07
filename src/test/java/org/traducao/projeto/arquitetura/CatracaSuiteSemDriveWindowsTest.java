package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a suíte tem de rodar no contêiner Linux. Caminho absoluto
 * de Windows cravado no fonte do teste é o defeito que quebrava
 * {@code DestinoPadraoKaraokeSimplesTest} com {@code /workspace/C:/animes/...}.
 *
 * <p>INVARIANTES DO DOMÍNIO: nenhum arquivo sob {@code src/test/java} contém
 * construção {@code Paths.get}/{@code Path.of} com letra de drive nem literal
 * de drive em string, exceto os três harnesses de medição listados por NOME —
 * neles o default do acervo é propriedade de sistema, não caminho de fixture.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: lista arquivo:linha e o trecho casado.
 */
class CatracaSuiteSemDriveWindowsTest {

    private static final Path RAIZ = Path.of("src/test/java");

    /**
     * Harnesses de medição do acervo real — exceção NOMINAL (nome de arquivo),
     * não por pasta nem por padrão genérico.
     */
    private static final Set<String> EXCECOES_NOMINAIS = Set.of(
        "AplicarAcentosNoAcervoIT.java",
        "MedicaoAnomaliaIntroduzidaIT.java",
        "MedicaoAuditoriaAcervoIT.java",
        // Entrou em 07/08/2026, DEPOIS desta catraca nascer — e foi a catraca que o pegou,
        // reprovando um arquivo que ainda nao existia quando ela foi escrita. Mesma natureza
        // dos tres acima: le o acervo real e "C:\animes" e DEFAULT de propriedade de sistema
        // (-Dkronos.acervo), nao caminho de fixture.
        "MedicaoDivergenciaPadraoMusicalIT.java"
    );

    /** Construção Path com letra de drive no primeiro argumento string. */
    private static final Pattern PATH_COM_DRIVE = Pattern.compile(
        "Paths?\\.get\\(\\s*\"[A-Za-z]:|Path\\.of\\(\\s*\"[A-Za-z]:");

    /** Literal de string com drive e separador. */
    private static final Pattern LITERAL_DRIVE = Pattern.compile(
        "\"[A-Za-z]:(?:\\\\|/)");

    @Test
    @DisplayName("nenhum teste crava Path com drive nem literal de drive — exceto medicao nominal")
    void suiteNaoCravaDriveWindows() throws IOException {
        List<String> ofensas = new ArrayList<>();

        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            arquivos
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !EXCECOES_NOMINAIS.contains(p.getFileName().toString()))
                .forEach(p -> varrer(p, ofensas));
        }

        assertTrue(ofensas.isEmpty(),
            () -> "Suíte com caminho absoluto de Windows cravado — quebra no contêiner Linux. "
                + "Use Path.of(\"a\", \"b\") / @TempDir / FileSystems.getDefault().getRootDirectories(). "
                + "Fixtures que PRECISAM de drive Windows: monte em runtime (ver FixtureCaminhoWindows).\n  "
                + String.join("\n  ", ofensas));
    }

    private static void varrer(Path arquivo, List<String> ofensas) {
        List<String> linhas;
        try {
            linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            ofensas.add(arquivo + ": falha ao ler (" + e.getMessage() + ")");
            return;
        }
        String relativo = RAIZ.relativize(arquivo).toString().replace('\\', '/');
        for (int i = 0; i < linhas.size(); i++) {
            String linha = removerComentarioDeLinha(linhas.get(i));
            casar(PATH_COM_DRIVE, relativo, i + 1, linha, ofensas);
            casar(LITERAL_DRIVE, relativo, i + 1, linha, ofensas);
        }
    }

    private static String removerComentarioDeLinha(String linha) {
        int idx = linha.indexOf("//");
        if (idx < 0) {
            return linha;
        }
        // Não corta // dentro de string — conta aspas simples o suficiente para o padrão de drive.
        int aspas = 0;
        for (int i = 0; i < idx; i++) {
            if (linha.charAt(i) == '"' && (i == 0 || linha.charAt(i - 1) != '\\')) {
                aspas++;
            }
        }
        return (aspas % 2 == 0) ? linha.substring(0, idx) : linha;
    }

    private static void casar(Pattern padrao, String arquivo, int linha, String texto, List<String> ofensas) {
        Matcher m = padrao.matcher(texto);
        while (m.find()) {
            ofensas.add(arquivo + ":" + linha + " → " + m.group());
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE. A varredura acima só vale se for capaz de REPROVAR —
     * uma verificação que não enxerga nada aprova a suíte inteira e passa por guarda.
     *
     * <p>Exercita as duas formas do defeito em linhas montadas à mão, sem tocar em arquivo real,
     * e confere que uma linha portável NÃO é acusada. Sem isto, um erro de escape no
     * {@code Pattern} deixaria a catraca verde para sempre — que é o falso-verde mais caro,
     * porque parece proteção.
     */
    @Test
    @DisplayName("instrumento calibrado: reprova as duas formas e aprova o codigo portavel")
    void instrumentoReprovaCasoDoente() {
        // As linhas doentes sao MONTADAS em runtime. Escreve-las como literal aqui faria a
        // catraca acusar o proprio caso-controle — o que de fato aconteceu na primeira versao
        // deste teste, e e a melhor prova de que a varredura enxerga: ela pegou a si mesma.
        String drive = "C" + ":";
        String barra = String.valueOf((char) 92);

        List<String> ofensas = new ArrayList<>();

        casar(PATH_COM_DRIVE, "Doente.java", 1,
            "Path entrada = Paths.get(\"" + drive + "\", \"animes\", \"Guilty Crown\");", ofensas);
        assertTrue(ofensas.size() == 1,
            "PATH_COM_DRIVE nao pegou Paths.get com drive — a forma que quebrava o "
                + "DestinoPadraoKaraokeSimplesTest no conteiner. Ofensas: " + ofensas);

        casar(LITERAL_DRIVE, "Doente.java", 2,
            "String acervo = \"" + drive + barra + barra + "animes\";", ofensas);
        assertTrue(ofensas.size() == 2,
            "LITERAL_DRIVE nao pegou o literal de drive em string. Ofensas: " + ofensas);

        // Codigo portavel NAO pode ser acusado: senao a catraca vira ruido e alguem a desliga.
        List<String> saoNaoAcusado = new ArrayList<>();
        casar(PATH_COM_DRIVE, "Sao.java", 1,
            "Path entrada = Path.of(\"animes\", \"Guilty Crown\");", saoNaoAcusado);
        casar(LITERAL_DRIVE, "Sao.java", 2,
            "String nome = \"Gundam: The 08th MS Team\";", saoNaoAcusado);
        assertTrue(saoNaoAcusado.isEmpty(),
            "falso positivo em codigo portavel — o \":\" de \"Gundam: The 08th\" nao pode ser "
                + "confundido com letra de drive: " + saoNaoAcusado);
    }
}
