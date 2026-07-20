package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link NormalizadorAspasService} — remover aspas de
 * borda que a FONTE não tinha, sem corromper aspas legítimas ou múltiplos segmentos citados.
 *
 * <p>INVARIANTES DO DOMÍNIO: só remove envelope de aspas quando o PT está envolto e o EN não;
 * um único par; tags de borda preservadas; aspas simples internas preservadas.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer remoção indevida ou preservação faltante reprova.
 */
class NormalizadorAspasServiceTest {

    private final NormalizadorAspasService norm = new NormalizadorAspasService();

    @Test
    @DisplayName("remove aspas de borda quando a fonte (EN) não tinha")
    void removeAspasQuandoFonteNaoTinha() {
        assertEquals("Claro!", norm.normalizar("Of course!", "\"Claro!\""));
    }

    @Test
    @DisplayName("mantém aspas quando a fonte (EN) já as tinha")
    void mantemAspasQuandoFonteTinha() {
        assertEquals("\"Fonte citada.\"", norm.normalizar("\"Quoted source.\"", "\"Fonte citada.\""));
    }

    @Test
    @DisplayName("preserva as tags de estilo de borda ao remover as aspas")
    void preservaTagsDeBorda() {
        assertEquals("{\\i1}Oi{\\i0}", norm.normalizar("Hi", "{\\i1}\"Oi\"{\\i0}"));
    }

    @Test
    @DisplayName("não toca fala que não está entre aspas")
    void naoTocaFalaSemAspas() {
        assertEquals("Sim, senhor.", norm.normalizar("Yes, sir.", "Sim, senhor."));
    }

    @Test
    @DisplayName("não remove quando há dois segmentos citados (não é envelope)")
    void naoRemoveQuandoHaDoisSegmentosCitados() {
        // "A" e "B" tem 4 aspas: remover a borda viraria 'A" e "B' — corrupção.
        assertEquals("\"A\" e \"B\"", norm.normalizar("Just A and B", "\"A\" e \"B\""));
    }

    @Test
    @DisplayName("preserva aspas simples internas ao remover o par duplo de borda")
    void preservaAspasSimplesInternas() {
        assertEquals("Um 'brinquedo'?!", norm.normalizar("A 'toy'?!", "\"Um 'brinquedo'?!\""));
    }

    @Test
    @DisplayName("remove aspas curvas de borda (“ ”) que a fonte não tinha")
    void removeAspasCurvas() {
        assertEquals("Bem-vindo.", norm.normalizar("Welcome.", "“Bem-vindo.”"));
    }
}
