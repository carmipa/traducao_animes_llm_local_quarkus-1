package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.util.ProcessoExternoUtil;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * PROPÓSITO DE NEGÓCIO: detecta metadados publicáveis do computador que está
 * gerando o dataset para que benchmarks não misturem hardware de máquinas.
 *
 * <p>INVARIANTES DO DOMÍNIO: CPU, GPUs e RAM vêm da mesma coleta local; valores
 * manuais nunca substituem a detecção; em sistemas híbridos, uma GPU dedicada
 * é priorizada como principal e todas as GPUs são preservadas na lista.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: tenta um fallback seguro da JVM e deixa
 * campos não detectáveis vazios, sem reutilizar configuração de outro host.
 */
@ApplicationScoped
public class AmbienteExecucaoDatasetService {

    private static final Logger log = LoggerFactory.getLogger(AmbienteExecucaoDatasetService.class);
    private static final Duration TIMEOUT_DETECCAO = Duration.ofSeconds(5);

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * PROPÓSITO DE NEGÓCIO: cria a fotografia sanitizada do hardware local para
     * acompanhar o snapshot de telemetria publicado nesta máquina.
     *
     * <p>INVARIANTES DO DOMÍNIO: respeita apenas as chaves de publicar e habilitar
     * detecção; não aceita aliases manuais de componentes físicos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna o fallback da JVM ou {@code null}
     * somente quando a publicação do ambiente estiver desabilitada.
     */
    public AmbienteExecucaoDataset detectar(TelemetriaDatasetProperties.Hardware config) {
        if (config != null && !config.publicarAmbienteExecucao()) {
            return null;
        }

        AmbienteExecucaoDataset detectado = null;
        if (config == null || config.permitirDeteccaoAutomatica()) {
            detectado = detectarWindows();
        }
        if (detectado == null) {
            detectado = detectarFallbackJava();
        }

        return detectado;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: coleta do Windows uma fotografia única de fabricante,
     * modelo, CPU, RAM e controladores gráficos disponíveis.
     *
     * <p>INVARIANTES DO DOMÍNIO: exclui adaptadores básicos da Microsoft, sanitiza
     * textos e seleciona a GPU principal somente entre itens da coleta atual.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna {@code null} para permitir o
     * fallback Java, sem produzir um ambiente parcialmente misturado.
     */
    private AmbienteExecucaoDataset detectarWindows() {
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return null;
        }
        String script = """
            $cs = Get-CimInstance Win32_ComputerSystem
            $cpu = Get-CimInstance Win32_Processor | Select-Object -First 1
            $gpus = @(Get-CimInstance Win32_VideoController |
                Where-Object { $_.Name -and $_.Name -notmatch 'Microsoft|Basic Display' } |
                Select-Object -ExpandProperty Name)
            [pscustomobject]@{
                fabricante = $cs.Manufacturer
                modeloMaquina = $cs.Model
                ramBytes = [int64]$cs.TotalPhysicalMemory
                cpu = $cpu.Name
                gpus = $gpus
            } | ConvertTo-Json -Compress -Depth 4
            """;
        try {
            var resultado = ProcessoExternoUtil.executar(List.of(
                "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", script
            ), TIMEOUT_DETECCAO, true);
            if (resultado.codigoSaida() != 0) {
                log.debug("Deteccao de hardware via PowerShell falhou: {}", saida(resultado));
                return null;
            }
            JsonNode json = mapper.readTree(saida(resultado));
            List<String> gpus = nomesGpu(json.path("gpus"));
            return new AmbienteExecucaoDataset(
                textoSeguro(json.path("fabricante").asText(null)),
                textoSeguro(json.path("modeloMaquina").asText(null)),
                textoSeguro(json.path("cpu").asText(null)),
                selecionarGpuPrincipal(gpus),
                gpus,
                arredondarGb(json.path("ramBytes").asLong(0L)),
                textoSeguro(System.getProperty("os.name")),
                textoSeguro(System.getProperty("os.arch")),
                true
            );
        } catch (Exception e) {
            log.debug("Nao foi possivel detectar hardware via PowerShell.", e);
            return null;
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: fornece o mínimo de contexto de hardware quando a
     * coleta nativa do sistema operacional não estiver disponível.
     *
     * <p>INVARIANTES DO DOMÍNIO: usa somente propriedades da JVM e memória física
     * reportada pelo MXBean; nunca injeta GPU configurada manualmente.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mantém a melhor memória disponível e
     * deixa fabricante, modelo e GPUs vazios.
     */
    private AmbienteExecucaoDataset detectarFallbackJava() {
        long memoriaBytes = Runtime.getRuntime().maxMemory();
        try {
            memoriaBytes = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean())
                .getTotalMemorySize();
        } catch (RuntimeException ignored) {
            // Mantem o fallback do Runtime quando o MXBean nao estiver disponivel.
        }
        return new AmbienteExecucaoDataset(
            null,
            null,
            textoSeguro(System.getenv("PROCESSOR_IDENTIFIER")),
            null,
            List.of(),
            arredondarGb(memoriaBytes),
            textoSeguro(System.getProperty("os.name")),
            textoSeguro(System.getProperty("os.arch")),
            false
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: normaliza a lista de GPUs retornada pelo PowerShell
     * para publicação segura e seleção da placa principal.
     *
     * <p>INVARIANTES DO DOMÍNIO: descarta nomes vazios ou inseguros, preserva a
     * ordem do sistema e não cria identificadores ausentes.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: retorna lista vazia para nó ausente.
     */
    static List<String> nomesGpu(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        List<String> nomes = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                String valor = textoSeguro(item.asText(null));
                if (valor != null) {
                    nomes.add(valor);
                }
            }
        } else {
            String valor = textoSeguro(node.asText(null));
            if (valor != null) {
                nomes.add(valor);
            }
        }
        return List.copyOf(nomes);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: escolhe automaticamente a GPU mais adequada para
     * contextualizar inferência local quando o computador possui vídeo híbrido.
     *
     * <p>INVARIANTES DO DOMÍNIO: NVIDIA/GeForce/RTX e Radeon RX têm precedência
     * sobre GPUs integradas Intel/AMD; o valor escolhido pertence sempre à lista.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista vazia retorna {@code null}; nomes
     * desconhecidos mantêm a primeira GPU detectada.
     */
    static String selecionarGpuPrincipal(List<String> gpus) {
        if (gpus == null || gpus.isEmpty()) {
            return null;
        }
        return gpus.stream().max(java.util.Comparator.comparingInt(AmbienteExecucaoDatasetService::prioridadeGpu))
            .orElse(gpus.getFirst());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: atribui uma prioridade técnica ao nome sanitizado da
     * GPU para distinguir placas dedicadas de vídeo integrado.
     *
     * <p>INVARIANTES DO DOMÍNIO: a pontuação depende somente do nome detectado e
     * mantém dedicadas NVIDIA/Radeon RX acima de integradas Intel/AMD.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nome desconhecido recebe prioridade
     * neutra e continua elegível, sem ser descartado.
     */
    private static int prioridadeGpu(String gpu) {
        String nome = gpu.toLowerCase(Locale.ROOT);
        if (nome.contains("nvidia") || nome.contains("geforce") || nome.contains("rtx")) return 400;
        if (nome.contains("radeon rx")) return 350;
        if (nome.contains("intel arc")) return 300;
        if (nome.contains("intel") || nome.contains("uhd") || nome.contains("iris")) return 100;
        if (nome.contains("integrated") || nome.contains("radeon(tm) graphics")) return 120;
        return 200;
    }

    private static Integer arredondarGb(long bytes) {
        if (bytes <= 0L) {
            return null;
        }
        return Math.toIntExact(Math.round(bytes / 1024.0 / 1024.0 / 1024.0));
    }

    private static String textoSeguro(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        if (limpo.isBlank() || limpo.length() > 120) {
            return null;
        }
        return limpo;
    }

    private static String saida(ProcessoExternoUtil.Resultado resultado) {
        return new String(resultado.stdout(), StandardCharsets.UTF_8);
    }
}
