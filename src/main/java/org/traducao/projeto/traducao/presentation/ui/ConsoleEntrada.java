package org.traducao.projeto.traducao.presentation.ui;
import org.traducao.projeto.core.presentation.ui.AnsiCores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class ConsoleEntrada {

    private static final BufferedReader LEITOR = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8)
    );

    public record CaminhosPastas(String modo, String entrada, String saida, String formato) {}

    private ConsoleEntrada() {}

    public static Optional<CaminhosPastas> solicitarPastas() {
        imprimirBanner();

        try {
            String modoOpcao = lerOpcional(
                AnsiCores.colorir("Escolha (1/2/3/4/5/6/7/8) [Enter = Interface WEB]: ", AnsiCores.CYAN)
            );

            if (modoOpcao == null) {
                return Optional.empty();
            }

            // Numeração segue a ordem natural do pipeline: primeiro auditar a
            // mídia, depois extrair/traduzir/corrigir a legenda e por fim remuxar.
            String modo = "1".equals(modoOpcao) ? "ANALISAR" :
                          "2".equals(modoOpcao) ? "EXTRAIR" :
                          "3".equals(modoOpcao) ? "TRADUZIR" :
                          "4".equals(modoOpcao) ? "CORRIGIR_CACHE" :
                          "5".equals(modoOpcao) ? "RASPAGEM_CORRECAO" :
                          "6".equals(modoOpcao) ? "RASPAGEM_REVISAO_LEGENDAS" :
                          "7".equals(modoOpcao) ? "REMUXAR" :
                          "8".equals(modoOpcao) ? "MAPEAR" : "WEB";

            imprimir("");
            if (modo.equals("ANALISAR")) {
                imprimir(AnsiCores.colorir(">>> MODO ANÁLISE DE MÍDIA SELECIONADO <<<", AnsiCores.CYAN, true));
                return solicitarPastasAnalisador(modo);
            } else if (modo.equals("EXTRAIR")) {
                imprimir(AnsiCores.colorir(">>> MODO EXTRAÇÃO DE LEGENDAS SELECIONADO <<<", AnsiCores.MAGENTA, true));
                return solicitarPastasExtrator(modo);
            } else if (modo.equals("TRADUZIR")) {
                imprimir(AnsiCores.colorir(">>> MODO TRADUÇÃO DE LEGENDAS VIA LLM SELECIONADO <<<", AnsiCores.GREEN, true));
                return solicitarPastasTraducao(modo);
            } else if (modo.equals("CORRIGIR_CACHE")) {
                imprimir(AnsiCores.colorir(">>> MODO CORREÇÃO DE TRADUÇÃO (LIMPAR CACHE) SELECIONADO <<<", AnsiCores.YELLOW, true));
                return solicitarPastasCorretor(modo);
            } else if (modo.equals("RASPAGEM_CORRECAO")) {
                imprimir(AnsiCores.colorir(">>> MODO CORREÇÃO DE TRADUÇÃO VIA SCRAPING (GOOGLE TRADUTOR) SELECIONADO <<<", AnsiCores.YELLOW, true));
                return solicitarPastasCorretor(modo);
            } else if (modo.equals("RASPAGEM_REVISAO_LEGENDAS")) {
                imprimir(AnsiCores.colorir(">>> MODO REVISÃO DE LEGENDAS (GOOGLE + AUDITORIA) SELECIONADO <<<", AnsiCores.YELLOW, true));
                return solicitarPastasRevisaoLegendas(modo);
            } else if (modo.equals("REMUXAR")) {
                imprimir(AnsiCores.colorir(">>> MODO REMUXER (JUNÇÃO DE VÍDEOS COM LEGENDAS) SELECIONADO <<<", AnsiCores.CYAN, true));
                return solicitarPastasRemuxer(modo);
            } else if (modo.equals("MAPEAR")) {
                imprimir(AnsiCores.colorir(">>> MODO MAPEAR PROJETO SELECIONADO <<<", AnsiCores.CYAN, true));
                String raiz = System.getProperty("user.dir");
                imprimir(AnsiCores.colorir("Diretório atual detectado como raiz: " + raiz, AnsiCores.GREEN));
                return Optional.of(new CaminhosPastas(modo, raiz, raiz, null));
            } else {
                imprimir(AnsiCores.colorir(">>> INICIANDO INTERFACE WEB (NAVEGADOR) <<<", AnsiCores.CYAN, true));
                return Optional.of(new CaminhosPastas("WEB", "", "", null));
            }
        } catch (IOException e) {
            imprimir(AnsiCores.colorir("ERRO ao ler do console: " + e.getMessage(), AnsiCores.RED, true));
            return Optional.empty();
        }
    }
    
    private static Optional<CaminhosPastas> solicitarPastasTraducao(String modo) throws IOException {
        String entrada = lerObrigatorio(
            AnsiCores.colorir(">>> Pasta com as legendas ORIGINAIS em INGLÊS (.ass/.ssa): ", AnsiCores.GREEN, true)
        );
        if (entrada == null) return Optional.empty();

        String saida = lerOpcional(
            AnsiCores.colorir(">>> Pasta de SAÍDA para as legendas em PORTUGUÊS (Enter = automático): ", AnsiCores.CYAN)
        );
        if (saida == null) return Optional.empty();

        imprimir("");
        imprimir(AnsiCores.colorir("Pastas OK. Subindo o tradutor...", AnsiCores.GREEN, true));
        return Optional.of(new CaminhosPastas(modo, entrada, saida, null));
    }
    
    private static Optional<CaminhosPastas> solicitarPastasRemuxer(String modo) throws IOException {
        String entrada = lerObrigatorio(
            AnsiCores.colorir(">>> Pasta com os arquivos de VÍDEO ORIGINAIS (.mkv): ", AnsiCores.GREEN, true)
        );
        if (entrada == null) return Optional.empty();

        String saida = lerOpcional(
            AnsiCores.colorir(">>> Pasta com as legendas traduzidas em PORTUGUÊS (.ass) [Enter = automático]: ", AnsiCores.CYAN)
        );
        if (saida == null) return Optional.empty();

        imprimir("");
        imprimir(AnsiCores.colorir("Pastas OK. Subindo o remuxer...", AnsiCores.GREEN, true));
        return Optional.of(new CaminhosPastas(modo, entrada, saida, null));
    }

    private static Optional<CaminhosPastas> solicitarPastasExtrator(String modo) throws IOException {
        String entrada = lerObrigatorio(
            AnsiCores.colorir(">>> Pasta com os vídeos em formato MKV (.mkv) contendo as legendas embutidas: ", AnsiCores.GREEN, true)
        );
        if (entrada == null) return Optional.empty();

        imprimir(AnsiCores.colorir("Formatos suportados: ASS, PGS, SRT", AnsiCores.DIM));
        String formato = lerOpcional(
            AnsiCores.colorir(">>> Qual formato extrair? [Enter = ASS]: ", AnsiCores.MAGENTA)
        );
        if (formato == null || formato.isBlank()) formato = "ASS";

        imprimir("");
        imprimir(AnsiCores.colorir("Tudo OK. Subindo o extrator de " + formato.toUpperCase() + "...", AnsiCores.GREEN, true));
        return Optional.of(new CaminhosPastas(modo, entrada, "", formato.toUpperCase()));
    }

    private static Optional<CaminhosPastas> solicitarPastasCorretor(String modo) throws IOException {
        String entrada = lerOpcional(
            AnsiCores.colorir(">>> Pasta onde está o cache de traduções (.cache.json) [Enter = padrão 'cache']: ", AnsiCores.GREEN, true)
        );
        if (entrada == null) return Optional.empty();
        if (entrada.isBlank()) {
            entrada = "cache";
        }

        imprimir("");
        imprimir(AnsiCores.colorir("Pasta de cache OK. Subindo o corretor...", AnsiCores.GREEN, true));
        return Optional.of(new CaminhosPastas(modo, entrada, "", null));
    }

    private static Optional<CaminhosPastas> solicitarPastasRevisaoLegendas(String modo) throws IOException {
        String entrada = lerObrigatorio(
            AnsiCores.colorir(">>> Pasta com legendas TRADUZIDAS em português (.ass/.ssa): ", AnsiCores.GREEN, true)
        );
        if (entrada == null) return Optional.empty();

        imprimir("");
        imprimir(AnsiCores.colorir("Pasta OK. Subindo revisão de legendas...", AnsiCores.GREEN, true));
        return Optional.of(new CaminhosPastas(modo, entrada, "", null));
    }

    private static Optional<CaminhosPastas> solicitarPastasAnalisador(String modo) throws IOException {
        String entrada = lerObrigatorio(
            AnsiCores.colorir(">>> Pasta com os vídeos ou caminho do arquivo individual (.mkv/.mp4/etc): ", AnsiCores.GREEN, true)
        );
        if (entrada == null) return Optional.empty();

        String saida = lerOpcional(
            AnsiCores.colorir(">>> Pasta de SAÍDA para os relatórios (Enter = pasta de entrada/relatorios_analise): ", AnsiCores.CYAN)
        );
        if (saida == null) return Optional.empty();

        imprimir("");
        imprimir(AnsiCores.colorir("Caminhos OK. Iniciando o analisador de mídia...", AnsiCores.GREEN, true));
        return Optional.of(new CaminhosPastas(modo, entrada, saida, null));
    }

    public static void imprimirErroSaida() {
        imprimir("");
        imprimir(AnsiCores.colorir("ERRO: interrupção ou erro no console.", AnsiCores.RED, true));
        imprimir(AnsiCores.colorir("Rode:  .\\gradlew.bat bootRun --console=plain", AnsiCores.YELLOW));
        imprimir("");
    }

    private static void imprimirBanner() {
        String linha = AnsiCores.colorir("=".repeat(62), AnsiCores.BLUE);
        imprimir("");
        imprimir(linha);
        imprimir(AnsiCores.colorir("  PIPELINE INDUSTRIAL DE TRADUÇÃO E REMUX", AnsiCores.YELLOW, true));
        imprimir(linha);
        imprimir("");
        imprimir(AnsiCores.colorir("O que você deseja fazer?", AnsiCores.WHITE));
        imprimir(AnsiCores.colorir("  [1] Análise de Mídia (Codecs, Sincronia, Auditoria Técnica)", AnsiCores.CYAN));
        imprimir(AnsiCores.colorir("  [2] Extração de Legendas embutidas em MKVs", AnsiCores.MAGENTA));
        imprimir(AnsiCores.colorir("  [3] Tradução de Legendas via LLM Local", AnsiCores.GREEN));
        imprimir(AnsiCores.colorir("  [4] Correção de Tradução (Limpar Cache)", AnsiCores.YELLOW));
        imprimir(AnsiCores.colorir("  [5] Correção de Tradução via Scraping (Google Tradutor Online)", AnsiCores.YELLOW));
        imprimir(AnsiCores.colorir("  [6] Revisão de Legendas (.ass via Google)", AnsiCores.YELLOW));
        imprimir(AnsiCores.colorir("  [7] Remuxer - Junção de Vídeos com Legendas", AnsiCores.CYAN));
        imprimir(AnsiCores.colorir("  [8] Mapear estrutura e taxonomia do projeto", AnsiCores.CYAN));
        imprimir("");
    }

    private static String lerObrigatorio(String prompt) throws IOException {
        String linha = lerLinha(prompt);
        if (linha == null) {
            imprimir(AnsiCores.colorir("ERRO: stdin fechado.", AnsiCores.RED, true));
            return null;
        }
        linha = linha.trim().replaceAll("[\"']", ""); // Strip quotes
        if (linha.isEmpty()) {
            imprimir(AnsiCores.colorir("ERRO: caminho vazio.", AnsiCores.RED, true));
            return null;
        }
        return linha;
    }

    private static String lerOpcional(String prompt) throws IOException {
        String linha = lerLinha(prompt);
        if (linha == null) return null;
        return linha.trim().replaceAll("[\"']", "");
    }

    private static String lerLinha(String prompt) throws IOException {
        System.out.print(prompt);
        System.out.flush();
        String linha = LEITOR.readLine();
        System.out.println(); // Previne que a barra de progresso do Gradle apague/sobrescreva a linha
        return linha;
    }

    private static void imprimir(String linha) {
        System.out.println(linha);
        System.out.flush();
    }
}

