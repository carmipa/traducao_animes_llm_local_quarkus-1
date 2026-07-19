package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore excepcional de Macross Delta (série TV) — roster completo,
 * núcleo Macross e mapa determinístico Delta.
 *
 * <p>INVARIANTES DO DOMÍNIO: Walküre ≠ Valkyrie; Var Syndrome; Delta Flight; Aerial Knights;
 * Windermere; proibido Veritech/Robotech.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoMacrossDelta implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta (serie TV, 2067, aglomerado Brisingr / Brísingr).
        - Premissa: Var Syndrome provoca furia em humanos e Zentradi; a Unidade Tatica Musical
          Walküre usa Fold Waves (cancao) sob protecao do Delta Flight (Chaos) contra o Reino
          de Windermere e seus Aerial Knights.

        === Walküre (idol / todas femininas) — NUNCA traduzir o nome do grupo ===
        - Freyja Wion (f) — Windermere; cantora emotiva.
        - Mikumo Guynemer (f) — diva principal / misteriosa.
        - Kaname Buccaneer (f) — lider da Walküre.
        - Makina Nakajima (f) — mecanica e cantora.
        - Reina Prowler (f) — hacker e cantora.

        === Delta Flight (Chaos / VF-31 Siegfried) ===
        - Hayate Immelmann (m) — piloto impulsivo.
        - Mirage Farina Jenius (f) — piloto de elite; neta de Max e Milia Jenius.
        - Arad Mölders / Arad Molders (m) — comandante do Delta Flight (Major Arad).
        - Chuck Mustang (m) — piloto e cozinheiro.
        - Messer Ihlefeld (m) — "As Branco".

        === Aerial Knights / Reino de Windermere ===
        - Keith Aero Windermere (m) — Cavaleiro Branco.
        - Heinz Nerich Windermere (m) — principe cantor (voz Fold).
        - Roid Brehm (m) — chanceler; plano Protoculture / Var.
        - Bogue Con-Vaart / Bogue Convaart (m).
        - Cassim Eberhard (m), Herman Kroos (m), Theodore Riddle (m) quando aparecerem.

        === Organizacoes / lugares / termos ===
        - Chaos (PMC); NUNS (New United Nations Spacy); New United Nations Government.
        - Walküre; Delta Flight; Aerial Knights; Windermere Kingdom; planeta Ragna; Al Shahal.
        - Var Syndrome (NUNCA so "Sindrome Var" como nome canonico — restaurar Var Syndrome).
        - Fold Waves / Bio-Fold Waves; Protoculture (NUNCA localizar como mitologia generica).
        - Deculture quando o dialogo trouxer.

        === Mecha / naves ===
        - VF-31 Siegfried (Delta Flight); Sv-262 Draken III (Aerial Knights); Macross Elysion.
        - Variable Fighter / Valkyrie (mecha) — distinto de Walküre (grupo).
        - Modos: Fighter Mode, GERWALK Mode, Battroid Mode — NUNCA traduzir os nomes dos modos.
        - Overtechnology; Reaction Weaponry.

        === Nucleo Macross (obrigatorio) ===
        - PROIBIDO lexico Robotech (Veritech etc.).
        - Musica Walküre = interferencia psicologica / fold real — nao "fundo musical" / OST.
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
     * <p>INVARIANTES DO DOMÍNIO: grafias oficiais; Walküre ≠ Valkyrie.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Freyja Wion", "Mikumo Guynemer", "Kaname Buccaneer", "Makina Nakajima",
            "Reina Prowler", "Hayate Immelmann", "Mirage Farina Jenius", "Arad Molders",
            "Chuck Mustang", "Messer Ihlefeld", "Keith Aero Windermere",
            "Heinz Nerich Windermere", "Roid Brehm", "Bogue Con-Vaart", "Cassim Eberhard",
            "Herman Kroos", "Walküre", "Windermere", "Var Syndrome", "Delta Flight",
            "Aerial Knights", "Chaos", "NUNS", "Fold Waves", "Protoculture", "VF-31 Siegfried",
            "Sv-262 Draken III", "Macross Elysion", "GERWALK", "Battroid", "Valkyrie",
            "Overtechnology", "Zentradi", "Deculture", "Brisingr"
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
