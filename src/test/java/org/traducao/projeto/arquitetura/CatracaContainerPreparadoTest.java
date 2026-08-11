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
     * PROPÓSITO DE NEGÓCIO: o KRONOS não pode se plugar sozinho num Redis que não
     * é dele.
     *
     * <p>O PREJUÍZO, medido em 06/08/2026: o default era
     * {@code redis://localhost:6379}, e esta máquina já tinha um
     * {@code redis-stack-server} de OUTRO projeto escutando nessa porta. No
     * primeiro boot o KRONOS conectou nele sozinho e o painel reportou
     * "conectado" — sem ninguém pedir, e sem nada avisar. Pior: o teste de
     * degradação passou verde justamente porque havia um Redis alheio
     * respondendo, ou seja, a ausência nunca chegou a ser exercitada.
     *
     * <p>É o que a decisão de 03/08 no cérebro proíbe: Redis PRÓPRIO, nunca o
     * compartilhado, porque reusar cria vínculo de confiança entre projetos.
     */
    @Test
    @DisplayName(".env nao pode carregar configuracao que a aplicacao le")
    void envNaoCarregaConfiguracaoDaAplicacao() throws IOException {
        // O PREJUÍZO, medido em 06/08/2026: o .env.example trazia
        // KRONOS_LLM_BASE_URL=http://host.docker.internal:1234/v1, pensado para o
        // contêiner. Só que o Quarkus lê .env da pasta de trabalho e trata cada
        // linha como configuração — a aplicação rodando NO WINDOWS passou a
        // apontar o LM Studio para um host que só existe dentro do Docker. O
        // sintoma chegou como teste de integração quebrado, longe da causa.
        Path exemplo = Path.of(".env.example");
        assertTrue(Files.isRegularFile(exemplo), ".env.example precisa existir");

        List<String> lidasPelaApp = List.of("KRONOS_LLM_BASE_URL", "KRONOS_REDIS_URL",
            "KRONOS_DATASET_REPO", "QUARKUS_HTTP_HOST");

        List<String> declaradas = Files.readAllLines(exemplo).stream()
            .map(String::strip)
            .filter(l -> !l.startsWith("#") && l.contains("="))
            .map(l -> l.substring(0, l.indexOf('=')).strip())
            .filter(lidasPelaApp::contains)
            .toList();

        assertTrue(declaradas.isEmpty(),
            "estas chaves sao lidas pela APLICACAO e no .env valeriam tambem fora do conteiner: "
                + declaradas);
    }

    @Test
    @DisplayName("o Redis padrao NAO pode ser localhost — o KRONOS nao adota Redis alheio")
    void redisPadraoNaoEhLocalhost() throws IOException {
        Path props = Path.of("src", "main", "resources", "application.properties");
        String conteudo = Files.readString(props);

        Matcher m = Pattern.compile("(?m)^quarkus\\.redis\\.hosts\\s*=\\s*(.+)$").matcher(conteudo);
        assertTrue(m.find(), "quarkus.redis.hosts precisa estar declarado");

        String valor = m.group(1);
        assertFalse(valor.contains("localhost") || valor.contains("127.0.0.1"),
            "default de Redis apontando para a maquina local faz o KRONOS adotar o Redis de outro "
                + "projeto que por acaso esteja no ar: " + valor);
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

    /**
     * PROPÓSITO DE NEGÓCIO: nenhum caminho do HOST pode entrar no contêiner por
     * default silencioso. Quem não configurou tem de receber uma recusa, não um
     * palpite.
     *
     * <p>O PREJUÍZO, medido em 11/08/2026: as duas linhas de volume traziam
     * {@code ${KRONOS_ACERVO:-C:/animes}} e
     * {@code ${KRONOS_DATASET_REPO_HOST:-../kronos-...}}. Sem {@code .env}, o
     * {@code docker compose config} respondia <b>0</b> e resolvia os defaults —
     * montava em silêncio um caminho que ninguém configurou. Um {@code C:/animes}
     * que exista e não seja o acervo entra sem aviso; no dataset, uma pasta que
     * não é o repositório real recebe {@code git init/add/commit} do botão
     * "Publicar Dataset", publicação que reporta sucesso sem publicar.
     *
     * <p>E caminho errado nunca virava erro. Medido em 11/08/2026 com o Docker no
     * ar: bind mount para caminho ausente NÃO é recusado — o Docker cria a pasta
     * e o contêiner a enxerga vazia. Apontar para o lugar errado produzia
     * "nada a traduzir", o mesmo sinal de "acervo não montado".
     *
     * <p>É a regra de falha fechada que {@code docs/ref-docker.md} já exigia da
     * tradução de caminho na borda, aplicada uma camada antes — e a mesma lição do
     * {@code ${VAR:?}} da REGRA-DESTE-DOCKER do site do Christiano, onde
     * {@code ${VAR}} vazio subia o contêiner com senha em branco.
     *
     * <p>Prova de fora do teste: {@code docker compose --env-file /dev/null config}
     * tem de sair <b>1</b> dizendo qual variável falta.
     */
    @Test
    @DisplayName("caminho do host no compose falha FECHADO — nada de default silencioso")
    void volumesNaoTemDefaultSilencioso() throws IOException {
        Path compose = Path.of("docker-compose.yml");
        assertTrue(Files.isRegularFile(compose), "docker-compose.yml na raiz do projeto");

        List<String> silenciosos = defaultsSilenciososEmVolumes(Files.readString(compose));
        assertTrue(silenciosos.isEmpty(),
            "variavel de caminho com default silencioso no docker-compose.yml: " + silenciosos
                + " — troque por ${NOME:?mensagem dizendo o que configurar no .env}");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: provar que a catraca acima enxerga o defeito. Guarda
     * exercitada só no arquivo são pode estar aprovando por não enxergar nada.
     *
     * <p>Cobre as três grafias que o compose aceita para "siga sem valor":
     * {@code ${VAR}}, {@code ${VAR:-x}} e {@code ${VAR-x}}. Buscar uma só mediria
     * a forma, não o invariante.
     */
    @Test
    @DisplayName("caso-controle: a catraca de volume reprova as tres grafias de default")
    void catracaDeVolumeReprovaCasoDoente() {
        String doente = """
            services:
              kronos:
                volumes:
                  - "${ACERVO_SEM_NADA}:/acervo"
                  - "${ACERVO_DOIS_PONTOS:-C:/animes}:/acervo2"
                  - "${ACERVO_TRACO-../algum/lugar}:/dataset"
                  - "${ACERVO_CERTO:?configure no .env}:/ok"
                  - ./cache:/app/cache
            """;

        List<String> achados = defaultsSilenciososEmVolumes(doente);
        assertTrue(achados.contains("ACERVO_SEM_NADA"), "nao viu ${VAR} sem valor: " + achados);
        assertTrue(achados.contains("ACERVO_DOIS_PONTOS"), "nao viu ${VAR:-default}: " + achados);
        assertTrue(achados.contains("ACERVO_TRACO"), "nao viu ${VAR-default}: " + achados);
        assertFalse(achados.contains("ACERVO_CERTO"), "reprovou a forma CORRETA ${VAR:?...}: " + achados);
    }

    /**
     * Devolve as variáveis interpoladas em linha de volume que NÃO exigem valor.
     *
     * <p>Só olha linha de volume ({@code - "..."} com alvo de montagem) porque em
     * {@code environment:} um default é legítimo — {@code KRONOS_LLM_BASE_URL}
     * aponta para o LM Studio local e funciona sem ninguém configurar nada.
     */
    private static List<String> defaultsSilenciososEmVolumes(String compose) {
        Pattern interpolacao = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)([^}]*)}");
        List<String> silenciosos = new ArrayList<>();
        for (String linha : compose.lines().map(String::strip).toList()) {
            if (!linha.startsWith("- ") || !linha.contains("${") || !linha.contains(":/")) {
                continue;
            }
            Matcher m = interpolacao.matcher(linha);
            while (m.find()) {
                if (!m.group(2).startsWith(":?")) {
                    silenciosos.add(m.group(1));
                }
            }
        }
        return silenciosos;
    }
}
