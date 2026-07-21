package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore completa para Macross Delta (série TV).
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre ≠ Valkyrie; Var Syndrome; Delta Flight; Aerial Knights;
 * sem Heimdall/Yami (filmes 2).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDelta implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta (serie TV, 2067, Brisingr).
        - Regra: corrigir APENAS nomenclatura. Walküre (grupo) ≠ Valkyrie (mecha).
        - Filmes 1/2 tem contextos separados — nao forcar Heimdall/Yami_Q_Ray aqui.

        === Walküre (femininas) ===
        - Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.

        === Delta Flight ===
        - Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang, Messer Ihlefeld;
          Ernest Johnson; Berger Stone.

        === Aerial Knights / Windermere Kingdom ===
        - Keith Aero Windermere, Heinz Nerich Windermere, Roid Brehm, Bogue Con-Vaart,
          Cassim Eberhard, Herman Kroos, Theodore Riddle.

        === Termos canonicos ===
        - Walküre (NUNCA Walkure ASCII se o oficial for Walküre); Var Syndrome;
          Fold Waves / Bio-Fold Waves; Delta Flight; Aerial Knights; Chaos; NUNS;
          Protoculture; Windermere Kingdom; Ragna; Al Shahal;
          VF-31 Siegfried; Sv-262 Draken III; Macross Elysion.
        - GERWALK / Battroid / Fighter Mode — nao traduzir nomes dos modos.
        - PROIBIDO Veritech. Zentradi grafia oficial.

        === Formas-ruim ===
        - Síndrome Var → Var Syndrome; Ondas Fold → Fold Waves;
          Esquadrão Delta → Delta Flight; Cavaleiros Aéreos → Aerial Knights;
          Reino de Windermere → Windermere Kingdom;
          Valquíria → Valkyrie (mecha); Walkure → Walküre (grupo).
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override
    public String getId() {
        return "macross_delta";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta (Série TV) - Revisao de Lore";
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
