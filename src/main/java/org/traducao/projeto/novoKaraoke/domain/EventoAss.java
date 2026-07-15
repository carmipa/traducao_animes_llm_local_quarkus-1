package org.traducao.projeto.novoKaraoke.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Um evento {@code Dialogue:} de um arquivo .ass, com a linha crua preservada
 * byte a byte. A conversão de karaokê NUNCA reescreve eventos que decide
 * manter — ela reemite {@link #linhaCrua()} — para garantir que diálogo,
 * placas e blocos preservados saiam idênticos ao arquivo de origem.
 *
 * @param linhaCrua  linha original completa, exatamente como lida do arquivo
 * @param camada     campo Layer
 * @param inicio     campo Start (mantido como texto para não perder precisão)
 * @param fim        campo End
 * @param estilo     campo Style
 * @param texto      campo Text (último campo, pode conter vírgulas)
 */
public record EventoAss(
    String linhaCrua,
    int camada,
    String inicio,
    String fim,
    String estilo,
    String texto
) {

    private static final Pattern PADRAO_REMOVE_TAGS = Pattern.compile("\\{[^}]*\\}");
    private static final Pattern PADRAO_TEMPO = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})[.,](\\d{2})");

    /** Interpreta uma linha "Dialogue: ..." ou retorna {@code null} se não for evento. */
    public static EventoAss interpretar(String linha) {
        if (linha == null || !linha.startsWith("Dialogue:")) {
            return null;
        }
        String corpo = linha.substring("Dialogue:".length()).stripLeading();
        // Formato: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
        String[] campos = corpo.split(",", 10);
        if (campos.length < 10) {
            return null;
        }
        int camada;
        try {
            camada = Integer.parseInt(campos[0].strip());
        } catch (NumberFormatException e) {
            camada = 0;
        }
        return new EventoAss(linha, camada, campos[1].strip(), campos[2].strip(), campos[3].strip(), campos[9]);
    }

    /** Texto visível na tela: sem blocos {@code {...}} e com quebras viradas espaço. */
    public String textoVisivel() {
        return PADRAO_REMOVE_TAGS.matcher(texto)
            .replaceAll("")
            .replace("\\N", " ")
            .replace("\\n", " ")
            .replace("\\h", " ")
            .strip();
    }

    /** Início em centésimos de segundo, para ordenação/agrupamento (-1 se ilegível). */
    public long inicioCs() {
        return tempoParaCs(inicio);
    }

    /** Fim em centésimos de segundo (-1 se ilegível). */
    public long fimCs() {
        return tempoParaCs(fim);
    }

    static long tempoParaCs(String tempo) {
        if (tempo == null) {
            return -1;
        }
        Matcher m = PADRAO_TEMPO.matcher(tempo.strip());
        if (!m.matches()) {
            return -1;
        }
        long horas = Long.parseLong(m.group(1));
        long minutos = Long.parseLong(m.group(2));
        long segundos = Long.parseLong(m.group(3));
        long centesimos = Long.parseLong(m.group(4));
        return ((horas * 60 + minutos) * 60 + segundos) * 100 + centesimos;
    }

    /** Formata centésimos de segundo de volta para o formato de tempo ASS (H:MM:SS.CC). */
    public static String csParaTempo(long cs) {
        long horas = cs / 360000;
        long minutos = (cs % 360000) / 6000;
        long segundos = (cs % 6000) / 100;
        long centesimos = cs % 100;
        return String.format("%d:%02d:%02d.%02d", horas, minutos, segundos, centesimos);
    }
}
