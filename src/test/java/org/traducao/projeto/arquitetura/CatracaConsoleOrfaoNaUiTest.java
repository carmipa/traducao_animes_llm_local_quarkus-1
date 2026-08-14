package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * PROPÓSITO DE NEGÓCIO: impede que uma tela do KRONOS tenha console que NUNCA recebe nada — o
 * defeito que faz um job saudável parecer travado e leva o operador a matar o processo.
 *
 * <h2>O prejuízo que obrigou esta catraca a existir, medido</h2>
 * Em 14/08/2026 a tela "2.2 Tradução sem Lore" ficou muda durante uma tradução que corria
 * normalmente. O servidor publicava no canal SSE {@code "traducao"} — literal, para qualquer
 * contexto — e o {@code consoleMap} de {@code app.js} não tinha entrada para
 * {@code traducao-sem-lore}. Resultado conferido em {@code logs/console-web.log}: <b>192 lotes</b>
 * de progresso entregues ao console da tela 2.1, enquanto a 2.2 exibia apenas as 7 linhas que o
 * próprio JS dela escreve.
 *
 * <p>O operador concluiu que havia travado e encerrou o processo, perdendo o arquivo em curso —
 * o cache só é gravado ao fim de cada arquivo. O custo não foi cosmético.
 *
 * <h2>Por que a checagem é console → mapa, e não o contrário</h2>
 * A pergunta útil é "este console tem quem o alimente?". A inversa — "todo canal tem console?" —
 * deixaria passar exatamente o caso ocorrido, porque o canal {@code traducao} TINHA console; o
 * que não tinha alimentação era o console órfão do outro lado.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo {@code id="console-*"} do {@code index.html} é valor de alguma chave do
 *       {@code consoleMap}, ou está na allowlist nominal abaixo com o motivo.</li>
 *   <li>Alvo ausente é NÃO VERIFICADO: se o HTML ou o {@code consoleMap} não forem encontrados,
 *       a catraca REPROVA em vez de passar por não ter achado nada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Lista os consoles órfãos pelo id exato e diz onde acrescentar o mapeamento.
 */
@DisplayName("catraca: nenhum console da UI fica órfão do consoleMap")
class CatracaConsoleOrfaoNaUiTest {

    /**
     * A UI NÃO é um arquivo só. Além do {@code index.html}, oito painéis são carregados
     * dinamicamente de subpastas ({@code revisaoLore/}, {@code traducaoKaraoke/}, ...) e cada um
     * declara o próprio console. A primeira versão desta catraca lia apenas o índice e acusou
     * como "fantasma" sete consoles que existem e funcionam — alarme falso, que é pior que guarda
     * nenhuma porque ensina a desligar o alarme.
     */
    private static final Path RAIZ_UI = Path.of("src/main/resources/static");
    private static final Path APP_JS = Path.of("src/main/resources/static/js/app.js");

    /** {@code id="console-alguma-coisa"} em qualquer elemento. */
    private static final Pattern ID_CONSOLE = Pattern.compile("id=\"(console-[a-z0-9-]+)\"");

    /** Valor de entrada do mapa: {@code 'chave': 'console-destino'}. */
    private static final Pattern ENTRADA_MAPA = Pattern.compile("'[a-z0-9-]+'\\s*:\\s*'(console-[a-z0-9-]+)'");

    /**
     * Consoles que legitimamente NÃO recebem evento do servidor — preenchidos só por JS local.
     * Exceção NOMINAL e com motivo; lista vazia é o estado saudável.
     */
    private static final Set<String> SO_CLIENTE = Set.of();

    @Test
    @DisplayName("todo console do index.html é destino de alguma entrada do consoleMap")
    void nenhumConsoleFicaOrfao() throws IOException {
        Set<String> consolesNoHtml = todosOsConsolesDaUi();
        Set<String> destinosNoMapa = capturar(ENTRADA_MAPA, recorteDoMapa(ler(APP_JS)));

        // Alvo vazio é NÃO VERIFICADO, nunca aprovação: uma regex que parou de casar reprovaria
        // zero órfãos e daria o mesmo verde de uma UI sã.
        assertFalse(consolesNoHtml.isEmpty(),
            "NÃO VERIFICADO: nenhum id=\"console-*\" encontrado sob " + RAIZ_UI + ". A UI mudou de "
                + "forma e esta catraca precisa ser reapontada antes de voltar a valer.");
        assertFalse(destinosNoMapa.isEmpty(),
            "NÃO VERIFICADO: consoleMap não encontrado ou vazio em " + APP_JS + ".");

        Set<String> orfaos = new TreeSet<>(consolesNoHtml);
        orfaos.removeAll(destinosNoMapa);
        orfaos.removeAll(SO_CLIENTE);

        assertTrue(orfaos.isEmpty(),
            () -> "CONSOLE ÓRFÃO na interface — existe no HTML e nenhuma entrada do consoleMap "
                + "aponta para ele, então NADA que o servidor publique chega nesta tela:\n  "
                + String.join("\n  ", orfaos)
                + "\n\nAcrescente a entrada em " + APP_JS + " (consoleMap) emparelhada com o canal "
                + "que o controller publica. Foi assim que a tela 2.2 ficou muda durante 192 lotes "
                + "em 14/08/2026, e o operador matou um job que estava saudável.");
    }

    /**
     * O outro lado do par: entrada do mapa apontando para id que não existe no HTML. Erro de
     * digitação aqui produz o mesmo silêncio, e nenhum erro no console do navegador.
     */
    @Test
    @DisplayName("toda entrada do consoleMap aponta para um id que existe no HTML")
    void nenhumaEntradaApontaParaOvazio() throws IOException {
        Set<String> consolesNoHtml = todosOsConsolesDaUi();
        Set<String> destinosNoMapa = capturar(ENTRADA_MAPA, recorteDoMapa(ler(APP_JS)));
        assertFalse(destinosNoMapa.isEmpty(), "NÃO VERIFICADO: consoleMap vazio");

        Set<String> apontamFantasma = new TreeSet<>(destinosNoMapa);
        apontamFantasma.removeAll(consolesNoHtml);

        assertTrue(apontamFantasma.isEmpty(),
            () -> "consoleMap aponta para id que NÃO existe no HTML — a linha é escrita num "
                + "elemento inexistente e some sem erro:\n  " + String.join("\n  ", apontamFantasma));
    }

    /**
     * O par que originou tudo, travado nominalmente. Genérico não basta aqui: as duas pontas
     * moram em linguagens diferentes e nada além deste teste as obriga a mudar juntas.
     */
    @Test
    @DisplayName("o par da tradução sem lore está completo nas DUAS pontas")
    void oParDaTraducaoSemLoreEstaCompleto() throws IOException {
        String js = ler(APP_JS);
        String controller = ler(Path.of(
            "src/main/java/org/traducao/projeto/traducao/presentation/web/TraducaoController.java"));

        assertTrue(js.contains("'traducao-sem-lore': 'console-traducao-sem-lore'"),
            "FRONTEND: a entrada do consoleMap sumiu. Sem ela a tela 2.2 volta a ficar muda.");
        assertTrue(controller.contains("\"traducao-sem-lore\""),
            "BACKEND: o canal SSE voltou a ser literal. O progresso da tradução sem lore passaria "
                + "a ser entregue ao console da tela 2.1 outra vez.");
        assertFalse(controller.contains("submeterJobComRelatorio(\"traducao\","),
            "BACKEND: submeterJobComRelatorio voltou ao literal \"traducao\" — é exatamente a "
                + "linha que causou o silêncio de 192 lotes em 14/08/2026.");
        // Conta a CHAMADA, com o receptor junto. Contar só o nome do método casava também a
        // palavra dentro de um comentário na linha 110 e reprovava o arquivo são — o instrumento
        // errado é tão capaz de dar alarme falso quanto de ser cego.
        assertEquals(1, contar(controller, "pipelineWebSupport.submeterJobComRelatorio("),
            "Apareceu um segundo disparo de job nesta rota; a escolha de canal por contexto "
                + "precisa valer para ele também.");
    }

    /** Todo {@code id="console-*"} de TODOS os {@code .html} da UI, índice e parciais. */
    private static Set<String> todosOsConsolesDaUi() throws IOException {
        if (!Files.isDirectory(RAIZ_UI)) {
            fail("NÃO VERIFICADO: " + RAIZ_UI + " não existe.");
        }
        Set<String> achados = new TreeSet<>();
        try (var arquivos = Files.walk(RAIZ_UI)) {
            for (Path p : arquivos.filter(p -> p.toString().endsWith(".html")).toList()) {
                achados.addAll(capturar(ID_CONSOLE, Files.readString(p, StandardCharsets.UTF_8)));
            }
        }
        return achados;
    }

    /** Só o bloco do consoleMap, para não capturar outras estruturas do arquivo. */
    private static String recorteDoMapa(String js) {
        int i = js.indexOf("consoleMap");
        if (i < 0) {
            return "";
        }
        int fim = js.indexOf("};", i);
        return fim < 0 ? js.substring(i) : js.substring(i, fim);
    }

    private static Set<String> capturar(Pattern padrao, String texto) {
        Set<String> achados = new LinkedHashSet<>();
        Matcher m = padrao.matcher(texto);
        while (m.find()) {
            achados.add(m.group(1));
        }
        return achados;
    }

    private static int contar(String texto, String agulha) {
        int n = 0;
        int i = texto.indexOf(agulha);
        while (i >= 0) {
            n++;
            i = texto.indexOf(agulha, i + agulha.length());
        }
        return n;
    }

    private static String ler(Path caminho) throws IOException {
        if (!Files.exists(caminho)) {
            fail("NÃO VERIFICADO: " + caminho + " não existe. Esta catraca não tem como olhar, e "
                + "'não olhei' nunca é 'está certo'.");
        }
        return Files.readString(caminho, StandardCharsets.UTF_8);
    }
}
