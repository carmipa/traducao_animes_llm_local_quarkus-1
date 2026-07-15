package org.traducao.projeto.traducao.presentation.web;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * PROPÓSITO DE NEGÓCIO: disponibiliza aos formulários web do KRONOS um seletor
 * nativo e responsivo para arquivos e pastas existentes no computador local.
 * <p>
 * INVARIANTES DO DOMÍNIO: existe no máximo um diálogo aberto por vez; o helper
 * gráfico deve executar em STA; caminhos trafegam em UTF-8/Base64 para preservar
 * acentos; o processo PowerShell é reutilizado e nunca recriado a cada clique.
 * <p>
 * COMPORTAMENTO EM CASO DE FALHA: reinicia o helper uma vez quando ele morre ou
 * perde o protocolo; após nova falha ou timeout, encerra o helper e devolve caminho
 * vazio, mantendo a interface utilizável e permitindo nova tentativa.
 */
@RestController
@RequestMapping("/api/dialogo")
public class DialogoArquivoController {

    private static final Logger log = LoggerFactory.getLogger(DialogoArquivoController.class);
    private static final Duration TIMEOUT_INICIALIZACAO = Duration.ofSeconds(10);
    private static final Duration TIMEOUT_DIALOGO = Duration.ofMinutes(3);
    private static final String MARCADOR_PRONTO = "__KRONOS_DIALOG_READY__";
    private static final String SCRIPT_HELPER = """
        $ErrorActionPreference = 'Stop'
        [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
        Add-Type -AssemblyName System.Windows.Forms | Out-Null
        [Console]::Out.WriteLine('__KRONOS_DIALOG_READY__')
        [Console]::Out.Flush()
        while (($requisicao = [Console]::In.ReadLine()) -ne $null) {
            $resultado = ''
            $owner = $null
            $dialogo = $null
            try {
                $partes = $requisicao -split '\\|', 3
                $tipo = $partes[0]
                $argumento = if ($partes.Length -gt 1 -and $partes[1]) {
                    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($partes[1]))
                } else { '' }
                $dirInicial = if ($partes.Length -gt 2 -and $partes[2]) {
                    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($partes[2]))
                } else { '' }

                if ($tipo -eq 'PING') {
                    $resultado = 'PONG'
                } else {
                    $owner = New-Object System.Windows.Forms.Form
                    $owner.TopMost = $true
                    $owner.ShowInTaskbar = $false
                    $owner.StartPosition = 'CenterScreen'
                    $owner.FormBorderStyle = 'FixedToolWindow'
                    $owner.Opacity = 0.01
                    $owner.Width = 1
                    $owner.Height = 1
                    $owner.Show()
                    $owner.Activate()
                    [System.Windows.Forms.Application]::DoEvents()

                    $dialogo = New-Object System.Windows.Forms.OpenFileDialog
                    if ($tipo -eq 'PASTA') {
                        $dialogo.Title = 'Selecione a pasta desejada'
                        $dialogo.CheckFileExists = $false
                        $dialogo.CheckPathExists = $true
                        $dialogo.ValidateNames = $false
                        $dialogo.FileName = 'Selecione esta pasta'
                    } else {
                        $dialogo.Title = 'Selecione o arquivo desejado'
                        if ($argumento) { $dialogo.Filter = $argumento }
                    }

                    if ($dirInicial -and (Test-Path -LiteralPath $dirInicial -PathType Container)) {
                        $dialogo.InitialDirectory = $dirInicial
                    }

                    if ($dialogo.ShowDialog($owner) -eq [System.Windows.Forms.DialogResult]::OK) {
                        $resultado = if ($tipo -eq 'PASTA') {
                            Split-Path $dialogo.FileName -Parent
                        } else { $dialogo.FileName }
                    }
                }
            } catch {
                [Console]::Error.WriteLine($_.Exception.ToString())
            } finally {
                if ($null -ne $dialogo) { $dialogo.Dispose() }
                if ($null -ne $owner) { $owner.Close(); $owner.Dispose() }
            }

            $resposta = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($resultado))
            [Console]::Out.WriteLine($resposta)
            [Console]::Out.Flush()
        }
        """;

    private final Object travaDialogo = new Object();
    private Process helper;
    private BufferedWriter entradaHelper;
    private BufferedReader saidaHelper;

    /**
     * PROPÓSITO DE NEGÓCIO: aquece o seletor nativo junto com o KRONOS para que
     * o primeiro clique tenha a mesma resposta rápida dos cliques seguintes.
     * <p>
     * INVARIANTES DO DOMÍNIO: o aquecimento só ocorre no Windows e não abre
     * nenhuma janela para Paulo.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: registra o diagnóstico sem impedir a subida
     * da aplicação; o primeiro clique fará uma nova tentativa automaticamente.
     */
    void aquecerSeletor(@Observes StartupEvent evento) {
        if (!ehWindows()) {
            return;
        }
        synchronized (travaDialogo) {
            try {
                iniciarHelper();
            } catch (Exception e) {
                log.warn("Nao foi possivel pre-aquecer o seletor nativo do Windows", e);
                encerrarHelper();
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: libera o processo gráfico auxiliar quando o KRONOS é
     * encerrado, evitando processos PowerShell órfãos no Windows.
     * <p>
     * INVARIANTES DO DOMÍNIO: encerra somente o processo criado por este controller.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: força a destruição do helper e não interfere
     * no desligamento da aplicação.
     */
    void desligarSeletor(@Observes ShutdownEvent evento) {
        synchronized (travaDialogo) {
            encerrarHelper();
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite escolher uma pasta local em qualquer formulário.
     * <p>
     * INVARIANTES DO DOMÍNIO: devolve a pasta pai escolhida, preservando caracteres
     * Unicode e serializando aberturas concorrentes.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: devolve {@code caminho=""} quando Paulo
     * cancela o diálogo, ocorre timeout ou o helper falha após a retentativa.
     */
    @GetMapping("/selecionar-pasta")
    public ResponseEntity<Map<String, String>> selecionarPasta(
            @RequestParam(required = false, defaultValue = "") String dirInicial) {
        return resposta(executarComando("PASTA", "", resolverDirInicial(dirInicial)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: permite escolher um arquivo local em formulários que
     * operam sobre uma legenda ou outro arquivo individual.
     * <p>
     * INVARIANTES DO DOMÍNIO: aplica um filtro WinForms válido e preserva o caminho
     * selecionado integralmente em UTF-8.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: devolve {@code caminho=""} no cancelamento,
     * timeout ou falha definitiva do helper.
     */
    @GetMapping("/selecionar-arquivo")
    public ResponseEntity<Map<String, String>> selecionarArquivo(
            @RequestParam(required = false, defaultValue = "Todos os arquivos (*.*)|*.*") String filtro,
            @RequestParam(required = false, defaultValue = "") String dirInicial) {
        return resposta(executarComando("ARQUIVO", normalizarFiltro(filtro), resolverDirInicial(dirInicial)));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: converte o diretório inicial pedido pelo formulário
     * (ex.: a pasta "cache" do projeto no módulo de Correção de Cache) em um
     * caminho absoluto que o seletor nativo possa abrir por padrão.
     * <p>
     * INVARIANTES DO DOMÍNIO: só devolve caminho quando ele existe e é uma pasta;
     * caminhos relativos são resolvidos contra o diretório de trabalho do KRONOS,
     * o mesmo que a aplicação usa para {@code Path.of("cache")}.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: entrada vazia, inexistente ou inválida
     * resulta em {@code ""}, e o diálogo abre no diretório padrão do sistema.
     */
    private String resolverDirInicial(String dirInicial) {
        if (dirInicial == null || dirInicial.isBlank()) {
            return "";
        }
        try {
            Path resolvido = Path.of(dirInicial.trim()).toAbsolutePath().normalize();
            if (Files.isDirectory(resolvido)) {
                return resolvido.toString();
            }
            log.debug("Diretorio inicial ignorado (nao e pasta existente): {}", resolvido);
        } catch (RuntimeException e) {
            log.debug("Diretorio inicial invalido ignorado: {}", dirInicial, e);
        }
        return "";
    }

    /**
     * PROPÓSITO DE NEGÓCIO: traduz o resultado interno do seletor para o contrato
     * JSON único consumido por todos os formulários web.
     * <p>
     * INVARIANTES DO DOMÍNIO: o mapa sempre contém a chave {@code caminho} e nunca
     * recebe valor nulo.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: converte resultado nulo em texto vazio.
     */
    private ResponseEntity<Map<String, String>> resposta(String caminho) {
        return ResponseEntity.ok(Map.of("caminho", caminho == null ? "" : caminho));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: envia uma solicitação ao helper já aquecido e obtém a
     * escolha de Paulo sem pagar novamente o custo de inicialização do PowerShell.
     * <p>
     * INVARIANTES DO DOMÍNIO: apenas uma troca de protocolo acontece por vez e uma
     * resposta pertence exatamente à solicitação que a originou.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: reinicia o helper e repete uma vez; se a
     * segunda tentativa falhar, registra o erro e retorna {@code null}.
     */
    private String executarComando(String tipo, String argumento, String dirInicial) {
        if (!ehWindows()) {
            log.warn("O seletor nativo de caminhos esta disponivel apenas no Windows");
            return null;
        }
        synchronized (travaDialogo) {
            for (int tentativa = 1; tentativa <= 2; tentativa++) {
                try {
                    iniciarHelper();
                    String argumentoCodificado = Base64.getEncoder()
                        .encodeToString(argumento.getBytes(StandardCharsets.UTF_8));
                    String dirInicialCodificado = Base64.getEncoder()
                        .encodeToString((dirInicial == null ? "" : dirInicial).getBytes(StandardCharsets.UTF_8));
                    entradaHelper.write(tipo + "|" + argumentoCodificado + "|" + dirInicialCodificado);
                    entradaHelper.newLine();
                    entradaHelper.flush();
                    String respostaCodificada = lerLinhaComTimeout(saidaHelper, TIMEOUT_DIALOGO);
                    return new String(Base64.getDecoder().decode(respostaCodificada), StandardCharsets.UTF_8).trim();
                } catch (Exception e) {
                    log.warn("Falha na tentativa {} de abrir o seletor nativo", tentativa, e);
                    encerrarHelper();
                }
            }
            return null;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: inicia uma única sessão PowerShell STA que atende todos
     * os botões Procurar durante o ciclo de vida da aplicação.
     * <p>
     * INVARIANTES DO DOMÍNIO: só publica o helper após receber o marcador de pronto;
     * o script viaja em UTF-16LE, formato exigido por {@code -EncodedCommand}.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException} e elimina qualquer
     * processo parcialmente inicializado.
     */
    private void iniciarHelper() throws IOException {
        if (helper != null && helper.isAlive()) {
            return;
        }
        encerrarHelper();
        String scriptCodificado = Base64.getEncoder()
            .encodeToString(SCRIPT_HELPER.getBytes(StandardCharsets.UTF_16LE));
        Process novoHelper = new ProcessBuilder(resolverPowershell(), "-NoLogo", "-NoProfile", "-NonInteractive",
            "-ExecutionPolicy", "Bypass", "-STA", "-EncodedCommand", scriptCodificado).start();
        BufferedWriter novaEntrada = new BufferedWriter(
            new OutputStreamWriter(novoHelper.getOutputStream(), StandardCharsets.UTF_8));
        BufferedReader novaSaida = new BufferedReader(
            new InputStreamReader(novoHelper.getInputStream(), StandardCharsets.UTF_8));
        iniciarLeituraErros(novoHelper);
        try {
            String marcador = lerLinhaComTimeout(novaSaida, TIMEOUT_INICIALIZACAO);
            if (!MARCADOR_PRONTO.equals(marcador)) {
                throw new IOException("Resposta inesperada ao iniciar helper: " + marcador);
            }
            novaEntrada.write("PING|");
            novaEntrada.newLine();
            novaEntrada.flush();
            String pingCodificado = lerLinhaComTimeout(novaSaida, TIMEOUT_INICIALIZACAO);
            String ping = new String(Base64.getDecoder().decode(pingCodificado), StandardCharsets.UTF_8);
            if (!"PONG".equals(ping)) {
                throw new IOException("Helper nao confirmou o protocolo persistente: " + ping);
            }
            helper = novoHelper;
            entradaHelper = novaEntrada;
            saidaHelper = novaSaida;
            log.info("Seletor nativo do Windows pre-aquecido e pronto");
        } catch (Exception e) {
            novoHelper.destroyForcibly();
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Timeout ao iniciar o helper do seletor nativo", e);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: captura diagnósticos do helper sem bloquear o protocolo
     * principal de caminhos selecionados.
     * <p>
     * INVARIANTES DO DOMÍNIO: stderr nunca é misturado ao stdout e o leitor daemon
     * não mantém a JVM viva.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: registra o erro de leitura em nível debug;
     * o fluxo principal detectará independentemente a morte do processo.
     */
    private void iniciarLeituraErros(Process processo) {
        Thread.ofVirtual().name("seletor-nativo-stderr").start(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(processo.getErrorStream(), StandardCharsets.UTF_8))) {
                reader.lines()
                    .filter(linha -> !linha.startsWith("#< CLIXML"))
                    .forEach(linha -> log.warn("Seletor nativo: {}", linha));
            } catch (Exception e) {
                log.debug("Leitura de stderr do seletor nativo encerrada", e);
            }
        });
    }

    /**
     * PROPÓSITO DE NEGÓCIO: limita o tempo de espera por inicialização ou interação
     * com o diálogo para que uma falha gráfica não prenda uma requisição para sempre.
     * <p>
     * INVARIANTES DO DOMÍNIO: uma linha completa representa uma única mensagem do
     * protocolo; EOF é tratado como falha do helper.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: lança {@link IOException} para EOF/erro e
     * {@link TimeoutException} ao exceder o limite configurado.
     */
    private String lerLinhaComTimeout(BufferedReader reader, Duration timeout) throws Exception {
        CompletableFuture<String> leitura = CompletableFuture.supplyAsync(() -> {
            try {
                return reader.readLine();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        String linha = leitura.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (linha == null) {
            throw new IOException("Helper do seletor nativo encerrou o protocolo");
        }
        return linha;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encerra e esquece a sessão gráfica atual para liberar
     * recursos ou permitir recuperação limpa na próxima tentativa.
     * <p>
     * INVARIANTES DO DOMÍNIO: referências Java são anuladas mesmo quando o processo
     * já terminou inesperadamente.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: força o término do processo sem propagar erro.
     */
    private void encerrarHelper() {
        Process processo = helper;
        helper = null;
        entradaHelper = null;
        saidaHelper = null;
        if (processo != null) {
            processo.destroy();
            try {
                if (!processo.waitFor(1, TimeUnit.SECONDS)) {
                    processo.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                processo.destroyForcibly();
            }
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que filtros antigos como {@code *.*} quebrem o
     * contrato exigido pelo OpenFileDialog do Windows.
     * <p>
     * INVARIANTES DO DOMÍNIO: o filtro devolvido sempre possui pares descrição/padrão.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: substitui valor vazio ou inválido pelo filtro
     * seguro de todos os arquivos.
     */
    private String normalizarFiltro(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return "Todos os arquivos (*.*)|*.*";
        }
        String[] partes = filtro.split("\\|", -1);
        if (partes.length % 2 != 0) {
            return "Todos os arquivos (*.*)|*.*";
        }
        for (String parte : partes) {
            if (parte.isBlank()) {
                return "Todos os arquivos (*.*)|*.*";
            }
        }
        return filtro;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica se o ambiente suporta o diálogo WinForms.
     * <p>
     * INVARIANTES DO DOMÍNIO: a decisão depende apenas do sistema operacional da JVM.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: uma propriedade ausente resulta em ambiente
     * não Windows, evitando tentativa de iniciar executável incompatível.
     */
    private boolean ehWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: localiza o Windows PowerShell independentemente do PATH
     * usado para iniciar o KRONOS.
     * <p>
     * INVARIANTES DO DOMÍNIO: prioriza o executável oficial dentro de System32.
     * <p>
     * COMPORTAMENTO EM CASO DE FALHA: retorna o nome simples para permitir resolução
     * pelo PATH; a criação do processo produzirá o diagnóstico definitivo.
     */
    private static String resolverPowershell() {
        String systemRoot = System.getenv("SystemRoot");
        if (systemRoot == null || systemRoot.isBlank()) {
            systemRoot = "C:\\Windows";
        }
        Path candidato = Path.of(systemRoot, "System32", "WindowsPowerShell", "v1.0", "powershell.exe");
        return Files.exists(candidato) ? candidato.toString() : "powershell.exe";
    }
}
