package org.traducao.projeto.core.texto.dicionarioOrtografia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: o tokenizador do dicionário separa palavras nas quebras do ASS. Sem isso
 * {@code "Dreamer...\NSonhador..."} dava {@code "NSonhador"} e o achatador colava o par (0083,
 * 27:00.47, 24/09/2026).
 *
 * <p>A1: o mesmo sinal superficial — um {@code N} maiúsculo no começo da palavra — continua
 * intacto quando é letra da palavra ({@code "Nada"}, {@code "Não"}), com ou sem quebra antes.
 */
class PalavrasDeSeparaQuebraAssTest {

    @Test
    void quebraDoAssSeparaAsPalavras() {
        assertEquals(List.of("Dreamer", "Sonhador"), CorretorOrtograficoLegenda.palavrasDe("Dreamer...\\NSonhador..."));
        assertEquals(List.of("fate", "Não", "posso"), CorretorOrtograficoLegenda.palavrasDe("fate\\NNão posso"));
        assertEquals(List.of("soft", "break"), CorretorOrtograficoLegenda.palavrasDe("soft\\nbreak"));
        assertEquals(List.of("espaco", "fixo"), CorretorOrtograficoLegenda.palavrasDe("espaco\\hfixo"));
    }

    @Test
    void enemMaiusculoDaPropriaPalavraFicaIntacto() {
        assertEquals(List.of("Nada", "Não", "Nunca"), CorretorOrtograficoLegenda.palavrasDe("Nada Não, Nunca"));
        assertEquals(List.of("NHK", "Naomi"), CorretorOrtograficoLegenda.palavrasDe("NHK\\N\\NNaomi"));
    }
}
