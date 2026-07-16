package org.traducao.projeto.traducao.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.traducao.presentation.ui.PastasExecucao;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: gate de paridade da subfase E4b. Congela, ANTES de destacar
 * os três CLIs externos (Extrator/Analisador/Remuxer) de {@code PastasExecucao} e
 * {@code TradutorProperties}, o comportamento legado de resolução do diretório de
 * saída que esses CLIs herdam hoje — de modo que a lógica inline que passará a viver
 * em cada CLI possa ser provada equivalente.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A normalização por {@code trim} da entrada e da saída ocorre em
 *       {@link PastasExecucao#configurar(String, String, String, TradutorProperties)},
 *       NÃO em {@link TradutorProperties#resolverDiretorioSaida()} (que apenas decide
 *       passthrough vs. fallback sobre valores já aparados).</li>
 *   <li>Composto legado (o que o CLI enxerga): saída ausente/vazia/blank ⇒
 *       {@code Path.of(entrada.trim()).resolve("traducao_ptbr")}; saída válida ⇒
 *       {@code Path.of(saida.trim())}.</li>
 *   <li>Nenhum dos três CLIs lê o diretório de cache de volta; a política de cache
 *       não entra nesta paridade.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência entre o composto legado e a fórmula esperada reprova o teste —
 * sinal de gate: NÃO prosseguir com a migração dos CLIs.
 */
@DisplayName("Gate E4b: paridade da resolução de diretório de saída herdada pelos CLIs")
class ParidadeResolucaoCaminhoE4bTest {

    private static final String ENTRADA = "base/anime/ep";

    /**
     * PROPÓSITO DE NEGÓCIO: reproduz fielmente o caminho legado que os CLIs usam hoje —
     * {@code PastasExecucao.configurar(...)} seguido de {@code diretorioSaida()}.
     * <p>INVARIANTES DO DOMÍNIO: usa {@code TradutorProperties} padrão apenas como
     * portador dos campos não relacionados à saída (lote/estilos/idiomas).
     * <p>COMPORTAMENTO EM CASO DE FALHA: entrada em branco faria {@code configurar}
     * lançar {@link IllegalArgumentException}; os casos deste teste sempre passam
     * entrada válida.
     */
    private static Path saidaLegada(String entrada, String saida) {
        PastasExecucao pastas = new PastasExecucao();
        pastas.configurar(entrada, saida, "", new TradutorProperties());
        return pastas.diretorioSaida();
    }

    @Test
    @DisplayName("saída ausente (null) → entrada/traducao_ptbr")
    void saidaAusenteCaiNoFallback() {
        assertEquals(Path.of(ENTRADA).resolve("traducao_ptbr"), saidaLegada(ENTRADA, null));
    }

    @Test
    @DisplayName("saída vazia (\"\") → entrada/traducao_ptbr")
    void saidaVaziaCaiNoFallback() {
        assertEquals(Path.of(ENTRADA).resolve("traducao_ptbr"), saidaLegada(ENTRADA, ""));
    }

    @Test
    @DisplayName("saída só com espaços → entrada/traducao_ptbr")
    void saidaBlankCaiNoFallback() {
        assertEquals(Path.of(ENTRADA).resolve("traducao_ptbr"), saidaLegada(ENTRADA, "   "));
    }

    @Test
    @DisplayName("saída explícita → Path.of(saida), sem fallback")
    void saidaExplicitaNaoCaiNoFallback() {
        assertEquals(Path.of("destino/ptbr"), saidaLegada(ENTRADA, "destino/ptbr"));
    }

    @Test
    @DisplayName("entrada com espaços laterais é normalizada por trim antes do fallback")
    void entradaComEspacosLateraisNormaliza() {
        assertEquals(
            Path.of(ENTRADA).resolve("traducao_ptbr"),
            saidaLegada("  " + ENTRADA + "  ", null));
    }

    @Test
    @DisplayName("saída explícita com espaços laterais é normalizada por trim")
    void saidaExplicitaComEspacosLateraisNormaliza() {
        assertEquals(Path.of("destino/ptbr"), saidaLegada(ENTRADA, "  destino/ptbr  "));
    }
}
