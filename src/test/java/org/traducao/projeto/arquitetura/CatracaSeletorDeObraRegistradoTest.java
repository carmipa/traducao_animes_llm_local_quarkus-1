package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: todo seletor de obra que existe numa tela precisa estar REGISTRADO no
 * {@code app.js}, que é quem o popula — e todo id registrado precisa existir em alguma tela.
 * Sem os dois sentidos, o combo abre vazio (e a tela parece quebrada) ou a lista aponta para um
 * elemento que não existe mais (e ninguém percebe, porque lista morta não faz barulho).
 *
 * <h2>O prejuízo que originou, registrado no próprio código</h2>
 * O comentário do {@code app.js} guarda o caso: <i>"A Tradução de Karaokê disparava o evento
 * desde sempre, mas ninguém repopulava o seletor dela: o painel é injetado depois do
 * carregamento inicial, então o combo de lore ficava vazio."</i> O defeito só apareceu quando
 * alguém foi pendurar outra coisa naquele seletor.
 *
 * <p>Em 18/08/2026 a mesma armadilha estava montada de novo: a 3.3 ganhou seletor de obra, e o
 * registro dele mora em QUATRO lugares diferentes do {@code app.js} (a lista que popula, a que
 * aplica a trava, a que classifica como auxiliar e o mapa do banner de capa). Nenhum compilador
 * liga um {@code .html} a um {@code .js}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo {@code id="<algo>-contexto"} de {@code <select>} nos HTMLs aparece no
 *       {@code app.js}.</li>
 *   <li>Todo {@code '<algo>-contexto'} citado no {@code app.js} existe em algum HTML.</li>
 *   <li><b>Alvo vazio é NÃO VERIFICADO:</b> zero seletor encontrado reprova, em vez de devolver
 *       verde por cegueira.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprova nomeando o id exato e o lado que falta. Não afirma nada sobre o COMPORTAMENTO do
 * seletor — só que ele está ligado dos dois lados; o que ele faz é dos testes da tela.
 */
class CatracaSeletorDeObraRegistradoTest {

    private static final Path RAIZ_ESTATICOS = Path.of("src", "main", "resources", "static");
    private static final Path APP_JS = RAIZ_ESTATICOS.resolve("js").resolve("app.js");

    /** O id do seletor de obra no HTML. O sufixo é a convenção do projeto. */
    private static final Pattern ID_NO_HTML = Pattern.compile("id=\"([a-z0-9-]+-contexto)\"");

    @Test
    @DisplayName("todo seletor de obra que existe numa tela esta registrado no app.js")
    void seletorNoHtmlTemDeEstarNoAppJs() throws IOException {
        assertTrue(Files.isRegularFile(APP_JS),
            "NAO VERIFICADO: " + APP_JS.toAbsolutePath() + " nao existe — a catraca nao aprova por cegueira");

        Set<String> noHtml = seletoresNoHtml(RAIZ_ESTATICOS);
        assertFalse(noHtml.isEmpty(),
            "NAO VERIFICADO: nenhum seletor de obra encontrado nos HTMLs. Ou a convencao do id mudou, "
                + "ou a varredura esta cega — os dois pedem olhar, nao verde.");

        String appJs = Files.readString(APP_JS, StandardCharsets.UTF_8);
        // As DUAS listas que importam, e não "aparece em algum lugar do arquivo". A primeira
        // versão desta catraca aceitava qualquer menção — e a mutação provou a fraqueza: tirando
        // o id das duas listas de população, o id continuava no mapa do banner e no ouvinte do
        // evento, e a guarda seguia VERDE enquanto o combo abriria vazio. É o defeito da
        // Tradução de Karaokê exatamente como ele aconteceu.
        String listaQuePopula = trechoDaLista(appJs, "carregarContextosAuxiliares([");
        String listaQueMonta = trechoDaLista(appJs, "todosSelects = [");

        Set<String> semRegistro = new TreeSet<>();
        for (String id : noHtml) {
            boolean nasDuas = listaQuePopula.contains("'" + id + "'") && listaQueMonta.contains("'" + id + "'");
            if (!nasDuas) {
                semRegistro.add(id);
            }
        }

        assertTrue(semRegistro.isEmpty(),
            "seletor(es) de obra que a tela mostra e o app.js nao popula nas DUAS listas: " + semRegistro
                + ". O combo abre VAZIO e a tela parece quebrada. Registre em "
                + "carregarContextosAuxiliares([...]) e em todosSelects = [...] "
                + "(e no mapa do banner, se houver capa).");
    }

    @Test
    @DisplayName("todo id registrado no app.js existe em alguma tela")
    void registroNoAppJsTemDeExistirNoHtml() throws IOException {
        String appJs = Files.readString(APP_JS, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("'([a-z0-9-]+-contexto)'").matcher(appJs);
        Set<String> citados = new TreeSet<>();
        while (m.find()) {
            citados.add(m.group(1));
        }
        assertFalse(citados.isEmpty(), "NAO VERIFICADO: o app.js nao cita seletor de obra nenhum");

        Set<String> noHtml = seletoresNoHtml(RAIZ_ESTATICOS);
        Set<String> orfaos = new TreeSet<>(citados);
        orfaos.removeAll(noHtml);

        assertTrue(orfaos.isEmpty(),
            "id(s) citados no app.js que nao existem em tela nenhuma: " + orfaos
                + ". Lista morta nao faz barulho — ou a tela foi removida e o registro ficou, "
                + "ou o id foi renomeado num lado so.");
    }

    /**
     * CASO-CONTROLE: a varredura precisa ter sido vista SEPARANDO o registrado do não
     * registrado. Sem isto, o verde acima pode ser cegueira do regex.
     */
    @Test
    @DisplayName("caso-controle: pega o seletor sem registro e deixa o registrado em paz")
    void varreduraDiscrimina(@TempDir Path raiz) throws IOException {
        Path tela = raiz.resolve("telaX/telaX.html");
        Files.createDirectories(tela.getParent());
        Files.writeString(tela,
            "<select id=\"registrado-contexto\"></select>\n"
                + "<select id=\"esquecido-contexto\"></select>\n", StandardCharsets.UTF_8);

        String appJsFalso = "const lista = ['registrado-contexto'];";

        Set<String> encontrados = seletoresNoHtml(raiz);
        assertEquals(Set.of("registrado-contexto", "esquecido-contexto"), encontrados,
            "o regex do id nao leu os dois seletores do HTML de controle");

        Set<String> semRegistro = new TreeSet<>();
        for (String id : encontrados) {
            if (!appJsFalso.contains("'" + id + "'")) {
                semRegistro.add(id);
            }
        }
        assertEquals(Set.of("esquecido-contexto"), semRegistro,
            "o instrumento nao discrimina: ou deixou o esquecido passar, ou acusou o registrado");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recorta o literal de array que começa em {@code marcador}, para a
     * checagem olhar a LISTA e não o arquivo inteiro.
     *
     * <p>INVARIANTES DO DOMÍNIO: marcador ausente devolve string vazia, o que faz TODOS os ids
     * reprovarem — se a forma da lista mudou, isso é revisão humana, não aprovação automática.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: colchete de fechamento ausente devolve vazio, pelo
     * mesmo motivo.
     */
    private String trechoDaLista(String appJs, String marcador) {
        int inicio = appJs.indexOf(marcador);
        if (inicio < 0) {
            return "";
        }
        int fim = appJs.indexOf(']', inicio);
        return fim < 0 ? "" : appJs.substring(inicio, fim);
    }

    private Set<String> seletoresNoHtml(Path raiz) throws IOException {
        Set<String> ids = new TreeSet<>();
        try (Stream<Path> s = Files.walk(raiz)) {
            for (Path html : s.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".html")).toList()) {
                Matcher m = ID_NO_HTML.matcher(Files.readString(html, StandardCharsets.UTF_8));
                while (m.find()) {
                    ids.add(m.group(1));
                }
            }
        }
        return ids;
    }
}
