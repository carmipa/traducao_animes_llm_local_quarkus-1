package org.traducao.projeto.mcp;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.traducao.projeto.analisadorMidia.application.AnalisarMidiaUseCase;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.ResultadoAnaliseLote;
import org.traducao.projeto.core.execucao.FilaExecucaoPipeline;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Ferramentas MCP (Model Context Protocol) expostas pelo KRONOS via transporte
 * SSE em {@code /mcp/sse}. Clientes MCP (ex.: Claude Code) acionam o pipeline
 * enquanto o servidor web ja esta rodando em modo dev.
 * <p>
 * Toda operação pesada passa pela mesma {@link FilaExecucaoPipeline} da UI: o
 * MCP não é uma porta paralela. Isso garante execução sequencial (MCP e UI não
 * disputam GPU/estado global), torna o job visível a {@code ocupada()} e o deixa
 * cancelável pelo "Parar".
 */
@Singleton
public class KronosMcpTools {

    private final AnalisarMidiaUseCase analisarMidiaUseCase;
    private final FilaExecucaoPipeline filaExecucao;

    @Inject
    public KronosMcpTools(AnalisarMidiaUseCase analisarMidiaUseCase, FilaExecucaoPipeline filaExecucao) {
        this.analisarMidiaUseCase = analisarMidiaUseCase;
        this.filaExecucao = filaExecucao;
    }

    @Tool(name = "ping", description = "Verifica se o servidor MCP do KRONOS esta online e responde.")
    public String ping() {
        return "KRONOS CORE MCP online. Pipeline de traducao de animes (Quarkus) pronto.";
    }

    @Tool(name = "analisar_midia",
          description = "Executa a auditoria tecnica (ffprobe) de um arquivo de video ou de uma pasta com videos: "
                      + "container, faixas de video/audio e classificacao de traduzibilidade das legendas "
                      + "(texto vs bitmap). Retorna um resumo estruturado; nao grava relatorio em disco "
                      + "(apenas a telemetria tecnica e persistida internamente).")
    public String analisarMidia(
            @ToolArg(name = "caminho", description = "Caminho de um arquivo de video (.mkv/.mp4/...) ou de uma pasta contendo videos.")
            String caminho) {

        if (caminho == null || caminho.isBlank()) {
            return "ERRO: informe o caminho de um arquivo ou pasta.";
        }

        // Path.of pode lançar InvalidPathException em caminhos sintaticamente
        // invalidos (ex.: caracteres reservados do SO); tratado como erro amigavel.
        Path entrada;
        try {
            entrada = Path.of(caminho.trim());
        } catch (InvalidPathException e) {
            return "ERRO: caminho invalido: " + caminho.trim();
        }

        if (!Files.exists(entrada)) {
            // Ecoa o caminho como informado, sem toAbsolutePath(), para nao
            // expor o diretorio de trabalho privado do servidor.
            return "ERRO: caminho nao encontrado: " + entrada;
        }

        // Recusa em vez de enfileirar atrás de um job possivelmente longo: o MCP
        // é síncrono e ficaria pendurado até o outro job terminar. Resposta
        // estruturada de ocupação (equivalente MCP ao HTTP 423 Locked).
        if (filaExecucao.ocupada()) {
            return "OCUPADO: o pipeline do KRONOS ja esta executando outro job. "
                 + "Aguarde a conclusao (ou pare pela interface web) e tente novamente.";
        }

        try {
            ResultadoAnaliseLote lote = filaExecucao.executarEAguardar(
                () -> analisarMidiaUseCase.executar(entrada, null));
            return montarResumo(lote);
        } catch (Exception e) {
            return "ERRO ao analisar '" + entrada + "': " + e.getMessage();
        }
    }

    private String montarResumo(ResultadoAnaliseLote lote) {
        StringBuilder sb = new StringBuilder();
        sb.append("Auditoria concluida: ").append(lote.resultados().size()).append(" arquivo(s)");
        if (lote.falhas() != null && !lote.falhas().isEmpty()) {
            sb.append(" (").append(lote.falhas().size()).append(" com falha)");
        }
        sb.append(".\n\n");

        for (AuditoriaResultado r : lote.resultados()) {
            sb.append("- ").append(r.nomeArquivo()).append('\n');
            sb.append("    container: ").append(r.container().formato())
              .append(" | duracao: ").append(formatarSegundos(r.container().duracaoSegundos()))
              .append(" | video: ").append(r.videos().size())
              .append(" | audio: ").append(r.audios().size())
              .append(" | legendas: ").append(r.legendas().size())
              .append('\n');
            for (LegendaInfo leg : r.legendas()) {
                sb.append("    legenda [").append(leg.indexRelativo() + 1).append("] ")
                  .append(leg.idioma()).append(" / ").append(leg.tipoCurto())
                  .append(" (").append(leg.categoria()).append(", ")
                  .append(leg.traduzivel() ? "traduzivel" : (leg.exigeOcr() ? "OCR" : "?"))
                  .append(")");
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String formatarSegundos(Double seconds) {
        if (seconds == null || seconds <= 0.0) {
            return "N/A";
        }
        long h = (long) (seconds / 3600.0);
        long m = (long) ((seconds % 3600.0) / 60.0);
        long s = (long) (seconds % 60.0);
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
