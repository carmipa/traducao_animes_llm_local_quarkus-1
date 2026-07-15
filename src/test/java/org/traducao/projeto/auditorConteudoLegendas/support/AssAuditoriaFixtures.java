package org.traducao.projeto.auditorConteudoLegendas.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AssAuditoriaFixtures {

    private AssAuditoriaFixtures() {}

    public static void escreverParLimpo(Path original, Path traduzido) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        String linhaOrig = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Hello\\NWorld\n";
        String linhaTrad = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Ola\\NMundo\n";
        Files.writeString(original, cabecalho + linhaOrig, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabecalho + linhaTrad, StandardCharsets.UTF_8);
    }

    public static void escreverParComQuebraExcessiva(Path original, Path traduzido) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        String linhaOrig = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,This is a single line.\n";
        String linhaTrad = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Esta\\Ne\\Numa\\Nunica\\Nlinha.\n";
        Files.writeString(original, cabecalho + linhaOrig, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabecalho + linhaTrad, StandardCharsets.UTF_8);
    }

    public static void escreverParComPlayResAlterado(Path original, Path traduzido) throws IOException {
        String cabOrig = cabecalhoComEstilos("1920", "1080", "Default");
        String cabTrad = cabecalhoComEstilos("1280", "720", "Default");
        String linha = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Texto\n";
        Files.writeString(original, cabOrig + linha, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabTrad + linha, StandardCharsets.UTF_8);
    }

    public static void escreverParComEstiloRemovido(Path original, Path traduzido) throws IOException {
        String cabOrig = cabecalhoComEstilos("1920", "1080", "Default", "Opening");
        String cabTrad = cabecalhoComEstilos("1920", "1080", "Default");
        String linha = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Texto\n";
        Files.writeString(original, cabOrig + linha, StandardCharsets.UTF_8);
        Files.writeString(traduzido, cabTrad + linha, StandardCharsets.UTF_8);
    }

    public static void escreverArquivoUnicoComAnomalias(Path arquivo) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        // Timestamp invalido: fim antes do inicio.
        String linha1 = "Dialogue: 0,0:00:05.00,0:00:03.00,Default,,0,0,0,,Fala com tempo invertido\n";
        // Bloco de override aberto e nunca fechado.
        String linha2 = "Dialogue: 0,0:00:06.00,0:00:08.00,Default,,0,0,0,,{\\i1 texto sem fechar\n";
        Files.writeString(arquivo, cabecalho + linha1 + linha2, StandardCharsets.UTF_8);
    }

    public static void escreverArquivoUnicoLimpo(Path arquivo) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        String linha = "Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Fala perfeitamente valida\n";
        Files.writeString(arquivo, cabecalho + linha, StandardCharsets.UTF_8);
    }

    /**
     * Sobreposições 100% intencionais: karaokê (OP + \k), placa (\pos) e uma fala
     * em outra camada. A regra de sobreposição não deve reportar nenhuma delas.
     */
    public static void escreverArquivoSobreposicaoIntencional(Path arquivo) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default", "Sign", "OP");
        StringBuilder sb = new StringBuilder(cabecalho);
        sb.append("Dialogue: 0,0:00:01.00,0:00:05.00,OP,,0,0,0,,{\\k50}la {\\k50}la\n");
        sb.append("Dialogue: 0,0:00:02.00,0:00:06.00,OP,,0,0,0,,{\\k50}na {\\k50}na\n");
        sb.append("Dialogue: 0,0:00:03.00,0:00:07.00,Sign,,0,0,0,,{\\pos(960,50)}Tokyo\n");
        sb.append("Dialogue: 5,0:00:03.50,0:00:08.00,Default,,0,0,0,,Fala em outra camada\n");
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Sobreposição real: dois diálogos comuns de mesmo estilo e mesma camada com
     * tempos que se cruzam. Deve gerar exatamente um alerta de sobreposição.
     */
    public static void escreverArquivoSobreposicaoReal(Path arquivo) throws IOException {
        String cabecalho = cabecalhoComEstilos("1920", "1080", "Default");
        StringBuilder sb = new StringBuilder(cabecalho);
        sb.append("Dialogue: 0,0:00:01.00,0:00:05.00,Default,,0,0,0,,Primeira fala comum\n");
        sb.append("Dialogue: 0,0:00:03.00,0:00:07.00,Default,,0,0,0,,Segunda fala comum sobreposta\n");
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String cabecalhoComEstilos(String playResX, String playResY, String... estilos) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Script Info]\n");
        sb.append("ScriptType: v4.00+\n");
        sb.append("PlayResX: ").append(playResX).append('\n');
        sb.append("PlayResY: ").append(playResY).append('\n');
        sb.append('\n');
        sb.append("[V4+ Styles]\n");
        sb.append("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding\n");
        for (String estilo : estilos) {
            sb.append("Style: ").append(estilo)
                .append(",Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,0,0,0,0,100,100,0,0,1,2,0,2,10,10,10,1\n");
        }
        sb.append('\n');
        sb.append("[Events]\n");
        sb.append("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text\n");
        return sb.toString();
    }
}
