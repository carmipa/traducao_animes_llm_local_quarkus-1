package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional do Filme 3 — Scars from an Assassin's Blade.
 *
 * <p>INVARIANTES DO DOMÍNIO: Argath/Zess ferido/Cleo capturada; sem Borcuse (filme 4+).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreBreakBlade3 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 3: Scars from an Assassin's Blade
          / The Mark of the Assassin's Dagger (Cicatrizes da Lâmina do Assassino).
        - Regra: último ataque da Valkyrie Squadron; Argath; Zess ferido; Cleo capturada
          (Narvi). SEM unidade Girge nem ofensiva de larga escala (filme 4).

        === Roster ===
        - Rygart Arrow, Hodr, Sigyn Erster, Zess, Cleo Saburafu, Narvi,
          Argath, Erekt, General Baldr, Lee (trauma do filme 2).

        === Termos ===
        - Delphine, Eltemis, Under-Golem, Golem, Quartz, un-sorcerer, Heavy Knight,
          Valkyrie Squadron, Kingdom of Krisna, Athens Commonwealth, Binonten,
          Broken Blade, Scars from an Assassin's Blade.

        === Formas-ruim ===
        - Eltemus → Eltemis; Delfine → Delphine; Esquadrão Valquíria → Valkyrie Squadron.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "break_blade_3"; }
    @Override public String getNomeExibicao() {
        return "Break Blade - Filme 3 - Cicatrizes da Lâmina do Assassino - Revisao de Lore";
    }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Break Blade na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaBreakBladeRevisao.mapa();
    }
}
