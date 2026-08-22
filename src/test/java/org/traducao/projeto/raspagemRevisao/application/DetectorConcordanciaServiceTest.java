package org.traducao.projeto.raspagemRevisao.application;

import org.junit.jupiter.api.DisplayName;
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

    // ==========================================================================================
    // FALSOS POSITIVOS medidos no acervo em 22/08/2026 — 67.178 pares EN/PT, 23 falas acusadas,
    // e SÓ QUATRO eram defeito de verdade. Cada bloco abaixo tem o caso doente (que tem de
    // continuar reprovando) junto do caso são (que parou de ser acusado), porque sem o par o
    // teste provaria só que a guarda ficou calada.
    // ==========================================================================================

    /**
     * PROPÓSITO DE NEGÓCIO: quando o inglês cita os DOIS gêneros, o masculino do português pode
     * estar traduzindo o {@code him} — e aí não há cruzamento nenhum.
     *
     * <p>As três falas do acervo nesta condição tinham tradução CORRETA. A mais clara:
     * {@code "making him her Adam"} -> {@code "fazer dele seu Adão"}, onde "dele" traduz o
     * {@code him} e "seu" traduz o {@code her}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: voltar a acusar aqui reenvia tradução correta ao LLM e
     * ensina o operador a ignorar o alarme.
     */
    @Test
    @DisplayName("him E her no mesmo original: o cruzamento de objeto nao acusa")
    void originalComOsDoisGenerosNaoAcusaCruzamentoDeObjeto() {
        assertFalse(detector.analisar(
            "Eve appears intent on making him her Adam.",
            "Mana parece determinada a fazer dele seu Adão.").suspeito());
        assertFalse(detector.analisar(
            "The newsman brought her back with him during our First Contact.",
            "O repórter a trouxe de volta com ele durante nosso Primeiro Contato.").suspeito());
        assertFalse(detector.analisar(
            "Seeing Amuro's heroics, Beltorchika willingly opened her heart to him.",
            "Vendo as proezas de Amuro, Beltorchika abriu o coração para ele.").suspeito());
    }

    /** CONTRA-CASO: com SÓ o feminino no original, o cruzamento continua sendo acusado. */
    @Test
    @DisplayName("so her no original: o cruzamento continua acusando")
    void originalComUmGeneroSoContinuaAcusandoOCruzamento() {
        assertTrue(detector.analisar(
            "I gave the letter to her.",
            "Eu dei a carta para ele.").suspeito(),
            "sem him no original, o 'para ele' é cruzamento de verdade");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: {@code cara} é vocativo masculino, mas também é ROSTO ("na cara
     * dela") e preço ("é cara"). A única acusação desta família no acervo era rosto.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acusar "na cara dela" manda ao LLM uma fala impecável.
     */
    @Test
    @DisplayName("cara com determinante e rosto, nao vocativo")
    void caraComDeterminanteNaoEVocativo() {
        ResultadoDeteccaoConcordancia rosto = detector.analisar(
            "Don't you ever say that to her face, got it?",
            "Você nunca fala isso na cara dela, entendeu?");
        assertFalse(rosto.suspeito(), () -> "motivos: " + rosto.motivos());
        ResultadoDeteccaoConcordancia propria = detector.analisar(
            "She looked at her own face.", "Ela olhou a própria cara.");
        assertFalse(propria.suspeito(), () -> "motivos: " + propria.motivos());
    }

    /** CONTRA-CASO: o vocativo de verdade — sem determinante — continua sendo acusado. */
    @Test
    @DisplayName("cara como VOCATIVO continua acusando")
    void caraComoVocativoContinuaAcusando() {
        assertTrue(detector.analisar(
            "Hey, she is right there!", "Ei, cara, ela está bem ali!").suspeito(),
            "sem determinante antes, 'cara' é tratamento e o original é feminino");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o adjetivo predicativo tem de concordar com a PESSOA de quem o
     * original fala — não com um substantivo qualquer da frase portuguesa.
     *
     * <p>Oito das 23 acusações do acervo vinham daqui, e as oito estavam certas: o adjetivo
     * concordava com "Algo", "Argama", "guerra", "causa", "era", "pessoa", ou era construção
     * impessoal.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acusar estas manda oito falas corretas ao modelo.
     */
    @Test
    @DisplayName("predicativo que concorda com substantivo da frase nao acusa")
    void predicativoDeOutroSubstantivoNaoAcusa() {
        assertFalse(detector.analisar(
            "Is something wrong with that girl?",
            "Algo está errado com aquela garota?").suspeito(), "errado <- Algo");
        assertFalse(detector.analisar(
            "Kou's a lost cause.", "A causa de Kou é perdida.").suspeito(), "perdida <- causa");
        assertFalse(detector.analisar(
            "That man is dead.", "Aquela pessoa esta morta.").suspeito(), "morta <- pessoa");
        assertFalse(detector.analisar(
            "Waiting for the right era? How typical of my brother.",
            "Esperando pela era certa? Quão típico do meu irmão.").suspeito(), "certa <- era");
        assertFalse(detector.analisar(
            "Is it okay to have her?", "É certo tê-la?").suspeito(), "construção impessoal");
    }

    /**
     * CONTRA-CASO do anterior, e o mais importante do bloco: o predicativo de sujeito ELÍPTICO
     * continua sendo acusado. É exatamente o defeito que a guarda existe para achar — sem este
     * teste, "parou de acusar" passaria como se fosse a correção.
     */
    @Test
    @DisplayName("predicativo de sujeito eliptico continua acusando")
    void predicativoDeSujeitoEllipticoContinuaAcusando() {
        assertTrue(detector.analisar("She is tired.", "Está cansado.").suspeito(),
            "sujeito elíptico com original feminino é o caso que a guarda existe para pegar");
        assertTrue(detector.analisar("He is tired.", "Está cansada.").suspeito(),
            "e o espelho masculino também");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: {@code Mistress} não casava {@code miss} — a palavra é "mistress",
     * não "miss" seguido de "tress" —, então o original ficava sem evidência feminina e as duas
     * falas do Macross II com "Senhora Chara" (tradução correta) eram acusadas.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: acusar "Senhora Chara" é acusar a tradução literal.
     */
    @Test
    @DisplayName("Mistress conta como referencia feminina no original")
    void mistressContaComoReferenciaFeminina() {
        assertFalse(detector.analisar(
            "Even without the help of Mistress Chara, Gottn will fulfill his duties splendidly!",
            "Mesmo sem a ajuda da Senhora Chara, Gottn cumprira suas deveres esplendidamente!")
            .suspeito());
    }

    /** CONTRA-CASO: sem NENHUMA referência feminina, o vocativo feminino continua acusando. */
    @Test
    @DisplayName("vocativo feminino sem referencia feminina continua acusando")
    void vocativoFemininoSemReferenciaFemininaContinuaAcusando() {
        assertTrue(detector.analisar(
            "Mr. Gottn will fulfill his duties.",
            "A senhora Gottn cumprirá seus deveres.").suspeito());
    }
}
