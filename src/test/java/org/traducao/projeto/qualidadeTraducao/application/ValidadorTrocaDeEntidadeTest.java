package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.domain.AlucinacaoDetectadaException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: sela o portão que acusa TROCA DE ENTIDADE — a tradução responder com
 * um nome da obra diferente do que o original usou.
 *
 * <h2>Por que nenhuma regra anterior pegava</h2>
 * O termo trocado é grafia canônica perfeita: não é resíduo em inglês, não é preâmbulo, não é
 * desproporcional. Só aparece comparando com o ORIGINAL. Medido no acervo em 2026-07-28:
 * <pre>
 *   Zeta   172 falas   "Four" (personagem)  -> "Quatro" e, em 15, "Quattro" (OUTRO personagem)
 *   ZZ      55 falas   "Zeta Gundam"        -> "ZZ Gundam"      (mecha diferente)
 *   ZZ      31 falas   "Argama"             -> "Nahel Argama"   (nave diferente)
 *   GC      20 falas   "Inori"              -> "Crow"           (0 ocorrências de "Crow" no inglês)
 * </pre>
 *
 * <h2>Onde o portão roda, e por que isso basta</h2>
 * {@code validarPar} é chamado em UM lugar: {@code AvaliadorTraducaoCache}, o portão de reuso do
 * cache. E é o lugar certo: recusar não apaga — {@code ProcessarArquivoUseCase} devolve a fala
 * para {@code textosPendentes}, ou seja, ela é RETRADUZIDA. Com a lore corrigida, o acervo se
 * conserta sozinho na próxima passada, em vez de reaproveitar o defeito para sempre.
 *
 * <h2>Invariantes do domínio</h2>
 * A regra genérica "termo protegido no PT ausente do EN" foi medida e descartada: 1447 disparos
 * em 59.625 falas, quase tudo normalização legítima. Por isso PAR declarado, não heurística. E
 * por isso cada teste que acusa vem com o vizinho que precisa passar.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: {@link AlucinacaoDetectadaException} com os dois termos e a
 * fala; o chamador preserva o original e manda retraduzir.
 */
class ValidadorTrocaDeEntidadeTest {

    @Test
    @DisplayName("personagem trocada por outra personagem de nome parecido")
    void fourNaoPodeVirarQuattro() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Four", "Quattro")));

        AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Four, wait!", "Quattro, espere!"));
        assertTrue(e.getMessage().contains("Four") && e.getMessage().contains("Quattro"),
            "a mensagem tem de nomear as duas entidades: " + e.getMessage());
    }

    @Test
    @DisplayName("a proibição vale nas duas direções")
    void quattroTambemNaoPodeVirarFour() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Four", "Quattro")));

        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Quattro, respond!", "Four, responda!"));
    }

    @Test
    @DisplayName("o numeral minúsculo não é o nome — comparação sensível à caixa")
    void numeralMinusculoNaoEhAEntidade() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Four", "Quattro")));

        // "four" numeral não casa com o nome "Four"; e "quatro" traduzido não é "Quattro".
        assertDoesNotThrow(() -> validador.validarPar("There are four enemies.", "Há quatro inimigos."));
    }

    @Test
    @DisplayName("fala que menciona as DUAS entidades de verdade não é acusada")
    void mencaoLegitimaAosDoisTermosPassa() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Argama", "Nahel Argama")));

        assertDoesNotThrow(() -> validador.validarPar(
            "The Argama and the Nahel Argama are docking.",
            "A Argama e a Nahel Argama estão atracando."));
    }

    /**
     * "Nahel Argama" CONTÉM "Argama". Sem comparar o termo inteiro por fronteira de palavra,
     * toda menção à Nahel Argama acusaria a si mesma.
     */
    @Test
    @DisplayName("termo que é substring do outro não acusa a si mesmo")
    void substringNaoAcusaASiMesma() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Argama", "Nahel Argama")));

        assertDoesNotThrow(() -> validador.validarPar(
            "Repair the Nahel Argama.", "Reparem a Nahel Argama."));
    }

    @Test
    @DisplayName("nave trocada pela sucessora é acusada")
    void argamaNaoPodeVirarNahelArgama() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Argama", "Nahel Argama")));

        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Repair the Argama.", "Reparem a Nahel Argama."));
    }

    @Test
    @DisplayName("persona de palco não substitui o nome — o caso Inori/Crow")
    void inoriNaoPodeVirarCrow() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Inori", "Crow")));

        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Inori!", "Crow!"));
        // E o inverso: quando a obra usa a persona, ela fica.
        assertDoesNotThrow(() -> validador.validarPar("Crow's on stage.", "Crow está no palco."));
    }

    @Test
    @DisplayName("obra sem pares declarados não muda de comportamento")
    void semParesDeclaradosNadaMuda() {
        var validador = new ValidadorTraducaoService(LoreAtivaFake.vazia());

        assertDoesNotThrow(() -> validador.validarPar("Inori!", "Crow!"));
    }

    /**
     * PROPÓSITO DE NEGÓCIO: acréscimo NÃO é troca. Achado ao vivo na retradução do 08th
     * (2026-07-28 20:05): o inglês {@code "So, you're the team killer, Sanders the Reaper."} foi
     * traduzido como {@code "Sanders, o Ceifador."} — correto, porque o nome do original
     * SOBREVIVEU e o apelido foi vertido — e o portão condenou. A fala ficou VAZIA no cache.
     *
     * <p>O vizinho que precisa continuar caindo está no mesmo teste de propósito: quando o nome
     * some, é troca de verdade, e é justamente onde ela custa mais — as 4 falas medidas eram
     * rádio do esquadrão em tratamento direto entre companheiros.
     */
    @Test
    @DisplayName("apelido acrescentado não é troca quando o nome do original sobrevive")
    void nomePreservadoNaoEhTroca() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Sanders", "Ceifador")));

        assertDoesNotThrow(() -> validador.validarPar(
            "So, you're the team killer, Sanders the Reaper.", "Sanders, o Ceifador."));

        AlucinacaoDetectadaException e = assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Karen! Sanders! Do you read me?", "Karen! Ceifador! Me escutam?"));
        assertTrue(e.getMessage().contains("Sanders") && e.getMessage().contains("Ceifador"),
            "a mensagem tem de nomear os dois tratamentos: " + e.getMessage());
    }

    /**
     * A guarda de preservação remove as ocorrências do termo ACUSADO antes de procurar o do
     * original. Sem essa ordem, o par cujo termo contém o outro cegaria a si mesmo: "Nahel Argama"
     * contém "Argama", então "Reparem a Nahel Argama" satisfaz {@code contemTermo(.., "Argama")}
     * por fronteira de palavra, e as 31 falas medidas no ZZ parariam de ser detectadas.
     */
    @Test
    @DisplayName("a guarda de preservação não cega o par cujo termo contém o outro")
    void guardaDePreservacaoNaoCegaSubstring() {
        var validador = new ValidadorTraducaoService(
            LoreAtivaFake.comPares(List.of("Argama", "Nahel Argama")));

        assertThrows(AlucinacaoDetectadaException.class,
            () -> validador.validarPar("Repair the Argama.", "Reparem a Nahel Argama."));
    }
}
