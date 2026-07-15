package org.traducao.projeto.traducao.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a semântica da normalização proprietária da chave de
 * episódio (proprietário único da Tradução Local), cobrindo espaços, caixa,
 * Unicode, extensão, números e nomes semelhantes.
 * <p>INVARIANTES DO DOMÍNIO: normalização conservadora e determinística; não funde
 * episódios distintos.
 * <p>COMPORTAMENTO EM CASO DE FALHA: divergência reprova a suíte.
 */
class NormalizadorNomeEpisodioTest {

    @Test
    @DisplayName("Espaços redundantes e diretórios são ignorados")
    void espacosEDiretorios() {
        assertEquals(
            NormalizadorNomeEpisodio.normalizar("ep01.ass"),
            NormalizadorNomeEpisodio.normalizar("  ep01.ass  "));
        assertEquals(
            NormalizadorNomeEpisodio.normalizar("ep01.ass"),
            NormalizadorNomeEpisodio.normalizar("pasta/sub\\ep01.ass"));
        // Espaço interno É significativo (não colapsa para vazio): distingue de "ep01".
        assertNotEquals(
            NormalizadorNomeEpisodio.normalizar("ep 01.ass"),
            NormalizadorNomeEpisodio.normalizar("ep01.ass"));
    }

    @Test
    @DisplayName("Caixa é insensível")
    void caixa() {
        assertEquals(
            NormalizadorNomeEpisodio.normalizar("ep01.ass"),
            NormalizadorNomeEpisodio.normalizar("EP01.ASS"));
    }

    @Test
    @DisplayName("Unicode composto e decomposto normalizam para a mesma chave (NFC)")
    void unicode() {
        String nfc = Normalizer.normalize("Walküre.ass", Normalizer.Form.NFC);
        String nfd = Normalizer.normalize("Walküre.ass", Normalizer.Form.NFD);
        assertNotEquals(nfc, nfd); // formas físicas diferentes
        assertEquals(
            NormalizadorNomeEpisodio.normalizar(nfc),
            NormalizadorNomeEpisodio.normalizar(nfd));
    }

    @Test
    @DisplayName("Extensões diferentes NÃO se fundem (ass != srt)")
    void extensao() {
        assertNotEquals(
            NormalizadorNomeEpisodio.normalizar("ep01.ass"),
            NormalizadorNomeEpisodio.normalizar("ep01.srt"));
    }

    @Test
    @DisplayName("Números distintos NÃO se fundem (ep1 != ep11)")
    void numeros() {
        assertNotEquals(
            NormalizadorNomeEpisodio.normalizar("ep1.ass"),
            NormalizadorNomeEpisodio.normalizar("ep11.ass"));
    }

    @Test
    @DisplayName("Nomes semelhantes permanecem distintos")
    void nomesSemelhantes() {
        assertNotEquals(
            NormalizadorNomeEpisodio.normalizar("ep01.ass"),
            NormalizadorNomeEpisodio.normalizar("ep01a.ass"));
    }

    @Test
    @DisplayName("Idempotência e nulo/branco")
    void idempotenciaENulo() {
        String uma = NormalizadorNomeEpisodio.normalizar("Pasta/EP 01.ASS");
        assertEquals(uma, NormalizadorNomeEpisodio.normalizar(uma));
        assertEquals("", NormalizadorNomeEpisodio.normalizar(null));
        assertEquals("", NormalizadorNomeEpisodio.normalizar("   "));
    }
}
