package org.traducao.projeto.legendasExtracao.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.legendasExtracao.domain.ExtratorException;
import org.traducao.projeto.legendasExtracao.domain.FaixaLegenda;
import org.traducao.projeto.legendasExtracao.domain.exceptions.ExtracaoTimeoutException;
import org.traducao.projeto.legendasExtracao.domain.ports.ExtratorVideoPort;
import org.traducao.projeto.legendasExtracao.infrastructure.config.ExtratorProperties;
import org.traducao.projeto.core.util.ProcessoExternoUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@Component
public class MkvToolNixAdapter implements ExtratorVideoPort {
    private static final Logger log = LoggerFactory.getLogger(MkvToolNixAdapter.class);
    private static final Duration TIMEOUT_IDENTIFICACAO = Duration.ofSeconds(60);
    private static final Duration TIMEOUT_EXTRACAO = Duration.ofMinutes(5);
    private static final Set<String> EXTENSOES_SUPORTADAS = Set.of(".mkv", ".webm");

    private final String mkvmergePath;
    private final String mkvextractPath;
    private final ObjectMapper objectMapper;

    public MkvToolNixAdapter(ExtratorProperties properties, ObjectMapper objectMapper) {
        this.mkvmergePath = localizarBinario(properties.resolverMkvmergePath(), "mkvmerge.exe");
        this.mkvextractPath = localizarBinario(properties.resolverMkvextractPath(), "mkvextract.exe");
        this.objectMapper = objectMapper;
    }

    private String localizarBinario(String caminhoConfigurado, String nomeExecutavel) {
        if (!caminhoConfigurado.equals(nomeExecutavel.replace(".exe", "")) && Files.exists(Path.of(caminhoConfigurado))) {
            return caminhoConfigurado;
        }

        List<String> caminhosPadrao = List.of(
            "C:\\Program Files\\MKVToolNix\\" + nomeExecutavel,
            "C:\\Program Files (x86)\\MKVToolNix\\" + nomeExecutavel
        );

        for (String caminho : caminhosPadrao) {
            if (Files.exists(Path.of(caminho))) {
                log.info("{} detectado no caminho padrão: {}", nomeExecutavel, caminho);
                return caminho;
            }
        }
        
        log.info("{} assumido via PATH.", nomeExecutavel);
        return nomeExecutavel.replace(".exe", "");
    }

    @Override
    public boolean suporta(Path arquivoVideo) {
        String nome = arquivoVideo.toString().toLowerCase();
        return EXTENSOES_SUPORTADAS.stream().anyMatch(nome::endsWith);
    }

    @Override
    public void validarInfraestrutura() {
        try {
            Process p1 = new ProcessBuilder(mkvmergePath, "--version").start();
            if (p1.waitFor() != 0) throw new ExtratorException("mkvmerge falhou na validação.");
            
            Process p2 = new ProcessBuilder(mkvextractPath, "--version").start();
            if (p2.waitFor() != 0) throw new ExtratorException("mkvextract falhou na validação.");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ExtratorException("Não foi possível localizar as ferramentas do MKVToolNix no sistema.", e);
        }
    }

    @Override
    public List<FaixaLegenda> identificarFaixas(Path mkvPath) {
        String jsonOutput = executarIdentificacao(mkvPath);
        List<FaixaLegenda> faixas = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonOutput);
            JsonNode tracksNode = root.path("tracks");
            if (tracksNode.isArray()) {
                for (JsonNode track : tracksNode) {
                    if ("subtitles".equals(track.path("type").asText())) {
                        int id = track.path("id").asInt();
                        String codec = track.path("codec").asText("");
                        JsonNode props = track.path("properties");
                        String codecId = props.path("codec_id").asText("");
                        String idioma = props.path("language").asText("und");
                        String nome = props.path("track_name").asText("Sem Titulo");
                        boolean isDefault = props.path("default_track").asBoolean(false);
                        boolean isForced = props.path("forced_track").asBoolean(false);

                        faixas.add(new FaixaLegenda(id, "subtitles", codec, codecId, idioma, nome, isDefault, isForced));
                    }
                }
            }
        } catch (IOException e) {
            throw new ExtratorException("Falha ao interpretar o JSON do mkvmerge para: " + mkvPath, e);
        }

        return faixas;
    }

    /**
     * Executa {@code mkvmerge --identify} e devolve o JSON. Isolado num método
     * {@code protected} para os testes substituírem o processo externo.
     */
    protected String executarIdentificacao(Path mkvPath) {
        List<String> cmd = List.of(
            mkvmergePath,
            "--identification-format", "json",
            "--identify",
            mkvPath.toString()
        );

        try {
            ProcessoExternoUtil.Resultado resultado = ProcessoExternoUtil.executar(cmd, TIMEOUT_IDENTIFICACAO, true);
            if (resultado.codigoSaida() != 0) {
                throw new ExtratorException("mkvmerge --identify falhou com exitCode " + resultado.codigoSaida());
            }
            return new String(resultado.stdout());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ExtratorException("Falha ao invocar mkvmerge para identificar: " + mkvPath, e);
        } catch (TimeoutException e) {
            throw new ExtracaoTimeoutException("Tempo limite excedido ao identificar faixas com mkvmerge: " + mkvPath, e);
        }
    }

    @Override
    public void extrairTrilha(Path mkvPath, int trackId, Path caminhoSaida) {
        List<String> cmd = List.of(
            mkvextractPath,
            "tracks",
            mkvPath.toString(),
            trackId + ":" + caminhoSaida.toString()
        );

        try {
            ProcessoExternoUtil.Resultado resultado = ProcessoExternoUtil.executar(cmd, TIMEOUT_EXTRACAO, true);
            String output = new String(resultado.stdout());

            if (resultado.codigoSaida() != 0) {
                throw new ExtratorException("mkvextract falhou: " + output);
            }

            if (!Files.exists(caminhoSaida)) {
                throw new ExtratorException("Extração finalizou mas arquivo não foi encontrado em: " + caminhoSaida);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ExtratorException("Falha ao invocar mkvextract para o arquivo: " + mkvPath, e);
        } catch (TimeoutException e) {
            throw new ExtracaoTimeoutException("Tempo limite excedido ao extrair trilha com mkvextract: " + mkvPath, e);
        }
    }
}
