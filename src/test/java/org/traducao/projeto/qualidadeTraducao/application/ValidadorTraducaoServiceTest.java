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
     * PROPÓSITO DE NEGÓCIO: impede que o LLM censure palavrão ou vaze markdown com asterisco
     * na legenda. Casos reais (ZZ/08th/Narrative): "Damn it!" -> "Merd**a**!", "God dammit!" ->
     * "Merd**", "You bastards!" -> "Vocês são uns *****es!". Neste projeto a fonte NUNCA traz
     * '*' e ações usam parênteses; logo '*' no texto visível é sempre artefato do modelo e a
     * fala deve ficar pendente (retraduz), nunca censurada/pontilhada na legenda.
     * <p>INVARIANTES DO DOMÍNIO: qualquer '*' visível dispara alucinação; palavrão por extenso
     * (sem asterisco) é tradução legítima e passa.
     * <p>COMPORTAMENTO EM CASO DE FALHA: asterisco aceito na legenda reprova.
     */
    @Test
    void rejeitaCensuraOuMarkdownComAsterisco() {
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Merd**a**!"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Merd**"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("Vocês são uns *****es!"));
        assertThrows(AlucinacaoDetectadaException.class, () ->
            validador.validarFala("{\\i1}Merd*, me larga!"));
    }

    @Test
    void aceitaPalavraoPorExtensoSemAsterisco() {
        // O objetivo do fix: palavrão traduzido fielmente, sem censura, é legítimo.
        assertDoesNotThrow(() -> validador.validarFala("Merda! Temos que pará-lo!"));
        assertDoesNotThrow(() -> validador.validarFala("Malditos! Eles me pegaram!"));
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

    /**
     * PROPÓSITO DE NEGÓCIO: caso REAL (Gundam 08th MS Team, ep 1) — a fonte
     * "About who I am and what I'm capable of." fez o LLM RECITAR o próprio prompt de
     * sistema em vez de traduzir, vazando um parágrafo como "Sou capaz de traduzir
     * legendas de anime do inglês para o português do Brasil, preservando sentido,
     * subtexto, intenção emocional...". Como está em PT e preserva a estrutura ASS,
     * escapava da validação e ia PARA A LEGENDA. É uma classe de meta-resposta distinta
     * da recusa: RECITAÇÃO DE CAPACIDADE/PAPEL do tradutor (eco do prompt de sistema em
     * {@code ContextoPrompt}).
     * <p>INVARIANTES DO DOMÍNIO: recitar a tarefa de tradução (traduzir legendas /
     * legendas de anime / intenção emocional — vocabulário do prompt) é meta-resposta e
     * vira alucinação; o chamador preserva o original.
     * <p>COMPORTAMENTO EM CASO DE FALHA (antes do fix): o vazamento é aceito e vai para a
     * legenda — este teste falha.
     */
    @Test
    void rejeitaRecitacaoDoPromptDeTraducao() {
        assertThrows(AlucinacaoDetectadaException.class, () -> validador.validarFala(
            "Sou capaz de traduzir legendas de anime do inglês para o português do Brasil, "
                + "preservando sentido, subtexto, intenção emocional e continuidade da cena."));
        assertThrows(AlucinacaoDetectadaException.class, () -> validador.validarFala(
            "Minha função é traduzir legendas de anime mantendo os marcadores de formatação."));
        assertThrows(AlucinacaoDetectadaException.class, () -> validador.validarFala(
            "Preservo o subtexto e a intenção emocional de cada cena."));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: o detector de recitação de prompt NÃO pode confundir diálogo
     * legítimo — "sou capaz de", "preservando" e "emocional" aparecem em fala normal de
     * anime e jamais podem virar meta-resposta por engano. Só a meta-referência à TAREFA
     * de tradução (traduzir legendas / legendas de anime / intenção emocional) dispara.
     * <p>INVARIANTES DO DOMÍNIO: mantém a calibração histórica (recusa real x fala legítima).
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer fala legítima bloqueada reprova o teste.
     */
    @Test
    void aceitaDialogoLegitimoQueNaoRecitaTarefaDeTraducao() {
        assertDoesNotThrow(() -> validador.validarFala("Sou capaz de derrotá-lo sozinho!"));
        assertDoesNotThrow(() -> validador.validarFala("Preservando a honra da família, seguirei em frente."));
        assertDoesNotThrow(() -> validador.validarFala("Ela é uma pessoa muito emocional."));
        assertDoesNotThrow(() -> validador.validarFala("Preciso preservar as evidências da cena do crime."));
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
