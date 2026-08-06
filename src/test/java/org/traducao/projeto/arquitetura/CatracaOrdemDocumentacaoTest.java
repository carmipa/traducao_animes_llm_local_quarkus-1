package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a numeração da etapa é a ordem de execução que o operador segue, e ela
 * vive em TRÊS lugares — o rótulo do menu, o nome do arquivo em {@code docs/} e o índice da
 * documentação dentro da aplicação. Esta catraca impede que os três contem histórias diferentes.
 *
 * <h2>O prejuízo que a originou</h2>
 * Em 05/08/2026 a numeração do menu foi refeita ({@code 1.1} … {@code 5.3}). O índice da
 * documentação NÃO acompanhou, e ficou dois dias ensinando uma ordem que o menu já não usava —
 * a Troca de Tipo aparecia em 8º lugar e o Karaokê Simples vinha ANTES da Tradução de Karaokê.
 * Ninguém percebeu, porque nada quebra quando um índice mente. Foi corrigido à mão em
 * 06/08/2026, junto com a renomeação dos 23 arquivos para {@code etapa-G.N-nome.md}.
 *
 * <p>Antes disso, o repositório chegou a ter QUATRO numerações simultâneas e divergentes:
 * rótulos do menu, comentários do HTML (com números repetidos), comentários do CSS e o mapa de
 * títulos do JavaScript. Numeração duplicada em lugares que ninguém compara sempre diverge.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Todo arquivo {@code docs/etapa-G.N-*.md} usa um número que EXISTE no menu. Documento
 *       apontando para etapa inexistente é documento órfão.</li>
 *   <li>O índice da documentação lista as etapas em ordem CRESCENTE. É o invariante que
 *       falhou de fato.</li>
 *   <li>O rótulo do índice começa com o MESMO número do arquivo que ele abre.</li>
 *   <li>Não há número de etapa repetido entre arquivos.</li>
 * </ul>
 *
 * <h2>Por que não exigir um doc por item de menu</h2>
 * Nem toda etapa tem página própria de propósito — a {@code 2.2 Tradução sem Lore} é variação
 * da {@code 2.1} e é descrita lá dentro. Exigir cobertura total transformaria a catraca em
 * geradora de arquivo vazio, que é pior do que a lacuna.
 *
 * <h2>Comportamento em caso de falha</h2>
 * A mensagem nomeia o arquivo, o número encontrado e o que se esperava — para o conserto não
 * exigir abrir o HTML e comparar à mão.
 */
class CatracaOrdemDocumentacaoTest {

    private static final Path INDEX = Path.of("src/main/resources/static/index.html");
    private static final Path DOCS = Path.of("docs");

    /** Nome de arquivo de etapa: {@code etapa-1.3-troca-tipo-legenda.md}. */
    private static final Pattern ARQUIVO_ETAPA = Pattern.compile("^etapa-(\\d\\.\\d)-[a-z0-9-]+\\.md$");

    /** Rótulo do menu: {@code <span>1.3 Troca de Tipo de Legenda</span>}. */
    private static final Pattern ROTULO_MENU = Pattern.compile("<span>(\\d\\.\\d) [^<]+</span>");

    /** Item do índice: {@code data-pagina="etapa-1.3-…" … data-titulo="1.3 …"}. */
    private static final Pattern ITEM_INDICE = Pattern.compile(
        "data-pagina=\"(etapa-(\\d\\.\\d)-[a-z0-9-]+)\"[\\s\\S]{0,120}?data-titulo=\"([^\"]*)\"");

    @Test
    @DisplayName("todo doc de etapa usa um numero que existe no menu")
    void todoDocDeEtapaUsaNumeroDoMenu() throws IOException {
        Set<String> doMenu = numerosDoMenu();
        assertTrue(doMenu.size() >= 10,
            "menu com menos numeros do que o esperado (" + doMenu.size() + ") — a catraca "
                + "estaria cega. Verifique se o seletor do nav-menu ainda casa.");

        List<String> orfaos = new ArrayList<>();
        for (Path arquivo : arquivosDeEtapa()) {
            String numero = numeroDoArquivo(arquivo);
            if (!doMenu.contains(numero)) {
                orfaos.add("  " + arquivo.getFileName() + "  — etapa " + numero
                    + " nao existe no menu");
            }
        }

        assertTrue(orfaos.isEmpty(), () -> """
            DOCUMENTO APONTANDO PARA ETAPA QUE NAO EXISTE NO MENU.

            O nome do arquivo carrega a etapa de proposito, para reordenar exigir renomear e o
            erro ficar visivel. Se a etapa mudou de numero, renomeie o arquivo e conserte as
            referencias (README, indice do index.html e links entre docs).

            Numeros que o menu conhece: """ + doMenu + """


            Orfaos:
            """ + String.join(System.lineSeparator(), orfaos));
    }

    @Test
    @DisplayName("o indice da documentacao segue a ordem crescente das etapas")
    void indiceSegueOrdemDoPipeline() throws IOException {
        // SÓ o índice lateral (doc-nav). Os cartões de atalho da tela de boas-vindas também
        // carregam data-pagina, mas são uma seleção fora de ordem por definição — varrer o
        // arquivo inteiro fazia a catraca reprovar o atalho como se fosse desordem do índice.
        List<String> naOrdemDoIndice = new ArrayList<>();
        Matcher m = ITEM_INDICE.matcher(blocoDoIndiceLateral());
        while (m.find()) {
            naOrdemDoIndice.add(m.group(2));
        }

        assertTrue(naOrdemDoIndice.size() >= 10,
            "indice com " + naOrdemDoIndice.size() + " etapas — abaixo do esperado. A catraca "
                + "estaria cega; verifique se o seletor data-pagina/data-titulo ainda casa.");

        List<String> ordenado = naOrdemDoIndice.stream().sorted().toList();
        assertTrue(naOrdemDoIndice.equals(ordenado), () -> """
            O INDICE DA DOCUMENTACAO ESTA FORA DA ORDEM DO PIPELINE.

            Foi exatamente isto que aconteceu em 05/08/2026 e passou dois dias sem ninguem ver:
            o menu foi renumerado e o indice ficou na ordem antiga, ensinando um fluxo que a
            aplicacao ja nao usava. Indice que mente nao quebra nada — por isso precisa de teste.

            Ordem encontrada: """ + naOrdemDoIndice + """

            Ordem esperada:   """ + ordenado);
    }

    @Test
    @DisplayName("o rotulo do indice casa com o numero do arquivo que ele abre")
    void rotuloDoIndiceCasaComOArquivo() throws IOException {
        List<String> divergentes = new ArrayList<>();
        Matcher m = ITEM_INDICE.matcher(Files.readString(INDEX, StandardCharsets.UTF_8));
        while (m.find()) {
            String pagina = m.group(1);
            String numeroDoArquivo = m.group(2);
            String titulo = m.group(3);
            if (!titulo.startsWith(numeroDoArquivo)) {
                divergentes.add("  " + pagina + "  rotulo=\"" + titulo
                    + "\"  deveria comecar com \"" + numeroDoArquivo + "\"");
            }
        }

        assertTrue(divergentes.isEmpty(), () -> """
            ROTULO DO INDICE DIVERGE DO NUMERO DO ARQUIVO.

            O operador le o rotulo e abre o arquivo; os dois tem de dizer a mesma etapa.
            """ + String.join(System.lineSeparator(), divergentes));
    }

    @Test
    @DisplayName("nenhum numero de etapa aparece em dois arquivos")
    void numeroDeEtapaNaoSeRepete() throws IOException {
        Set<String> vistos = new LinkedHashSet<>();
        List<String> repetidos = new ArrayList<>();
        for (Path arquivo : arquivosDeEtapa()) {
            String numero = numeroDoArquivo(arquivo);
            if (!vistos.add(numero)) {
                repetidos.add("  " + numero + " — " + arquivo.getFileName());
            }
        }

        assertTrue(repetidos.isEmpty(), () -> """
            DUAS PAGINAS REIVINDICAM A MESMA ETAPA.

            Ate 05/08/2026 os comentarios do index.html tinham 8, 9 e 10 repetidos, e por isso
            nenhum deles queria dizer coisa alguma. Numero repetido e numero morto.

            """ + String.join(System.lineSeparator(), repetidos));
    }

    /**
     * Recorta o índice lateral da documentação ({@code <nav class="doc-nav">}), que é o único
     * lugar onde a ORDEM importa. Fora dele há atalhos que apontam para as mesmas páginas numa
     * seleção deliberadamente fora de sequência.
     */
    private static String blocoDoIndiceLateral() throws IOException {
        String html = Files.readString(INDEX, StandardCharsets.UTF_8);
        int inicio = html.indexOf("class=\"doc-nav\"");
        assertTrue(inicio >= 0, "indice lateral da documentacao (doc-nav) nao encontrado");
        int fim = html.indexOf("</nav>", inicio);
        assertTrue(fim > inicio, "doc-nav sem fechamento");
        return html.substring(inicio, fim);
    }

    /** Números de etapa declarados nos rótulos do menu lateral. */
    private static Set<String> numerosDoMenu() throws IOException {
        String html = Files.readString(INDEX, StandardCharsets.UTF_8);
        int inicio = html.indexOf("<nav class=\"nav-menu\">");
        int fim = html.indexOf("</nav>", inicio);
        assertTrue(inicio >= 0 && fim > inicio, "nav-menu nao encontrado no index");

        Set<String> numeros = new LinkedHashSet<>();
        Matcher m = ROTULO_MENU.matcher(html.substring(inicio, fim));
        while (m.find()) {
            numeros.add(m.group(1));
        }
        return numeros;
    }

    private static List<Path> arquivosDeEtapa() throws IOException {
        try (Stream<Path> arquivos = Files.list(DOCS)) {
            return arquivos
                .filter(p -> ARQUIVO_ETAPA.matcher(p.getFileName().toString()).matches())
                .sorted()
                .toList();
        }
    }

    private static String numeroDoArquivo(Path arquivo) {
        Matcher m = ARQUIVO_ETAPA.matcher(arquivo.getFileName().toString());
        return m.matches() ? m.group(1) : "";
    }
}
