package org.traducao.projeto.lore.eightsix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.qualidadeTraducao.application.EnforcadorTermosLore;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PROPÓSITO DE NEGÓCIO: garante que "Spearhead" volte a ser Spearhead quando é o ESQUADRÃO, e
 * continue traduzido quando é a arma.
 *
 * <h2>O prejuízo que originou</h2>
 * Ordem de Paulo em 2026-08-15: <i>"Spearhead é uma palavra de lore que não pode ser
 * traduzida"</i>. A medição da retradução completa do 86 no mesmo dia (23 episódios,
 * aya-expanse-8b) mostrou 27 ocorrências no inglês, 23 preservadas e 4 perdidas — e as 4 não
 * são a mesma coisa. Três são o esquadrão virando invenção, duas delas nem palavra
 * ({@code Esquadroe}, {@code Esquadroa}); a quarta é {@code "A spearhead."}, minúsculo, que é
 * a arma e cuja tradução {@code "A ponta de lança."} está CORRETA.
 *
 * <h2>Invariantes do domínio</h2>
 * <ul>
 *   <li>A maiúscula é o discriminador, não a palavra. Proteger {@code spearhead} cru trocaria
 *       uma linha boa por uma errada.</li>
 *   <li>Quem separa os dois é o mecanismo, não a lista: {@code contarCanonico} usa
 *       {@code flags = 0} para termo de uma palavra, então a restauração exige a grafia exata
 *       {@code Spearhead} no inglês.</li>
 * </ul>
 *
 * <h2>Comportamento em caso de falha</h2>
 * Reprovar no primeiro caso significa que o esquadrão voltou a sair inventado na legenda.
 * Reprovar no CASO-CONTROLE é pior: significa que a proteção passou a corromper fala legítima,
 * que é o dano que o mapa de terminologia existe para evitar.
 */
@DisplayName("Spearhead: esquadrão restaurado, arma preservada")
class SpearheadMinusculoContinuaTraduzidoTest {

    private final EnforcadorTermosLore enforcador = new EnforcadorTermosLore();
    private final Map<String, String> correcoes = org.traducao.projeto.lore.LoreDeTeste.obra("eight_six").correcoesTerminologia();

    /** As três falas REAIS que saíram erradas na retradução de 15/08. */
    @Test
    @DisplayName("CASO DOENTE: as três formas inventadas voltam a ser Spearhead")
    void esquadraoInventadoEhRestaurado() {
        assertEquals(
            "Oficialmente conhecido como Spearhead.",
            enforcador.reforcar(
                "Officially called the Spearhead Squadron.",
                "Oficialmente conhecido como Esquadroe de Ponta.",
                correcoes),
            "'Esquadroe' nao e palavra do portugues e chegou a legenda final do ep 01");

        assertEquals(
            "Este é o comandante do Spearhead.",
            enforcador.reforcar(
                "This is the captain of the Spearhead Squadron.",
                "Este é o comandante do Esquadroa de Ponta.",
                correcoes));

        assertEquals(
            "Spearhead.",
            enforcador.reforcar("Spearhead.", "Espada-Faca.", correcoes));
    }

    /**
     * O CASO-CONTROLE, e é ele que impede a correção de virar dano: minúsculo é a ARMA.
     * Sem este assert, a proteção teria trocado uma tradução correta por um termo de lore
     * fora de lugar — e ninguém perceberia, porque o defeito só apareceria assistindo.
     */
    @Test
    @DisplayName("CASO SÃO: 'a spearhead' minúsculo continua sendo ponta de lança")
    void armaMinusculaNaoEhTocada() {
        assertEquals(
            "A ponta de lança.",
            enforcador.reforcar("A spearhead.", "A ponta de lança.", correcoes),
            "a arma virou nome de esquadrao: a protecao passou a corromper fala legitima");
    }

    /** O termo canônico ausente do inglês nunca autoriza reescrita — a regra de sempre. */
    @Test
    @DisplayName("sem 'Spearhead' no original, nada é reescrito")
    void semCanonicoNoOriginalNaoMexe() {
        assertEquals(
            "A espada-faca dele quebrou.",
            enforcador.reforcar("His knife-sword broke.", "A espada-faca dele quebrou.", correcoes),
            "reescreveu sem o termo canonico no ingles: o mapa virou busca-e-troca cega");
    }
}
