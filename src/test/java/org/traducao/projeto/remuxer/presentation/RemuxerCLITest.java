package org.traducao.projeto.remuxer.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o {@link RemuxerCLI}, após a E4b, resolve a pasta de
 * legendas PTBR reproduzindo fielmente — como duplicação consciente e autorizada — a
 * política legada {@code TradutorProperties.resolverDiretorioSaida()}: saída explícita
 * quando informada, senão o fallback {@code entrada/traducao_ptbr}, com {@code trim}
 * preservado e sem depender de {@code TradutorProperties} ou {@code PastasExecucao}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Saída ausente/vazia/blank ⇒ {@code pastaVideos.resolve("traducao_ptbr")}.</li>
 *   <li>Saída útil ⇒ {@code Path.of(valor.trim())}.</li>
 *   <li>A resolução da saída nunca devolve {@code null}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência do fallback ou da normalização reprova o teste.
 */
@DisplayName("E4b: RemuxerCLI resolve saída com fallback traducao_ptbr")
class RemuxerCLITest {

    private static final Path VIDEOS = Path.of("videos/anime");

    @Test
    @DisplayName("saída ausente → entrada/traducao_ptbr")
    void saidaAusenteCaiNoFallback() {
        assertEquals(VIDEOS.resolve("traducao_ptbr"),
            RemuxerCLI.resolverDiretorioSaida(VIDEOS, Optional.empty()));
    }

    @Test
    @DisplayName("saída vazia → entrada/traducao_ptbr")
    void saidaVaziaCaiNoFallback() {
        assertEquals(VIDEOS.resolve("traducao_ptbr"),
            RemuxerCLI.resolverDiretorioSaida(VIDEOS, Optional.of("")));
    }

    @Test
    @DisplayName("saída só com espaços → entrada/traducao_ptbr")
    void saidaBlankCaiNoFallback() {
        assertEquals(VIDEOS.resolve("traducao_ptbr"),
            RemuxerCLI.resolverDiretorioSaida(VIDEOS, Optional.of("   ")));
    }

    @Test
    @DisplayName("saída explícita → Path.of(valor), sem fallback")
    void saidaExplicita() {
        assertEquals(Path.of("legendas/ptbr"),
            RemuxerCLI.resolverDiretorioSaida(VIDEOS, Optional.of("legendas/ptbr")));
    }

    @Test
    @DisplayName("saída explícita com espaços laterais é normalizada por trim")
    void saidaExplicitaComEspacos() {
        assertEquals(Path.of("legendas/ptbr"),
            RemuxerCLI.resolverDiretorioSaida(VIDEOS, Optional.of("  legendas/ptbr  ")));
    }

    @Test
    @DisplayName("entrada é normalizada por trim")
    void entradaComTrim() {
        assertEquals(Path.of("videos/anime"),
            RemuxerCLI.resolverEntrada(Optional.of("  videos/anime  ")));
    }
}
