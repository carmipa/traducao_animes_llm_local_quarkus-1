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
        "MedicaoDivergenciaPadraoMusicalIT.java",
        // Entrou em 07/08/2026 pelo mesmo caminho do anterior: a catraca o pegou no primeiro
        // `gradlew test` e a exceção é NOMINAL, não uma frouxidão de padrão — mede quantas linhas
        // curtas de música o classificador ainda manda traduzir, lendo o acervo real, e
        // "C:\animes" é o default de -Dkronos.acervo. Trocar por caminho relativo faria o harness
        // medir o vazio e reportar "nenhum furo" com a mesma cara de "furo nenhum encontrado".
        "MedicaoLinhaCurtaKaraokeIT.java",
        // Entrou em 08/08/2026 respondendo "o problema foi corrigido para TODOS os animes?":
        // cruza toda legenda x todo cache do acervo real. "C:\animes" e o default de
        // -Dkronos.acervo; caminho relativo faria o harness medir o vazio e reportar zero
        // colisoes por nao ter olhado nada.
        "MedicaoColisaoCacheEntreObrasIT.java",
        // Entrou em 12/08/2026, e de novo foi a catraca que pegou no primeiro `gradlew test`.
        // Mesma natureza dos anteriores: compara mistral x aya no ARTEFATO em disco do Gundam
        // Unicorn, e a pasta da obra e caminho de acervo real, nao fixture. O teste ja e
        // PULADO por Assumptions quando o acervo nao existe — no conteiner ele nao quebra,
        // declara "NAO VERIFICADO", que e diferente de passar.
        "MedicaoUnicornMistralXAyaIT.java",
        // Entrou em 12/08/2026, mesma natureza: varre o acervo real para responder em que
        // estilos o achatamento seria a UNICA protecao contra a camada musical ir ao LLM.
        // Tambem e PULADO por Assumptions quando o acervo nao existe.
        "OndeOAchatamentoValeriaIT.java",
        // Entrou em 12/08/2026: mede pergunta do original que virou afirmacao na traducao — o
        // erro FLUENTE, que passa por eco, residuo e pendencia sem ser notado. Le o acervo real.
        "MedicaoPerguntaQueViraAfirmacaoIT.java",
        // Entrou em 12/08/2026, irma da anterior: mede negacao do original que sumiu na
        // traducao — "I can't do it" -> "Eu posso fazer isso" inverte a cena e passa em todos
        // os validadores. Le o acervo real.
        "MedicaoNegacaoPerdidaIT.java",
        // Entrou em 12/08/2026: mede acentuacao obrigatoria faltando no portugues entregue.
        // Nasceu de "circunstancias" x "circunstancias" na MESMA fala do E02, onde o mistral
        // acerta e a aya erra — eixo em que os dois nao empatam. Le o acervo real.
        "MedicaoAcentuacaoFaltanteIT.java",
        // Entrou em 12/08/2026: mede genero trocado entre original e traducao — o eixo em que o
        // ingles nao marca e o portugues marca em tudo. Le o acervo real.
        "MedicaoGeneroTrocadoIT.java",
        // Entrou em 12/08/2026: fecha o buraco que a anterior declarou — genero IMPLICITO, o
        // participio que discorda do personagem citado. Le a ficha do contexto de producao e o
        // acervo real.
        "MedicaoGeneroImplicitoIT.java",
        // Entrou em 12/08/2026: gera o gold set para LEITURA HUMANA — os pares mais divergentes
        // entre mistral e aya, ordenados por suspeita. Le o acervo real.
        "GoldSetLeituraHumanaIT.java",
        // Entrou em 12/08/2026: mede o que a quebra \N esconde do detector de concordancia,
        // rodando o detector de PRODUCAO duas vezes por fala. Le a pasta de cache real.
        "MedicaoCegueiraQuebraLinhaIT.java",
        // Entrou em 12/08/2026: mede o que um ADVERBIO entre o verbo e o participio esconde do
        // mesmo detector ("Ela esta muito cansado" nao e acusada). Le a pasta de cache real.
        "MedicaoAdverbioEntreVerboEParticipioIT.java",
        // Entrou em 13/08/2026: repete no Zeta a comparacao mistral x aya do Unicorn, sobre o
        // CACHE (a retraducao gravou por cima do .ass). Le cache/ e backups/ reais.
        "MedicaoZetaMistralXAyaIT.java",
        // Entrou em 13/08/2026: mede o VOLUME que o detector de nome proprio produz num acervo
        // real antes de o numero dele ser usado para julgar qualquer coisa — a versao anterior
        // dessa ideia foi removida por 57,7% de falso positivo, e o erro de metodo foi nao medir
        // primeiro. Le as legendas do Memories (1995); PULADO por Assumptions se a pasta faltar.
        "MedicaoNomeProprioAcervoIT.java",
        // Entrou em 14/08/2026: mede quantas falas em FRANCES o pipeline daria por ja
        // traduzidas e nunca enviaria ao LLM. Le as legendas do Memories (1995).
        "MedicaoFonteFrancesaIT.java"
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
