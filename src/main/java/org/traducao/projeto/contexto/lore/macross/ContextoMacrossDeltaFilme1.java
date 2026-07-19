package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore excepcional do filme Macross Delta — Passionate Walküre
 * (releitura cinematográfica da guerra Windermere / Var).
 *
 * <p>INVARIANTES DO DOMÍNIO: mesmos canônicos da série Delta; NÃO misturar com Absolute Live
 * (filme 2: Heimdall / Yami_Q_Ray).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoMacrossDeltaFilme1 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta the Movie: Passionate Walküre
          (劇場版マクロスΔ 激情のワルキューレ / Gekijou no Walküre).
        - Natureza: releitura cinematografica dos eventos da serie TV (2067, Brisingr) —
          Walküre + Delta Flight vs Reino de Windermere / Aerial Knights / plano Var + Fold Waves.
        - NAO confundir com Absolute Live!!!!!! (filme 2 — Heimdall, Yami_Q_Ray, Macross Gigant).

        === Walküre (femininas) ===
        - Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.

        === Delta Flight (Chaos / VF-31 Siegfried) ===
        - Hayate Immelmann (m), Mirage Farina Jenius (f), Arad Molders (m),
          Chuck Mustang (m), Messer Ihlefeld (m).

        === Aerial Knights / Windermere ===
        - Keith Aero Windermere (m), Heinz Nerich Windermere (m), Roid Brehm (m),
          Bogue Con-Vaart (m), Cassim Eberhard (m), Herman Kroos (m).

        === Orgs / termos / lugares ===
        - Chaos, NUNS, Walküre, Delta Flight, Aerial Knights, Windermere Kingdom,
          Var Syndrome, Fold Waves, Protoculture, planeta Ragna, Al Shahal.
        - Walküre (grupo) ≠ Valkyrie (mecha Variable Fighter).

        === Mecha / naves ===
        - VF-31 Siegfried, Sv-262 Draken III, Macross Elysion.
        - Fighter / GERWALK / Battroid Mode — NUNCA traduzir nomes dos modos.
        - PROIBIDO Veritech / Robotech.

        === Tom ===
        - Idol + mecha cinematografico; Freyja emotiva; Hayate impulsivo; Keith nobre/hostil;
          Roid ideologo Protoculture; cancoes cantaveis sem notas editoriais.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Macross Delta: Filme 1 - Passionate Walküre", LORE);

    @Override
    public String getId() {
        return "macross_delta_filme1";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta: Filme 1 - Passionate Walküre";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster e termos do filme Passionate Walküre.
     *
     * <p>INVARIANTES DO DOMÍNIO: sem Heimdall/Yami_Q_Ray (exclusivos do filme 2).
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
            "Walküre", "Windermere", "Var Syndrome", "Delta Flight", "Aerial Knights",
            "Chaos", "NUNS", "Fold Waves", "Protoculture", "VF-31 Siegfried",
            "Sv-262 Draken III", "Macross Elysion", "GERWALK", "Battroid", "Valkyrie",
            "Overtechnology", "Passionate Walküre"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Delta (mesmo da série TV).
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
