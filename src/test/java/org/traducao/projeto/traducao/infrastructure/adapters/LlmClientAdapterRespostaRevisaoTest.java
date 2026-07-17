package org.traducao.projeto.traducao.infrastructure.adapters;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: garante que respostas do Tower/Mistral com raciocínio
 * ou formatação auxiliar entreguem somente a fala final à revisão de legendas.
 *
 * <p>INVARIANTES DO DOMÍNIO: todos os marcadores ASS esperados permanecem na
 * saída e explicações do modelo nunca entram na legenda.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: resposta incompatível produz texto vazio,
 * obrigando o cliente a tentar novamente em vez de publicar estrutura quebrada.
 */
class LlmClientAdapterRespostaRevisaoTest {

    /**
     * PROPÓSITO DE NEGÓCIO: aceita a fala final após o bloco de raciocínio do modelo.
     * <p>INVARIANTES DO DOMÍNIO: os dois marcadores continuam presentes e ordenados.
     * <p>COMPORTAMENTO EM CASO DE FALHA: divergência reprova o teste.
     */
    @Test
    void removeRaciocinioEPrefixoPreservandoTags() {
        String resposta = """
            <think>Vou analisar a frase antes de responder.</think>
            Tradução corrigida: Os federais não são [[TAG0]]tão[[TAG1]] burros.
            """;

        String normalizada = LlmClientAdapter.normalizarLinhaUnica(
            resposta, List.of("[[TAG0]]", "[[TAG1]]"));

        assertEquals("Os federais não são [[TAG0]]tão[[TAG1]] burros.", normalizada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: recupera a tradução contida em cerca Markdown.
     * <p>INVARIANTES DO DOMÍNIO: a cerca e seu identificador não chegam ao ASS.
     * <p>COMPORTAMENTO EM CASO DE FALHA: conteúdo divergente reprova o teste.
     */
    @Test
    void removeCercaMarkdown() {
        String normalizada = LlmClientAdapter.normalizarLinhaUnica(
            "```text\nUma unidade móvel...\n```", List.of());

        assertEquals("Uma unidade móvel...", normalizada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: seleciona a fala após uma explicação introdutória.
     * <p>INVARIANTES DO DOMÍNIO: somente a última linha útil vira legenda.
     * <p>COMPORTAMENTO EM CASO DE FALHA: explicação publicada reprova o teste.
     */
    @Test
    void selecionaUltimaLinhaUtilSemTags() {
        String normalizada = LlmClientAdapter.normalizarLinhaUnica(
            "Segue a revisão solicitada:\nGraças a Deus.", List.of());

        assertEquals("Graças a Deus.", normalizada);
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede publicação quando o modelo perde uma tag ASS.
     * <p>INVARIANTES DO DOMÍNIO: ausência de qualquer marcador invalida a resposta.
     * <p>COMPORTAMENTO EM CASO DE FALHA: devolve vazio para acionar nova tentativa.
     */
    @Test
    void rejeitaRespostaQuePerdeMarcador() {
        String normalizada = LlmClientAdapter.normalizarLinhaUnica(
            "Os federais não são tão burros.", List.of("[[TAG0]]", "[[TAG1]]"));

        assertEquals("", normalizada);
    }
}
