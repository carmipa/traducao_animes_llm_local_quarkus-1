package org.traducao.projeto.lore.gundam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.lore.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROPÓSITO DE NEGÓCIO: fixa o núcleo de terminologia UC compartilhado e a INVARIANTE de
 * consistência — todo canônico específico da obra restaurado pelo mapa também está protegido
 * no {@code termosProtegidos} (para a tradução sair certa sem depender da revisão de lore).
 *
 * <p>INVARIANTES DO DOMÍNIO: o núcleo cobre Newtype/Oldtype/Mobile Suit/Mobile Armor/Beam
 * Saber/Beam Rifle; {@code comExtras} mescla sem perder o núcleo.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: qualquer canônico de extra fora do roster reprova.
 */
class CorrecoesTerminologiaGundamUcTest {

    /**
     * Canônicos do núcleo UC (padrão compartilhado; não exigidos no roster de obra não-UC).
     *
     * <p>{@code Beam Sabers} entrou quando o run de Gundam ZZ mostrou o plural sendo localizado
     * ("Por que você acha que tem sabres de luz?"). As famílias Mobile Suit e Mobile Armor já
     * traziam singular E plural; a de Beam Saber não trazia — ausência que só apareceu quando o
     * mapa passou a restaurar a forma plural.
     */
    private static final Set<String> NUCLEO_CANONICOS = Set.of(
        "Newtype", "Newtypes", "Oldtype", "Oldtypes", "Mobile Suit", "Mobile Suits",
        "Mobile Armor", "Mobile Armors", "Beam Saber", "Beam Sabers", "Beam Rifle",
        "Normal Suit", "Normal Suits", "Powered Spacesuit",
        // Entraram no núcleo em 2026-07-27, vindos do catálogo espelho da revisão de lore.
        // Pertencem aqui pelo mesmo motivo que "Newtype": são vocabulário UNIVERSAL da Universal
        // Century, presentes na lore de toda a era — não termo próprio de uma obra. A isenção
        // desta lista vale só para isso; canônico específico de obra continua obrigado a estar no
        // termosProtegidos dela.
        //
        // "Minovsky particles" NÃO está aqui porque saiu do mapa: o dono do acervo decidiu que
        // "Partículas Minovsky" é forma aceitável e não deve ser revertida.
        "Spacenoid", "Earthnoid",
        // "Bright" entra pela MESMA porta que Spacenoid/Earthnoid: é vocabulário da Universal
        // Century inteira, não termo próprio de uma obra — Bright Noa aparece de 0079 a Unicorn.
        // As obras trazem "Bright Noa" no termosProtegidos, e a forma composta não casa o
        // "Bright" sozinho que quebrou no Zeta; exigir o roster aqui reprovaria F91, Victory,
        // NT e Hathaway, onde o personagem nem aparece.
        "Bright");

    @Test
    @DisplayName("nucleo cobre os termos UC canonicos")
    void nucleoCobreTermosUc() {
        Map<String, String> m = org.traducao.projeto.lore.LoreDeTeste.terminologia("gundam_ms_igloo");
        assertEquals("Newtype", m.get("Novo Tipo"));
        assertEquals("Oldtype", m.get("Velho Tipo"));
        assertEquals("Mobile Suit", m.get("Traje Móvel"));
        assertEquals("Mobile Armor", m.get("Armadura Móvel"));
        assertEquals("Beam Saber", m.get("Sabre de Raio"));
        assertEquals("Beam Rifle", m.get("Fuzil de Feixe"));
    }

    @Test
    @DisplayName("comExtras mescla os extras sem perder o nucleo")
    void comExtrasMesclaSemPerderNucleo() {
        Map<String, String> m = org.traducao.projeto.lore.LoreDeTeste.terminologia("gundam_cca");
        assertEquals("Axis", m.get("Eixo"));
        assertEquals("Newtype", m.get("Novo Tipo"));
    }

    @Test
    @DisplayName("INVARIANTE: canonico de extra da obra esta no termosProtegidos")
    void canonicoDeExtraEstaNoRoster() {
        // Contextos do pacote gundam (sem subpacote) — cobrem os extras representativos.
        verificarConsistencia(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_f91"));
        verificarConsistencia(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_victory"));
        verificarConsistencia(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_unicorn"));
        verificarConsistencia(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_nt"));
        verificarConsistencia(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_origin"));
        verificarConsistencia(org.traducao.projeto.lore.LoreDeTeste.obra("gundam_hathaway"));
    }

    private void verificarConsistencia(ProvedorContexto ctx) {
        Set<String> roster = ctx.termosProtegidos();
        ctx.correcoesTerminologia().values().stream()
            .filter(canonico -> !NUCLEO_CANONICOS.contains(canonico))
            .forEach(canonico -> assertTrue(roster.contains(canonico),
                () -> ctx.getNomeExibicao() + ": canonico '" + canonico
                    + "' do mapa deve estar no termosProtegidos"));
    }
}
