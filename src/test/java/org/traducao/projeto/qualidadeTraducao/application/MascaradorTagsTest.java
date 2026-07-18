package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que cache reutilizado não danifique estilo,
 * posicionamento nem quebras estruturais das legendas ASS/SSA.
 *
 * <p>INVARIANTES DO DOMÍNIO: somente o texto visível pode mudar; perda, criação,
 * alteração ou reordenação de tags invalida a tradução armazenada.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: cada divergência produz uma asserção falsa
 * explícita, impedindo regressões que aceitariam cache estruturalmente corrompido.
 */
class MascaradorTagsTest {

    private final MascaradorTags mascarador = new MascaradorTags();

    /**
     * PROPÓSITO DE NEGÓCIO: valida a checagem de marcadores no texto MASCARADO, usada
     * dentro do retry para rejeitar resposta com {@code [[TAGn]]} corrompido.
     * <p>INVARIANTES DO DOMÍNIO: preservado só quando o multiconjunto de marcadores bate;
     * perder, duplicar ou inventar marcador reprova; nulo é falso.
     * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer divergência aceita reprova.
     */
    @Test
    void marcadoresPreservadosDetectaPerdaDuplicacaoEInvencao() {
        assertTrue(mascarador.marcadoresPreservados("[[TAG0]]A flower", "[[TAG0]]Uma flor"));
        assertFalse(mascarador.marcadoresPreservados("[[TAG0]]A flower", "Uma flor"), "marcador perdido");
        assertFalse(mascarador.marcadoresPreservados("[[TAG0]]A flower", "[[TAG0]][[TAG0]]Uma flor"), "marcador duplicado");
        assertFalse(mascarador.marcadoresPreservados("[[TAG0]]A flower", "[[TAG0]]Uma flor[[TAG1]]"), "marcador inventado");
        assertFalse(mascarador.marcadoresPreservados("[[TAG0]]x", null), "nulo é falso");
        assertTrue(mascarador.marcadoresPreservados("[[TAG1]]x[[TAG0]]y", "[[TAG0]]a[[TAG1]]b"),
            "ordem diferente com o mesmo conjunto é preservado");
    }

    @Test
    void aceitaTraducaoQuePreservaTagsEQuebras() {
        String original = "{\\i1}We have to leave.\\NNow!{\\i0}";
        String traduzido = "{\\i1}Temos que partir.\\NAgora!{\\i0}";

        assertTrue(mascarador.preservaEstruturaOriginal(original, traduzido));
    }

    @Test
    void rejeitaCacheQuePerdeOuAlteraTags() {
        String original = "{\\fad(0,300)}A mobile suit...\\NRun!";

        assertFalse(mascarador.preservaEstruturaOriginal(
            original, "Um mobile suit...\\NCorra!"));
        assertFalse(mascarador.preservaEstruturaOriginal(
            original, "{\\fad(0,900)}Um mobile suit...\\NCorra!"));
    }

    @Test
    void rejeitaCacheQueCriaOuReordenaEstrutura() {
        String original = "{\\i1}Wait!{\\i0}";

        assertFalse(mascarador.preservaEstruturaOriginal(
            original, "{\\i0}Espere!{\\i1}"));
        assertFalse(mascarador.preservaEstruturaOriginal(
            original, "{\\i1}Espere!\\NAgora!{\\i0}"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: impede que desenhos vetoriais ASS sejam enviados
     * ao revisor linguístico como se fossem falas em inglês.
     * <p>INVARIANTES DO DOMÍNIO: modo de desenho {@code \\p1} não contém texto
     * traduzível, mesmo quando seus comandos usam letras ASCII.
     * <p>COMPORTAMENTO EM CASO DE FALHA: classificação verdadeira reprova o teste.
     */
    @Test
    void ignoraDesenhoVetorialAss() {
        assertFalse(mascarador.contemTextoTraduzivel(
            "{\\blur2\\p1\\c&H161414&}m 0 0 l 1440 0 1440 1080 0 1080"));
    }
}
