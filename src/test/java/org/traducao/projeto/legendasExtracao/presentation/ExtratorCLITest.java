package org.traducao.projeto.legendasExtracao.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o {@link ExtratorCLI}, após a E4b, resolve a pasta
 * de vídeos exclusivamente a partir de {@code tradutor.diretorio-entrada}, com a mesma
 * normalização por {@code trim} do fluxo legado, sem qualquer dependência de
 * {@code TradutorProperties} ou {@code PastasExecucao}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Entrada ausente, vazia ou só com espaços ⇒ {@code null} (inválida).</li>
 *   <li>Entrada útil ⇒ {@code Path.of(valor.trim())}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência na normalização reprova o teste.
 */
@DisplayName("E4b: ExtratorCLI resolve apenas o diretório de entrada")
class ExtratorCLITest {

    @Test
    @DisplayName("entrada ausente → null")
    void entradaAusente() {
        assertNull(ExtratorCLI.resolverEntrada(Optional.empty()));
    }

    @Test
    @DisplayName("entrada vazia → null")
    void entradaVazia() {
        assertNull(ExtratorCLI.resolverEntrada(Optional.of("")));
    }

    @Test
    @DisplayName("entrada só com espaços → null")
    void entradaBlank() {
        assertNull(ExtratorCLI.resolverEntrada(Optional.of("   ")));
    }

    @Test
    @DisplayName("entrada válida → Path.of(valor)")
    void entradaValida() {
        assertEquals(Path.of("videos/anime"), ExtratorCLI.resolverEntrada(Optional.of("videos/anime")));
    }

    @Test
    @DisplayName("entrada com espaços laterais é normalizada por trim")
    void entradaComEspacosLaterais() {
        assertEquals(Path.of("videos/anime"), ExtratorCLI.resolverEntrada(Optional.of("  videos/anime  ")));
    }
}
