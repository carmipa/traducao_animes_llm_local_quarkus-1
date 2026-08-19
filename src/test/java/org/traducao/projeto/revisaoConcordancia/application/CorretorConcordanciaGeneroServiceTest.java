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

    /**
     * O caso original desta asserção era {@code "o menina viu a menino"}. O segundo erro —
     * {@code "a menino"} — deixou de ser corrigido em 18/08/2026, e isso é DELIBERADO: o
     * {@code a} saiu do padrão porque, no acervo real, ele nunca apareceu como artigo errado e
     * apareceu 15 vezes como preposição CERTA ({@code "Graças a Deus"}). Ver
     * {@code CorretorConcordanciaGeneroService.ART_FEM_NO_PADRAO} e o teste
     * {@link #naoEstragaPreposicaoAAntesDeSubstantivoMasculino()}.
     *
     * <p>A intenção do teste — dois erros na MESMA linha — continua exercitada, agora com um
     * determinante que não é ambíguo com preposição.
     */
    @Test
    @DisplayName("corrige múltiplos erros de gênero na mesma linha")
    void corrigeMultiplosErrosNaMesmaLinha() {
        assertEquals(Optional.of("a menina viu um menino"), corretor.corrigir("o menina viu uma menino"));
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

    /**
     * O 'a' antes de substantivo masculino é PREPOSIÇÃO, não artigo — e está certo.
     *
     * <p>As três falas abaixo são do acervo real, não inventadas. Medido em 18/08/2026 sobre
     * 726 legendas: a tela mudaria 15 falas, e <b>14 eram "a Deus" correto</b> contra 1 conserto
     * verdadeiro. Este teste é o que impede o 'a' de voltar para o lado feminino do padrão.
     */
    @Test
    @DisplayName("preposição 'a' antes de substantivo masculino NÃO vira artigo masculino")
    void naoEstragaPreposicaoAAntesDeSubstantivoMasculino() {
        assertTrue(corretor.corrigir("Graças a Deus, você está vivo.").isEmpty());
        assertTrue(corretor.corrigir("Ore a Deus, não a mim.").isEmpty());
        assertTrue(corretor.corrigir("Vamos conversar de homem a homem.").isEmpty());
    }

    /**
     * CÓPIA CONSCIENTE da 3.1 (ordem de Paulo, 18/08/2026): possessivo de parentesco, que é
     * concordância de gênero pura e não precisa do inglês.
     *
     * <p>Ganho medido no acervo: ZERO — nenhuma das 332.545 falas tem o caso. Está aqui para a
     * tradução de amanhã, e o zero fica declarado em vez de escondido.
     */
    @Test
    @DisplayName("possessivo de parentesco: 'minha pai' vira 'meu pai', e o espelho tambem")
    void corrigePossessivoDeParentesco() {
        assertEquals(Optional.of("Meu pai chegou cedo."), corretor.corrigir("Minha pai chegou cedo."));
        assertEquals(Optional.of("minha mãe voltou."), corretor.corrigir("meu mãe voltou."));
        assertEquals(Optional.of("Vi sua irmã ontem."), corretor.corrigir("Vi seu irmã ontem."));
    }

    @Test
    @DisplayName("possessivo correto e parentesco no genero certo nao sao tocados")
    void naoTocaPossessivoCorreto() {
        assertTrue(corretor.corrigir("Meu pai chegou cedo.").isEmpty());
        assertTrue(corretor.corrigir("Minha mãe voltou.").isEmpty());
        // "meu irmão" e correto — e foi exatamente o que um grep com \b sobre texto multibyte
        // acusou como "meu irmã" (122 falsos positivos) antes de olharem a linha inteira.
        assertTrue(corretor.corrigir("Ele é meu irmão.").isEmpty());
    }

    /**
     * A expressão idiomática, com a DIVERGÊNCIA declarada em relação ao original da 3.1: aqui a
     * forma sem acento também é aceita, porque a aya-expanse produz texto sem acento (197 casos
     * medidos no Unicorn).
     */
    @Test
    @DisplayName("'gracas ao deus' vira 'gracas a Deus' — com e sem acento na entrada")
    void corrigeExpressaoIdiomatica() {
        assertEquals(Optional.of("Graças a Deus, você está vivo."),
            corretor.corrigir("Graças ao deus, você está vivo."));
        assertEquals(Optional.of("Graças a Deus, você está vivo."),
            corretor.corrigir("Gracas ao deus, você está vivo."));
    }

    @Test
    @DisplayName("a forma correta da expressao continua intocada")
    void naoTocaExpressaoCorreta() {
        assertTrue(corretor.corrigir("Graças a Deus, você está vivo.").isEmpty());
        assertTrue(corretor.corrigir("Ore a Deus, não a mim.").isEmpty());
    }

    /**
     * O contra-teste da correção acima: é ele que separa "parou de estragar" de "parou de
     * funcionar". Sem esta linha, apagar o padrão inteiro passaria no teste anterior.
     *
     * <p>A segunda asserção é a ÚNICA correção legítima que a medição encontrou no acervo
     * inteiro — {@code "Aquela garoto poderia ser um Newtype."}, Gundam ZZ.
     */
    @Test
    @DisplayName("determinante feminino de verdade antes de substantivo masculino continua corrigido")
    void continuaCorrigindoDeterminanteFemininoDeVerdade() {
        assertEquals(Optional.of("Chamei um menino."), corretor.corrigir("Chamei uma menino."));
        assertEquals(Optional.of("Aquele garoto poderia ser um Newtype."),
            corretor.corrigir("Aquela garoto poderia ser um Newtype."));
        assertEquals(Optional.of("Vi a menina no parque."), corretor.corrigir("Vi o menina no parque."));
    }
}
