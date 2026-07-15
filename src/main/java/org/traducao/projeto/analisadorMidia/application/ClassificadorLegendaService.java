package org.traducao.projeto.analisadorMidia.application;

import org.springframework.stereotype.Service;
import org.traducao.projeto.analisadorMidia.domain.LegendaInfo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PROPÓSITO DE NEGÓCIO: classifica tecnicamente cada faixa de legenda pelo dado
 * VITAL da Análise de Mídia — a traduzibilidade: legenda de TEXTO (ASS/SSA/SRT/
 * WebVTT/MOV_TEXT) é extraível e traduzível; BITMAP (PGS/VobSub/DVB) exige OCR;
 * ausência de faixa é RAW/hardsub. Decide se um episódio segue no pipeline de
 * tradução.
 *
 * <p>INVARIANTES DO DOMÍNIO: PGS e VobSub são bitmap (imagem), NÃO hardsub;
 * ausência de faixa softsub NÃO prova hardsub; uma faixa de texto tem prioridade
 * sobre bitmap no veredito de traduzibilidade do arquivo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: entradas nulas de codec/formato caem em
 * "Desconhecido"/"DESCONHECIDO" sem exceção.
 */
@Service
public class ClassificadorLegendaService {

    // Classificação por traduzibilidade: TEXTO é extraível e traduzível;
    // BITMAP/hardsub exige OCR e não entra no pipeline de tradução direto.
    static final Set<String> TIPOS_TEXTO = Set.of("ASS", "SSA", "SRT", "WEBVTT", "MOV_TEXT");
    static final Set<String> TIPOS_BITMAP = Set.of("PGS", "VOBSUB", "DVB", "HARDSUB");

    /**
     * PROPÓSITO DE NEGÓCIO: mapeia (codecId, formato) do ffprobe para
     * [rótulo completo, tipo curto].
     * <p>INVARIANTES DO DOMÍNIO: PGS/VobSub são rotulados como Bitmap, nunca
     * Hardsub; o tipo curto alimenta {@link #categoria}/{@link #traduzivel}.
     * <p>COMPORTAMENTO EM CASO DE FALHA: codec/formato nulos ou não reconhecidos
     * retornam {@code {"Desconhecido", "DESCONHECIDO"}}.
     */
    public String[] classificar(String codecId, String formato) {
        String codecUpper = (codecId != null ? codecId : "").toUpperCase();
        String formatoUpper = (formato != null ? formato : "").toUpperCase();

        if (codecUpper.contains("ASS") || formatoUpper.equals("ASS")) {
            return new String[]{"ASS (Estilizada com cores e posicionamento)", "ASS"};
        }
        if (codecUpper.contains("SSA") || formatoUpper.equals("SSA")) {
            return new String[]{"SSA (Estilizada - SubStation Alpha)", "SSA"};
        }
        if (codecUpper.contains("PGS") || codecUpper.contains("HDMV") || formatoUpper.contains("PGS")) {
            return new String[]{"PGS (Bitmap - imagem, nao extraivel para texto sem OCR)", "PGS"};
        }
        if (codecUpper.contains("VOBSUB") || formatoUpper.contains("VOBSUB") || formatoUpper.contains("DVD_SUBTITLE")) {
            return new String[]{"VobSub (Bitmap DVD - Nao extraivel para texto)", "VOBSUB"};
        }
        if (codecUpper.contains("DVBSUB") || formatoUpper.contains("DVB")) {
            return new String[]{"DVB Subtitle (Bitmap - Nao extraivel para texto)", "DVB"};
        }
        if (codecUpper.contains("WEBVTT") || codecUpper.contains("VTT") || formatoUpper.contains("WEBVTT")) {
            return new String[]{"WebVTT (Texto simples com timing web)", "WEBVTT"};
        }
        if (codecUpper.contains("UTF8") || codecUpper.contains("SUBRIP") || formatoUpper.contains("SRT") || formatoUpper.equals("UTF-8")) {
            return new String[]{"SRT/SubRip (Simples - Recomendado para traducao)", "SRT"};
        }
        if (codecUpper.contains("TX3G") || codecUpper.contains("MOV_TEXT") || formatoUpper.contains("TIMED TEXT")) {
            return new String[]{"MOV_TEXT/TX3G (Legenda de texto MP4)", "MOV_TEXT"};
        }
        if (codecUpper.equals("IN_SCREEN")) {
            return new String[]{"Hardsub (Queimada na tela - Nao extraivel)", "HARDSUB"};
        }

        return new String[]{"Desconhecido", "DESCONHECIDO"};
    }

    /** Categoria didática (TEXTO/BITMAP/DESCONHECIDO) do tipo curto. */
    public String categoria(String tipoCurto) {
        if (TIPOS_TEXTO.contains(tipoCurto)) {
            return "TEXTO";
        }
        if (TIPOS_BITMAP.contains(tipoCurto)) {
            return "BITMAP";
        }
        return "DESCONHECIDO";
    }

    /** {@code true} se o tipo curto é uma legenda de texto (extraível/traduzível). */
    public boolean ehTexto(String tipoCurto) {
        return TIPOS_TEXTO.contains(tipoCurto);
    }

    /** {@code true} se o tipo curto é uma legenda bitmap (exige OCR). */
    public boolean ehBitmap(String tipoCurto) {
        return TIPOS_BITMAP.contains(tipoCurto);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: veredito de traduzibilidade do ARQUIVO a partir das
     * legendas detectadas — texto é traduzível; bitmap exige OCR; sem legenda é
     * RAW/hardsub.
     * <p>INVARIANTES DO DOMÍNIO: uma faixa de texto tem prioridade sobre bitmap.
     * <p>COMPORTAMENTO EM CASO DE FALHA: lista vazia devolve o rótulo "N/A - sem
     * legenda"; tipos não classificados caem em "INDETERMINADO".
     */
    public String verdictTraducao(List<LegendaInfo> legendas) {
        List<String> tiposTexto = legendas.stream().map(LegendaInfo::tipoCurto)
            .filter(TIPOS_TEXTO::contains).distinct().collect(Collectors.toList());
        if (!tiposTexto.isEmpty()) {
            return "SIM (" + String.join(", ", tiposTexto) + ")";
        }
        List<String> tiposBitmap = legendas.stream().map(LegendaInfo::tipoCurto)
            .filter(TIPOS_BITMAP::contains).distinct().collect(Collectors.toList());
        if (!tiposBitmap.isEmpty()) {
            return "NAO - bitmap (" + String.join(", ", tiposBitmap) + "), precisa de OCR";
        }
        if (!legendas.isEmpty()) {
            return "INDETERMINADO (" + legendas.getFirst().tipoCurto() + ")";
        }
        return "N/A - sem legenda (RAW ou hardsub)";
    }
}
