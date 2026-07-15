package org.traducao.projeto.analisadorMidia.infrastructure.adapters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.traducao.projeto.analisadorMidia.domain.*;
import org.traducao.projeto.core.util.ProcessoExternoUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Component
public class FfprobeAdapter {

    private static final Logger log = LoggerFactory.getLogger(FfprobeAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executa ffprobe no vídeo e obtém o JSON com as informações gerais e faixas.
     */
    public AuditoriaResultado analisarMidia(Path caminhoVideo) {
        String jsonString = executarFfprobeJson(caminhoVideo);
        try {
            JsonNode root = objectMapper.readTree(jsonString);

            // Parsing do Container. Duração de fallback vinda dos streams: o
            // ffprobe às vezes reporta a duração nos streams mas não no format.
            JsonNode formatNode = root.get("format");
            double fallbackDuracao = maiorDuracaoDeStream(root.get("streams"));
            ContainerInfo container = parseContainer(formatNode, fallbackDuracao);

            // Parsing das faixas (streams)
            List<VideoInfo> videos = new ArrayList<>();
            List<AudioInfo> audios = new ArrayList<>();
            List<LegendaInfo> legendas = new ArrayList<>();
            List<AnexoInfo> anexos = new ArrayList<>();

            JsonNode streamsNode = root.get("streams");
            int legendaIdx = 0; // index relativo para ffprobe select_streams s:<idx>

            if (streamsNode != null && streamsNode.isArray()) {
                for (JsonNode stream : streamsNode) {
                    int index = stream.path("index").asInt(-1);
                    String codecType = stream.path("codec_type").asText("");

                    if ("video".equals(codecType)) {
                        videos.add(parseVideo(stream, index));
                    } else if ("audio".equals(codecType)) {
                        audios.add(parseAudio(stream, index));
                    } else if ("subtitle".equals(codecType)) {
                        legendas.add(parseLegenda(stream, index, legendaIdx++));
                    } else if ("attachment".equals(codecType)) {
                        anexos.add(parseAnexo(stream));
                    }
                }
            }

            List<CapituloInfo> capitulos = parseCapitulos(root.path("chapters"));

            return new AuditoriaResultado(
                caminhoVideo,
                caminhoVideo.getFileName().toString(),
                container,
                videos,
                audios,
                legendas,
                capitulos,
                anexos,
                new ArrayList<>()
            );
        } catch (IOException e) {
            throw new AnalisadorException("Erro ao interpretar o JSON do ffprobe para " + caminhoVideo, e);
        }
    }

    /**
     * Executa o ffprobe e devolve o JSON cru. Isolado num método {@code protected}
     * para os testes substituírem o processo externo (sem ffprobe real).
     */
    protected String executarFfprobeJson(Path caminhoVideo) {
        List<String> cmd = List.of(
            "ffprobe", "-v", "quiet", "-print_format", "json",
            "-show_format", "-show_streams", "-show_chapters",
            caminhoVideo.toAbsolutePath().toString()
        );

        try {
            log.debug("Executando: {}", String.join(" ", cmd));
            ProcessoExternoUtil.Resultado resultado = ProcessoExternoUtil.executar(cmd, TIMEOUT);

            if (resultado.codigoSaida() != 0) {
                String stderr = new String(resultado.stderr(), StandardCharsets.UTF_8);
                throw new AnalisadorException("ffprobe falhou com código " + resultado.codigoSaida() + ". Erro: " + stderr);
            }
            return new String(resultado.stdout(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AnalisadorException("Erro de E/S ao executar o ffprobe em " + caminhoVideo, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalisadorException("Execução do ffprobe foi interrompida para " + caminhoVideo, e);
        } catch (TimeoutException e) {
            throw new AnalisadorException("Tempo limite excedido ao executar ffprobe em " + caminhoVideo, e);
        }
    }

    /**
     * Executa ffprobe para extrair os timestamps de pacotes de uma legenda.
     * Retorna um array com [primeiroPts, ultimoPts].
     */
    public double[] obterTimestampsLegenda(Path caminhoVideo, int indexRelativoLegenda) {
        List<String> cmd = List.of(
            "ffprobe", "-v", "quiet", "-select_streams", "s:" + indexRelativoLegenda,
            "-show_entries", "packet=pts_time,duration_time", "-of", "json",
            caminhoVideo.toAbsolutePath().toString()
        );

        try {
            log.debug("Executando para pacotes de legenda s:{}: {}", indexRelativoLegenda, String.join(" ", cmd));
            ProcessoExternoUtil.Resultado resultado = ProcessoExternoUtil.executar(cmd, TIMEOUT);

            if (resultado.codigoSaida() != 0) {
                String stderr = new String(resultado.stderr(), StandardCharsets.UTF_8);
                log.warn("ffprobe ao ler pacotes de legenda falhou com código {}. Erro: {}", resultado.codigoSaida(), stderr);
                return null;
            }

            String jsonString = new String(resultado.stdout(), StandardCharsets.UTF_8);
            JsonNode root = objectMapper.readTree(jsonString);
            JsonNode packetsNode = root.get("packets");

            if (packetsNode == null || !packetsNode.isArray() || packetsNode.isEmpty()) {
                return null;
            }

            Double primeiroPts = null;
            Double ultimoPts = null;

            // Busca primeiro pacote com pts_time válido
            for (JsonNode packet : packetsNode) {
                String ptsStr = packet.path("pts_time").asText(null);
                if (ptsStr != null) {
                    try {
                        primeiroPts = Double.parseDouble(ptsStr);
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Busca último pacote com pts_time válido de trás para frente
            for (int i = packetsNode.size() - 1; i >= 0; i--) {
                JsonNode packet = packetsNode.get(i);
                String ptsStr = packet.path("pts_time").asText(null);
                if (ptsStr != null) {
                    try {
                        double pts = Double.parseDouble(ptsStr);
                        double duration = 0.0;
                        String durStr = packet.path("duration_time").asText(null);
                        if (durStr != null) {
                            try {
                                duration = Double.parseDouble(durStr);
                            } catch (NumberFormatException ignored) {}
                        }
                        ultimoPts = pts + duration;
                        break;
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (primeiroPts != null && ultimoPts != null) {
                return new double[]{primeiroPts, ultimoPts};
            }

        } catch (Exception e) {
            log.warn("Erro ao obter timestamps de pacotes para a legenda s:{}. Erro: {}", indexRelativoLegenda, e.getMessage());
        }

        return null;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapeia o bloco {@code format} do ffprobe para o
     * domínio, garantindo uma duração utilizável mesmo quando o container não a
     * reporta (usa a maior duração de trilha como fallback estruturado).
     *
     * <p>INVARIANTES DO DOMÍNIO: nunca retorna {@code null}; a duração é a do
     * container quando válida (&gt; 0) e, só então, o fallback das trilhas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code formatNode} nulo devolve um
     * {@link ContainerInfo} com metadados "N/A" e apenas o fallback de duração;
     * valores numéricos ausentes/ inválidos caem nos defaults (0), sem exceção.
     */
    private ContainerInfo parseContainer(JsonNode formatNode, double fallbackDuracaoSegundos) {
        if (formatNode == null) {
            return new ContainerInfo("N/A", 0L, Math.max(fallbackDuracaoSegundos, 0.0), 0L, "N/A");
        }

        String formato = formatNode.path("format_name").asText("N/A");
        long tamanhoBytes = formatNode.path("size").asLong(0L);
        double duracao = formatNode.path("duration").asDouble(0.0);
        if (duracao <= 0.0 && fallbackDuracaoSegundos > 0.0) {
            duracao = fallbackDuracaoSegundos;
        }
        long bitrate = formatNode.path("bit_rate").asLong(0L);
        
        String encoder = "N/A";
        JsonNode tags = formatNode.get("tags");
        if (tags != null) {
            encoder = tags.path("encoder").asText(tags.path("ENCODER").asText("N/A"));
        }

        return new ContainerInfo(formato, tamanhoBytes, duracao, bitrate, encoder);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: obtém uma duração de fallback a partir das trilhas
     * quando o container não a reporta — o ffprobe frequentemente traz a duração
     * apenas no stream de vídeo (campo {@code duration} ou tag {@code DURATION}).
     *
     * <p>INVARIANTES DO DOMÍNIO: usa a MAIOR duração encontrada entre as trilhas
     * (a mais representativa do arquivo); ignora trilhas sem duração válida.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: {@code streamsNode} nulo/ não-array ou
     * durações ausentes/ inválidas resultam em {@code 0.0}, sem exceção.
     */
    private double maiorDuracaoDeStream(JsonNode streamsNode) {
        if (streamsNode == null || !streamsNode.isArray()) {
            return 0.0;
        }
        double maior = 0.0;
        for (JsonNode stream : streamsNode) {
            double duracao = stream.path("duration").asDouble(0.0);
            if (duracao <= 0.0) {
                JsonNode tags = stream.path("tags");
                String durTag = tags.path("DURATION").asText(tags.path("duration").asText(""));
                if (!durTag.isBlank()) {
                    duracao = converterDuracaoTagParaSegundos(durTag);
                }
            }
            if (duracao > maior) {
                maior = duracao;
            }
        }
        return maior;
    }

    private VideoInfo parseVideo(JsonNode stream, int index) {
        String codecId = stream.path("codec_name").asText("N/A").toUpperCase();
        String format = stream.path("codec_long_name").asText("N/A");
        int width = stream.path("width").asInt(0);
        int height = stream.path("height").asInt(0);

        // Identifica profundidade de cor por pix_fmt (ex: yuv420p10le -> 10 bits)
        int bitDepth = 8;
        String pixFmt = stream.path("pix_fmt").asText("");
        if (pixFmt.contains("10le") || pixFmt.contains("10be") || pixFmt.contains("10")) {
            bitDepth = 10;
        } else if (pixFmt.contains("12")) {
            bitDepth = 12;
        }

        // Calcula o FPS
        double fps = 0.0;
        String rFrameRate = stream.path("r_frame_rate").asText("0/0");
        if (rFrameRate.contains("/")) {
            try {
                String[] parts = rFrameRate.split("/");
                double num = Double.parseDouble(parts[0]);
                double den = Double.parseDouble(parts[1]);
                if (den > 0) {
                    fps = num / den;
                }
            } catch (Exception ignored) {}
        }

        String dar = stream.path("display_aspect_ratio").asText("N/A");
        long bitrate = stream.path("bit_rate").asLong(0L);

        return new VideoInfo(index, codecId, format, width, height, bitDepth, fps, dar, bitrate);
    }

    private AudioInfo parseAudio(JsonNode stream, int index) {
        String codec = stream.path("codec_name").asText("N/A").toUpperCase();
        int channels = stream.path("channels").asInt(0);
        double sampleRate = stream.path("sample_rate").asDouble(0.0) / 1000.0;
        long bitrate = stream.path("bit_rate").asLong(0L);

        String idioma = "Desconhecido";
        String titulo = "(Sem titulo)";

        JsonNode tags = stream.get("tags");
        if (tags != null) {
            idioma = tags.path("language").asText(tags.path("LANGUAGE").asText("Desconhecido"));
            titulo = tags.path("title").asText(tags.path("TITLE").asText("(Sem titulo)"));
        }

        return new AudioInfo(index, idioma, codec, channels, sampleRate, bitrate, titulo);
    }

    private LegendaInfo parseLegenda(JsonNode stream, int index, int indexRelativo) {
        String format = stream.path("codec_name").asText("N/A").toUpperCase();
        String codecId = stream.path("codec_name").asText("N/A");

        String idioma = "Desconhecido";
        String titulo = "(Sem titulo)";
        double duracao = stream.path("duration").asDouble(0.0);

        JsonNode tags = stream.get("tags");
        if (tags != null) {
            idioma = tags.path("language").asText(tags.path("LANGUAGE").asText("Desconhecido"));
            titulo = tags.path("title").asText(tags.path("TITLE").asText("(Sem titulo)"));
            
            // Tenta obter duracao das tags de duracao do mkv
            String durationTag = tags.path("DURATION").asText(tags.path("duration").asText(""));
            if (!durationTag.isBlank() && duracao <= 0.0) {
                duracao = converterDuracaoTagParaSegundos(durationTag);
            }
        }

        JsonNode disp = stream.path("disposition");
        boolean isDefault = disp.path("default").asInt(0) == 1;
        boolean isForced = disp.path("forced").asInt(0) == 1;
        boolean acessibilidade = disp.path("hearing_impaired").asInt(0) == 1
            || disp.path("visual_impaired").asInt(0) == 1;

        // Classificação (tipo/categoria/traduzibilidade) é preenchida no use case.
        return new LegendaInfo(
            index, indexRelativo, idioma, format, codecId, titulo,
            null, null, null, false, false, false,
            isDefault, isForced, acessibilidade,
            duracao > 0.0 ? duracao : null, null
        );
    }

    private AnexoInfo parseAnexo(JsonNode stream) {
        JsonNode tags = stream.path("tags");
        String nome = tags.path("filename").asText(tags.path("FILENAME").asText("(sem nome)"));
        String mime = tags.path("mimetype").asText(tags.path("MIMETYPE").asText("N/A"));
        long tamanho = stream.path("extradata_size").asLong(0L);
        return new AnexoInfo(nome, mime, tamanho);
    }

    private List<CapituloInfo> parseCapitulos(JsonNode chaptersNode) {
        List<CapituloInfo> capitulos = new ArrayList<>();
        if (chaptersNode == null || !chaptersNode.isArray()) {
            return capitulos;
        }
        int numero = 1;
        for (JsonNode cap : chaptersNode) {
            double inicio = parseTempo(cap.path("start_time").asText(null));
            double fim = parseTempo(cap.path("end_time").asText(null));
            String titulo = cap.path("tags").path("title").asText("Capitulo " + numero);
            capitulos.add(new CapituloInfo(numero++, titulo, inicio, fim));
        }
        return capitulos;
    }

    private static double parseTempo(String valor) {
        if (valor == null || valor.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double converterDuracaoTagParaSegundos(String durTag) {
        try {
            durTag = durTag.replace(',', '.');
            String[] parts = durTag.split(":");
            if (parts.length == 3) {
                double h = Double.parseDouble(parts[0]);
                double m = Double.parseDouble(parts[1]);
                double s = Double.parseDouble(parts[2]);
                return h * 3600.0 + m * 60.0 + s;
            } else if (parts.length == 2) {
                double m = Double.parseDouble(parts[0]);
                double s = Double.parseDouble(parts[1]);
                return m * 60.0 + s;
            }
        } catch (Exception ignored) {}
        return 0.0;
    }
}
