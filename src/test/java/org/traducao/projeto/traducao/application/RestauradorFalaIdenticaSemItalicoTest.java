package org.traducao.projeto.traducao.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.DetectorTraducaoIdenticaService;
import org.traducao.projeto.qualidadeTraducao.application.MascaradorTags;
import org.traducao.projeto.qualidadeTraducao.domain.LoreAtivaPort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Congela o resgate das falas-nome que o LLM devolveu certas mas sem o itálico.
 *
 * <p>Os casos POSITIVOS são as 8 falas que ficaram em inglês na tela na execução do 08th MS Team
 * de 2026-07-31; os NEGATIVOS são as fronteiras que impedem o resgate de virar porta dos fundos.
 */
class RestauradorFalaIdenticaSemItalicoTest {

    /** Termos protegidos REAIS do 08th MS Team, como declarados na lore da obra. */
    private static final class Lore08th implements LoreAtivaPort {
        @Override public Set<String> termosProtegidosAtivos() {
            return Set.of("Shiro Amada", "Karen Joshua", "Terry Sanders Jr.", "Sanders",
                "Eledore Massis", "Michel Ninorich", "Aina Sahalin", "Gundam Ez8");
        }
        @Override public String obterLoreAtiva() { return ""; }
    }

    private final RestauradorFalaIdenticaSemItalico restaurador =
        new RestauradorFalaIdenticaSemItalico(
            new MascaradorTags(),
            new DetectorTraducaoIdenticaService(new Lore08th()),
            new DescarteItalicoUltimoRecurso(),
            new Lore08th());

    @Test
    @DisplayName("as 8 falas presas do 08th voltam com o itálico intacto")
    void restauraAsFalasNomeMedidasNaExecucaoReal() {
        assertEquals("{\\i1}Eledore!", restaurador.restaurarSePossivel("{\\i1}Eledore!", "Eledore!"));
        assertEquals("{\\i1}Michel!", restaurador.restaurarSePossivel("{\\i1}Michel!", "Michel!"));
        assertEquals("{\\i1}Karen!", restaurador.restaurarSePossivel("{\\i1}Karen!", "Karen!"));
        assertEquals("{\\i1}Shiro! Shiro!",
            restaurador.restaurarSePossivel("{\\i1}Shiro! Shiro!", "Shiro! Shiro!"));
        assertEquals("{\\i1}Eledore! Michel!",
            restaurador.restaurarSePossivel("{\\i1}Eledore! Michel!", "Eledore! Michel!"));
        assertEquals("{\\i1}Sanders...", restaurador.restaurarSePossivel("{\\i1}Sanders...", "Sanders..."));
        assertEquals("{\\i1}Eledore...", restaurador.restaurarSePossivel("{\\i1}Eledore...", "Eledore..."));
        assertEquals("{\\i1}Sanders!!!{\\i0}",
            restaurador.restaurarSePossivel("{\\i1}Sanders!!!{\\i0}", "Sanders!!!"));
    }

    @Test
    @DisplayName("texto traduzido de verdade NÃO é restaurado — perder o itálico ali tem custo")
    void naoRestauraQuandoOTextoVisivelMudou() {
        assertNull(restaurador.restaurarSePossivel(
            "I {\\i1}will{\\i0} fight for my friends!", "Eu lutarei pelos meus amigos!"));
    }

    @Test
    @DisplayName("tag que não é itálico cancela: \\pos e \\an8 mudam o que se vê na tela")
    void naoRestauraQuandoAPerdaNaoEItalico() {
        assertNull(restaurador.restaurarSePossivel("{\\pos(320,240)}Eledore!", "Eledore!"));
        assertNull(restaurador.restaurarSePossivel("{\\an8\\i1}Eledore!", "Eledore!"));
    }

    @Test
    @DisplayName("tag inventada pelo modelo é corrupção, não perda")
    void naoRestauraQuandoOModeloInventouTag() {
        assertNull(restaurador.restaurarSePossivel("{\\i1}Eledore!", "{\\i1}{\\b1}Eledore!"));
    }

    @Test
    @DisplayName("eco de frase comum continua reprovado — identidade tem de ser legítima")
    void naoRestauraEcoDeFraseQueDeviaSerTraduzida() {
        assertNull(restaurador.restaurarSePossivel(
            "{\\i1}Get out of here right now!", "Get out of here right now!"));
    }

    @Test
    @DisplayName("inglês corrente em Title Case NÃO é restaurado, mesmo com deveManterIdentico=true")
    void naoRestauraInglesCorrenteQueACapitalizacaoAceitaria() {
        // Contraexemplos levantados na revisão de 2026-07-31 e confirmados por sonda: para TODOS
        // estes, deveManterIdentico devolve true — a régua dele é capitalização. Só a exigência
        // de "palavra é termo da lore" os separa de um nome próprio.
        for (String ingles : new String[] {
            "Shoot!", "Liar!", "Idiot!", "Attack!", "Silence!", "Impossible!", "Coward!",
            "Traitor!", "Monster!", "Surrender!", "Destroy!",
            "Visual Contact!", "Emergency Landing!", "Main Engine!", "Direct Hit!",
            "Maximum Speed!", "Final Battle!", "All Units!", "Secret Base!"
        }) {
            assertNull(restaurador.restaurarSePossivel("{\\i1}" + ingles + "{\\i0}", ingles),
                "não pode restaurar inglês traduzível: " + ingles);
        }
    }

    @Test
    @DisplayName("nome próprio misturado com palavra comum não passa — a fala INTEIRA tem de ser lore")
    void naoRestauraNomeMisturadoComTextoTraduzivel() {
        assertNull(restaurador.restaurarSePossivel("{\\i1}Eledore, shoot!", "Eledore, shoot!"));
    }

    @Test
    @DisplayName("obra sem termos protegidos: o resgate simplesmente não age")
    void naoAgeQuandoALoreEstaVazia() {
        RestauradorFalaIdenticaSemItalico semLore = new RestauradorFalaIdenticaSemItalico(
            new MascaradorTags(),
            new DetectorTraducaoIdenticaService(new Lore08th()),
            new DescarteItalicoUltimoRecurso(),
            new LoreVazia());
        assertNull(semLore.restaurarSePossivel("{\\i1}Eledore!", "Eledore!"));
    }

    private static final class LoreVazia implements LoreAtivaPort {
        @Override public Set<String> termosProtegidosAtivos() { return Set.of(); }
        @Override public String obterLoreAtiva() { return ""; }
    }

    @Test
    @DisplayName("estrutura que já bate não passa por aqui — quem decide é o portão")
    void naoAgeQuandoAsTagsJaSaoIguais() {
        assertNull(restaurador.restaurarSePossivel("{\\i1}Eledore!", "{\\i1}Eledore!"));
    }

    @Test
    @DisplayName("entrada nula ou vazia devolve null sem lançar")
    void toleraEntradaDegenerada() {
        assertNull(restaurador.restaurarSePossivel(null, "Eledore!"));
        assertNull(restaurador.restaurarSePossivel("{\\i1}Eledore!", null));
        assertNull(restaurador.restaurarSePossivel("{\\i1}Eledore!", "   "));
    }
}
