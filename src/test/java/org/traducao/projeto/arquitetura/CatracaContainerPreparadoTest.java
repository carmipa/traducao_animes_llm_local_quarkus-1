package org.traducao.projeto.arquitetura;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: impede que o KRONOS volte a depender de coisas que só
 * existem na máquina do Paulo, agora que ele também roda em contêiner.
 *
 * <h2>Por que catraca e não confiança</h2>
 * As três regras abaixo nasceram de defeito real encontrado em 06/08/2026, e
 * todas as três são invisíveis: o código compila, a suíte passa e a tela abre.
 * Só quebram no ambiente onde ninguém está olhando — ou, no caso do
 * {@code .hidden}, quebram silenciosamente na tela e ninguém associa a causa.
 */
class CatracaContainerPreparadoTest {

    private static final Path ESTATICO = Path.of("src", "main", "resources", "static");
    private static final Path BASE_CSS = ESTATICO.resolve("css").resolve("base.css");
    private static final Path APP_JS = ESTATICO.resolve("js").resolve("app.js");
    private static final Path APPLICATION_YML =
        Path.of("src", "main", "resources", "application.yml");

    /**
     * PROPÓSITO DE NEGÓCIO: o endereço do LM Studio precisa continuar vindo de
     * variável de ambiente.
     *
     * <p>O PREJUÍZO: dentro de um contêiner, {@code 127.0.0.1} é o próprio
     * contêiner, e o LM Studio está no host. Uma chave fixa faz a tradução morrer
     * no PRIMEIRO lote com recusa de conexão — e não há teste de unidade que
     * pegue isso, porque fora do contêiner o valor fixo funciona perfeitamente.
     */
    @Test
    @DisplayName("o endereco do LM Studio NAO pode voltar a ser fixo no application.yml")
    void enderecoDoLlmSegueConfiguravel() throws IOException {
        String yml = Files.readString(APPLICATION_YML);

        List<String> fixas = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^\\s*base-url:\\s*\"?([^\"\\n]+)\"?\\s*$").matcher(yml);
        while (m.find()) {
            if (!m.group(1).contains("${")) {
                fixas.add(m.group(1).trim());
            }
        }

        assertTrue(fixas.isEmpty(),
            "base-url fixa no application.yml quebra a traducao dentro do conteiner: " + fixas);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: precisa existir um utilitário {@code .hidden} GLOBAL.
     *
     * <p>O PREJUÍZO, medido em 06/08/2026: o projeto não tinha um, e cada tela
     * escrevia o seu escopado ao próprio painel. Sobraram SEIS painéis sem regra
     * nenhuma — em Troca de Tipo de Legenda o "Resultado da Auditoria" abria já
     * visível antes de qualquer escaneamento, e em Revisão de Lore as duas abas
     * de formulário apareciam empilhadas. O JavaScript sempre esteve certo; não
     * havia CSS do outro lado.
     *
     * <p>Exige {@code !important} porque sem ele qualquer regra de mesma
     * especificidade declarada depois vence por ordem de cascata.
     */
    @Test
    @DisplayName("existe .hidden GLOBAL, e com !important")
    void existeHiddenGlobal() throws IOException {
        String css = Files.readString(BASE_CSS);

        Matcher m = Pattern.compile("(?m)^\\.hidden\\s*\\{([^}]*)}").matcher(css);
        assertTrue(m.find(), "base.css precisa declarar um seletor .hidden global");
        assertTrue(m.group(1).contains("display") && m.group(1).contains("none"),
            ".hidden global tem de ocultar de fato");
        assertTrue(m.group(1).contains("!important"),
            "sem !important o .hidden perde para regra de mesma especificidade declarada depois");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: todo elemento marcado com {@code class="... hidden"}
     * tem de estar coberto — o utilitário global existe justamente para isso, e
     * este teste mede quantos dependem dele.
     *
     * <p>É a prova de que a catraca anterior protege algo concreto: sem o global,
     * este número vira a quantidade de elementos possivelmente visíveis por engano.
     */
    @Test
    @DisplayName("os elementos que dependem de .hidden estao contados, nao presumidos")
    void elementosQueDependemDeHiddenEstaoCobertos() throws IOException {
        Pattern usoNoHtml = Pattern.compile("class=\"[^\"]*\\bhidden\\b[^\"]*\"");
        int usos = 0;
        try (Stream<Path> arquivos = Files.walk(ESTATICO)) {
            for (Path p : arquivos.filter(f -> f.toString().endsWith(".html")).toList()) {
                Matcher m = usoNoHtml.matcher(Files.readString(p));
                while (m.find()) {
                    usos++;
                }
            }
        }
        assertTrue(usos > 0, "instrumento cego: nenhum uso de .hidden encontrado no HTML");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o diálogo NATIVO do Windows não pode voltar a ser o
     * único caminho de escolha de pasta.
     *
     * <p>O PREJUÍZO: {@code DialogoArquivoController} executa
     * {@code powershell.exe} no servidor para abrir um {@code OpenFileDialog}.
     * Num contêiner Linux não há powershell, Windows Forms nem display — os 27
     * botões "Procurar..." de 8 telas ficariam inertes, sem erro visível.
     *
     * <p>A catraca exige que o app.js consulte a capacidade ANTES e tenha um
     * caminho alternativo. Sem a consulta, descobrir a ausência custaria até 3
     * minutos de timeout por clique.
     */
    @Test
    @DisplayName("a escolha de pasta tem alternativa ao dialogo nativo do Windows")
    void escolhaDePastaNaoDependeSoDoWindows() throws IOException {
        String js = Files.readString(APP_JS);

        assertTrue(js.contains("/api/dialogo/capacidade"),
            "o app precisa PERGUNTAR se o dialogo nativo existe, nao descobrir por timeout");
        assertTrue(js.contains("/api/navegador/pastas"),
            "o navegador alternativo precisa consumir o endpoint do servidor");

        // Exige o PONTO DE CHAMADA, não a existência do nome. A primeira versao
        // desta catraca so procurava a string "abrirNavegadorPastas" — e passou
        // verde num caso doente onde a chamada tinha sido trocada por
        // `return null`, porque a DEFINICAO da funcao continuava no arquivo.
        // Funcao declarada e nunca chamada e exatamente o defeito a evitar.
        Matcher chamada = Pattern.compile("return\\s+abrirNavegadorPastas\\s*\\(").matcher(js);
        assertTrue(chamada.find(),
            "escolherCaminho tem de CAIR no navegador do servidor quando nao ha dialogo nativo");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o Dockerfile precisa continuar embutindo os binários
     * externos.
     *
     * <p>O PREJUÍZO que a imagem resolve: {@code ffmpeg}, {@code ffprobe} e
     * {@code mkvextract} são invocados pelo KRONOS e não são Java. Sem eles na
     * imagem, extração e validação falham dentro do contêiner com erro de
     * "comando não encontrado" no meio de um pipeline longo.
     */
    @Test
    @DisplayName("a imagem embute ffmpeg e mkvtoolnix")
    void imagemEmbuteBinariosExternos() throws IOException {
        Path dockerfile = Path.of("Dockerfile");
        assertTrue(Files.isRegularFile(dockerfile), "Dockerfile na raiz do projeto");

        String conteudo = Files.readString(dockerfile);
        assertTrue(conteudo.contains("ffmpeg"), "ffmpeg tem de entrar na imagem");
        assertTrue(conteudo.contains("mkvtoolnix"), "mkvtoolnix (mkvextract) tem de entrar na imagem");

        // Só as linhas FROM. A primeira versao desta catraca varria o arquivo
        // inteiro e reprovou o proprio COMENTARIO que explica por que a UBI nao
        // serve — instrumento que le prosa como se fosse instrucao.
        List<String> basesUbi = conteudo.lines()
            .map(String::strip)
            .filter(l -> l.startsWith("FROM "))
            .filter(l -> l.contains("ubi") || l.contains("redhat"))
            .toList();
        assertTrue(basesUbi.isEmpty(),
            "base UBI nao tem ffmpeg nos repositorios; a imagem usa base Debian/Ubuntu de proposito: " + basesUbi);
    }
}
