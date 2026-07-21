package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional do Filme 4 — The Land of Calamity.
 *
 * <p>INVARIANTES DO DOMÍNIO: Narvi/Girge/True/Borcuse; sem duelo na aldeia (filme 5).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreBreakBlade4 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 4: The Land of Calamity /
          The Earth of Calamity (A Terra da Calamidade).
        - Regra: força especial Narvi/Nile/Loggin/Girge; morte de General True (Io/Nike);
          Borcuse vs Baldr. SEM massacre da aldeia/Regatz (filme 5).

        === Roster ===
        - Rygart Arrow, Sigyn Erster, Hodr, Cleo Saburafu, Narvi, Nile, Loggin,
          Girge, General Baldr, General True, General Borcuse (Borcuse Deussenrudolf),
          Io, Nike, Zess (fora do front).

        === Termos ===
        - Delphine, Eltemis, Hykelion, Golem, Quartz, un-sorcerer, Heavy Knight,
          Orlando Empire, Kingdom of Krisna, Athens Commonwealth, Arakan,
          Broken Blade, The Land of Calamity.

        === Formas-ruim ===
        - Hykélion → Hykelion; Eltemus → Eltemis; Império de Orlando → Orlando Empire.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "break_blade_4"; }
    @Override public String getNomeExibicao() {
        return "Break Blade - Filme 4 - A Terra da Calamidade - Revisao de Lore";
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
