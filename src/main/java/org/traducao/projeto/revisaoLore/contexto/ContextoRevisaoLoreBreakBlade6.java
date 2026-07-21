package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional do Filme 6 — Fortress of Lamentation.
 *
 * <p>INVARIANTES DO DOMÍNIO: desfecho ANIME dos 6 filmes (≠ mangá completo).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreBreakBlade6 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 6: Fortress of Lamentation /
          Enclave of Lamentations (Fortaleza do Lamentar).
        - Regra: cerco de Binonten; Greta / Quartz armor; Captain Sakura; Delphine forma final;
          Borcuse. Desfecho ANIME — NÃO importar epílogo do mangá.

        === Roster ===
        - Rygart Arrow, Sigyn Erster, Hodr, General Baldr, Narvi, Nile, Loggin,
          Girge (memória), General Borcuse, Io, Bades, Captain Sakura, Greta,
          Cleo Saburafu, Zess, Gram.

        === Termos ===
        - Delphine, Hykelion, Eltemis, Golem, Quartz, un-sorcerer, Heavy Knight,
          Kingdom of Krisna, Athens Commonwealth, Orlando Empire, Binonten,
          Fortress of Lamentation, Broken Blade.

        === Formas-ruim ===
        - Quartzo → Quartz; Hykélion → Hykelion; Delfine → Delphine;
          Império de Orlando → Orlando Empire.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "break_blade_6"; }
    @Override public String getNomeExibicao() {
        return "Break Blade - Filme 6 - Fortaleza do Lamentar - Revisao de Lore";
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
