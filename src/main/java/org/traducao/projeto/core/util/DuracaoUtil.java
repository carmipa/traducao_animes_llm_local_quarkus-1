package org.traducao.projeto.core.util;

/**
 * Formata durações de jobs para o relatório final dos consoles da UI
 * (ex.: "1h 04min 12s", "3min 08s", "45s", "0,8s"). Todos os módulos usam o
 * mesmo formato para o usuário comparar execuções entre etapas do pipeline.
 */
public final class DuracaoUtil {

    // Consoles da UI em hora local (decisão oficial 2026-07-05).
    private static final java.time.format.DateTimeFormatter FORMATO_HORA =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

    private DuracaoUtil() {
    }

    /**
     * Linha padrão de encerramento dos consoles da UI: toda operação, em
     * qualquer módulo, termina com o mesmo resumo de nome + tempo + hora.
     */
    public static String linhaRelatorioFinal(String nomeOperacao, long inicioMs) {
        return org.traducao.projeto.traducao.presentation.ui.AnsiCores.CYAN
            + "[RELATÓRIO FINAL] Operação: " + nomeOperacao
            + " | Tempo total: " + formatar(System.currentTimeMillis() - inicioMs)
            + " | Término: " + java.time.LocalTime.now().format(FORMATO_HORA)
            + org.traducao.projeto.traducao.presentation.ui.AnsiCores.RESET;
    }

    public static String formatar(long duracaoMs) {
        if (duracaoMs < 0) {
            duracaoMs = 0;
        }
        if (duracaoMs < 10_000) {
            return String.format(java.util.Locale.ROOT, "%.1fs", duracaoMs / 1000.0).replace('.', ',');
        }
        long totalSegundos = duracaoMs / 1000;
        long horas = totalSegundos / 3600;
        long minutos = (totalSegundos % 3600) / 60;
        long segundos = totalSegundos % 60;
        if (horas > 0) {
            return String.format(java.util.Locale.ROOT, "%dh %02dmin %02ds", horas, minutos, segundos);
        }
        if (minutos > 0) {
            return String.format(java.util.Locale.ROOT, "%dmin %02ds", minutos, segundos);
        }
        return segundos + "s";
    }
}
