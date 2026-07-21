package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore agregada dos filmes Macross Delta.
 *
 * <p>INVARIANTES DO DOMÍNIO: filme 1 ≠ filme 2; Walküre ≠ Valkyrie; Yami_Q_Ray / Heimdall
 * só Absolute Live; preferir contextos filme1/filme2 quando possível.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDeltaFilmes implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta (Filmes) — Passionate Walküre + Absolute Live!!!!!!
        - Preferir macross_delta_filme1 / macross_delta_filme2 quando for so um filme.
        - Walküre ≠ Valkyrie; PROIBIDO Veritech.

        === Passionate Walküre (filme 1) ===
        - Windermere Kingdom / Aerial Knights / Var Syndrome / Fold Waves /
          VF-31 Siegfried / Macross Elysion. SEM Heimdall / Yami_Q_Ray.

        === Absolute Live!!!!!! (filme 2) ===
        - Heimdall (Ian Cromwell); Yami_Q_Ray (+ Yami Mikumo/Freyja/Kaname/Makina/Reina);
          Max Jenius / Macross Gigant; VF-31AX Kairos-Plus; Star Singer.

        === Roster comum ===
        - Walküre: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Delta Flight: Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang,
          Messer Ihlefeld.

        === Formas-ruim ===
        - Walkure → Walküre; Síndrome Var → Var Syndrome; Ondas Fold → Fold Waves;
          Esquadrão Delta → Delta Flight; Cavaleiros Aéreos → Aerial Knights;
          Heimdal → Heimdall; Yami Q Ray → Yami_Q_Ray; Gigante Macross → Macross Gigant;
          Valquíria → Valkyrie.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "macross_delta_filmes";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta (Filmes) - Revisao de Lore";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Delta na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local Delta.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossDeltaRevisao.mapa();
    }
}
