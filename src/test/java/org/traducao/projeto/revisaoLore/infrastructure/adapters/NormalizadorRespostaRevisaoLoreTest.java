package org.traducao.projeto.revisaoLore.infrastructure.adapters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a normalização das respostas do LLM de lore,
 * garantindo a paridade com o comportamento efetivo anterior — remoção de
 * raciocínio, cerca Markdown e rótulos, seleção de uma única linha e preservação
 * dos marcadores {@code [[TAGn]]}.
 * <p>INVARIANTES DO DOMÍNIO: nenhuma rede; apenas lógica de string pura.
 * <p>COMPORTAMENTO EM CASO DE FALHA: divergência de normalização reprova a suíte.
 */
class NormalizadorRespostaRevisaoLoreTest {

    private final NormalizadorRespostaRevisaoLore normalizador = new NormalizadorRespostaRevisaoLore();

    @Test
    @DisplayName("Remove bloco <think> e devolve a fala final")
    void removeRaciocinio() {
        String texto = "<think>vou revisar</think>\nShin chegou.";
        assertEquals("Shin chegou.", normalizador.normalizarLinhaUnica(texto, List.of()));
    }

    @Test
    @DisplayName("Remove cerca Markdown preservando o conteúdo")
    void removeCercaMarkdown() {
        String texto = "```\nShin chegou.\n```";
        assertEquals("Shin chegou.", normalizador.normalizarLinhaUnica(texto, List.of()));
    }

    @Test
    @DisplayName("Remove prefixos de rótulo (ex.: 'Tradução corrigida:')")
    void removePrefixos() {
        assertEquals("Shin chegou.",
            normalizador.normalizarLinhaUnica("Traducao corrigida: Shin chegou.", List.of()));
        assertEquals("Shin chegou.",
            normalizador.normalizarLinhaUnica("Resposta: Shin chegou.", List.of()));
    }

    @Test
    @DisplayName("Sem marcadores esperados, usa a última linha útil")
    void selecionaUltimaLinha() {
        assertEquals("segunda", normalizador.normalizarLinhaUnica("primeira\nsegunda", List.of()));
    }

    @Test
    @DisplayName("Preserva marcadores [[TAGn]] e escolhe a linha que os contém todos")
    void preservaMarcadores() {
        String texto = "explicacao sem tag\n[[TAG0]]Shin[[TAG1]] chegou.";
        assertEquals("[[TAG0]]Shin[[TAG1]] chegou.",
            normalizador.normalizarLinhaUnica(texto, List.of("[[TAG0]]", "[[TAG1]]")));
    }

    @Test
    @DisplayName("Rejeita (vazio) quando algum marcador esperado desaparece")
    void rejeitaQuandoMarcadorSome() {
        String texto = "[[TAG0]]Shin chegou.";
        assertEquals("", normalizador.normalizarLinhaUnica(texto, List.of("[[TAG0]]", "[[TAG1]]")));
    }

    @Test
    @DisplayName("Texto nulo ou vazio devolve string vazia")
    void nuloOuVazio() {
        assertEquals("", normalizador.normalizarLinhaUnica(null, List.of()));
        assertEquals("", normalizador.normalizarLinhaUnica("   ", List.of()));
    }

    @Test
    @DisplayName("extrairMarcadores preserva ordem e elimina duplicatas")
    void extraiMarcadores() {
        List<String> marcadores = normalizador.extrairMarcadores("[[TAG0]]a[[TAG1]]b[[TAG0]]");
        assertEquals(List.of("[[TAG0]]", "[[TAG1]]"), marcadores);
        assertTrue(normalizador.extrairMarcadores(null).isEmpty());
    }
}
