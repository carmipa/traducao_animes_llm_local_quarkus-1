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

    /**
     * PROPÓSITO DE NEGÓCIO: variação numérica colada É música, e passou a ser capturada.
     *
     * <h2>Inversão deliberada em 07/08/2026, autorizada por Paulo</h2>
     * Este teste afirmava o CONTRÁRIO até esta data: {@code assertFalse} em {@code OP1} e
     * {@code ED2}, com a nota "comportamento HISTÓRICO preservado (E3c não altera a regra)".
     * O comportamento histórico era o DEFEITO — {@code \bed\b} não alcança {@code ED2} porque
     * o dígito é caractere de palavra, e {@code ED2} é música de verdade: 207 linhas no acervo.
     *
     * <p>A divergência que isso criava foi medida por {@code MedicaoDivergenciaPadraoMusicalIT}:
     * <b>1.385 linhas em 1.719.242 falas</b>, onde {@code PoliticaEstiloMusical} dizia "não é
     * música" e {@code DetectorEfeitoKaraokeService} dizia que era. Na auditoria do mesmo dia,
     * isso produziu 364 falsos "defeitos de tradução" no Gundam Unicorn.
     *
     * <p>Com a convergência em {@code PadraoEstiloMusical}, a medição foi a ZERO. As 1.385
     * linhas afetadas estão em 8 obras — Guilty Crown (633), Gundam ZZ (460), Unicorn (207),
     * DanMachi (70), Gundam 0083 (15).
     */
    @Test
    @DisplayName("variação numérica colada É capturada (fronteira de LETRA, desde 07/08/2026)")
    void variacoesNumericasColadas() {
        assertTrue(COM_LISTA.estiloIgnorado("OP1"),
            "o dígito é fronteira de letra; com \\b este estilo escapava");
        assertTrue(COM_LISTA.estiloIgnorado("ED2"),
            "207 linhas do acervo — é a camada de ending, não diálogo");
        assertTrue(COM_LISTA.estiloIgnorado("OP_S2"),
            "o sublinhado é caractere de palavra e derrotava o \\b: 170 linhas");

        // O que NÃO mudou: letra seguida de letra continua fora, senão "Editor" viraria música.
        assertFalse(COM_LISTA.estiloIgnorado("Editor"));
        assertFalse(COM_LISTA.estiloIgnorado("Opera"));
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
