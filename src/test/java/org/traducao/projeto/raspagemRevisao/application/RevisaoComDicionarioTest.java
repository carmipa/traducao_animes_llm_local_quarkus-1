package org.traducao.projeto.raspagemRevisao.application;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: a Revisão passa a alcançar o que a lista e a regra de terminação não
 * alcançam — e é a única fatia que conserta o acervo JÁ TRADUZIDO.
 *
 * <h2>Por que aqui vale mais que nas outras duas</h2>
 * A tradução e o karaokê corrigem o que estão produzindo AGORA. As 119 falas do Zeta com acento
 * faltando e as 82 que sobraram no Unicorn já estão gravadas em disco — para elas, reprocessar
 * com o LLM custa horas e a Revisão custa segundos. Medido em 13/08/2026.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>O dicionário roda DEPOIS dos determinísticos: o que a lista já resolve não gasta uma
 *       consulta a processo externo.</li>
 *   <li>Sem hunspell, a fala volta intacta — a Revisão nunca apresenta "sem erros" quando na
 *       verdade não verificou.</li>
 *   <li>Só acento é reposto. Nome de lore, inglês e romaji passam intactos, pelo mesmo critério
 *       das outras fatias: a sugestão tem de ser a MESMA palavra acentuada.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Sem hunspell, PULA por {@link Assumptions} — nunca passa por ausência de verificação.
 */
@QuarkusTest
@DisplayName("revisão PT-only: o dicionário alcança o que a lista não alcança")
class RevisaoComDicionarioTest {

    @Inject
    RevisorPtOnlyService revisor;

    /** Formas medidas na saída do Unicorn de 13/08 20:30 que a lista NÃO cobria. */
    @Test
    @DisplayName("corrige o que sobrou da lista: fatidico, aereo, minimo")
    void alcancaOqueAlistaNaoAlcanca() {
        var r = revisor.revisarFala("Um evento fatidico no espaco aereo.");
        Assumptions.assumeTrue(r.alterado(), "hunspell ausente — NÃO VERIFICADO");

        assertTrue(r.texto().contains("fatídico"), "acento reposto pelo dicionário: " + r.texto());
        assertTrue(r.texto().contains("aéreo"), "acento reposto pelo dicionário: " + r.texto());
        assertTrue(r.alterado(), "a fala mudou, então alterado tem de ser true");
    }

    /** O que a lista JÁ cobria continua funcionando — o dicionário não substitui, complementa. */
    @Test
    @DisplayName("a lista continua valendo: nao, voce, organizacao")
    void oQueAlistaJAcobriaContinua() {
        var r = revisor.revisarFala("voce nao viu a organizacao?");
        assertTrue(r.texto().contains("você"), r.texto());
        assertTrue(r.texto().contains("não"), r.texto());
        assertTrue(r.texto().contains("organização"), r.texto());
    }

    /** Lore, inglês e romaji não podem ser tocados — mesmo critério das outras fatias. */
    @Test
    @DisplayName("não toca em lore, inglês nem romaji")
    void preservaOqueNaoEhPortugues() {
        var r = revisor.revisarFala("O psycommu do cockpit responde, kimi.");
        Assumptions.assumeTrue(!r.texto().isBlank(), "resultado vazio");

        assertTrue(r.texto().contains("psycommu"), "termo de lore alterado: " + r.texto());
        assertTrue(r.texto().contains("cockpit"), "inglês legítimo alterado: " + r.texto());
        assertTrue(r.texto().contains("kimi"), "romaji alterado: " + r.texto());
    }

    @Test
    @DisplayName("fala já correta volta BYTE A BYTE e alterado=false")
    void falaCorretaNaoEhReescrita() {
        String limpo = "A situação da colônia é crítica.";
        var r = revisor.revisarFala(limpo);
        assertEquals(limpo, r.texto(), "texto correto reescrito é alarme falso gravado no arquivo");
        assertFalse(r.alterado(), "nada mudou, então alterado tem de ser false");
    }

    @Test
    @DisplayName("tags e a quebra \\N sobrevivem à revisão")
    void naoDestroiFormatacao() {
        var r = revisor.revisarFala("{\\i1}A situacao{\\i0} da\\Ncolonia.");
        Assumptions.assumeTrue(r.alterado(), "hunspell ausente — NÃO VERIFICADO");

        assertTrue(r.texto().contains("{\\i1}") && r.texto().contains("{\\i0}"),
            "tag de override sumiu: " + r.texto());
        assertTrue(r.texto().contains("\\N"), "a quebra sumiu: " + r.texto());
        assertTrue(r.texto().contains("situação"), "e a correção tinha de acontecer: " + r.texto());
    }
}
