package org.traducao.projeto.legenda.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: caracteriza a regra pura {@link PoliticaEstiloMusical#estiloIgnorado(String)}
 * herdada de {@code TradutorProperties.estiloIgnorado} — lista configurada + heurísticas
 * + regex de fronteira de palavra — travando o comportamento HISTÓRICO exato após o move
 * para o módulo {@code legenda}.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Match de lista é case-insensitive; heurística e regex idênticas ao comportamento anterior.</li>
 *   <li>{@code null}/blank → {@code false}; a política não decide sozinha o envio ao LLM.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Divergência de qualquer caso reprova, sinalizando quebra da regra migrada.
 */
class PoliticaEstiloMusicalTest {

    private static final PoliticaEstiloMusical COM_LISTA =
        new PoliticaEstiloMusical(List.of("Song JP", "Char's Counterattack"));
    private static final PoliticaEstiloMusical LISTA_VAZIA =
        new PoliticaEstiloMusical(List.of());

    @Test
    @DisplayName("lista configurada: match direto (e case-insensitive)")
    void listaConfigurada() {
        assertTrue(COM_LISTA.estiloIgnorado("Song JP"));
        assertTrue(COM_LISTA.estiloIgnorado("Char's Counterattack"));
        assertTrue(COM_LISTA.estiloIgnorado("SONG JP"), "lista deve ser case-insensitive");
        assertTrue(COM_LISTA.estiloIgnorado("song jp"), "lista deve ser case-insensitive");
    }

    @Test
    @DisplayName("null e blank retornam false")
    void nullEBlank() {
        assertFalse(COM_LISTA.estiloIgnorado(null));
        assertFalse(COM_LISTA.estiloIgnorado(""));
        assertFalse(COM_LISTA.estiloIgnorado("   "));
    }

    @Test
    @DisplayName("lista vazia: heurística/regex continuam valendo, independentes da lista")
    void listaVazia() {
        assertTrue(LISTA_VAZIA.estiloIgnorado("Song JP"), "heurística 'song' independe da lista");
        assertTrue(LISTA_VAZIA.estiloIgnorado("Karaoke"));
        assertFalse(LISTA_VAZIA.estiloIgnorado("Char's Counterattack"), "sem lista, não é musical por si só");
        assertFalse(LISTA_VAZIA.estiloIgnorado("Default"));
    }

    @Test
    @DisplayName("palavras-chave musicais (case-insensitive por contains)")
    void palavrasChave() {
        assertTrue(COM_LISTA.estiloIgnorado("Karaoke"));
        assertTrue(COM_LISTA.estiloIgnorado("Romaji"));
        assertTrue(COM_LISTA.estiloIgnorado("Insert"));
        assertTrue(COM_LISTA.estiloIgnorado("Opening Theme"));
        assertTrue(COM_LISTA.estiloIgnorado("OP - Romaji"), "contém 'romaji'");
    }

    @Test
    @DisplayName("abreviações OP/ED como token isolado (regex de fronteira de palavra)")
    void abreviacoesOpEd() {
        assertTrue(COM_LISTA.estiloIgnorado("OP"));
        assertTrue(COM_LISTA.estiloIgnorado("ED"));
        assertTrue(COM_LISTA.estiloIgnorado("ED-ROM"), "hífen é fronteira de palavra");
        assertTrue(COM_LISTA.estiloIgnorado("OP - English"), "'OP' isolado casa a regex");
    }

    @Test
    @DisplayName("variações numéricas coladas NÃO são capturadas (fronteira de palavra — comportamento histórico)")
    void variacoesNumericasColadas() {
        // "OP1"/"ED2" não têm fronteira entre a letra e o dígito: a regex \bop\b/\bed\b
        // exige token isolado. Comportamento HISTÓRICO preservado (E3c não altera a regra).
        assertFalse(COM_LISTA.estiloIgnorado("OP1"));
        assertFalse(COM_LISTA.estiloIgnorado("ED2"));
    }

    @Test
    @DisplayName("estilos não musicais e falsos positivos evitados")
    void naoMusicais() {
        assertFalse(COM_LISTA.estiloIgnorado("Default"));
        assertFalse(COM_LISTA.estiloIgnorado("Dialogue"));
        assertFalse(COM_LISTA.estiloIgnorado("Sign"), "'sign' não contém 'sing'");
        assertFalse(COM_LISTA.estiloIgnorado("Title"));
    }
}
