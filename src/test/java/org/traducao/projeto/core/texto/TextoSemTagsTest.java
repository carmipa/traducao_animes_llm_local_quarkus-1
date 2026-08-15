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

    /**
     * A linha REAL do OP do 86, copiada byte a byte do manifesto
     * {@code kronos_traducao_karaoke_20260814_192920.json}. Até 2026-08-14 esta classe a vetava
     * por conter {@code \t(}, ela caía no mascarador, o modelo não devolvia o {@code [[TAG0]]} e a
     * tradução — <b>correta</b> — era descartada. Só esta frase perdeu 650 traduções numa execução.
     *
     * <p>Este é o CASO DOENTE do conserto: se alguém restaurar o veto por texto inteiro, ele cai.
     */
    @Test
    @DisplayName("CASO DOENTE: \\t( na BORDA entra no recorte — a linha real do OP do 86")
    void animacaoNaBordaEntraNoRecorte() {
        String real = "{\\an2\\pos(960,170)\\c&H002F68&\\2c&HC4B2AD&\\blur3.5"
            + "\\t(-301,-300,\\3c&HAAA618&)\\t(2100,2300,\\3c&H2F68D1&)"
            + "\\t(-301,-300,\\c&HAAA618&)\\t(2100,2300,\\c&H2F68D1&)}A flower blooms only to be crushed";

        TextoSemTags t = TextoSemTags.decompor(real).orElseThrow(
            () -> new AssertionError("a linha do 86 voltou a ser vetada — 2.979 traducoes corretas "
                + "foram descartadas assim em 14/08/2026"));
        assertEquals("A flower blooms only to be crushed", t.textoLimpo(),
            "ao LLM vai so a frase: sem chave, sem \\t(, sem marcador");
        assertEquals("", t.sufixo());
        assertEquals(real.substring(0, real.indexOf('}') + 1), t.prefixo(),
            "a moldura animada volta LITERAL — nada e redistribuido nesta classe");
    }

    /**
     * O outro lado do caso-controle: a animação volta a vestir a tradução sem perder uma cor, e o
     * {@code \t(} continua exatamente onde estava.
     */
    @Test
    @DisplayName("a moldura animada veste a traducao sem alterar nenhuma tag")
    void animacaoNaBordaRecompoeLiteral() {
        String real = "{\\an2\\pos(960,170)\\blur3.5\\t(-301,-300,\\3c&HAAA618&)}"
            + "A flower blooms only to be crushed";
        TextoSemTags t = TextoSemTags.decompor(real).orElseThrow();
        assertEquals("{\\an2\\pos(960,170)\\blur3.5\\t(-301,-300,\\3c&HAAA618&)}"
            + "Uma flor desabrocha apenas para ser esmagada",
            t.recompor("Uma flor desabrocha apenas para ser esmagada"));
    }

    /**
     * CASO SÃO que NÃO pode passar: {@code \t(} no MEIO segue vetado. Ali a animação pertence a uma
     * palavra específica, e recolocá-la exigiria alinhamento palavra a palavra — o mesmo motivo de
     * qualquer tag no miolo. Sem este teste, o conserto acima viraria licença geral.
     */
    @Test
    @DisplayName("VETO mantido: \\t( no MEIO continua fora do recorte")
    void vetaAnimacaoNoMeio() {
        assertEquals(Optional.empty(),
            TextoSemTags.decompor("Eu {\\t(0,500,\\fs40)}nunca{\\r} vou embora"),
            "a animacao pertence a 'nunca'; em portugues a palavra muda de lugar e de tamanho");
        assertEquals(Optional.empty(),
            TextoSemTags.decompor("{\\an8}O portao {\\t(0,500,\\frx30)}fechou agora"));
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
