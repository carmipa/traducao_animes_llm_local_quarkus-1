package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o reconhecimento da fala de vários locutores e a remontagem dela — os
 * casos são os do teste ponta a ponta de 09/10/2026 (Reconguista I em SRT, Sidonia e Patlabor).
 * <p>INVARIANTES DO DOMÍNIO: só a forma inequívoca é separada; o travessão original de cada
 * locutor volta byte a byte; a quebra fica exatamente entre os locutores.
 * <p>COMPORTAMENTO EM CASO DE FALHA: separar o que não é diálogo, ou remontar torto, reprova.
 */
class FalaDeLocutoresTest {

    @Test
    @DisplayName("Dois locutores com '- ': separa os textos e remonta com o travessão e a quebra entre eles")
    void separaERemontaDoisLocutores() {
        FalaDeLocutores fala = FalaDeLocutores.decompor("- This way.\\N- Right.").orElseThrow();
        assertEquals(List.of("This way.", "Right."), fala.textos());
        assertEquals(Optional.of("- Por aqui.\\N- Certo."), fala.recompor(List.of("Por aqui.", "Certo.")));
    }

    @Test
    @DisplayName("O travessão que o modelo repetir no começo da tradução não sai dobrado")
    void travessaoDoModeloNaoDobra() {
        FalaDeLocutores fala = FalaDeLocutores.decompor("- Raraiya!\\N- You okay?").orElseThrow();
        assertEquals(Optional.of("- Raraiya!\\N- Você está bem?"),
            fala.recompor(List.of("- Raraiya!", "-Você está bem?")));
    }

    @Test
    @DisplayName("Prefixos '--' e '-' sem espaço são preservados como vieram")
    void preservaPrefixosOriginais() {
        FalaDeLocutores dupla = FalaDeLocutores.decompor("--I'm scared!\\N--Oh no...").orElseThrow();
        assertEquals(Optional.of("--Estou com medo!\\N--Ah, não..."),
            dupla.recompor(List.of("Estou com medo!", "Ah, não...")));
        FalaDeLocutores mista = FalaDeLocutores.decompor("-I'll do it.\\N- Don't close your hatch.").orElseThrow();
        assertEquals(Optional.of("-Eu faço.\\N- Não feche a escotilha."),
            mista.recompor(List.of("Eu faço.", "Não feche a escotilha.")));
    }

    /**
     * Fronteira (A1): todas estas têm o MESMO sinal superficial — {@code \N} e travessão — e nenhuma
     * pode ser separada, porque não é inequivocamente fala de locutores.
     */
    @Test
    @DisplayName("Fronteira: \\N com travessão que NÃO é fala de locutores segue o caminho de sempre")
    void naoSeparaOQueNaoEDialogoDeLocutores() {
        for (String fala : Arrays.asList(
                "- Look!\\NThat's it.",               // o segundo não tem travessão
                "Look!\\N- That's it.",               // o primeiro não tem travessão
                "{\\i1}- Look!\\N- What?{\\i0}",        // tag de estilo: moldura tem dono próprio
                "- ...\\N- Right.",                   // trecho sem letra
                "- Only one speaker",                 // sem quebra
                "co-pilot\\N- Go",                    // hífen de palavra no começo não é travessão
                null)) {
            assertTrue(FalaDeLocutores.decompor(fala).isEmpty(), () -> "nao podia separar: " + fala);
        }
    }

    @Test
    @DisplayName("Locutor sem tradução: a fala inteira fica sem montagem (nunca meia traduzida)")
    void semTraducaoDeUmLocutorNaoMonta() {
        FalaDeLocutores fala = FalaDeLocutores.decompor("- This way.\\N- Right.").orElseThrow();
        assertTrue(fala.recompor(Arrays.asList("Por aqui.", null)).isEmpty());
        assertTrue(fala.recompor(List.of("Por aqui.", "  ")).isEmpty());
        assertTrue(fala.recompor(List.of("Por aqui.", "-")).isEmpty());
        assertTrue(fala.recompor(List.of("Por aqui.")).isEmpty());
    }

    @Test
    @DisplayName("Três locutores: as duas quebras ficam entre eles")
    void tresLocutores() {
        FalaDeLocutores fala = FalaDeLocutores.decompor("- Who?\\N- Me.\\N- Go!").orElseThrow();
        assertEquals(Optional.of("- Quem?\\N- Eu.\\N- Vai!"), fala.recompor(List.of("Quem?", "Eu.", "Vai!")));
    }
}
