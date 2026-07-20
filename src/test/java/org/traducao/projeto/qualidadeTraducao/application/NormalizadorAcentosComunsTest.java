package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link NormalizadorAcentosComuns} — repor acento só
 * nas formas que sem ele NUNCA são palavra válida, sem tocar homógrafos nem sentido.
 *
 * <p>INVARIANTES DO DOMÍNIO: só o dicionário curado; fronteira de palavra; caixa preservada;
 * homógrafos ({@code esta}, {@code e}, {@code as vezes}) intocados.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer correção indevida ou faltante reprova.
 */
class NormalizadorAcentosComunsTest {

    private final NormalizadorAcentosComuns norm = new NormalizadorAcentosComuns();

    @Test
    @DisplayName("repõe acento em formas inequívocas (nao/voce/tambem/ate)")
    void corrigeFormasInequivocas() {
        assertEquals("Não, você também vem até aqui?",
            norm.normalizar("Nao, voce tambem vem ate aqui?"));
    }

    @Test
    @DisplayName("repõe infância (caso do 08th)")
    void corrigeInfancia() {
        assertEquals("Desde a infância, tantos dias.",
            norm.normalizar("Desde a infancia, tantos dias."));
    }

    @Test
    @DisplayName("preserva a caixa do achado (primeira maiúscula e tudo maiúsculo)")
    void preservaCaixa() {
        assertEquals("Ninguém! NÃO!", norm.normalizar("Ninguem! NAO!"));
    }

    @Test
    @DisplayName("distingue voce de voces (mais longa primeiro)")
    void distingueVoceDeVoces() {
        assertEquals("vocês e você", norm.normalizar("voces e voce"));
    }

    @Test
    @DisplayName("NÃO toca homógrafos nem palavras válidas (esta/e/as vezes)")
    void naoTocaHomografos() {
        // 'esta'(this), 'e'(and), 'as vezes'(the times) são ambíguos: ficam intocados.
        String s = "esta e as vezes que lutei";
        assertEquals("esta e as vezes que lutei", norm.normalizar(s));
    }

    @Test
    @DisplayName("NÃO casa fragmento dentro de palavra (ate em Kate/mate)")
    void naoCasaFragmento() {
        assertEquals("Kate comeu mate", norm.normalizar("Kate comeu mate"));
    }

    @Test
    @DisplayName("preserva tags de estilo ao corrigir")
    void preservaTags() {
        assertEquals("{\\i1}Não vou além.", norm.normalizar("{\\i1}Nao vou alem."));
    }
}
