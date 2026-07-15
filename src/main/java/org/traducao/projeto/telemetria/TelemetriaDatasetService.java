package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.ProcessoExternoUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: publica a telemetria acumulada como dataset público num repositório Git
 * DEDICADO ({@code kronos-anime-translation-telemetry-dataset}, seguindo a
 * convenção {@code [NomeDoSistema]-telemetry-dataset} para dados de pesquisa/ML).
 * <p>
 * O serviço é auto-suficiente: se o repositório local não existir, ele clona o
 * remoto configurado (ou inicializa um novo e associa o remoto); na primeira
 * publicação gera README com declaração de anonimização (LGPD/GDPR), LICENSE e
 * a estrutura {@code metrics/}. Cada publicação = 1 commit + push, e o
 * histórico Git é o versionamento natural dos snapshots.
 * <p>
 * <p>INVARIANTES DO DOMÍNIO: a sanitização deliberada mantém
 * carrega apenas MÉTRICAS: nada de textos de legenda (os avisos viram
 * contagem), nada de caminhos de máquina (o campo {@code detalhe} das
 * operações é descartado e nomes de episódio perdem qualquer diretório); o
 * ambiente de hardware pertence integralmente à máquina publicadora.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: erros de geração, Git ou rede interrompem a
 * publicação com {@link IOException}, preservando o snapshot anterior.
 */
@ApplicationScoped
public class TelemetriaDatasetService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaDatasetService.class);

    static final String NOME_ARQUIVO_DATASET = "kronos-telemetria-dataset.json";
    private static final Duration TIMEOUT_GIT = Duration.ofSeconds(30);
    private static final Duration TIMEOUT_REDE = Duration.ofMinutes(2);

    private final TelemetriaService telemetria;
    private final TelemetriaDatasetProperties propriedades;
    private final AmbienteExecucaoDatasetService ambienteExecucao;
    private final ObjectMapper mapper = new ObjectMapper();

    public TelemetriaDatasetService(
            TelemetriaService telemetria,
            TelemetriaDatasetProperties propriedades,
            AmbienteExecucaoDatasetService ambienteExecucao) {
        this.telemetria = telemetria;
        this.propriedades = propriedades;
        this.ambienteExecucao = ambienteExecucao;
    }

    /** Resultado da publicação, devolvido ao painel de Telemetria. */
    public record ResultadoPublicacao(String repositorio, String commit, boolean pushOk, String mensagem) {}

    public synchronized ResultadoPublicacao publicar() throws IOException {
        Path repo = Path.of(propriedades.repositorioLocal()).toAbsolutePath().normalize();
        prepararRepositorio(repo);
        garantirDocumentosBase(repo);

        TelemetriaResumo resumo = telemetria.gerarResumo(Path.of("cache"));
        Path pastaMetrics = repo.resolve("metrics");
        Files.createDirectories(pastaMetrics);
        Path arquivo = pastaMetrics.resolve(NOME_ARQUIVO_DATASET);
        // Pretty-print proposital: o arquivo é lido por humanos no GitHub.
        mapper.writerWithDefaultPrettyPrinter().writeValue(arquivo.toFile(),
            montarDatasetSanitizado(resumo, mapper, ambienteExecucao.detectar(propriedades.hardware())));
        log.info("Dataset de telemetria gerado em {}", arquivo);

        git(repo, TIMEOUT_GIT, "add", "README.md", "LICENSE", "metrics/" + NOME_ARQUIVO_DATASET);
        String mensagemCommit = String.format(Locale.ROOT,
            "dataset: snapshot com %d episódios e %d operações",
            resumo.totalEpisodios(), resumo.operacoes() != null ? resumo.operacoes().size() : 0);
        ProcessoExternoUtil.Resultado commit = git(repo, TIMEOUT_GIT, "commit", "-m", mensagemCommit);
        boolean semMudancas = commit.codigoSaida() != 0
            && saida(commit).toLowerCase(Locale.ROOT).contains("nothing to commit");
        if (commit.codigoSaida() != 0 && !semMudancas) {
            throw new IOException("git commit falhou no repositório do dataset: " + resumir(saida(commit)));
        }
        String hash = saida(git(repo, TIMEOUT_GIT, "rev-parse", "--short", "HEAD")).trim();

        // Push sempre (mesmo sem commit novo): publica commits pendentes de
        // tentativas anteriores sem rede/sem repositório remoto criado.
        ProcessoExternoUtil.Resultado push = git(repo, TIMEOUT_REDE, "push");
        if (push.codigoSaida() != 0 && saida(push).contains("--set-upstream")) {
            push = git(repo, TIMEOUT_REDE, "push", "-u", "origin", "HEAD");
        }
        if (push.codigoSaida() != 0 && saida(push).contains("[rejected]")) {
            // Repositório remoto criado com README/commits próprios (caso real
            // de 2026-07-09): integra o histórico preferindo a versão LOCAL em
            // conflito — o gerador local é a fonte de verdade do dataset.
            git(repo, TIMEOUT_REDE, "pull", "--no-edit", "--allow-unrelated-histories", "-X", "ours", "origin", "main");
            push = git(repo, TIMEOUT_REDE, "push", "-u", "origin", "HEAD");
        }
        boolean pushOk = push.codigoSaida() == 0;
        if (!pushOk) {
            log.warn("git push do dataset falhou: {}", saida(push));
        }

        String mensagem = montarMensagem(semMudancas, pushOk, hash, saida(push));
        return new ResultadoPublicacao(repo.toString(), semMudancas ? "sem mudanças" : hash, pushOk, mensagem);
    }

    private String montarMensagem(boolean semMudancas, boolean pushOk, String hash, String saidaPush) {
        if (pushOk) {
            return semMudancas
                ? "Dataset já estava atualizado — nenhum commit novo; push confirmado."
                : "Dataset publicado no repositório dedicado (commit " + hash + ").";
        }
        String dica = saidaPush != null && (saidaPush.contains("not found") || saidaPush.contains("does not exist"))
            ? " Crie o repositório \"" + nomeRepositorioRemoto() + "\" no GitHub e publique novamente."
            : "";
        return (semMudancas ? "Dataset já estava atualizado" : "Commit " + hash + " criado localmente")
            + ", mas o push falhou: " + resumir(saidaPush) + dica;
    }

    private String nomeRepositorioRemoto() {
        String remoto = propriedades.repositorioRemoto();
        if (remoto == null || remoto.isBlank()) {
            return "kronos-anime-translation-telemetry-dataset";
        }
        String semSufixo = remoto.replaceFirst("\\.git$", "");
        int barra = semSufixo.lastIndexOf('/');
        return barra >= 0 ? semSufixo.substring(barra + 1) : semSufixo;
    }

    /** Clona o remoto configurado ou inicializa um repositório novo com o remoto associado. */
    private void prepararRepositorio(Path repo) throws IOException {
        if (Files.isDirectory(repo.resolve(".git"))) {
            return;
        }
        String remoto = propriedades.repositorioRemoto();
        if (remoto != null && !remoto.isBlank()) {
            log.info("Repositório do dataset não existe em {}; clonando {}", repo, remoto);
            ProcessoExternoUtil.Resultado clone = executarGit(
                List.of("git", "clone", remoto, repo.toString()), TIMEOUT_REDE);
            if (clone.codigoSaida() == 0) {
                return;
            }
            log.warn("Clone do dataset falhou ({}); inicializando repositório local novo.", resumir(saida(clone)));
        }
        Files.createDirectories(repo);
        ProcessoExternoUtil.Resultado init = git(repo, TIMEOUT_GIT, "init", "-b", "main");
        if (init.codigoSaida() != 0) {
            throw new IOException("git init falhou em " + repo + ": " + resumir(saida(init)));
        }
        if (remoto != null && !remoto.isBlank()) {
            git(repo, TIMEOUT_GIT, "remote", "add", "origin", remoto);
        }
    }

    /**
     * Bootstrap do repositório na primeira publicação: README com formato dos
     * dados e declaração de anonimização (LGPD/GDPR) + LICENSE (MIT) — os três
     * itens que a comunidade procura primeiro num repositório de dataset.
     */
    private void garantirDocumentosBase(Path repo) throws IOException {
        Path readme = repo.resolve("README.md");
        if (!Files.exists(readme)) {
            Files.writeString(readme, README_DATASET, StandardCharsets.UTF_8);
        }
        Path licenca = repo.resolve("LICENSE");
        if (!Files.exists(licenca)) {
            Files.writeString(licenca, textoLicencaMit(), StandardCharsets.UTF_8);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta o snapshot sanitizado por episódio e operação.
     *
     * <p>INVARIANTES DO DOMÍNIO: não inclui
     * textos de fala (avisos viram {@code quantidadeAvisos}) e sem caminhos de
     * máquina ({@code detalhe} descartado; episódio reduzido ao nome do arquivo).
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ambiente ausente é simplesmente omitido;
     * métricas obrigatórias continuam sendo serializadas no formato atual.
     */
    static ObjectNode montarDatasetSanitizado(TelemetriaResumo resumo, ObjectMapper mapper) {
        return montarDatasetSanitizado(resumo, mapper, null);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: serializa métricas e a fotografia automática do host
     * no schema público vigente do dataset.
     *
     * <p>INVARIANTES DO DOMÍNIO: formato 2 não contém override manual de hardware,
     * textos de legenda ou caminhos locais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ambiente nulo é omitido e o restante do
     * snapshot permanece válido.
     */
    static ObjectNode montarDatasetSanitizado(
            TelemetriaResumo resumo,
            ObjectMapper mapper,
            AmbienteExecucaoDataset ambienteExecucao) {
        ObjectNode root = mapper.createObjectNode();
        root.put("dataset", "kronos-anime-translation-telemetry-dataset");
        root.put("versaoFormato", 2);
        root.put("geradoEm", Instant.now().toString());
        root.put("descricao", "Métricas operacionais de tradução de legendas de anime com LLM 100% local "
            + "(LM Studio). Sem textos de legenda e sem caminhos de máquina — apenas métricas.");

        adicionarAmbienteExecucao(root, ambienteExecucao);

        ObjectNode agregado = root.putObject("resumo");
        agregado.put("totalEpisodiosTraduzidos", resumo.totalEpisodios());
        agregado.put("totalLinhasTraduzidas", resumo.totalLinhas());
        agregado.put("tempoMedioPorLinhaMs", resumo.tempoMedioPorLinhaMs());
        agregado.put("totalFalasReaproveitadasDoCache", resumo.totalCacheHits());
        agregado.put("alucinacoesLlmPrevenidas", resumo.alucinacoesPrevenidas());
        agregado.put("respostasTraducaoRejeitadas", resumo.respostasTraducaoRejeitadas());
        agregado.put("falhasTraducaoRecuperadas", resumo.falhasTraducaoRecuperadas());
        agregado.put("fallbacksTraducaoMantidos", resumo.fallbacksTraducaoMantidos());
        agregado.put("arquivosRenomeados", resumo.arquivosSanitizados());
        agregado.put("totalOperacoesRegistradas", resumo.operacoes() != null ? resumo.operacoes().size() : 0);

        ArrayNode traducoes = root.putArray("traducoesLlm");
        if (resumo.traducoesLlm() != null) {
            for (LlmTelemetria t : resumo.traducoesLlm()) {
                ObjectNode item = traducoes.addObject();
                item.put("episodio", apenasNomeDeArquivo(t.nomeEpisodio()));
                item.put("anime", t.animeNome());
                item.put("temporada", t.temporada());
                item.put("modeloLlm", t.modeloLlm());
                item.put("totalLinhas", t.totalLinhas());
                item.put("falasTraduzidas", t.falasTraduzidas());
                item.put("falasDoCache", t.falasDoCache());
                item.put("tempoTotalMs", t.tempoTotalMs());
                item.put("quantidadeAvisos", contarAvisos(t.errosOcorridos()));
                item.put("registradoEm", t.registradoEm());
            }
        }

        ArrayNode operacoes = root.putArray("operacoes");
        if (resumo.operacoes() != null) {
            for (OperacaoTelemetria op : resumo.operacoes()) {
                ObjectNode item = operacoes.addObject();
                item.put("tipo", op.tipo());
                item.put("tempoTotalMs", op.tempoTotalMs());
                item.put("arquivosProcessados", op.arquivosProcessados());
                item.put("itensDetectados", op.itensDetectados());
                item.put("itensCorrigidos", op.itensCorrigidos());
                item.put("registradoEm", op.registradoEm());
            }
        }
        return root;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: adiciona ao JSON a fotografia coerente do computador
     * responsável pela geração do snapshot.
     *
     * <p>INVARIANTES DO DOMÍNIO: GPU principal pertence à lista detectada e campos
     * ausentes não são inventados nem herdados de configuração.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: ambiente nulo não cria o bloco; lista de
     * GPUs vazia é publicada como array vazio.
     */
    private static void adicionarAmbienteExecucao(ObjectNode root, AmbienteExecucaoDataset ambiente) {
        if (ambiente == null) {
            return;
        }
        ObjectNode node = root.putObject("ambienteExecucao");
        putIfPresent(node, "fabricante", ambiente.fabricante());
        putIfPresent(node, "modeloMaquina", ambiente.modeloMaquina());
        putIfPresent(node, "cpu", ambiente.cpu());
        putIfPresent(node, "gpuPrincipal", ambiente.gpuPrincipal());
        ArrayNode gpus = node.putArray("gpusDetectadas");
        ambiente.gpusDetectadas().forEach(gpus::add);
        if (ambiente.ramTotalGb() != null) {
            node.put("ramTotalGb", ambiente.ramTotalGb());
        }
        putIfPresent(node, "sistemaOperacional", ambiente.sistemaOperacional());
        putIfPresent(node, "arquitetura", ambiente.arquitetura());
        node.put("hardwareColetadoAutomaticamente", ambiente.hardwareColetadoAutomaticamente());
    }

    private static void putIfPresent(ObjectNode node, String campo, String valor) {
        if (valor != null && !valor.isBlank()) {
            node.put(campo, valor);
        }
    }

    private static final java.util.regex.Pattern MARCADOR_AVISOS_OMITIDOS =
        java.util.regex.Pattern.compile("\\(\\+(\\d+) avisos omitidos");

    /**
     * Conta os avisos REAIS do episódio: a telemetria canônica guarda no
     * máximo 30 avisos + uma linha-resumo "(+N avisos omitidos...)" (ver
     * {@link TelemetriaService}); aqui o total é reconstituído para o dataset
     * não subnotificar a métrica de qualidade.
     */
    static int contarAvisos(List<String> avisos) {
        if (avisos == null || avisos.isEmpty()) {
            return 0;
        }
        var matcher = MARCADOR_AVISOS_OMITIDOS.matcher(avisos.getLast());
        if (matcher.find()) {
            return avisos.size() - 1 + Integer.parseInt(matcher.group(1));
        }
        return avisos.size();
    }

    static String apenasNomeDeArquivo(String nome) {
        if (nome == null) {
            return null;
        }
        String normalizado = nome.replace('\\', '/');
        int barra = normalizado.lastIndexOf('/');
        return barra >= 0 ? normalizado.substring(barra + 1) : nome;
    }

    private ProcessoExternoUtil.Resultado git(Path repo, Duration timeout, String... argumentos) throws IOException {
        List<String> comando = new ArrayList<>(List.of("git", "-C", repo.toString()));
        comando.addAll(List.of(argumentos));
        return executarGit(comando, timeout);
    }

    private ProcessoExternoUtil.Resultado executarGit(List<String> comando, Duration timeout) throws IOException {
        try {
            return ProcessoExternoUtil.executar(comando, timeout, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Publicação do dataset interrompida.", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException("Comando git excedeu o tempo limite: " + String.join(" ", comando), e);
        }
    }

    private static String saida(ProcessoExternoUtil.Resultado resultado) {
        return new String(resultado.stdout(), StandardCharsets.UTF_8);
    }

    private static String resumir(String texto) {
        String plano = texto == null ? "" : texto.replaceAll("\\s+", " ").trim();
        return plano.length() > 180 ? plano.substring(0, 177) + "..." : plano;
    }

    private static String textoLicencaMit() {
        return "MIT License\n\n"
            + "Copyright (c) " + Year.now() + " Paulo André Carminati\n\n"
            + "Permission is hereby granted, free of charge, to any person obtaining a copy\n"
            + "of this software and associated documentation files (the \"Software\"), to deal\n"
            + "in the Software without restriction, including without limitation the rights\n"
            + "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n"
            + "copies of the Software, and to permit persons to whom the Software is\n"
            + "furnished to do so, subject to the following conditions:\n\n"
            + "The above copyright notice and this permission notice shall be included in all\n"
            + "copies or substantial portions of the Software.\n\n"
            + "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n"
            + "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n"
            + "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n"
            + "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n"
            + "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n"
            + "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n"
            + "SOFTWARE.\n";
    }

    private static final String README_DATASET = """
        # KRONOS CORE — Telemetry Dataset

        [English](#english) | [Português](#portugues)

        <a id="english"></a>

        ## English

        Operational telemetry dataset from [KRONOS CORE](https://github.com/carmipa/traducao_animes_llm_local_quarkus), an anime subtitle translation pipeline that runs local LLM inference through LM Studio.

        Source translator project: [carmipa/traducao_animes_llm_local_quarkus](https://github.com/carmipa/traducao_animes_llm_local_quarkus)

        This repository is meant to expose reproducible performance and pipeline metrics, not subtitle content. Each commit is a dataset snapshot; the Git history is the public timeline.

        ### Repository Layout

        ```text
        ├── README.md
        ├── LICENSE
        └── metrics/
            └── kronos-telemetria-dataset.json
        ```

        ### Data Format

        The dataset uses a custom UTF-8 JSON format with `versaoFormato` for schema evolution.

        #### `ambienteExecucao`

        Safe execution-environment metadata for benchmark context.

        | Field | Meaning |
        |-------|---------|
        | `fabricante` / `modeloMaquina` | Generic manufacturer and machine model reported by the OS |
        | `cpu` | Public CPU name |
        | `gpuPrincipal` | Dedicated GPU selected automatically from the current machine |
        | `gpusDetectadas` | All GPUs reported by the current operating system/driver |
        | `ramTotalGb` | Rounded total physical RAM in GB |
        | `sistemaOperacional` / `arquitetura` | Runtime platform, without username, hostname, paths, IPs or device IDs |
        | `hardwareColetadoAutomaticamente` | Whether the values were collected automatically from the local system |

        #### `resumo`

        Aggregate metrics.

        | Field | Meaning |
        |-------|---------|
        | `totalEpisodiosTraduzidos` | Episodes processed by the LLM translation pipeline |
        | `totalLinhasTraduzidas` | Subtitle dialogue lines translated |
        | `tempoMedioPorLinhaMs` | Average translation latency per dialogue line |
        | `totalFalasReaproveitadasDoCache` | Dialogue lines resolved from persistent cache without another LLM call |
        | `alucinacoesLlmPrevenidas` | LLM responses rejected by anti-hallucination guards |
        | `respostasTraducaoRejeitadas` | Invalid model attempts rejected before persistence |
        | `falhasTraducaoRecuperadas` | Lines recovered by a later validated retry |
        | `fallbacksTraducaoMantidos` | Distinct lines still pending after retry exhaustion |
        | `arquivosRenomeados` | Files normalized by the rename module |
        | `totalOperacoesRegistradas` | Recorded pipeline operations across modules |

        #### `traducoesLlm[]`

        Per-episode LLM translation metrics.

        | Field | Meaning |
        |-------|---------|
        | `episodio` | Subtitle filename only, without directories |
        | `anime` / `temporada` | Work and season |
        | `modeloLlm` | Local model id reported by LM Studio |
        | `totalLinhas` / `falasTraduzidas` / `falasDoCache` | Workload and translation source |
        | `tempoTotalMs` | Total episode translation duration |
        | `quantidadeAvisos` | Count of quality warnings, without warning text |
        | `registradoEm` | UTC ISO-8601 timestamp |

        #### `operacoes[]`

        Generic pipeline-operation metrics: `tipo`, `tempoTotalMs`, `arquivosProcessados`, `itensDetectados`, `itensCorrigidos`, `registradoEm`.

        This covers remuxing, subtitle extraction, lore/review steps, karaoke processing, file renaming and audits.

        ### Privacy And Anonymization

        This dataset does not publish subtitle text, local machine paths, usernames, hostnames, IP addresses, MAC addresses, serial numbers, device identifiers, credentials, tokens or API keys.

        The only public identifiers are release/work names, local LLM model ids and generic hardware metadata useful for benchmark interpretation.

        ### Generation

        The dataset is generated from the KRONOS CORE Telemetry panel through the **Publicar Dataset** button. KRONOS sanitizes the accumulated telemetry, writes `metrics/kronos-telemetria-dataset.json`, commits the snapshot and pushes it to this repository.

        ### License

        [MIT](LICENSE) — free use with attribution.

        <a id="portugues"></a>

        ## Português

        Dataset de telemetria operacional do [KRONOS CORE](https://github.com/carmipa/traducao_animes_llm_local_quarkus), uma esteira de tradução de legendas de anime que executa inferência LLM local via LM Studio.

        Projeto do tradutor: [carmipa/traducao_animes_llm_local_quarkus](https://github.com/carmipa/traducao_animes_llm_local_quarkus)

        Este repositório existe para expor métricas reprodutíveis de performance e pipeline, não conteúdo de legendas. Cada commit é um snapshot do dataset; o histórico Git é a linha do tempo pública.

        ### Estrutura Do Repositório

        ```text
        ├── README.md
        ├── LICENSE
        └── metrics/
            └── kronos-telemetria-dataset.json
        ```

        ### Formato Dos Dados

        O dataset usa JSON próprio em UTF-8, com `versaoFormato` para evolução do schema.

        #### `ambienteExecucao`

        Metadados seguros do ambiente de execução para contextualizar benchmarks.

        | Campo | Significado |
        |-------|-------------|
        | `fabricante` / `modeloMaquina` | Fabricante e modelo genérico reportados pelo sistema operacional |
        | `cpu` | Nome público do processador |
        | `gpuPrincipal` | GPU dedicada selecionada automaticamente na máquina atual |
        | `gpusDetectadas` | Todas as GPUs reportadas pelo sistema operacional/driver atual |
        | `ramTotalGb` | RAM física total arredondada em GB |
        | `sistemaOperacional` / `arquitetura` | Plataforma de execução, sem usuário, hostname, caminhos, IPs ou IDs de dispositivo |
        | `hardwareColetadoAutomaticamente` | Indica se os valores foram coletados automaticamente do sistema local |

        #### `resumo`

        Métricas agregadas.

        | Campo | Significado |
        |-------|-------------|
        | `totalEpisodiosTraduzidos` | Episódios processados pelo pipeline de tradução LLM |
        | `totalLinhasTraduzidas` | Falas de legenda traduzidas |
        | `tempoMedioPorLinhaMs` | Latência média de tradução por fala |
        | `totalFalasReaproveitadasDoCache` | Falas resolvidas pelo cache persistente sem nova chamada ao LLM |
        | `alucinacoesLlmPrevenidas` | Respostas de LLM rejeitadas pelas guardas anti-alucinação |
        | `respostasTraducaoRejeitadas` | Tentativas inválidas rejeitadas antes da persistência |
        | `falhasTraducaoRecuperadas` | Falas recuperadas por tentativa posterior validada |
        | `fallbacksTraducaoMantidos` | Falas distintas ainda pendentes após esgotar tentativas |
        | `arquivosRenomeados` | Arquivos padronizados pelo módulo de renomeação |
        | `totalOperacoesRegistradas` | Operações de pipeline registradas entre os módulos |

        #### `traducoesLlm[]`

        Métricas de tradução LLM por episódio.

        | Campo | Significado |
        |-------|-------------|
        | `episodio` | Nome do arquivo de legenda, sem diretórios |
        | `anime` / `temporada` | Obra e temporada |
        | `modeloLlm` | Modelo local usado, conforme id reportado pelo LM Studio |
        | `totalLinhas` / `falasTraduzidas` / `falasDoCache` | Volume e origem das traduções |
        | `tempoTotalMs` | Duração total da tradução do episódio |
        | `quantidadeAvisos` | Contagem de avisos de qualidade, sem texto dos avisos |
        | `registradoEm` | Timestamp UTC em ISO-8601 |

        #### `operacoes[]`

        Métricas genéricas por operação de pipeline: `tipo`, `tempoTotalMs`, `arquivosProcessados`, `itensDetectados`, `itensCorrigidos`, `registradoEm`.

        Cobre remux, extração de legendas, revisões de lore/concordância, karaokê, renomeação de arquivos e auditorias.

        ### Privacidade E Anonimização

        Este dataset não publica texto de legenda, caminhos locais da máquina, nomes de usuário, hostnames, endereços IP, endereços MAC, números de série, identificadores de dispositivo, credenciais, tokens ou chaves de API.

        Os únicos identificadores públicos são nomes de obras/releases, ids de modelos LLM locais e metadados genéricos de hardware úteis para interpretar benchmarks.

        ### Geração

        O dataset é gerado pelo painel de Telemetria do KRONOS CORE através do botão **Publicar Dataset**. O KRONOS sanitiza a telemetria acumulada, escreve `metrics/kronos-telemetria-dataset.json`, commita o snapshot e faz push para este repositório.

        ### Licença

        [MIT](LICENSE) — uso livre com atribuição.
        """;
}
