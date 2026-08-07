package org.traducao.projeto.legenda.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
 *   <li>Letras japonesas e romaji protegidos devem permanecer INTACTOS. A versão em
 *       INGLÊS que acompanha o karaokê TAMBÉM não é traduzida pelas etapas de fala:
 *       pela REGRA DE ESCOPO (Paulo, 2026-07-25), música e karaokê pertencem à fatia
 *       {@code traducaoKaraoke}. Até 2026-07-28 quatro fluxos abriam exceção para a
 *       letra latina via {@code eKaraokeOuMusicaTraduzivel}, e o custo foi medido: 10 de
 *       13 episódios do Gundam 08th bloqueados por retradução em massa, e 1.008 dos
 *       1.027 eventos que a revisão do Zeta alterou eram letra de música.</li>
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
        // 2. A forma do nome musical é decidida por PadraoEstiloMusical — fonte ÚNICA desde
        // 07/08/2026. Antes, esta classe tinha a própria heurística (substring + \b) e o
        // DetectorEfeitoKaraokeService tinha outra (lookaround de letra), e as duas discordavam
        // em 1.385 linhas do acervo: o \b não alcançava OP_S2 (o "_" é caractere de palavra) e
        // o lookaround não conhecia "romaji" nem o plural de "songs".
        return PadraoEstiloMusical.nomeDeclaraMusica(estilo);
    }
}
