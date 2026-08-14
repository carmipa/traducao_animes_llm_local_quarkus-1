package org.traducao.projeto.qualidadeTraducao.application.nomeProprio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa a severidade do extrator. Cada assert aqui corresponde a uma família
 * de falso positivo que a versão descartada da heurística produzia.
 *
 * <h2>Por que estes casos e não outros</h2>
 * As entradas vêm do formato real: fala com tag de override, fala quebrada com {@code \N}, ênfase
 * em caixa alta e diálogo começando por nome. São as quatro formas em que maiúscula aparece numa
 * legenda sem significar nome próprio.
 */
@DisplayName("nome próprio: o extrator é severo de propósito")
class ExtratorCandidatosNomeProprioTest {

    @Test
    @DisplayName("meio de frase entra: é o único lugar onde maiúscula informa algo")
    void meioDeFraseEhCandidata() {
        Set<String> c = ExtratorCandidatosNomeProprio.candidatas("Take me to Sky before dawn.");
        assertTrue(c.contains("Sky"), "capitalizada no meio da frase tem de ser candidata: " + c);
    }

    /**
     * O falso positivo de maior volume na heurística antiga. Maiúscula em início de frase é
     * ortografia obrigatória e não diz nada — nem sobre nome, nem sobre nada.
     */
    @Test
    @DisplayName("início de frase NÃO entra — texto, após pontuação e após \\N")
    void inicioDeFraseNaoEhCandidata() {
        assertFalse(ExtratorCandidatosNomeProprio.candidatas("Sky is falling.").contains("Sky"),
            "primeira palavra do texto virou candidata");
        assertFalse(ExtratorCandidatosNomeProprio.candidatas("Run! Sky is falling.").contains("Sky"),
            "palavra após '!' virou candidata");
        assertFalse(ExtratorCandidatosNomeProprio.candidatas("Listen:\\NSky is falling.").contains("Sky"),
            "após a quebra \\N o tratamento é conservador: não é candidata");
    }

    @Test
    @DisplayName("CAIXA ALTA fica fora: é ênfase, e sigla tem tratamento próprio")
    void caixaAltaNaoEhCandidata() {
        Set<String> c = ExtratorCandidatosNomeProprio.candidatas("I said STOP the RZ engine now.");
        assertFalse(c.contains("STOP"), "ênfase em caixa alta virou candidata: " + c);
        assertFalse(c.contains("RZ"), "sigla virou candidata: " + c);
    }

    @Test
    @DisplayName("conteúdo de tag ASS não é fala")
    void tagNaoEntra() {
        Set<String> c = ExtratorCandidatosNomeProprio.candidatas(
            "{\\fnGandhi Sans\\pos(10,20)}we lost him near Sky.");
        assertFalse(c.contains("Gandhi"), "nome de fonte virou candidata: " + c);
        assertFalse(c.contains("Sans"), "nome de fonte virou candidata: " + c);
        assertTrue(c.contains("Sky"), "a fala de verdade tem de continuar sendo lida: " + c);
    }

    /**
     * A tag vira ESPAÇO e não some. Se sumisse, {@code {\i1}Kaine} passaria a ser começo de texto
     * e o nome deixaria de ser candidato — perda silenciosa.
     */
    @Test
    @DisplayName("tag no meio não cria nem apaga fronteira de frase")
    void tagViraEspacoNaoSumico() {
        Set<String> c = ExtratorCandidatosNomeProprio.candidatas("we found {\\i1}Kaine{\\i0} alive.");
        assertTrue(c.contains("Kaine"), "nome cercado de tag sumiu: " + c);
    }

    @Test
    @DisplayName("curta demais e com dígito ficam fora")
    void formasQueNaoSaoNome() {
        Set<String> c = ExtratorCandidatosNomeProprio.candidatas("and I saw Al and Zx9 there.");
        assertFalse(c.contains("Al"), "duas letras não é nome de personagem: " + c);
        assertFalse(c.contains("I"), "pronome I virou candidata: " + c);
    }

    @Test
    @DisplayName("bordas: nulo, vazio e só tag devolvem conjunto vazio")
    void bordas() {
        assertEquals(Set.of(), ExtratorCandidatosNomeProprio.candidatas(null));
        assertEquals(Set.of(), ExtratorCandidatosNomeProprio.candidatas(""));
        assertEquals(Set.of(), ExtratorCandidatosNomeProprio.candidatas("   "));
        assertEquals(Set.of(), ExtratorCandidatosNomeProprio.candidatas("{\\pos(10,20)}"));
    }

    /**
     * Genitivo é o MESMO nome. Se o apóstrofo abrisse frase, {@code Kaine's} seria lido como duas
     * palavras e a segunda entraria como candidata fantasma.
     */
    @Test
    @DisplayName("genitivo não vira duas palavras")
    void genitivoNaoQuebra() {
        Set<String> c = ExtratorCandidatosNomeProprio.candidatas("this is Kaine's ship, not yours.");
        assertTrue(c.contains("Kaine"), "o nome sumiu no genitivo: " + c);
        assertFalse(c.contains("Ship"), "genitivo criou candidata inexistente: " + c);
    }
}
