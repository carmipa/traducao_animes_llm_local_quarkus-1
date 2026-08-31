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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: toda tela que <b>faz alguma coisa</b> diz, dentro dela mesma, o que a
 * operação toca, o que ela exige e o que ela NÃO faz.
 *
 * <h2>O prejuízo que originou (31/08/2026)</h2>
 * Paulo apontou a caixa "Passadas de revisão" da tela 3.1 e disse: <i>"esse tipo de informação é o
 * que falta nas outras páginas"</i>. Estava certo — a caixa existia em <b>4</b> telas e faltava em
 * <b>9</b>. Numa delas o silêncio custava caro: a 1.2 grava numa pasta de saída <b>PLANA</b>, e dois
 * episódios com o mesmo nome em temporadas diferentes fazem o segundo <b>sobrescrever</b> a legenda
 * do primeiro. Isso estava escrito na documentação e não na tela — e quem extrai não lê a
 * documentação antes de clicar.
 *
 * <h2>Por que ISTO é uma catraca, e não uma revisão manual</h2>
 * A informação envelhece do jeito mais silencioso que existe: a tela ganha um botão novo, e a ficha
 * continua descrevendo o mundo antigo. Uma catraca não resolve isso sozinha — mas garante que uma
 * tela NOVA não nasça muda, que é onde a padronização se perde.
 *
 * <h2>O que se exige de cada ficha</h2>
 * <ul>
 *   <li>cabeçalho com ícone, título e etiqueta — a etiqueta diz a RELAÇÃO entre os passos
 *       ("rode na ordem", "independentes", "somente leitura");</li>
 *   <li>descrição da caixa;</li>
 *   <li>ao menos um item, e todo item com título, descrição e <b>pelo menos um selo</b> — o selo é
 *       o que responde "isto escreve onde?" sem obrigar a ler o parágrafo;</li>
 *   <li>selo de vocabulário conhecido: {@code escreve}, {@code llm}, {@code rede}, {@code perigo},
 *       {@code seguro}. Classe nova sem CSS vira texto sem estilo, que passa despercebido em
 *       revisão de código e aparece feio para quem usa.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Nomeia a tela e o que falta. Alvo vazio REPROVA: zero telas encontradas seria o padrão do HTML ter
 * mudado, e zero violações sairia como aprovação.
 */
@DisplayName("ficha da operacao: toda tela que age explica o que ela toca")
class CatracaFichaDaOperacaoTest {

    private static final Path ESTATICOS = Path.of("src/main/resources/static");
    private static final Path CSS = Path.of("src/main/resources/static/css");

    /**
     * Telas que NÃO agem — não têm o que explicar. Exceção NOMINAL, com o motivo de cada uma:
     * lista genérica ("painéis de sistema") deixaria a próxima tela muda entrar sem ninguém decidir.
     */
    private static final Map<String, String> SEM_OPERACAO = Map.ofEntries(
        Map.entry("inicio", "é o painel de entrada: só navega para as outras"),
        Map.entry("mapa", "gera o índice do próprio código; não toca em legenda"),
        Map.entry("telemetria", "observabilidade, somente leitura"),
        Map.entry("desempenho", "mostra a última medição; a tela não mede"),
        Map.entry("documentacao", "exibe as páginas de docs"),
        Map.entry("sobre", "texto institucional"),
        // CASCA DE MÓDULO, e isto foi CONFERIDO, não suposto: o <div class="panel"> no index.html
        // está vazio (48 a 80 caracteres de comentário) e o conteúdo é injetado do arquivo próprio,
        // que TEM ficha e é verificado logo abaixo por ARQUIVOS_PROPRIOS. A exceção é de ENDEREÇO —
        // a ficha existe, só mora noutro arquivo —, e não de dispensa.
        Map.entry("revisao-concordancia", "casca: o conteúdo vem de revisaoConcordancia.html"),
        Map.entry("revisao-lore", "casca: o conteúdo vem de revisaoLore.html"),
        Map.entry("troca-tipo-legenda", "casca: o conteúdo vem de trocaTipoLegenda.html"),
        Map.entry("auditor-conteudo", "casca: o conteúdo vem de auditorConteudoLegendas.html"),
        Map.entry("renomear-arquivos", "casca: o conteúdo vem de renomearArquivos.html"),
        Map.entry("novo-karaoke", "casca: o conteúdo vem de novoKaraoke.html"),
        Map.entry("traducao-karaoke", "casca: o conteúdo vem de traducaoKaraoke.html"));

    /** Telas de arquivo próprio que também carregam ficha. */
    private static final Set<String> ARQUIVOS_PROPRIOS = Set.of(
        "auditorConteudoLegendas", "novoKaraoke", "renomearArquivos",
        "revisaoConcordancia", "revisaoLore", "traducaoKaraoke", "trocaTipoLegenda");

    private record Ficha(String tela, String html) {}

    private static String ler(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    /** As telas do index.html que precisam de ficha, já sem as declaradas sem operação. */
    private static List<Ficha> telasDoIndex() throws IOException {
        String s = ler(ESTATICOS.resolve("index.html"));
        List<int[]> pos = new ArrayList<>();
        List<String> nomes = new ArrayList<>();
        Matcher m = Pattern.compile("id=\"panel-([a-z0-9-]+)\"").matcher(s);
        while (m.find()) {
            pos.add(new int[]{m.start()});
            nomes.add(m.group(1));
        }
        List<Ficha> fora = new ArrayList<>();
        for (int i = 0; i < nomes.size(); i++) {
            if (SEM_OPERACAO.containsKey(nomes.get(i))) {
                continue;
            }
            int ini = pos.get(i)[0];
            int fim = i + 1 < pos.size() ? pos.get(i + 1)[0] : s.length();
            fora.add(new Ficha(nomes.get(i), s.substring(ini, fim)));
        }
        return fora;
    }

    private static List<Ficha> telasDeArquivoProprio() throws IOException {
        List<Ficha> fora = new ArrayList<>();
        for (String nome : ARQUIVOS_PROPRIOS) {
            Path p = ESTATICOS.resolve(nome).resolve(nome + ".html");
            if (Files.isRegularFile(p)) {
                fora.add(new Ficha(nome, ler(p)));
            }
        }
        return fora;
    }

    private static List<Ficha> todas() throws IOException {
        List<Ficha> fora = new ArrayList<>(telasDoIndex());
        fora.addAll(telasDeArquivoProprio());
        return fora;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE (regra 9) do leitor — ele reconhece uma ficha completa e
     * NÃO inventa uma onde não há.
     *
     * <p>Sem isto, um HTML reestruturado devolveria zero telas, zero violações e build verde.
     */
    @Test
    @DisplayName("CONTROLE: o leitor reconhece ficha completa e cala em HTML sem ficha")
    void leitorCalibrado() {
        String comFicha = """
            <div class="glass-card op-caixa">
              <div class="op-caixa-cabecalho">
                <span class="material-symbols-outlined op-caixa-icone">radar</span>
                <h3>Titulo</h3><span class="op-caixa-etiqueta">somente leitura</span>
              </div>
              <p class="op-caixa-desc">desc</p>
              <ol class="op-lista"><li class="op-linha">
                <div class="op-texto"><span class="op-titulo">t</span><span class="op-desc">d</span>
                <span class="op-selos"><span class="op-selo op-selo-seguro">nao escreve</span></span>
              </div></li></ol>
            </div>""";
        assertTrue(temFichaCompleta(comFicha), "o leitor nao reconheceu uma ficha bem formada");
        assertEquals(Set.of("seguro"), selosDe(comFicha));

        assertTrue(!temFichaCompleta("<div>tela sem ficha nenhuma</div>"),
            "o leitor inventou ficha onde nao ha");
        assertTrue(selosDe("<div>nada</div>").isEmpty(), "o leitor inventou selo onde nao ha");
    }

    private static boolean temFichaCompleta(String html) {
        return html.contains("op-caixa")
            && html.contains("op-caixa-cabecalho")
            && html.contains("op-caixa-icone")
            && html.contains("op-caixa-desc")
            && html.contains("op-lista")
            && html.contains("op-linha")
            && html.contains("op-titulo")
            && html.contains("op-selo");
    }

    private static Set<String> selosDe(String html) {
        Set<String> fora = new LinkedHashSet<>();
        Matcher m = Pattern.compile("op-selo op-selo-([a-z]+)").matcher(html);
        while (m.find()) {
            fora.add(m.group(1));
        }
        return fora;
    }

    @Test
    @DisplayName("toda tela que AGE tem a ficha da operacao, completa")
    void todaTelaQueAgeTemFicha() throws IOException {
        List<Ficha> telas = todas();
        assertTrue(telas.size() >= 10,
            "NAO VERIFICADO: achei " + telas.size() + " telas com operacao. Sao mais de dez; isso e "
                + "o padrao do HTML ter mudado, e nao as telas terem sumido.");

        List<String> mudas = new ArrayList<>();
        for (Ficha f : telas) {
            if (!f.html().contains("op-caixa")) {
                mudas.add(f.tela() + ": sem ficha da operacao nenhuma");
            } else if (!temFichaCompleta(f.html())) {
                List<String> falta = new ArrayList<>();
                if (!f.html().contains("op-caixa-cabecalho")) {
                    falta.add("cabecalho");
                }
                if (!f.html().contains("op-caixa-desc")) {
                    falta.add("descricao da caixa");
                }
                if (!f.html().contains("op-linha")) {
                    falta.add("nenhum item");
                }
                if (!f.html().contains("op-selo")) {
                    falta.add("nenhum SELO (e o selo que diz onde a operacao escreve)");
                }
                mudas.add(f.tela() + ": ficha incompleta — falta " + String.join(", ", falta));
            }
        }
        assertTrue(mudas.isEmpty(), """
            TELA QUE AGE SEM EXPLICAR O QUE FAZ:
              %s

            Foi assim que a 1.2 ficou sem avisar que a pasta de saida e PLANA — dois episodios com
            o mesmo nome em temporadas diferentes, e o segundo sobrescreve a legenda do primeiro.
            Estava na documentacao; quem clica nao le a documentacao antes.""".formatted(
                String.join("\n  ", mudas)));
    }

    @Test
    @DisplayName("todo selo usa vocabulario que o CSS conhece")
    void seloUsaVocabularioConhecido() throws IOException {
        Set<String> noCss = new LinkedHashSet<>();
        try (Stream<Path> s = Files.walk(CSS)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".css")).toList()) {
                Matcher m = Pattern.compile("\\.op-selo-([a-z]+)").matcher(ler(p));
                while (m.find()) {
                    noCss.add(m.group(1));
                }
            }
        }
        assertTrue(noCss.size() >= 3,
            "NAO VERIFICADO: o CSS declarou " + noCss.size() + " variantes de selo — padrao mudou");

        List<String> inventados = new ArrayList<>();
        for (Ficha f : todas()) {
            for (String selo : selosDe(f.html())) {
                if (!noCss.contains(selo)) {
                    inventados.add(f.tela() + " usa 'op-selo-" + selo + "', que o CSS nao conhece");
                }
            }
        }
        assertTrue(inventados.isEmpty(),
            "SELO SEM ESTILO (aparece como texto cru para quem usa):\n  "
                + String.join("\n  ", inventados)
                + "\n  Vocabulario disponivel: " + noCss);
    }
}
