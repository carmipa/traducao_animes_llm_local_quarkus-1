package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore completa de Macross Delta (série TV, 2067 / Brisingr) —
 * Walküre, Delta Flight, Aerial Knights e Var Syndrome.
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre ≠ Valkyrie; Var Syndrome; Delta Flight; Aerial Knights;
 * Windermere; Protoculture; proibido Veritech/Robotech. Filmes 1/2 têm contextos próprios.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoMacrossDelta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta (serie TV, 2067, aglomerado Brisingr).
        - Premissa: Var Syndrome provoca furia em humanos e Zentradi; a Unidade Tatica Musical
          Walküre usa Fold Waves (cancao) sob protecao do Delta Flight (Chaos) contra o
          Windermere Kingdom e seus Aerial Knights.
        - Filmes Passionate Walküre / Absolute Live!!!!!! tem contextos separados — nao misturar
          Heimdall / Yami_Q_Ray aqui.

        === Walküre (idol / todas femininas) — NUNCA traduzir o nome do grupo ===
        - Freyja Wion (f) — Windermere; cantora emotiva.
        - Mikumo Guynemer (f) — diva principal / misteriosa.
        - Kaname Buccaneer (f) — lider da Walküre.
        - Makina Nakajima (f) — mecanica e cantora.
        - Reina Prowler (f) — hacker e cantora.

        === Delta Flight (Chaos / VF-31 Siegfried) ===
        - Hayate Immelmann (m) — piloto impulsivo.
        - Mirage Farina Jenius (f) — piloto de elite; neta de Max e Milia Jenius.
        - Arad Molders / Arad Mölders (m) — Major Arad, comandante do Delta Flight.
        - Chuck Mustang (m); Messer Ihlefeld (m) — "As Branco".
        - Ernest Johnson (m) — capitao da Macross Elysion quando aparecer.
        - Berger Stone (m) — Chaos / inteligencia quando aparecer.

        === Aerial Knights / Windermere Kingdom ===
        - Keith Aero Windermere (m) — Cavaleiro Branco.
        - Heinz Nerich Windermere (m) — principe cantor (voz Fold).
        - Roid Brehm (m) — chanceler; plano Protoculture / Var.
        - Bogue Con-Vaart (m); Cassim Eberhard (m); Herman Kroos (m);
          Theodore Riddle (m) quando aparecerem.

        === Orgs / lugares / termos ===
        - Chaos (PMC); NUNS / New United Nations Spacy; New United Nations Government.
        - Walküre; Delta Flight; Aerial Knights; Windermere Kingdom;
          planeta Ragna; Al Shahal; Brisingr.
        - Var Syndrome (NUNCA "Sindrome Var" como nome canonico).
        - Fold Waves / Bio-Fold Waves; Protoculture; Deculture quando o dialogo trouxer.

        === Mecha / naves ===
        - VF-31 Siegfried; Sv-262 Draken III; Macross Elysion.
        - Variable Fighter / Valkyrie (mecha) — distinto de Walküre (grupo).
        - Fighter Mode / GERWALK Mode / Battroid Mode — NUNCA traduzir nomes dos modos.
        - Overtechnology; Reaction Weaponry.

        === Regras duras ===
        - Walküre ≠ Valkyrie; PROIBIDO Veritech / Robotech.
        - Musica Walküre = interferencia psicologica / fold — nao "OST".
        - Tom: idol + mecha militar; Freyja emotiva; Hayate impulsivo; Keith nobre/hostil; Roid ideologo.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Delta (Série TV)", LORE);

    @Override
    public String getId() {
        return "macross_delta";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta (Série TV)";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege elenco Walküre / Delta Flight / Aerial Knights e termos Delta.
     *
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais; Walküre ≠ Valkyrie; sem Heimdall/Yami.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Freyja Wion", "Mikumo Guynemer", "Kaname Buccaneer", "Makina Nakajima",
            "Reina Prowler", "Hayate Immelmann", "Mirage Farina Jenius",
            "Arad Molders", "Arad Mölders", "Chuck Mustang", "Messer Ihlefeld",
            "Ernest Johnson", "Berger Stone",
            "Keith Aero Windermere", "Heinz Nerich Windermere", "Roid Brehm",
            "Bogue Con-Vaart", "Cassim Eberhard", "Herman Kroos", "Theodore Riddle",
            "Walküre", "Windermere", "Windermere Kingdom", "Var Syndrome",
            "Delta Flight", "Aerial Knights", "Chaos", "NUNS",
            "New United Nations Spacy", "Fold Waves", "Bio-Fold Waves",
            "Protoculture", "Deculture", "VF-31 Siegfried", "Sv-262 Draken III",
            "Macross Elysion", "GERWALK", "Battroid", "Valkyrie", "Variable Fighter",
            "Overtechnology", "Reaction Weaponry", "Zentradi", "Brisingr",
            "Ragna", "Al Shahal"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Delta (Valkyrie/Walküre/Var/Fold/Aerial Knights).
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
