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
     * A SEGUNDA LEVA de substantivos, medida no acervo em 19/08/2026 pelo
     * {@code MedicaoConcordanciaPorDicionarioIT}. Cada fala abaixo é do acervo real, não
     * inventada — foi assim que se descobriu que a lista curada de 24 palavras via 1 erro em
     * 332.545 falas enquanto estes passavam.
     */
    @Test
    @DisplayName("substantivos da segunda leva: falas REAIS do acervo que ninguem corrigia")
    void corrigeSubstantivosMedidosNoAcervo() {
        assertEquals(Optional.of("Em outras palavras, uma isca."),
            corretor.corrigir("Em outras palavras, um isca."));
        assertEquals(Optional.of("A base e uma isca."), corretor.corrigir("A base e um isca."));
        assertEquals(Optional.of("A cortina esta fechada."),
            corretor.corrigir("O cortina esta fechada."));
    }

    /**
     * O PLURAL, medido em 19/08/2026 antes de ser escrito: dos 238 pares distintos discordantes
     * do acervo, 27 eram de plural e a leitura separou <b>17 ocorrências de erro real</b> — as
     * falas abaixo são delas.
     */
    @Test
    @DisplayName("plural: determinante plural discordante e corrigido, falas REAIS do acervo")
    void corrigeDeterminantePlural() {
        assertEquals(Optional.of("as crianças"), corretor.corrigir("os crianças"));
        assertEquals(Optional.of("Aquelas crianças correram."), corretor.corrigir("Aqueles crianças correram."));
        assertEquals(Optional.of("Eu estava ajudando nos reparos."),
            corretor.corrigir("Eu estava ajudando nas reparos."));
        assertEquals(Optional.of("as botas"), corretor.corrigir("os botas"));
    }

    /**
     * A discordância de NÚMERO é outro defeito, e esta tela não o corrige. Se ela casasse
     * determinante singular com substantivo plural, {@code "o meninas"} viraria
     * {@code "a meninas"} — um erro trocado por outro, que é o oposto do contrato.
     */
    @Test
    @DisplayName("numero divergente NAO e tocado — corrigir genero ali trocaria um erro por outro")
    void naoTocaQuandoONumeroDiverge() {
        assertTrue(corretor.corrigir("o meninas chegaram").isEmpty());
        assertTrue(corretor.corrigir("os menina chegou").isEmpty());
    }

    /**
     * Os INDEFINIDOS, e a poda que a medição impôs à família ANTES de qualquer escrita.
     *
     * <p>Acrescentei sete pares de uma vez e medi no acervo: das 5 falas que apareceram, <b>3
     * eram falso positivo</b>. {@code "Você é muito criança"} está CERTO — ali {@code muito} é
     * advérbio invariável — e {@code "esses alvos são todos iscas"} concorda com <i>alvos</i>,
     * não com <i>iscas</i>. Ficaram só os que não têm essa ambiguidade.
     */
    @Test
    @DisplayName("indefinidos: 'algumas reparos' vira 'alguns reparos', e o adverbio fica intocado")
    void corrigeIndefinidosSemTocarNoAdverbio() {
        assertEquals(Optional.of("alguns reparos básicos"), corretor.corrigir("algumas reparos básicos"));
        assertEquals(Optional.of("muitas crianças"), corretor.corrigir("muitos crianças"));
        // ADVÉRBIO invariável: está certo, e mexer seria estragar.
        assertTrue(corretor.corrigir("Você é muito criança para entender isso.").isEmpty());
        // "todos" concorda com o antecedente "alvos", não com "iscas".
        assertTrue(corretor.corrigir("Esses alvos no solo são todos iscas.").isEmpty());
    }

    @Test
    @DisplayName("plural correto nao e tocado")
    void naoTocaPluralCorreto() {
        assertTrue(corretor.corrigir("as crianças correram").isEmpty());
        assertTrue(corretor.corrigir("os meninos chegaram").isEmpty());
        // "os caras" é a construção CORRETA que dominava o ruído da medição: 86 ocorrências.
        assertTrue(corretor.corrigir("Esses caras são perigosos.").isEmpty());
    }

    /**
     * A MEIA-CORREÇÃO que o teste pegou antes de virar dano: em {@code "Essa e a nossa orgulho."}
     * trocar só o possessivo devolveria {@code "a nosso orgulho"} — uma discordância NOVA entre
     * artigo e possessivo, onde antes havia uma só. E o artigo não pode ser trocado junto porque
     * o {@code a} também é preposição ({@code "entreguei a meu pai"} está certo).
     *
     * <p>A fala fica como está. Deixar a linha pior é o que a tela nunca pode fazer; deixá-la
     * como estava é pendência, e pendência é honesta.
     */
    @Test
    @DisplayName("possessivo precedido de artigo NAO e tocado — meia-correcao deixaria a linha pior")
    void naoFazMeiaCorrecaoQuandoHaArtigoAntesDoPossessivo() {
        assertTrue(corretor.corrigir("Essa e a nossa orgulho.").isEmpty());
        assertTrue(corretor.corrigir("Entreguei a meu pai.").isEmpty());
    }

    /**
     * Os POSSESSIVOS, que entraram junto: metade dos erros medidos tinha determinante possessivo,
     * e não artigo — {@code nossa orgulho}, {@code minha afeto}, {@code sua destino}.
     */
    @Test
    @DisplayName("possessivo tambem concorda: 'minha afeto' vira 'meu afeto'")
    void corrigePossessivoComSubstantivo() {
        assertEquals(Optional.of("meu afeto"), corretor.corrigir("minha afeto"));
        assertEquals(Optional.of("seu destino"), corretor.corrigir("sua destino"));
        assertEquals(Optional.of("Sua catapulta esta pronta."), corretor.corrigir("Seu catapulta esta pronta."));
    }

    /**
     * As palavras que a medição encontrou e que foram RECUSADAS de propósito — ambíguas ou de
     * gênero fixo contrário à terminação. Elas apareceram na lista de candidatos, e entrar nela
     * teria transformado construção correta em dano.
     */
    @Test
    @DisplayName("as ambiguas recusadas continuam intocadas — guia, figura, pirata, foto, mecha")
    void naoTocaAsAmbiguasRecusadas() {
        assertTrue(corretor.corrigir("O guia chegou.").isEmpty());
        assertTrue(corretor.corrigir("A guia chegou.").isEmpty());
        assertTrue(corretor.corrigir("O figura apareceu de novo.").isEmpty());
        assertTrue(corretor.corrigir("Um pirata do espaço.").isEmpty());
        assertTrue(corretor.corrigir("Uma foto antiga.").isEmpty());
        assertTrue(corretor.corrigir("Este mecha e novo.").isEmpty());
        assertTrue(corretor.corrigir("Aquele caça decolou.").isEmpty());
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
