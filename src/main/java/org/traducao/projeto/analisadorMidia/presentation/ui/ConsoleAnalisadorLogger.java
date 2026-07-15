package org.traducao.projeto.analisadorMidia.presentation.ui;

import org.springframework.stereotype.Component;
import org.traducao.projeto.analisadorMidia.domain.*;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.nio.file.Path;

@Component
public class ConsoleAnalisadorLogger {

    public void cabecalho(String titulo) {
        String divisor = AnsiCores.colorir("=".repeat(80), AnsiCores.BLUE);
        System.out.println(divisor);
        System.out.println(AnsiCores.colorir("  " + titulo, AnsiCores.YELLOW, true));
        System.out.println(divisor);
        System.out.flush();
    }

    public void cabecalhoGrande(String titulo) {
        String divisor = AnsiCores.colorir("=".repeat(80), AnsiCores.MAGENTA);
        System.out.println("\n" + divisor);
        System.out.println(AnsiCores.colorir("  >>> " + titulo.toUpperCase() + " <<<", AnsiCores.YELLOW, true));
        System.out.println(divisor);
        System.out.flush();
    }

    public void info(String msg) {
        System.out.println("  [INFO] " + msg);
        System.out.flush();
    }

    public void sucesso(String msg) {
        System.out.println(AnsiCores.colorir("  [OK] ", AnsiCores.GREEN, true) + AnsiCores.colorir(msg, AnsiCores.GREEN));
        System.out.flush();
    }

    public void alerta(String msg) {
        System.out.println(AnsiCores.colorir("  [AVISO] ", AnsiCores.YELLOW, true) + AnsiCores.colorir(msg, AnsiCores.YELLOW));
        System.out.flush();
    }

    public void erro(String msg) {
        System.out.println(AnsiCores.colorir("  [ERRO CRÍTICO] ", AnsiCores.RED, true) + AnsiCores.colorir(msg, AnsiCores.RED));
        System.out.flush();
    }

    public void imprimirResultado(AuditoriaResultado res) {
        System.out.println("\n" + AnsiCores.colorir("=".repeat(80), AnsiCores.BLUE));
        System.out.println(AnsiCores.colorir("AUDITORIA TÉCNICA: " + res.nomeArquivo(), AnsiCores.CYAN, true));
        System.out.println(AnsiCores.colorir("=".repeat(80), AnsiCores.BLUE));

        // 1. Container/Geral
        System.out.println("\n" + AnsiCores.colorir("ESTRUTURA GERAL", AnsiCores.MAGENTA, true));
        System.out.println("- Formato do Container: " + res.container().formato());

        double tamanhoGB = res.container().tamanhoBytes() / (1024.0 * 1024.0 * 1024.0);
        double tamanhoMB = res.container().tamanhoBytes() / (1024.0 * 1024.0);
        System.out.printf("- Tamanho: %s%n", String.format("%.2f GiB (%.0f MB)", tamanhoGB, tamanhoMB));
        System.out.println("- Duração Total: " + formatarSegundos(res.container().duracaoSegundos()));

        long brGeral = res.container().bitrateGeral();
        System.out.println("- Bitrate Geral: " + (brGeral > 0 ? (brGeral / 1000) + " kbps" : "N/A"));
        System.out.println("- Aplicação de Escrita: " + res.container().aplicacaoEscrita());

        // 2. Fluxos de Vídeo
        System.out.println("\n" + AnsiCores.colorir("FLUXOS DE VÍDEO", AnsiCores.MAGENTA, true));
        for (VideoInfo v : res.videos()) {
            System.out.printf("  Fluxo %d (Track ID: %d)%n", v.index(), v.index());
            System.out.println("    Codec: " + v.codecId() + " (" + v.format() + ")");
            System.out.println("    Resolução: " + v.width() + "x" + v.height() + "p");
            System.out.println("    Profundidade de Cor: " + v.bitDepth() + " bits");
            System.out.printf("    FPS: %s%n", String.format("%.3f fps", v.fps()));
            System.out.println("    Aspect Ratio: " + v.displayAspectRatio());
            System.out.println("    Bitrate: " + (v.bitrate() > 0 ? (v.bitrate() / 1000) + " kbps" : "N/A"));
        }

        // 3. Fluxos de Áudio
        System.out.println("\n" + AnsiCores.colorir("FLUXOS DE ÁUDIO", AnsiCores.MAGENTA, true));
        for (AudioInfo a : res.audios()) {
            System.out.printf("  Fluxo %d (Track ID: %d)%n", a.index(), a.index());
            System.out.println("    Idioma: " + a.idioma());
            System.out.println("    Codec/Formato: " + a.format());
            System.out.println("    Canais: " + a.channels());
            System.out.printf("    Amostragem: %s%n", String.format("%.1f kHz", a.sampleRateKHz()));
            System.out.println("    Bitrate: " + (a.bitrate() > 0 ? (a.bitrate() / 1000) + " kbps" : "N/A"));
            System.out.println("    Título: " + a.titulo());
        }

        // 4. Fluxos de Legenda
        System.out.println("\n" + AnsiCores.colorir("FAIXAS DE LEGENDAS", AnsiCores.MAGENTA, true));
        if (res.legendas().isEmpty()) {
            System.out.println(AnsiCores.colorir("    NENHUMA FAIXA DE LEGENDA ENCONTRADA", AnsiCores.RED, true));
            System.out.println(AnsiCores.colorir("    - Pode ser RAW (sem softsub); hardsub NÃO confirmado por esta análise", AnsiCores.YELLOW));
        } else {
            for (LegendaInfo leg : res.legendas()) {
                System.out.printf("  Legenda %d (Track ID: %d)%n", leg.indexRelativo() + 1, leg.index());
                System.out.println("    Idioma: " + leg.idioma());
                System.out.println("    Formato: " + leg.formato());

                String corTipo = obterCorPorTipo(leg.tipoCurto());
                System.out.println("    Tipo: " + AnsiCores.colorir(leg.tipoCompleto() + " (" + leg.categoria() + ")", corTipo));
                System.out.println("    Codec ID: " + leg.codecId());
                System.out.println("    Título: " + leg.titulo());
                System.out.printf("    Flags: default=%s forced=%s acessibilidade=%s%n",
                    leg.isDefault(), leg.isForced(), leg.acessibilidade());
                System.out.printf("    Extraível: %s | Traduzível: %s | Exige OCR: %s%n",
                    leg.extraivel(), leg.traduzivel(), leg.exigeOcr());

                if (leg.duracaoSegundos() != null) {
                    System.out.println("    Duração Legenda: " + formatarSegundos(leg.duracaoSegundos()) + " (informativo)");
                }
                if (leg.diferencaFimSegundos() != null) {
                    System.out.printf("    Diferença p/ o vídeo: %s (informativo)%n",
                        String.format("%+.3fs", leg.diferencaFimSegundos()));
                }
            }
        }

        // 5. Resumo Final
        System.out.println("\n" + AnsiCores.colorir("RESUMO FINAL", AnsiCores.MAGENTA, true));
        int totalFaixas = 1 + res.videos().size() + res.audios().size() + res.legendas().size();
        System.out.println("  Total de Faixas: " + totalFaixas);
        System.out.println("    Vídeo(s): " + AnsiCores.colorir(String.valueOf(res.videos().size()), AnsiCores.CYAN));
        System.out.println("    Áudio(s): " + AnsiCores.colorir(String.valueOf(res.audios().size()), AnsiCores.GREEN));
        System.out.println("    Legenda(s): " + AnsiCores.colorir(String.valueOf(res.legendas().size()), AnsiCores.YELLOW));

        for (LegendaInfo leg : res.legendas()) {
            String tituloStr = leg.titulo() != null && !leg.titulo().isBlank() ? " - " + leg.titulo() : "";
            String corTipo = obterCorPorTipo(leg.tipoCurto());

            System.out.printf("      [%d] %s: %s | %s: %s | %s: %s%s%n",
                leg.index(),
                AnsiCores.colorir("Idioma", AnsiCores.CYAN),
                leg.idioma(),
                AnsiCores.colorir("Tipo", AnsiCores.CYAN),
                AnsiCores.colorir(leg.tipoCurto(), corTipo, true),
                AnsiCores.colorir("Formato", AnsiCores.CYAN),
                leg.formato(),
                tituloStr
            );
        }

        System.out.println("\n" + AnsiCores.colorir("Auditoria finalizada com sucesso!", AnsiCores.GREEN));
        System.out.println(AnsiCores.colorir("=".repeat(80), AnsiCores.BLUE) + "\n");
        System.out.flush();
    }

    private String obterCorPorTipo(String tipoCurto) {
        if (tipoCurto == null) return "";
        return switch (tipoCurto.toUpperCase()) {
            case "ASS", "SSA" -> AnsiCores.YELLOW;
            case "PGS", "VOBSUB", "DVB", "HARDSUB" -> AnsiCores.RED;
            case "SRT", "WEBVTT", "MOV_TEXT" -> AnsiCores.GREEN;
            default -> "";
        };
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
