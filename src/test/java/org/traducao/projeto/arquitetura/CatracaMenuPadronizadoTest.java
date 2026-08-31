package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: o menu lateral é a porta de tudo. Item sem painel, painel sem
 * documentação e documentação inalcançável são defeitos que <b>ninguém vê no código</b> — só
 * quem clica, e quem clica é o Paulo.
 *
 * <h2>O prejuízo que originou (27/08/2026)</h2>
 * Uma conferência item a item do menu, pedida porque nenhuma guarda cobria isso, achou três
 * coisas em 21 itens:
 * <ul>
 *   <li><b>{@code 2.2 Tradução sem Lore} sem página de documentação.</b> A tela existia, o menu
 *       apontava para ela, e o painel "Documentação" não tinha o que abrir. As outras 14 etapas
 *       numeradas tinham.</li>
 *   <li><b>ícone {@code spellcheck} repetido</b> entre a 3.1 e a 3.3 — duas telas diferentes com
 *       o mesmo símbolo na barra, e o painel de docs já usava {@code match_word} para a 3.3.</li>
 *   <li><b>{@code btn-menu-sair} sem {@code data-target}</b>, único caso. É legítimo (é ação, não
 *       seção), e por isso vira exceção DECLARADA aqui — não silêncio.</li>
 * </ul>
 *
 * <p>Antes disto o projeto já tinha a {@code CatracaPaginaDeDocumentacaoAbreTest}, que confere se
 * a página ABRE, e a {@code CatracaOrdemDocumentacaoTest}, que confere numeração e ordem. Nenhuma
 * perguntava se a página <b>existe para cada tela</b>. É a mesma lição de 19/08/2026, quando 14
 * páginas respondiam HTTP 400 e a guarda de então conferia só o nome.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>{@code id} de item de menu é sempre {@code btn-menu-<data-target>}.</li>
 *   <li>Item numerado {@code N.M} tem {@code docs/etapa-N.M-*.md}.</li>
 *   <li>Todo {@code data-target} tem uma {@code <section id="panel-<target>">}.</li>
 *   <li>Toda página em {@code docs/} está no painel, e todo item do painel existe em disco.</li>
 *   <li>Ícone não se repete entre itens de menu.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Cada regra nomeia o item e o que falta. Alvo vazio REPROVA: um {@code index.html} que deixasse
 * de casar o padrão do menu daria zero itens, e zero violações — aprovação por cegueira.
 */
@DisplayName("menu lateral: padronizado, com painel e com documentação")
class CatracaMenuPadronizadoTest {

    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path DOCS = Path.of("docs");

    /**
     * Itens que legitimamente NÃO abrem uma seção. Exceção NOMINAL, com o motivo — lista genérica
     * ("itens que terminam em -sair") abriria a porta para o próximo item mal formado entrar sem
     * ninguém decidir.
     */
    private static final Map<String, String> SEM_PAINEL_POR_DESENHO = Map.of(
        "btn-menu-sair", "é AÇÃO (encerra a aplicação), não seção: não há painel para abrir");

    private record ItemDeMenu(String target, String id, String icone, String rotulo) {}

    private static String html() throws IOException {
        return Files.readString(INDEX, StandardCharsets.UTF_8);
    }

    private static List<ItemDeMenu> itensDoMenu(String html) {
        int i = html.indexOf("<nav class=\"nav-menu\">");
        int j = html.indexOf("</nav>", i);
        if (i < 0 || j < 0) {
            return List.of();
        }
        String nav = html.substring(i, j);
        List<ItemDeMenu> fora = new ArrayList<>();
        Matcher m = Pattern
            .compile("<button class=\"nav-item[^\"]*\"([^>]*)>(.*?)</button>", Pattern.DOTALL)
            .matcher(nav);
        while (m.find()) {
            String atributos = m.group(1);
            String corpo = m.group(2);
            fora.add(new ItemDeMenu(
                grupo(atributos, "data-target=\"([^\"]+)\""),
                grupo(atributos, "id=\"([^\"]+)\""),
                grupo(corpo, "nav-icon\">([^<]+)<"),
                grupo(corpo, "<span>([^<]+)</span>")));
        }
        return fora;
    }

    private static String grupo(String texto, String padrao) {
        Matcher m = Pattern.compile(padrao).matcher(texto);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) do leitor do menu — ele acha um item bem
     * formado e NÃO acha num HTML sem menu.
     *
     * <p>Sem isto, um {@code index.html} reestruturado devolveria zero itens, zero violações e
     * build verde: a guarda aprovaria por não enxergar nada.
     */
    @Test
    @DisplayName("CONTROLE: o leitor do menu acha item bem formado e cala em HTML sem menu")
    void leitorDeMenuCalibrado() {
        String comMenu = """
            <nav class="nav-menu">
              <button class="nav-item" data-target="x" id="btn-menu-x">
                <span class="material-symbols-outlined nav-icon">radar</span>
                <span>9.9 Teste</span>
              </button>
            </nav>""";
        List<ItemDeMenu> achados = itensDoMenu(comMenu);
        assertEquals(1, achados.size(), "o leitor nao achou um item bem formado");
        assertEquals("x", achados.get(0).target());
        assertEquals("btn-menu-x", achados.get(0).id());
        assertEquals("radar", achados.get(0).icone());
        assertEquals("9.9 Teste", achados.get(0).rotulo());

        assertTrue(itensDoMenu("<div>sem menu nenhum</div>").isEmpty(),
            "o leitor inventou item onde nao ha menu");
    }

    @Test
    @DisplayName("todo item tem id btn-menu-<target>, e quem nao tem painel esta DECLARADO")
    void idSegueOpadraoEasExcecoesSaoDeclaradas() throws IOException {
        List<ItemDeMenu> itens = itensDoMenu(html());
        assertTrue(itens.size() >= 15,
            "NAO VERIFICADO: o leitor achou " + itens.size() + " itens de menu. O painel tem mais "
                + "de quinze; isso e o padrao do HTML ter mudado, e nao o menu ter encolhido.");

        List<String> problemas = new ArrayList<>();
        for (ItemDeMenu item : itens) {
            if (item.target() == null) {
                if (!SEM_PAINEL_POR_DESENHO.containsKey(item.id())) {
                    problemas.add(item.id() + ": sem data-target e sem excecao declarada");
                }
                continue;
            }
            if (!("btn-menu-" + item.target()).equals(item.id())) {
                problemas.add(item.id() + ": deveria ser btn-menu-" + item.target());
            }
            if (item.icone() == null || item.icone().isBlank()) {
                problemas.add(item.id() + ": sem icone");
            }
            if (item.rotulo() == null || item.rotulo().isBlank()) {
                problemas.add(item.id() + ": sem rotulo");
            }
        }
        assertTrue(problemas.isEmpty(),
            "ITENS DE MENU FORA DO PADRAO:\n  " + String.join("\n  ", problemas));
    }

    @Test
    @DisplayName("todo data-target tem uma section panel-<target> na mesma pagina")
    void todoItemAbreUmPainel() throws IOException {
        String html = html();
        Set<String> paineis = new LinkedHashSet<>();
        Matcher m = Pattern.compile("id=\"panel-([a-z0-9-]+)\"").matcher(html);
        while (m.find()) {
            paineis.add(m.group(1));
        }
        assertTrue(paineis.size() >= 15,
            "NAO VERIFICADO: achei " + paineis.size() + " paineis — o padrao do id mudou");

        List<String> semPainel = itensDoMenu(html).stream()
            .map(ItemDeMenu::target)
            .filter(t -> t != null && !paineis.contains(t))
            .toList();
        assertTrue(semPainel.isEmpty(),
            "ITEM DE MENU QUE NAO ABRE NADA: " + semPainel + "\n"
                + "  O clique troca de seção por data-target; sem <section id=\"panel-X\"> a tela "
                + "fica em branco e nenhum teste de backend percebe.");
    }

    @Test
    @DisplayName("toda etapa numerada N.M do menu tem docs/etapa-N.M-*.md")
    void todaEtapaNumeradaTemDocumentacao() throws IOException {
        Set<String> paginas = paginasEmDisco();
        assertTrue(paginas.size() >= 20,
            "NAO VERIFICADO: achei " + paginas.size() + " paginas em docs/ — o acervo de "
                + "documentacao nao encolheu sozinho");

        List<String> semDoc = new ArrayList<>();
        for (ItemDeMenu item : itensDoMenu(html())) {
            if (item.rotulo() == null) {
                continue;
            }
            Matcher m = Pattern.compile("^(\\d+\\.\\d+)\\s").matcher(item.rotulo());
            if (!m.find()) {
                continue;
            }
            String numero = m.group(1);
            boolean tem = paginas.stream().anyMatch(p -> p.startsWith("etapa-" + numero + "-"));
            if (!tem) {
                semDoc.add(numero + " " + item.rotulo() + "  (esperava docs/etapa-" + numero
                    + "-*.md)");
            }
        }
        assertTrue(semDoc.isEmpty(), """
            TELA SEM PAGINA DE DOCUMENTACAO:
              %s

            Foi assim que a "2.2 Tradução sem Lore" ficou de fora: a tela existia, o menu
            apontava, e o painel Documentação nao tinha o que abrir.""".formatted(
                String.join("\n  ", semDoc)));
    }

    @Test
    @DisplayName("o painel de documentacao lista TODAS as paginas, e so paginas que existem")
    void painelEdocsSeCobremNosDoisSentidos() throws IOException {
        String html = html();
        Set<String> noPainel = new LinkedHashSet<>();
        Matcher m = Pattern.compile("data-pagina=\"([^\"]+)\"").matcher(html);
        while (m.find()) {
            noPainel.add(m.group(1));
        }
        Set<String> emDisco = paginasEmDisco();
        assertTrue(noPainel.size() >= 20,
            "NAO VERIFICADO: o painel listou " + noPainel.size() + " paginas — padrao mudou");

        Set<String> semEntrada = new LinkedHashSet<>(emDisco);
        semEntrada.removeAll(noPainel);
        Set<String> semArquivo = new LinkedHashSet<>(noPainel);
        semArquivo.removeAll(emDisco);

        assertTrue(semEntrada.isEmpty(),
            "PAGINA ESCRITA E INALCANCAVEL pelo painel: " + semEntrada
                + "\n  Documento que ninguem abre e documento que ninguem le.");
        assertTrue(semArquivo.isEmpty(),
            "O PAINEL APONTA PARA ARQUIVO QUE NAO EXISTE: " + semArquivo
                + "\n  O clique devolve HTTP 404 na cara de quem abriu.");
    }

    @Test
    @DisplayName("icone nao se repete entre itens do menu")
    void iconeNaoSeRepete() throws IOException {
        Map<String, List<String>> porIcone = new TreeMap<>();
        for (ItemDeMenu item : itensDoMenu(html())) {
            if (item.icone() != null) {
                porIcone.computeIfAbsent(item.icone(), k -> new ArrayList<>()).add(item.rotulo());
            }
        }
        assertTrue(porIcone.size() >= 15,
            "NAO VERIFICADO: achei " + porIcone.size() + " icones distintos — padrao mudou");

        Map<String, List<String>> repetidos = new LinkedHashMap<>();
        porIcone.forEach((icone, telas) -> {
            if (telas.size() > 1) {
                repetidos.put(icone, telas);
            }
        });
        assertTrue(repetidos.isEmpty(), """
            ICONE REPETIDO ENTRE TELAS DIFERENTES:
              %s

            Na barra lateral o icone e o que se reconhece antes do texto. Dois iguais fazem o
            operador clicar na tela errada — e a 3.1 e a 3.3 fazem coisas muito diferentes.
            """.formatted(repetidos));
    }

    private static Set<String> paginasEmDisco() throws IOException {
        try (Stream<Path> s = Files.list(DOCS)) {
            Set<String> fora = new LinkedHashSet<>();
            s.filter(p -> p.getFileName().toString().endsWith(".md"))
                .forEach(p -> {
                    String n = p.getFileName().toString();
                    fora.add(n.substring(0, n.length() - 3));
                });
            return fora;
        }
    }
}
