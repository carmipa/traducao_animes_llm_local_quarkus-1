package org.traducao.projeto.legendasExtracao.presentation.ui;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.presentation.ui.AnsiCores;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class ConsoleExtratorLogger {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void info(String mensagem) {
        imprimir(null, "INFO", mensagem);
    }

    public void aviso(String mensagem) {
        imprimir(AnsiCores.YELLOW, "AVISO", AnsiCores.YELLOW + mensagem + AnsiCores.RESET);
    }

    public void sucesso(String mensagem) {
        imprimir(AnsiCores.GREEN, "OK", AnsiCores.GREEN + mensagem + AnsiCores.RESET);
    }

    public void erro(String mensagem) {
        imprimir(AnsiCores.RED, "ERRO", AnsiCores.RED + mensagem + AnsiCores.RESET);
    }

    // Tag colorida em negrito (chama atenção), corpo da mensagem em peso normal
    // (mais fácil de ler em blocos de texto maiores) — INFO fica sem cor nenhuma.
    private void imprimir(String corNivel, String nivel, String mensagemFormatada) {
        String tempo = LocalTime.now().format(TIME_FORMATTER);
        String tag = corNivel != null
                ? String.format("%s%s%-6s%s", AnsiCores.BOLD, corNivel, nivel, AnsiCores.RESET)
                : String.format("%-6s", nivel);
        System.out.printf("[%s] [%s] %s%n", tempo, tag, mensagemFormatada);
    }

    public void cabecalho(String titulo) {
        System.out.println("\n" + AnsiCores.CYAN + "=".repeat(80));
        System.out.printf("%55s%n", titulo);
        System.out.println("=".repeat(80) + AnsiCores.RESET + "\n");
    }
}
