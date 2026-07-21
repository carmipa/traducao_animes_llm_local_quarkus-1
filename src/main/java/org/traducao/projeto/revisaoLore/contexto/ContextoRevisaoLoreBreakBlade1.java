package org.traducao.projeto.revisaoLore.contexto;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional do Filme 1 — The Time of Awakening.
 *
 * <p>INVARIANTES DO DOMÍNIO: canônicos Break Blade; sem Lee nem arcos dos filmes 2–6
 * como foco; sem import de {@code contexto.lore}.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreBreakBlade1 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Broken Blade / Break Blade — Movie 1: The Time of Awakening (O Tempo do Despertar).
        - Regra: 1º dos 6 filmes; NÃO adiantar cessar-fogo/Lee (filme 2) nem campanhas posteriores.
        - Valkyrie Squadron (Zess) ≠ Macross Valkyrie.

        === Roster ===
        - Rygart Arrow (un-sorcerer), Hodr, Sigyn Erster, Zess, Cleo Saburafu, Lee,
          Argath, Erekt, General Baldr, Loquis.

        === Termos ===
        - Delphine (Under-Golem; preferir Delphine a Delphing), Golem, Quartz, un-sorcerer,
          Kingdom of Krisna, Athens Commonwealth, Orlando Empire, Valkyrie Squadron,
          Binonten, Cruzon, Assam, Assam Military Academy, Heavy Knight, Broken Blade.

        === Formas-ruim ===
        - Não-feiticeiro/Sem-magia → un-sorcerer; Quartzo → Quartz; Delfine/Delphing → Delphine;
          Reino de Krisna/Krishna → Kingdom of Krisna; Esquadrão Valquíria → Valkyrie Squadron.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "break_blade_1"; }
    @Override public String getNomeExibicao() {
        return "Break Blade - Filme 1 - O Tempo do Despertar - Revisao de Lore";
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
