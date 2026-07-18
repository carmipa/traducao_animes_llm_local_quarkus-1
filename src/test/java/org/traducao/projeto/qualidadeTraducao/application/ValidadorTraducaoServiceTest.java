package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorTraducaoServiceTest {

    private final ValidadorTraducaoService validador = new ValidadorTraducaoService();

    @Test
    void rejeitaRotuloTraducaoNoInicio() {
        // Caso real (Gundam Narrative): LLM rotulou a resposta em vez de só traduzir.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Tradução: {\\r\\pos(488,23)}ep"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Traducao : Ele nunca vai desistir."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Saída: {=68}{\\pos(1192,40)}"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Resposta: ele nunca vai desistir."));
    }

    @Test
    void rejeitaMarcadorErroTraducaoLegado() {
        // Caso real (G-Reconguista): marcador do pipeline Python antigo na legenda final.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("[ERRO_TRADUCAO: The Garanden!]"));
    }

    @Test
    void aceitaFalaComPalavraTraducaoNoMeio() {
        assertDoesNotThrow(() ->
            validador.validarFala("A tradução deste documento levou anos."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede o bug histórico em que o LLM, em vez de traduzir,
     * devolvia uma recusa ou meta-resposta ("não recebi nenhuma linha para traduzir",
     * "Sem tradução.") e esse texto ia direto para a legenda, no lugar da fala.
     * <p>INVARIANTES DO DOMÍNIO: a recusa em PT-BR (ou EN) é tratada como alucinação;
     * o chamador preserva o original. Casos reais extraídos do cache do software antigo
     * (Gundam 08th MS Team) e do MKV PT-BR do ep 6 ("Dunno").
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer recusa aceita reprova o teste.
     */
    @Test
    void rejeitaRecusaOuMetaRespostaDoLlm() {
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Desculpe, mas não recebi nenhuma linha para traduzir. "
                + "Por favor, forneça-me as linhas que deseja traduzir."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Sem tradução."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Nenhuma tradução encontrada para \"900\"."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("\"Kalent!\" -- tradução em português"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Desculpe, mas não tenho contexto para traduzir a linha \"Dunno\". "
                + "Por favor, pode me fornecer mais informações sobre onde essa linha é utilizada?"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Desculpe, mas preciso de mais contexto para traduzir essa linha. "
                + "Por favor, pode me fornecer o contexto da cena?"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Não tenho acesso ao contexto específico da fala que você considera relevante."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("As an AI, I cannot translate this without more context."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: garante que o detector de recusa não confunda diálogo
     * legítimo — falas reais de anime usam "por favor", "desculpe" e "não posso"
     * o tempo todo, e não podem ser preservadas sem tradução por engano.
     * <p>INVARIANTES DO DOMÍNIO: só a meta-referência à tarefa de tradução dispara o
     * bloqueio; "por favor"/"desculpe"/"não posso" sozinhos jamais bastam.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer fala legítima bloqueada reprova o teste.
     */
    @Test
    void aceitaDialogoLegitimoComPorFavorDesculpeENaoPosso() {
        assertDoesNotThrow(() -> validador.validarFala("Por favor, aguarde."));
        assertDoesNotThrow(() -> validador.validarFala("Desculpe-me, não."));
        assertDoesNotThrow(() -> validador.validarFala("Por favor, conecte-me ao Almirante Mauri."));
        assertDoesNotThrow(() -> validador.validarFala(
            "Se isso se transformar em uma zona de batalha, não posso garantir sua segurança."));
        assertDoesNotThrow(() -> validador.validarFala(
            "E não dê desculpas só porque Eledore não está aqui!"));
    }

    @Test
    void rejeitaResiduoInglesEmFalaMista() {
        // Caso real (86): linha metade PT, metade EN.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Se você terminou sua missão, it's seu dever me dar um relatório."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: cobre os resíduos reais que a Opção 6 declarou
     * incorretamente como conformes no Gundam Narrative.
     * <p>INVARIANTES DO DOMÍNIO: cada exemplo contém inglês visível fora da lore.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer fala aceita reprova o teste.
     */
    @Test
    void rejeitaResiduosReaisDoGundamNarrative() {
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Will transform a sociedade humana até seu núcleo."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Or rather, he thought."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Ensign Jona."));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Ensign Jona! Luta para recuperar a situação!"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Unknown, senhor! Incomensurável!"));
    }

    @Test
    void aceitaFalaLimpaEmPortugues() {
        assertDoesNotThrow(() ->
            validador.validarFala("Com força e esforço, vamos vencer esta batalha."));
    }

    @Test
    void aceitaComentarioAssEmInglesDentroDeChaves() {
        // Caso real (DanMachi): comentários de fansub no original são preservados
        // e não são texto visível — não podem disparar resíduo.
        assertDoesNotThrow(() ->
            validador.validarFala("Melhor levar-me com você. {Yes, ma'am}"));
        assertDoesNotThrow(() ->
            validador.validarFala("Vamos embora daqui. {it's a pun with the previous line}"));
    }

    @Test
    void rejeitaPreambuloDepoisDeTagAss() {
        // A âncora ^ do padrão de preâmbulo deve valer para o texto VISÍVEL,
        // não para a string crua começando com {\i1}.
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("{\\i1}Tradução: Ele nunca vai desistir.{\\i0}"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que a abreviação coloquial inglesa `Feds`
     * sobreviva numa fala declarada como PT-BR.
     * <p>INVARIANTES DO DOMÍNIO: `federais` permanece aceito; somente o token
     * inglês isolado é rejeitado.
     * <p>COMPORTAMENTO EM CASO DE FALHA: aceitação do resíduo reprova o teste.
     */
    @Test
    void rejeitaFedsComoResiduoIngles() {
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Mesmo que os Feds não sejam tão burros."));
        assertDoesNotThrow(() ->
            validador.validarFala("Nem os federais são tão burros."));
    }
}
