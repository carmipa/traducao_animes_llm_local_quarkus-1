package org.traducao.projeto.revisaoConcordancia.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o contrato do {@link CorretorConcordanciaGeneroService} — corrigir
 * gênero inequívoco (artigo↔substantivo e predicativo de ela/ele) sem tocar o ambíguo.
 *
 * <p>INVARIANTES DO DOMÍNIO: só gênero conhecido; caixa preservada; fala correta e ambíguo intocados.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer troca indevida ou faltante reprova.
 */
class CorretorConcordanciaGeneroServiceTest {

    private final CorretorConcordanciaGeneroService corretor = new CorretorConcordanciaGeneroService();

    @Test
    @DisplayName("artigo masculino antes de substantivo feminino → flip do artigo")
    void corrigeArtigoMascComSubstFem() {
        assertEquals(Optional.of("Vi a menina no parque."), corretor.corrigir("Vi o menina no parque."));
    }

    @Test
    @DisplayName("artigo feminino antes de substantivo masculino → flip do artigo")
    void corrigeArtigoFemComSubstMasc() {
        assertEquals(Optional.of("Chamei um menino."), corretor.corrigir("Chamei uma menino."));
    }

    @Test
    @DisplayName("preserva a caixa inicial do artigo trocado")
    void preservaCaixaDoArtigo() {
        assertEquals(Optional.of("A menina chegou."), corretor.corrigir("O menina chegou."));
    }

    @Test
    @DisplayName("predicativo de 'ela' no masculino → feminino")
    void corrigePredicativoEla() {
        assertEquals(Optional.of("Ela está cansada."), corretor.corrigir("Ela está cansado."));
    }

    @Test
    @DisplayName("predicativo de 'ele' no feminino → masculino")
    void corrigePredicativoEle() {
        assertEquals(Optional.of("Ele parece perdido."), corretor.corrigir("Ele parece perdida."));
    }

    @Test
    @DisplayName("fala correta não é alterada")
    void naoTocaFalaCorreta() {
        assertTrue(corretor.corrigir("A menina está cansada.").isEmpty());
    }

    @Test
    @DisplayName("substantivo ambíguo/fora da lista não é tocado")
    void naoTocaSubstantivoAmbiguo() {
        // "problema" (masc) não está na lista de gênero inequívoco → nada muda.
        assertTrue(corretor.corrigir("o problema era grande").isEmpty());
    }

    @Test
    @DisplayName("preserva as tags ASS de borda ao corrigir")
    void preservaTagsAss() {
        assertEquals(Optional.of("{\\i1}A menina chegou."), corretor.corrigir("{\\i1}O menina chegou."));
    }

    @Test
    @DisplayName("corrige múltiplos erros de gênero na mesma linha")
    void corrigeMultiplosErrosNaMesmaLinha() {
        assertEquals(Optional.of("a menina viu o menino"), corretor.corrigir("o menina viu a menino"));
    }

    @Test
    @DisplayName("concordância correta (fem+fem, masc+masc) não é tocada")
    void naoTocaConcordanciaCorreta() {
        assertTrue(corretor.corrigir("A amiga e o amigo chegaram.").isEmpty());
    }

    @Test
    @DisplayName("não casa artigo como fragmento de outra palavra (sem espaço)")
    void naoCasaFragmentoDePalavra() {
        // "domina" contém "do"+"mina", mas sem espaço entre artigo e substantivo não há erro.
        assertTrue(corretor.corrigir("Ela domina a arte.").isEmpty());
    }

    @Test
    @DisplayName("predicativo com verbo variado (parece/é) também é corrigido")
    void corrigePredicativoComVerboVariado() {
        assertEquals(Optional.of("Ela parece cansada."), corretor.corrigir("Ela parece cansado."));
        assertEquals(Optional.of("Ele é perdido."), corretor.corrigir("Ele é perdida."));
    }
}
