package org.traducao.projeto.contexto.lore.macross;

import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore agregada dos filmes Macross Delta (Passionate Walküre +
 * Absolute Live!!!!!!) — referência/agregadora, sem {@code @Component} (fora dos 53 CDI).
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre ≠ Valkyrie; filme 1 ≠ filme 2 — preferir contextos
 * específicos ({@code macross_delta_filme1}/{@code macross_delta_filme2});
 * Heimdall/Yami_Q_Ray só do Absolute Live. Não entra no manifesto E7a.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
public class ContextoMacrossDeltaFilmes implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta (Filmes) — Passionate Walküre + Absolute Live!!!!!!
        - Preferir contextos separados (filme1 / filme2) quando o arquivo for so um dos filmes.
        - Walküre ≠ Valkyrie. PROIBIDO Veritech.

        === Passionate Walküre (filme 1) ===
        - Releitura da guerra Windermere / Var Syndrome / Fold Waves.
        - Walküre + Delta Flight vs Aerial Knights / Windermere Kingdom.
        - Mecha: VF-31 Siegfried; Sv-262 Draken III; Macross Elysion.
        - SEM Heimdall / Yami_Q_Ray / Macross Gigant neste arco.

        === Absolute Live!!!!!! (filme 2) ===
        - Continuidade INEDITA pos-serie: Heimdall (Ian Cromwell), Yami_Q_Ray (+ Yami *),
          Max Jenius / Macross Gigant, VF-31AX Kairos-Plus, Star Singer.
        - NAO tratar como condensacao do filme 1.

        === Roster comum ===
        - Walküre: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Delta Flight: Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang,
          Messer Ihlefeld.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Delta (Filmes)", LORE);

    @Override
    public String getId() {
        return "macross_delta_filmes";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta (Filmes)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: une termos críticos dos dois filmes Delta.
     *
     * <p>INVARIANTES DO DOMÍNIO: Yami_Q_Ray / Heimdall / Passionate Walküre oficiais.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Freyja Wion", "Mikumo Guynemer", "Kaname Buccaneer", "Makina Nakajima",
            "Reina Prowler", "Hayate Immelmann", "Mirage Farina Jenius", "Arad Molders",
            "Chuck Mustang", "Messer Ihlefeld", "Keith Aero Windermere",
            "Heinz Nerich Windermere", "Roid Brehm", "Bogue Con-Vaart",
            "Maximilian Jenius", "Max Jenius", "Ian Cromwell", "Lady M",
            "Heimdall", "Yami_Q_Ray", "Yami Mikumo", "Yami Freyja", "Yami Kaname",
            "Yami Makina", "Yami Reina", "Star Singer",
            "Walküre", "Var Syndrome", "Delta Flight", "Aerial Knights",
            "Fold Waves", "Protoculture", "Chaos", "NUNS",
            "VF-31 Siegfried", "VF-31AX Kairos-Plus", "Sv-262 Draken III",
            "Macross Elysion", "Macross Gigant", "Passionate Walküre", "Absolute Live",
            "GERWALK", "Battroid", "Valkyrie", "Windermere Kingdom"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa Delta completo (TV + filmes).
     *
     * <p>INVARIANTES DO DOMÍNIO: {@link CorrecoesTerminologiaMacrossDelta}.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossDelta.mapa();
    }
}
