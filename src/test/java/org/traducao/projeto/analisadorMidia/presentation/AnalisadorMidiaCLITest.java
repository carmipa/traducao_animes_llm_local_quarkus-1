package org.traducao.projeto.analisadorMidia.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PROPÓSITO DE NEGÓCIO: prova que o {@link AnalisadorMidiaCLI}, após a E4b, resolve
 * entrada e saída exclusivamente a partir de {@code tradutor.diretorio-entrada} e
 * {@code tradutor.diretorio-saida}, preservando o comportamento legado — inclusive a
 * saída OPCIONAL que resulta em {@code null} quando não informada (sem o fallback
 * {@code traducao_ptbr}, exclusivo do remux) — sem depender de {@code TradutorProperties}
 * ou {@code PastasExecucao}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Entrada ausente/vazia/blank ⇒ {@code null} (inválida); útil ⇒ {@code Path.of(trim)}.</li>
 *   <li>Saída ausente/vazia/blank ⇒ {@code null} (sem pasta de saída); útil ⇒ {@code Path.of(trim)}.</li>
 *   <li>A saída NUNCA cai em {@code traducao_ptbr}.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Qualquer divergência na normalização ou o surgimento indevido de fallback reprova o teste.
 */
@DisplayName("E4b: AnalisadorMidiaCLI resolve entrada e saída opcional")
class AnalisadorMidiaCLITest {

    @Test
    @DisplayName("entrada válida → Path.of(valor) com trim")
    void entradaValida() {
        assertEquals(Path.of("midia/anime"), AnalisadorMidiaCLI.resolverEntrada(Optional.of("  midia/anime  ")));
    }

    @Test
    @DisplayName("entrada ausente/vazia/blank → null")
    void entradaInvalida() {
        assertNull(AnalisadorMidiaCLI.resolverEntrada(Optional.empty()));
        assertNull(AnalisadorMidiaCLI.resolverEntrada(Optional.of("")));
        assertNull(AnalisadorMidiaCLI.resolverEntrada(Optional.of("   ")));
    }

    @Test
    @DisplayName("saída ausente → null")
    void saidaAusente() {
        assertNull(AnalisadorMidiaCLI.resolverSaidaOpcional(Optional.empty()));
    }

    @Test
    @DisplayName("saída vazia → null")
    void saidaVazia() {
        assertNull(AnalisadorMidiaCLI.resolverSaidaOpcional(Optional.of("")));
    }

    @Test
    @DisplayName("saída só com espaços → null")
    void saidaBlank() {
        assertNull(AnalisadorMidiaCLI.resolverSaidaOpcional(Optional.of("   ")));
    }

    @Test
    @DisplayName("saída explícita → Path.of(valor) com trim, sem fallback traducao_ptbr")
    void saidaExplicita() {
        Path saida = AnalisadorMidiaCLI.resolverSaidaOpcional(Optional.of("  relatorios/saida  "));
        assertEquals(Path.of("relatorios/saida"), saida);
    }
}
