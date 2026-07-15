package org.traducao.projeto.analisadorMidia.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.AudioInfo;
import org.traducao.projeto.analisadorMidia.domain.AuditoriaResultado;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;
import org.traducao.projeto.analisadorMidia.domain.VideoInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * PROPÓSITO DE NEGÓCIO: produz o relatório textual da auditoria de uma mídia a
 * partir do resultado JÁ CLASSIFICADO (fonte única de verdade). Serve de base
 * para exibição/exportação textual sem reimplementar a classificação — evita
 * duas regras de formatação divergentes.
 *
 * <p>INVARIANTES DO DOMÍNIO: lê apenas o domínio estruturado
 * ({@link AuditoriaResultado}); não reexecuta ffprobe nem reclassifica; a
 * terminologia segue a do domínio (bitmap ≠ hardsub).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: método puro sem I/O; listas vazias produzem
 * as seções vazias correspondentes, sem exceção.
 */
@Service
public class RelatorioMidiaTextoFormatter {

    private static final String LINHA = "=".repeat(80);

    private final ClassificadorLegendaService classificador;

    public RelatorioMidiaTextoFormatter(ClassificadorLegendaService classificador) {
        this.classificador = classificador;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: monta as linhas do relatório técnico textual da mídia.
     * <p>INVARIANTES DO DOMÍNIO: as durações usam o mesmo formato do domínio; os
     * indicadores temporais são informativos (sem veredito de sincronia).
     * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas de duração viram "N/A".
     */
    public List<String> formatar(AuditoriaResultado r) {
        List<String> logs = new ArrayList<>();

        logs.add(LINHA);
        logs.add("INICIANDO AUDITORIA TECNICA: " + r.nomeArquivo());
        logs.add(LINHA);

        long tamanhoBytes = r.container().tamanhoBytes();
        double tamanhoMB = tamanhoBytes / (1024.0 * 1024.0);
        double tamanhoGB = tamanhoBytes / (1024.0 * 1024.0 * 1024.0);

        logs.add("Validação OK");
        logs.add(String.format("Tamanho: %.2f GiB (%.0f MB)", tamanhoGB, tamanhoMB));
        logs.add("Formatos detectados e mapeados via ffprobe com sucesso.\n");

        logs.add(LINHA);
        logs.add("FORMATO DE LEGENDA DETECTADO");
        logs.add(LINHA);
        if (r.legendas().isEmpty()) {
            logs.add("  NENHUMA FAIXA DE LEGENDA ENCONTRADA (RAW; hardsub nao confirmado por esta analise)");
        } else {
            for (LegendaInfo leg : r.legendas()) {
                String tituloResumo = leg.titulo() != null && !leg.titulo().isBlank() ? " - " + leg.titulo() : "";
                logs.add(String.format("  [%d] %s | Idioma: %s%s",
                    leg.indexRelativo() + 1, leg.tipoCompleto(), leg.idioma(), tituloResumo));
            }
        }
        logs.add("");

        logs.add(LINHA);
        logs.add("ESTRUTURA GERAL");
        logs.add(LINHA);
        logs.add("Formato do Conteiner");
        logs.add("  " + r.container().formato());
        logs.add("Tamanho do Arquivo");
        logs.add(String.format("  %.2f GiB", tamanhoGB));
        logs.add("Duracao Total");
        logs.add("  " + formatarSegundos(r.container().duracaoSegundos()));
        logs.add("Bitrate Geral");
        logs.add("  " + (r.container().bitrateGeral() > 0 ? (r.container().bitrateGeral() / 1000) + " kbps" : "N/A"));
        logs.add("Aplicacao de Escrita");
        logs.add("  " + r.container().aplicacaoEscrita());

        logs.add("\n" + LINHA);
        logs.add("FLUXOS DE VIDEO");
        logs.add(LINHA);
        for (VideoInfo v : r.videos()) {
            logs.add(String.format("\n  Fluxo %d (Track ID: %d)", v.index(), v.index()));
            logs.add(String.format("    Codec: %s (%s)", v.codecId(), v.format()));
            logs.add(String.format("    Resolucao: %dx%dp", v.width(), v.height()));
            logs.add(String.format("    Profundidade de Cor: %d bits", v.bitDepth()));
            logs.add(String.format("    Taxa de Quadros (FPS): %.3f fps", v.fps()));
            logs.add(String.format("    Aspect Ratio: %s", v.displayAspectRatio()));
            logs.add(String.format("    Bitrate: %s", v.bitrate() > 0 ? (v.bitrate() / 1000) + " kbps" : "N/A"));
        }

        logs.add("\n" + LINHA);
        logs.add("FLUXOS DE AUDIO");
        logs.add(LINHA);
        for (AudioInfo a : r.audios()) {
            logs.add(String.format("\n  Fluxo %d (Track ID: %d)", a.index(), a.index()));
            logs.add(String.format("    Idioma: %s", a.idioma()));
            logs.add(String.format("    Codec/Formato: %s", a.format()));
            logs.add(String.format("    Canais: %d", a.channels()));
            logs.add(String.format("    Taxa de Amostragem: %.1f kHz", a.sampleRateKHz()));
            logs.add(String.format("    Bitrate: %s", a.bitrate() > 0 ? (a.bitrate() / 1000) + " kbps" : "N/A"));
            logs.add(String.format("    Titulo: %s", a.titulo()));
        }

        logs.add("\n" + LINHA);
        logs.add("FAIXAS DE LEGENDAS");
        logs.add(LINHA);

        if (r.legendas().isEmpty()) {
            logs.add("\n    NENHUMA FAIXA DE LEGENDA ENCONTRADA");
            logs.add("    - Pode ser um arquivo RAW (sem faixa de legenda softsub)");
            logs.add("    - A legenda pode estar embutida como hardsub (NAO confirmado por esta analise)");
        } else {
            for (LegendaInfo leg : r.legendas()) {
                logs.add(String.format("\n  Legenda %d (Track ID: %d)", leg.indexRelativo() + 1, leg.index()));
                logs.add(String.format("    Idioma: %s", leg.idioma()));
                logs.add(String.format("    Formato: %s", leg.formato()));
                logs.add(String.format("    Tipo: %s (%s)", leg.tipoCompleto(), leg.categoria()));
                logs.add(String.format("    Codec ID: %s", leg.codecId()));
                logs.add(String.format("    Titulo: %s", leg.titulo()));
                logs.add(String.format("    Flags: default=%s forced=%s acessibilidade=%s",
                    leg.isDefault(), leg.isForced(), leg.acessibilidade()));
                logs.add(String.format("    Extraivel: %s | Traduzivel: %s | Exige OCR: %s",
                    leg.extraivel(), leg.traduzivel(), leg.exigeOcr()));

                if (leg.duracaoSegundos() != null) {
                    logs.add(String.format("    Duracao Legenda: %s", formatarSegundos(leg.duracaoSegundos())));
                }
                if (leg.diferencaFimSegundos() != null) {
                    logs.add(String.format("    Diferenca p/ o video: %+.3fs (informativo)", leg.diferencaFimSegundos()));
                }
            }
        }

        logs.add("\n" + LINHA);
        logs.add("RESUMO FINAL");
        logs.add(LINHA);
        logs.add("  Total de Faixas: " + (1 + r.videos().size() + r.audios().size() + r.legendas().size()));
        logs.add("    Video(s): " + r.videos().size());
        logs.add("    Audio(s): " + r.audios().size());
        logs.add("    Legenda(s): " + r.legendas().size());

        for (LegendaInfo info : r.legendas()) {
            String tituloStr = info.titulo() != null && !info.titulo().isBlank() ? " - " + info.titulo() : "";
            logs.add(String.format("      [%d] Idioma: %s | Tipo: %s | Formato: %s%s",
                info.index(), info.idioma(), info.tipoCurto(), info.formato(), tituloStr));
        }

        // Dado vital para a tradução: dá para extrair texto e traduzir este arquivo?
        logs.add("  Traduzivel (legenda de texto): " + classificador.verdictTraducao(r.legendas()));

        logs.add("\n" + LINHA);
        logs.add("Auditoria finalizada com sucesso!");
        logs.add(LINHA + "\n");

        return logs;
    }

    private String formatarSegundos(Double seconds) {
        if (seconds == null || seconds <= 0.0) {
            return "N/A";
        }
        long h = (long) (seconds / 3600.0);
        long m = (long) ((seconds % 3600.0) / 60.0);
        double s = seconds % 60.0;
        return String.format("%02d:%02d:%06.3f", h, m, s);
    }
}
