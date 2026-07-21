package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional do Filme 5 — The Gap Between Life and Death.
 *
 * <p>INVARIANTES DO DOMÍNIO: Regatz/Hykelion/Girge; sem cerco final Binonten (filme 6).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreBreakBlade5 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 5: The Gap Between Life and Death /
          The Horizon Between Life and Death (A Fronteira entre a Vida e a Morte).
        - Regra: Borcuse → aldeia de Rygart; Regatz; duelo Delphine vs Hykelion; Girge.
          SEM fortaleza final / forma final Delphine do filme 6.

        === Roster ===
        - Rygart Arrow, Regatz, Narvi, Nile, Loggin, Girge,
          General Borcuse (Borcuse Deussenrudolf), General Baldr, Sigyn Erster, Hodr,
          Cleo Saburafu, Zess.

        === Termos ===
        - Delphine, Hykelion, Eltemis, Golem, Quartz, un-sorcerer, Heavy Knight,
          Sparta, Kingdom of Krisna, Athens Commonwealth, Broken Blade,
          The Gap Between Life and Death.

        === Formas-ruim ===
        - Hykélion → Hykelion; Não-feiticeiro → un-sorcerer; Delfine → Delphine.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "break_blade_5"; }
    @Override public String getNomeExibicao() {
        return "Break Blade - Filme 5 - A Fronteira entre a Vida e a Morte - Revisao de Lore";
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
