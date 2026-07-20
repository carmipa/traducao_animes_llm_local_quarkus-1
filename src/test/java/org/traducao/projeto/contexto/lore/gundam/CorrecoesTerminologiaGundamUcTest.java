package org.traducao.projeto.contexto.lore.gundam;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

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

    /** Canônicos do núcleo UC (padrão compartilhado; não exigidos no roster de obra não-UC). */
    private static final Set<String> NUCLEO_CANONICOS = Set.of(
        "Newtype", "Oldtype", "Mobile Suit", "Mobile Suits",
        "Mobile Armor", "Mobile Armors", "Beam Saber", "Beam Rifle");

    @Test
    @DisplayName("nucleo cobre os termos UC canonicos")
    void nucleoCobreTermosUc() {
        Map<String, String> m = CorrecoesTerminologiaGundamUc.mapa();
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
        Map<String, String> m = CorrecoesTerminologiaGundamUc.comExtras(Map.of("Eixo", "Axis"));
        assertEquals("Axis", m.get("Eixo"));
        assertEquals("Newtype", m.get("Novo Tipo"));
    }

    @Test
    @DisplayName("INVARIANTE: canonico de extra da obra esta no termosProtegidos")
    void canonicoDeExtraEstaNoRoster() {
        // Contextos do pacote gundam (sem subpacote) — cobrem os extras representativos.
        verificarConsistencia(new ContextoGundamF91());
        verificarConsistencia(new ContextoGundamVictory());
        verificarConsistencia(new ContextoGundamUnicorn());
        verificarConsistencia(new ContextoGundamNT());
        verificarConsistencia(new ContextoGundamOrigin());
        verificarConsistencia(new ContextoGundamHathaway());
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
