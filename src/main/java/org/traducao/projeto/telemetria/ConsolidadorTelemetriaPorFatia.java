package org.traducao.projeto.telemetria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.traducao.projeto.core.io.DiretorioBaseKronos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * PROPÓSITO DE NEGÓCIO: reúne num arquivo POR FATIA toda a telemetria de
 * operação que hoje vive espalhada, sanitizando o caminho no meio do percurso —
 * é o que torna cada aba do painel e cada dataset publicável possíveis.
 *
 * <h2>O prejuízo que originou</h2>
 * Medido no acervo em 06/08/2026:
 * <pre>
 *   6.601  operações registradas no total
 *   6.488  presas em 21 pastas de relatórios/, que o publicador NUNCA varreu
 *     113  no logs/telemetria_compartilhada.json
 *      85  efetivamente publicadas no repositório do dataset
 * </pre>
 * Ou seja: <b>1,3% do trabalho medido chegou à comunidade</b>. E ninguém
 * percebeu, porque o commit do dataset diz "85 operações" e 85 parece plausível.
 *
 * <p>A telemetria é gravada ao lado do RESULTADO de cada operação, na pasta de
 * relatório daquela obra — o que é ótimo para conferir na hora e péssimo para
 * publicar. Este consolidador é a ponte entre as duas necessidades: a cópia local
 * continua onde está, e a visão publicável passa a existir.
 *
 * <h2>INVARIANTES DO DOMÍNIO</h2>
 * <ul>
 *   <li><b>Não destrói a origem.</b> Lê de {@code relatorios/} e escreve em
 *       {@code logs/}; nenhum arquivo de origem é tocado. Consolidação que apaga
 *       a fonte transforma erro de agrupamento em perda de dado.</li>
 *   <li>Todo {@code detalhe} passa pelo {@link SanitizadorTelemetria}. Sem isso,
 *       consolidar seria juntar num só lugar os 4.532 caminhos absolutos e as
 *       2.843 pastas de usuário que os arquivos carregam.</li>
 *   <li><b>Idempotente por chave de ocorrência.</b> Rodar duas vezes não duplica:
 *       a chave é tipo + instante + detalhe já sanitizado. Sem isso, cada
 *       execução inflaria o dataset com cópias e ninguém saberia o número real.</li>
 *   <li>Tipo sem fatia mapeada vai para {@code outros} e CONTINUA no consolidado.
 *       Registro descartado por não ter classificação é dado perdido em silêncio.</li>
 * </ul>
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: arquivo ilegível é pulado com aviso e a
 * consolidação segue — um JSON corrompido numa pasta de obra não pode impedir a
 * publicação das outras vinte. O relatório devolvido diz quantos foram pulados.
 */
@ApplicationScoped
public class ConsolidadorTelemetriaPorFatia {

    private static final Logger log = LoggerFactory.getLogger(ConsolidadorTelemetriaPorFatia.class);

    private static final String ARQUIVO_ORIGEM = "telemetria_compartilhada.json";
    private static final String PREFIXO_DESTINO = "telemetria_fatia_";
    private static final String PASTA_RELATORIOS = "relatorios";
    private static final String PASTA_LOGS = "logs";

    /** Profundidade máxima de varredura em relatorios/. Pastas de obra são rasas. */
    private static final int PROFUNDIDADE = 4;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Resultado da consolidação, para quem chamou poder provar o que aconteceu em
     * vez de confiar.
     *
     * @param arquivosLidos quantos {@code telemetria_compartilhada.json} foram abertos
     * @param arquivosPulados quantos estavam ilegíveis
     * @param operacoesLidas total bruto encontrado, antes de deduplicar
     * @param operacoesGravadas total após deduplicação
     * @param porFatia quantas operações ficaram em cada fatia
     */
    public record Resultado(int arquivosLidos, int arquivosPulados, int operacoesLidas,
                            int operacoesDeTeste, int operacoesGravadas,
                            Map<String, Integer> porFatia) {}

    /**
     * Marca de operação que veio de execução de TESTE, não de trabalho real.
     *
     * <p>Medido em 07/08/2026: <b>285 das 775 operações distintas (36,8%)</b> do
     * acervo têm detalhe apontando para um diretório temporário de JUnit —
     * {@code C:\Users\...\AppData\Local\Temp\junit-<numero>\ep02_pt.ass}. São
     * resíduo de execuções de teste anteriores ao {@code DiretorioBaseKronos},
     * que existe justamente para impedir que a suíte contamine os diretórios
     * operacionais reais.
     *
     * <p>O sanitizador já as redige, então não vazam caminho. Mas publicar um
     * dataset onde 36,8% das linhas são {@code [redigido]} vindo de teste é
     * publicar ruído como se fosse medição — e quem consome não tem como saber.
     */
    private static final java.util.regex.Pattern RESIDUO_DE_TESTE =
        java.util.regex.Pattern.compile("junit-?\\d{6,}|[/\\\\]Temp[/\\\\]junit", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * PROPÓSITO DE NEGÓCIO: executa a consolidação e devolve o que foi feito.
     *
     * <p>INVARIANTES DO DOMÍNIO: a contagem devolvida é o artefato de prova —
     * {@code operacoesLidas} versus {@code operacoesGravadas} mostra quanto era
     * duplicata, e a soma de {@code porFatia} tem de bater com o gravado.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: nunca lança por causa de um arquivo; só
     * propaga falha de escrita do destino, que é erro de ambiente e não de dado.
     */
    public Resultado consolidar() throws IOException {
        Map<String, Map<String, ObjectNode>> porFatia = new TreeMap<>();
        int lidos = 0;
        int pulados = 0;
        int brutas = 0;
        int deTeste = 0;

        for (Path origem : localizarOrigens()) {
            JsonNode raiz;
            try {
                raiz = mapper.readTree(origem.toFile());
            } catch (IOException | RuntimeException e) {
                pulados++;
                log.warn("Telemetria ilegivel, pulada: {} ({})", origem, e.getMessage());
                continue;
            }
            lidos++;
            JsonNode operacoes = raiz == null ? null : raiz.get("operacoes");
            if (operacoes == null || !operacoes.isArray()) {
                continue;
            }
            for (JsonNode op : operacoes) {
                brutas++;
                if (RESIDUO_DE_TESTE.matcher(texto(op, "detalhe")).find()) {
                    deTeste++;
                    continue;
                }
                acrescentar(porFatia, op);
            }
        }

        Map<String, Integer> contagem = new TreeMap<>();
        int gravadas = 0;
        for (Map.Entry<String, Map<String, ObjectNode>> e : porFatia.entrySet()) {
            gravar(e.getKey(), e.getValue().values());
            contagem.put(e.getKey(), e.getValue().size());
            gravadas += e.getValue().size();
        }

        log.info("Telemetria consolidada por fatia: {} arquivo(s) lido(s), {} pulado(s), "
            + "{} operacao(oes) bruta(s), {} residuo(s) de teste descartado(s) -> {} gravada(s) "
            + "em {} fatia(s).",
            lidos, pulados, brutas, deTeste, gravadas, contagem.size());

        return new Resultado(lidos, pulados, brutas, deTeste, gravadas, contagem);
    }

    /**
     * Acrescenta a operação já sanitizada ao balde da fatia, com chave de
     * ocorrência para que rodar de novo não duplique.
     */
    private void acrescentar(Map<String, Map<String, ObjectNode>> porFatia, JsonNode op) {
        String tipo = texto(op, "tipo");
        String detalhe = SanitizadorTelemetria.sanitizar(texto(op, "detalhe"));
        String instante = texto(op, "registradoEm");

        ObjectNode limpa = mapper.createObjectNode();
        limpa.put("tipo", tipo);
        limpa.put("detalhe", detalhe);
        limpa.put("tempoTotalMs", numero(op, "tempoTotalMs"));
        limpa.put("arquivosProcessados", numero(op, "arquivosProcessados"));
        limpa.put("itensDetectados", numero(op, "itensDetectados"));
        limpa.put("itensCorrigidos", numero(op, "itensCorrigidos"));
        limpa.put("registradoEm", instante);

        String fatia = FatiaTelemetria.de(tipo);
        String chave = tipo + " " + instante + " " + detalhe;
        porFatia.computeIfAbsent(fatia, f -> new LinkedHashMap<>()).putIfAbsent(chave, limpa);
    }

    private void gravar(String fatia, Iterable<ObjectNode> operacoes) throws IOException {
        ObjectNode raiz = mapper.createObjectNode();
        raiz.put("fatia", fatia);
        raiz.put("schemaVersion", "1.0");
        ArrayNode array = raiz.putArray("operacoes");
        operacoes.forEach(array::add);

        Path destino = DiretorioBaseKronos.resolver(PASTA_LOGS).resolve(PREFIXO_DESTINO + fatia + ".json");
        Files.createDirectories(destino.getParent());
        mapper.writeValue(destino.toFile(), raiz);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: encontra todos os arquivos de telemetria espalhados.
     *
     * <p>INVARIANTES DO DOMÍNIO: inclui o de {@code logs/} junto com os de
     * {@code relatorios/} — os 113 registros de lá também precisam entrar, e
     * esquecê-los seria repetir em menor escala o defeito que motivou tudo isto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: pasta ausente devolve lista vazia sem
     * lançar; acervo novo ainda não tem relatório nenhum.
     */
    private List<Path> localizarOrigens() {
        List<Path> encontrados = new ArrayList<>();

        Path emLogs = DiretorioBaseKronos.resolver(PASTA_LOGS).resolve(ARQUIVO_ORIGEM);
        if (Files.isRegularFile(emLogs)) {
            encontrados.add(emLogs);
        }

        Path relatorios = DiretorioBaseKronos.resolver(PASTA_RELATORIOS);
        if (!Files.isDirectory(relatorios)) {
            return encontrados;
        }
        try (Stream<Path> caminhada = Files.walk(relatorios, PROFUNDIDADE)) {
            caminhada.filter(Files::isRegularFile)
                .filter(p -> ARQUIVO_ORIGEM.equals(p.getFileName().toString()))
                .forEach(encontrados::add);
        } catch (IOException e) {
            log.warn("Falha ao varrer {}: {}", relatorios, e.getMessage());
        }
        return encontrados;
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode v = no == null ? null : no.get(campo);
        return v == null || v.isNull() ? "" : v.asText();
    }

    private static long numero(JsonNode no, String campo) {
        JsonNode v = no == null ? null : no.get(campo);
        return v == null || v.isNull() ? 0L : v.asLong();
    }
}
