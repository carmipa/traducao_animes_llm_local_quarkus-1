package org.traducao.projeto.legenda.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PROPÓSITO DE NEGÓCIO: política de IDENTIFICAÇÃO de estilos ASS potencialmente
 * MUSICAIS/preserváveis (letra japonesa, romaji, karaokê, aberturas/encerramentos),
 * usada EM CONJUNTO com o detector de conteúdo do pipeline. A subfase E3c apenas
 * transfere o PROPRIETÁRIO desta regra — antes em
 * {@code TradutorProperties.estiloIgnorado} — para o módulo compartilhado
 * {@code legenda}, SEM alterar o comportamento funcional do fluxo.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Sinaliza um estilo quando ele está na lista configurada
 *       ({@code tradutor.estilos-ignorados}, comparação case-insensitive) OU quando é
 *       reconhecido pelas heurísticas/regex de palavras musicais já existentes.</li>
 *   <li>Esta política, SOZINHA, NÃO decide se uma linha será enviada ao LLM. A decisão
 *       final de preservação considera TAMBÉM o conteúdo da linha, avaliado pelo
 *       {@code DetectorEfeitoKaraokeService} (ex.: {@code eKaraokeOuMusicaTraduzivel},
 *       {@code devePreservarKaraokeOriginal}), que permanece o proprietário dessa parte.</li>
 *   <li>Letras japonesas e romaji protegidos devem permanecer INTACTOS; a versão em
 *       INGLÊS que acompanha o karaokê PODE continuar traduzível quando o detector
 *       assim determinar.</li>
 *   <li>Guarda uma cópia IMUTÁVEL da lista recebida, preservando ordem, case,
 *       duplicatas e elementos vazios — sem trim, dedup ou normalização.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Estilo {@code null} ou em branco retorna {@code false} (não identificado como musical).
 * Um retorno {@code false} significa apenas "não identificado como estilo musical por
 * esta política" — NÃO significa, por si só, "diálogo padrão" nem "linha traduzível".
 */
public final class PoliticaEstiloMusical {

    private static final Pattern PALAVRAS_CHAVE_MUSICA =
        Pattern.compile("(?i)\\b(song|music|karaoke|romaji|opening|ending|theme|insert|op|ed|sing)\\b");

    private final List<String> estilosIgnorados;

    public PoliticaEstiloMusical(List<String> estilosIgnorados) {
        // Cópia imutável fiel: preserva ordem, case, duplicatas e vazios; sem normalização.
        this.estilosIgnorados = Collections.unmodifiableList(new ArrayList<>(estilosIgnorados));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: identifica se um estilo ASS deve ser tratado como MUSICAL
     * (candidato a preservação) por esta política — pela lista configurada ou pelas
     * heurísticas/regex de palavras musicais. É apenas o primeiro sinal; a preservação
     * efetiva depende também do detector de conteúdo.
     *
     * <p>INVARIANTES: comparação de lista case-insensitive; heurística e regex idênticas
     * ao comportamento histórico (sem normalização nova).
     *
     * <p>FALHA: estilo {@code null}/em branco → {@code false}.
     */
    public boolean estiloIgnorado(String estilo) {
        if (estilo == null || estilo.isBlank()) {
            return false;
        }
        // 1. Check da lista explícita configurada no application.yml
        if (estilosIgnorados.stream().anyMatch(e -> e.equalsIgnoreCase(estilo))) {
            return true;
        }
        // 2. Check heurístico de palavras-chave comuns de músicas e karaokês
        String est = estilo.toLowerCase();
        if (est.contains("song") || est.contains("music") || est.contains("karaoke")
            || est.contains("romaji") || est.contains("opening") || est.contains("ending")
            || est.contains("theme") || est.contains("insert") || est.contains("sing")) {
            return true;
        }
        // Check de limites de palavras para abreviações curtas como OP, ED, OP1, ED2, etc.
        return PALAVRAS_CHAVE_MUSICA.matcher(estilo).find();
    }
}
