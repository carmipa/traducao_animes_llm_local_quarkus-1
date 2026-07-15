package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.raspagemRevisao.domain.ResultadoDeteccaoConcordancia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: comprova que a revisão automática encontra divergências
 * objetivas de gênero sem reescrever falas corretas por inferência do falante.
 *
 * <p>INVARIANTES DO DOMÍNIO: evidência explícita continua detectável; `I/you`
 * e palavras polissêmicas como `cara` não produzem falso positivo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer regressão reprova o teste antes
 * que uma proposta indevida alcance o cache operacional.
 */
class DetectorConcordanciaServiceTest {

    private final DetectorConcordanciaService detector = new DetectorConcordanciaService();

    @Test
    void textoLimpoNaoEhSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar("She said hi.", "ela disse oi.");
        assertFalse(r.suspeito());
        assertTrue(r.motivos().isEmpty());
    }

    @Test
    void traducaoNulaOuVaziaEhLimpo() {
        assertFalse(detector.analisar("She said", null).suspeito());
        assertFalse(detector.analisar("She said", "   ").suspeito());
    }

    @Test
    void artigoMasculinoComSubstantivoFemininoEhSuspeito() {
        ResultadoDeteccaoConcordancia r = detector.analisar(null, "aquele garota apareceu.");
        assertTrue(r.suspeito());
        assertFalse(r.motivos().isEmpty());
    }

    @Test
    void sujeitoElaComPredicadoMasculinoEhSuspeito() {
        assertTrue(detector.analisar(null, "ela está cansado.").suspeito());
    }

    @Test
    void sheNoOriginalMasEleNaTraducaoEhSuspeito() {
        assertTrue(detector.analisar("She smiled at me.", "ele sorriu para mim.").suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva gênero escolhido na tradução quando a fala
     * inglesa isolada não identifica quem está falando.
     * <p>INVARIANTES DO DOMÍNIO: primeira pessoa não permite inferir gênero.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
     */
    @Test
    void primeiraPessoaSemContextoDeGeneroNaoEhSuspeita() {
        assertFalse(detector.analisar("I am so tired.", "estou cansado.").suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede neutralização semântica de perguntas dirigidas
     * a personagem cujo gênero não está codificado na fala inglesa isolada.
     * <p>INVARIANTES DO DOMÍNIO: `you` não define gênero do interlocutor.
     * <p>COMPORTAMENTO EM CASO DE FALHA: marcação de `vivo` reprova o teste.
     */
    @Test
    void segundaPessoaSemContextoDeGeneroNaoEhSuspeita() {
        assertFalse(detector.analisar("Are you alive?!", "Você está vivo?!").suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva o uso brasileiro de `cara` como sinônimo
     * feminino de rosto em traduções naturais.
     * <p>INVARIANTES DO DOMÍNIO: `sua cara` possui concordância válida.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso conflito nominal reprova o teste.
     */
    @Test
    void suaCaraComoRostoNaoEhSuspeita() {
        assertFalse(detector.analisar("It's written all over your face.",
            "Isso fica estampado na sua cara.").suspeito());
    }

    @Test
    void objetoPronominalCorretoNaoGeraFalsoPositivo() {
        // "She told him" -> "Ela disse a ele": 'a ele' é objeto correto; não deve
        // marcar pelo 'ele' isolado (regra removerObjetoPronominal).
        ResultadoDeteccaoConcordancia r = detector.analisar("She told him.", "ela disse a ele.");
        assertFalse(r.suspeito(), () -> "não deveria marcar; motivos=" + r.motivos());
    }

    @Test
    void tagsAssNaoAtrapalhamADeteccao() {
        assertTrue(detector.analisar(null, "{\\i1}ela está cansado{\\i0}").suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: preserva expressões e possessivos naturais do PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: preposição de `graças a Deus` e `seu` ligado a
     * `nome` não concordam com o substantivo feminino anterior.
     * <p>COMPORTAMENTO EM CASO DE FALHA: falso positivo reprova o teste.
     */
    @Test
    void expressoesCorretasNaoGeramFalsoPositivo() {
        assertFalse(detector.analisar("Thank goodness.", "Graças a Deus.").suspeito());
        assertFalse(detector.analisar(
            "And Aina gave that girl her own name.",
            "E Aina deu àquela garota seu próprio nome.").suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: detecta troca inequívoca de pai por mãe sem depender do LLM.
     * <p>INVARIANTES DO DOMÍNIO: `dad` sem referência materna torna `mãe` incompatível.
     * <p>COMPORTAMENTO EM CASO DE FALHA: ausência do motivo reprova o teste.
     */
    @Test
    void parentescoInvertidoEhSuspeito() {
        assertTrue(detector.analisar(
            "Make sure my dad gets this info!", "Minha mãe precisa saber disso!").suspeito());
    }

    /**
     * PROPÓSITO DE NEGÓCIO: bloqueia palavrão aleatório, mas preserva insultos
     * fortes quando a fala inglesa contém esse tom explícito ou interrompido.
     * <p>INVARIANTES DO DOMÍNIO: `son of a hitch` é tratado como o eufemismo/erro
     * observado para `son of a bitch` e não deve ser suavizado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: classificação invertida reprova o teste.
     */
    @Test
    void respeitaIntensidadeDoInsultoOriginal() {
        assertTrue(detector.analisar("You are late!", "Filho da puta!").suspeito());
        assertFalse(detector.analisar("You son of a hitch!", "Filho da puta!").suspeito());
        assertFalse(detector.analisar("You son of a bitch!", "Filho da puta!").suspeito());
        assertTrue(detector.analisar("You son of a...!", "Filho da mãe!").suspeito());
    }
}
