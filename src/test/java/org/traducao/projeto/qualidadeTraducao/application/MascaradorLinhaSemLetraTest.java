package org.traducao.projeto.qualidadeTraducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que linha SEM NENHUMA LETRA não é considerada
 * traduzível e portanto nunca chega ao LLM.
 *
 * <p>Os casos vêm da execução real de Gundam Unicorn RE:0096 em 2026-07-29 (22
 * episódios): contagens regressivas produziram 23 das 26 pendências do lote, todas
 * pelo mesmo mecanismo — sem âncora semântica o modelo devolve a lore do prompt de
 * sistema. As guardas barravam, mas depois de gastar a chamada.
 *
 * <p>INVARIANTES COBERTAS: dígito e pontuação não são idioma; uma letra sozinha já
 * basta para traduzir; alfabetos não latinos contam como letra.
 */
class MascaradorLinhaSemLetraTest {

    private final MascaradorTags mascarador = new MascaradorTags();

    @Test
    @DisplayName("contagens regressivas reais do Unicorn não são traduzíveis")
    void linhaSoComNumerosNaoVaiAoLlm() {
        // Cada uma destas produziu uma invenção de lore no run de 2026-07-29.
        assertFalse(mascarador.contemTextoTraduzivel("45..."));      // -> "O Unicorn Gundam, não é?"
        assertFalse(mascarador.contemTextoTraduzivel("8..."));       // -> "Lembranças... são como areia entre os dedos."
        assertFalse(mascarador.contemTextoTraduzivel("5. 4."));      // -> "Mineva: Eu sou Audrey Burne."
        assertFalse(mascarador.contemTextoTraduzivel("22. 21. 20. 19."));
        assertFalse(mascarador.contemTextoTraduzivel("1!"));
        assertFalse(mascarador.contemTextoTraduzivel("27%!"));
        assertFalse(mascarador.contemTextoTraduzivel("18%!"));
    }

    @Test
    @DisplayName("número com tag ASS também não é traduzível — a tag não é texto")
    void tagNaoTornaLinhaTraduzivel() {
        assertFalse(mascarador.contemTextoTraduzivel("{\\pos(1232,1050)\\fad(50,200)\\blur2}45..."));
        assertFalse(mascarador.contemTextoTraduzivel("{\\i1}30. 29.{\\i0}"));
    }

    @Test
    @DisplayName("UMA letra já basta: o filtro corta só quem não tem nenhuma")
    void umaLetraBasta() {
        assertTrue(mascarador.contemTextoTraduzivel("45 minutos"));
        assertTrue(mascarador.contemTextoTraduzivel("Oh...?"));
        assertTrue(mascarador.contemTextoTraduzivel("A"));
        assertTrue(mascarador.contemTextoTraduzivel("1500 hours"));
        // Termo protegido tem letra e SEGUE traduzível: quem resolve é a guarda de
        // termos, não este filtro. Não é o mesmo problema.
        assertTrue(mascarador.contemTextoTraduzivel("Sieg Zeon, Sieg Zeon,\\NSieg Zeon..."));
    }

    @Test
    @DisplayName("alfabeto não latino conta como letra")
    void japonesEhLetra() {
        assertTrue(mascarador.contemTextoTraduzivel("ガンダム"));
        assertTrue(mascarador.contemTextoTraduzivel("機動戦士"));
    }

    @Test
    @DisplayName("vazio, nulo e pontuação pura continuam fora")
    void bordas() {
        assertFalse(mascarador.contemTextoTraduzivel(null));
        assertFalse(mascarador.contemTextoTraduzivel(""));
        assertFalse(mascarador.contemTextoTraduzivel("   "));
        assertFalse(mascarador.contemTextoTraduzivel("..."));
        assertFalse(mascarador.contemTextoTraduzivel("!?!"));
    }
}
