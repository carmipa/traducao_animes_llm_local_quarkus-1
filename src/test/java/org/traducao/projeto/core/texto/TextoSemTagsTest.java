package org.traducao.projeto.core.texto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: garante que a fala volte do LLM dentro da moldura original — mesmas tags,
 * texto em português — sem que nenhum marcador tenha viajado.
 *
 * <p>As entradas saem do acervo real (Guilty Crown, medido em 07/08/2026): 8,3% das falas de
 * diálogo têm tag só na borda e 4,0% têm tag no meio, que é o caso deliberadamente fora do recorte.
 */
class TextoSemTagsTest {

    @Test
    @DisplayName("tag de borda: o LLM recebe SO a frase, sem tag nem marcador")
    void tagDeBordaSaiDoTexto() {
        TextoSemTags t = TextoSemTags.decompor("{\\an8}A bridge, huh?").orElseThrow();
        assertEquals("A bridge, huh?", t.textoLimpo(),
            "e este texto que vai ao LLM — sem [[TAG0]], sem chaves");
        assertEquals("{\\an8}", t.prefixo());
        assertEquals("", t.sufixo());
    }

    @Test
    @DisplayName("recompoe a traducao dentro da moldura original")
    void recompoeDentroDaMoldura() {
        TextoSemTags t = TextoSemTags.decompor("{\\an8}A bridge, huh?").orElseThrow();
        assertEquals("{\\an8}Uma ponte, é?", t.recompor("Uma ponte, é?"),
            "a fala de 2026-07-22 que o pipeline descartava por marcador ausente");
    }

    @Test
    @DisplayName("prefixo e sufixo saem literais, na ordem")
    void prefixoESufixoLiterais() {
        TextoSemTags t = TextoSemTags.decompor("{\\i1}Run!{\\i0}").orElseThrow();
        assertEquals("Run!", t.textoLimpo());
        assertEquals("{\\i1}Corra!{\\i0}", t.recompor("Corra!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CASO-CONTROLE do recorte. Tag no meio marca UMA palavra; recolocá-la
     * exigiria saber qual palavra do português corresponde. Ficar de fora é decisão, não omissão.
     */
    @Test
    @DisplayName("VETO: tag no MEIO nao entra no recorte — segue pelo caminho antigo")
    void vetaTagNoMeio() {
        assertEquals(Optional.empty(), TextoSemTags.decompor("Eu {\\i1}nunca{\\i0} vou embora"),
            "o italico pertence a 'nunca'; em portugues a palavra muda de lugar e de tamanho");
        assertEquals(Optional.empty(),
            TextoSemTags.decompor("{\\an8}O portao{\\b1} fechou{\\b0} agora{\\an0}"));
    }

    @Test
    @DisplayName("VETO: \\t( (animacao) nao entra — ali a tag nao e moldura estatica")
    void vetaAnimacao() {
        assertEquals(Optional.empty(), TextoSemTags.decompor("{\\t(0,500,\\frx30)}Titulo"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: guarda de FRONTEIRA. A quebra {@code \N} tem dono desde 2026-07-23 —
     * {@code IsoladorQuebraDialogo} — e esta classe NÃO pode virar a segunda implementação dela.
     *
     * <p>Paulo levantou o {@code \N} em 07/08/2026 e a primeira versão desta classe chegou a
     * tratá-lo; a leitura do código mostrou que o comportamento pedido (tirar antes de mascarar,
     * repor em fronteira de palavra) já existia inteiro. Este teste trava a decisão.
     */
    @Test
    @DisplayName("FRONTEIRA: a quebra \\N NAO e tratada aqui — o dono e IsoladorQuebraDialogo")
    void quebraNaoEhTratadaAqui() {
        assertEquals(Optional.empty(),
            TextoSemTags.decompor("We have to hurry\\Nbefore they close the gate"),
            "sem tag de borda nao ha moldura a separar; o \\N sozinho e assunto do isolador");

        TextoSemTags t = TextoSemTags.decompor("{\\an8}Prime Minister\\Ntomorrow").orElseThrow();
        assertEquals("Prime Minister\\Ntomorrow", t.textoLimpo(),
            "o \\N atravessa como parte do texto — esta classe so tira a MOLDURA");
    }

    /**
     * PROPÓSITO DE NEGÓCIO: CONTRA-TESTE. 87,6% das falas de diálogo já viajam limpas. Elas não
     * podem entrar neste caminho — mudar o que já funciona é risco sem ganho.
     */
    @Test
    @DisplayName("texto puro nao entra no recorte: 87,6% do dialogo segue identico")
    void textoPuroNaoEntra() {
        assertEquals(Optional.empty(), TextoSemTags.decompor("Ainda não acabou!"));
        assertEquals(Optional.empty(), TextoSemTags.decompor(""));
        assertEquals(Optional.empty(), TextoSemTags.decompor(null));
    }

    @Test
    @DisplayName("FALHA FECHADA: traducao inutil devolve o original byte a byte")
    void falhaFechada() {
        TextoSemTags t = TextoSemTags.decompor("{\\an8}A bridge, huh?").orElseThrow();
        assertEquals("{\\an8}A bridge, huh?", t.recompor(null));
        assertEquals("{\\an8}A bridge, huh?", t.recompor("  "));
        assertEquals("{\\an8}A bridge, huh?", t.recompor("{\\an8}"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: INVARIANTE CENTRAL — nenhuma tag ASS e nenhum marcador de controle
     * saem daqui rumo ao LLM. É a razão de existir da classe, e este teste falha se alguém
     * reintroduzir qualquer um dos dois.
     */
    @Test
    @DisplayName("INVARIANTE CENTRAL: o texto enviado nao tem tag ASS nem marcador [[TAG")
    void textoEnviadoNaoTemTagNemMarcador() {
        for (String entrada : new String[]{
            "{\\an8}A bridge, huh?", "{\\i1}Run!{\\i0}",
            "{\\an8\\pos(960,50)}Prime Minister's residence{\\an0}",
            "{\\b1}Nao se mexa!{\\b0}"}) {
            String enviado = TextoSemTags.decompor(entrada).orElseThrow().textoLimpo();
            assertFalse(enviado.contains("{"), "sobrou abertura de tag em: " + enviado);
            assertFalse(enviado.contains("}"), "sobrou fechamento de tag em: " + enviado);
            assertFalse(enviado.contains("[[TAG"), "sobrou marcador em: " + enviado);
        }
    }

    /**
     * PROPÓSITO DE NEGÓCIO: caracteriza a unificação feita em 07/08/2026 — o critério
     * "tags só na borda" tem um dono, e {@code TradutorLotesService.soPrefixo} delega para cá.
     * Se alguém reintroduzir a cópia lá, este teste não pega; quem pega é
     * {@code CatracaRegraDuplicadaEntreFatiasTest}. Aqui fica o contrato do dono.
     */
    @Test
    @DisplayName("tagsSoNaBorda: o criterio unico, consultado tambem pelo dedup")
    void criterioUnicoDeBorda() {
        assertTrue(TextoSemTags.tagsSoNaBorda("{\\an8}A bridge, huh?"));
        assertTrue(TextoSemTags.tagsSoNaBorda("{\\i1}Run!{\\i0}"));
        assertFalse(TextoSemTags.tagsSoNaBorda("Eu {\\i1}nunca{\\i0} vou"));
        assertFalse(TextoSemTags.tagsSoNaBorda("Ainda não acabou!"),
            "texto sem tag nao tem borda a separar");
        assertFalse(TextoSemTags.tagsSoNaBorda(null));
    }
}
