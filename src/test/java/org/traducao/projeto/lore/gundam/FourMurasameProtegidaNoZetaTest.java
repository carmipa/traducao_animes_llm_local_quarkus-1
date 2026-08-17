package org.traducao.projeto.lore.gundam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.LoreDeTeste;
import org.traducao.projeto.lore.domain.ProvedorContexto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: prova que {@code Four} — a personagem Four Murasame — está protegida no
 * catálogo do Zeta, de forma que uma proposta que a traduza para {@code Quatro} seja RECUSADA.
 *
 * <h2>O prejuízo MEDIDO que originou — 2026-08-17</h2>
 * Nos 50 {@code .ass} do Zeta, contra o espelho inglês:
 * <ul>
 *   <li>188 falas têm {@code Four} maiúsculo isolado (fora de {@code Side Four});</li>
 *   <li><b>126 já viraram {@code Quatro}</b> no acervo — <i>"Amuro logo percebe que Quatro é
 *       diferente"</i>, <i>"Quatro! Tenha cuidado com sua linguagem"</i>;</li>
 *   <li>62 preservaram o nome;</li>
 *   <li>9 falas usam {@code four} minúsculo, que é o numeral.</li>
 * </ul>
 *
 * <h2>Por que a REGRA e não o mapa — decisão do Paulo</h2>
 * Palavras dele: <i>"usa a própria regra do sistema, nada de reescrever"</i>. O mapa de
 * terminologia é indexado pela forma ERRADA, e a chave {@code Quatro} já pertence ao Quattro
 * Bajeena (o Char) — dois personagens chegam ao português como a mesma palavra e o mapa não sabe
 * separá-los. A regra sabe, porque olha o INGLÊS: original tem {@code Four}, proposta não tem,
 * proposta recusada.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>Esta regra NÃO conserta as 126 já quebradas: reparo é reescrita, e reescrever foi vetado.
 *       Ela impede que NOVAS falas quebrem.</li>
 *   <li>{@code Four Murasame} (forma completa) continua na lista — tirar uma não substitui a
 *       outra, porque a proteção casa a grafia canônica inteira.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar aqui significa que alguém tirou {@code Four} do catálogo e o nome da personagem voltou
 * a poder ser traduzido em toda fala nova do Zeta.
 */
class FourMurasameProtegidaNoZetaTest {

    @Test
    @DisplayName("Four isolada esta protegida no catalogo do Zeta")
    void fourIsoladaEstaProtegida() {
        ProvedorContexto zeta = LoreDeTeste.obra("gundam_zeta");

        assertTrue(zeta.termosProtegidos().contains("Four"),
            "sem 'Four' isolada na lista, a regra nao tem o que recusar e o nome da personagem "
                + "volta a virar 'Quatro' em toda fala nova. Termos com 'Four': "
                + zeta.termosProtegidos().stream().filter(t -> t.contains("Four")).toList());

        assertTrue(zeta.termosProtegidos().contains("Four Murasame"),
            "a forma completa continua valendo: uma nao substitui a outra");
    }

    /**
     * O CONTRA-CASO que impede a entrada de virar reescrita por engano: o mapa de terminologia
     * NÃO pode ganhar uma entrada {@code Quatro -> Four}, porque a chave {@code Quatro} pertence
     * ao Quattro Bajeena. Se alguém tentar, o Char passa a ser chamado de Four.
     */
    @Test
    @DisplayName("o mapa continua mandando Quatro para Quattro, nao para Four")
    void mapaDeTerminologiaContinuaApontandoParaQuattro() {
        ProvedorContexto zeta = LoreDeTeste.obra("gundam_zeta");
        String canonico = zeta.correcoesTerminologia().get("Quatro");

        assertFalse("Four".equals(canonico),
            "trocar isto renomeia o CHAR: 'Quatro' e a forma errada de Quattro Bajeena, e o mapa "
                + "e indexado pela forma errada. A protecao da Four Murasame mora na REGRA "
                + "(recusa), nunca aqui.");
    }
}
