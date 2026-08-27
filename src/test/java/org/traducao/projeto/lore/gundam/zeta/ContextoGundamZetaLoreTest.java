package org.traducao.projeto.lore.gundam.zeta;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regressão dos nomes canônicos observados nas legendas reais de Zeta. */
class ContextoGundamZetaLoreTest {

    @Test
    void protegeTermosRecorrentesMedidosNosCinquentaEpisodios() {
        org.traducao.projeto.lore.domain.ProvedorContexto contexto = org.traducao.projeto.lore.LoreDeTeste.obra("gundam_zeta");
        Set<String> protegidos = contexto.termosProtegidos();

        Set.of(
            "Mont Blanc", "Dogosse Gier", "Von Braun City", "Green Oasis", "Green Noa",
            "Bosnia", "Sudori", "Haro", "Shinta", "Qum", "Rosammy", "Manack",
            "Operation Apollo", "Operation Maelstrom", "Baund Doc"
        ).forEach(termo -> assertTrue(protegidos.contains(termo), termo));

        assertTrue(contexto.obterPromptSistema().contains("Dogosse Gier"));
        assertFalse(contexto.obterPromptSistema().contains("Dogosse Giar"));
    }

    /**
     * {@code Fa} e {@code Bright} seguem FORA: isolados colidem com palavra comum e a proteção
     * ignora caixa.
     *
     * <h2>Por que FOUR saiu desta lista em 2026-08-17</h2>
     * A regra era a mesma para os três, mas ninguém havia MEDIDO a colisão da {@code Four}.
     * Medição nos 50 {@code .ass} do Zeta contra o espelho inglês:
     * <ul>
     *   <li>{@code Four} maiúsculo isolado (fora de {@code Side Four}): <b>188 falas</b>, e são
     *       a personagem Four Murasame;</li>
     *   <li>{@code four} minúsculo (numeral): <b>9 falas</b>, todas JÁ corretas no acervo;</li>
     *   <li><b>126 das 188 já viraram {@code Quatro}</b> — o nome da personagem está quebrado no
     *       acervo desde antes desta mudança.</li>
     * </ul>
     * 188 contra 9 inverte a conta que justificava a exclusão. Decisão do Paulo, com o número na
     * mão: <i>"usa a própria regra do sistema, nada de reescrever"</i> — a regra RECUSA a proposta
     * que perdeu o termo, e recusar numa das 9 significa manter o que já está certo.
     *
     * <p><b>Isto NÃO é catraca baixada para o código passar.</b> {@code Fa} e {@code Bright}
     * continuam barrados e sem medição; quem quiser tirá-los da lista mede primeiro, como se fez
     * aqui. Ver {@code FourMurasameProtegidaNoZetaTest}.
     */
    @Test
    void naoProtegeAliasesCurtosAmbiguos() {
        Set<String> protegidos = org.traducao.projeto.lore.LoreDeTeste.obra("gundam_zeta").termosProtegidos();

        assertFalse(protegidos.contains("Fa"));
        assertFalse(protegidos.contains("Bright"));
    }

    @Test
    void corrigeSomenteVariantesMedidasQuandoOCanonicoExistirNoIngles() {
        var correcoes = org.traducao.projeto.lore.LoreDeTeste.obra("gundam_zeta").correcoesTerminologia();

        assertTrue("Dogosse Gier".equals(correcoes.get("Dogosse Giar")));
        // `Rosamia -> Rosammy` SAIU do mapa em 27/08/2026, e esta linha agora exige a AUSENCIA
        // dela. Os dois nomes sao legitimos: `Rosammy` e o apelido que Kamille usa para
        // `Rosamia`, e a propria prosa desta lore ja mandava preservar cada um como o original
        // usa. O mapa dizia o contrario, e quem escreve na legenda e o mapa.
        //
        // O prejuizo: o corretor deterministico da 3.2 ia transformar
        //     EN "I'm Rosamia! Not Rosammy!"  ->  "Eu sou Rosammy! Nao Rosammy!"
        // A personagem esta insistindo no proprio nome contra o apelido; achatar os dois mata a
        // cena. Ver ContrasteDeNomesNaoEhFormaRuimTest, que sela a CLASSE do defeito.
        assertTrue(correcoes.get("Rosamia") == null,
            "Rosamia voltou ao mapa como forma-ruim de Rosammy: os dois nomes sao legitimos e a "
                + "fala 'I'm Rosamia! Not Rosammy!' vira 'Eu sou Rosammy! Nao Rosammy!'");
        assertTrue("Qum".equals(correcoes.get("Quem")));
        assertTrue("Manack".equals(correcoes.get("Mancack")));
        assertTrue("Ramsus".equals(correcoes.get("Ramus")));
        assertTrue("Batch".equals(correcoes.get("Lote")));
        assertTrue("Green Noa".equals(correcoes.get("Verde Noa")));
        assertTrue("Von Braun City".equals(correcoes.get("cidade de Von Braun")));
    }
}
