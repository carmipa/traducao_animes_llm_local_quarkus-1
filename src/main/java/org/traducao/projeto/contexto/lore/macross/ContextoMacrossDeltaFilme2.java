package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

import java.util.Map;
import java.util.Set;

/**
 * PROPÓSITO DE NEGÓCIO: lore excepcional do filme Macross Delta — Absolute Live!!!!!!
 * (continuação inédita pós-série: Heimdall, Yami_Q_Ray, legado Jenius).
 *
 * <p>INVARIANTES DO DOMÍNIO: Heimdall; Yami_Q_Ray + prefixo Yami; VF-31AX Kairos-Plus;
 * Macross Gigant; NÃO fundir com Passionate Walküre (filme 1).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; termos e mapa imutáveis.
 */
@Component
public class ContextoMacrossDeltaFilme2 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Delta the Movie: Absolute Live!!!!!!
          (劇場版マクロスΔ 絶対LIVE!!!!!! / Zettai Live!!!!!!).
        - Continuidade: historia INEDITA pos-serie TV (nao e condensacao do filme 1).
        - NAO misturar com Passionate Walküre (filme 1 — foco Windermere / Var / Elysion).

        === Ameaca ===
        - Heimdall — organizacao clandestina (Ian Cromwell).
        - Yami_Q_Ray — unidade idol/cibernetica rival; manter underscore e grafia Yami_Q_Ray.
        - Espelhos Yami (femininas): Yami Mikumo, Yami Freyja, Yami Kaname, Yami Makina, Yami Reina.
          Sempre prefixo "Yami" + nome; nao traduzir Yami.

        === Walküre (femininas) ===
        - Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.

        === Delta Flight / legado ===
        - Hayate Immelmann (m), Mirage Farina Jenius (f), Arad Molders (m),
          Chuck Mustang (m), Messer Ihlefeld (m) quando aparecerem.
        - Bogue Con-Vaart (m) — pode atuar com Delta Flight (callsign Delta 6) nesta obra.
        - Maximilian Jenius / Max Jenius (m) — capitao da Macross Gigant.
        - Lady M (f) quando o dialogo trouxer.
        - Ian Cromwell (m) — lider Heimdall.

        === Mecha / naves ===
        - VF-31AX Kairos-Plus; VF-31 Siegfried / variantes conforme cena.
        - Sv-262 Draken III / variantes; Macross Gigant.
        - Fighter / GERWALK / Battroid — NUNCA traduzir nomes dos modos.
        - Walküre (grupo) ≠ Valkyrie (mecha). PROIBIDO Veritech.

        === Termos ===
        - Absolute Live; Heimdall; Yami_Q_Ray; Fold Waves; Chaos; NUNS; Walküre.
        - Tom: show ao vivo + combate; rivalidade Walküre vs Yami_Q_Ray; legado Jenius.
        """;

    private static final String PROMPT = ContextoPrompt.montar(
        "Macross Delta: Filme 2 - Absolute Live!!!!!!", LORE);

    @Override
    public String getId() {
        return "macross_delta_filme2";
    }

    @Override
    public String getNomeExibicao() {
        return "Macross Delta: Filme 2 - Absolute Live!!!!!!";
    }

    @Override
    public String obterPromptSistema() {
        return PROMPT;
    }

    /**
     * PROPÓSITO DE NEGÓCIO: protege roster Absolute Live (Heimdall / Yami_Q_Ray / Gigant).
     *
     * <p>INVARIANTES DO DOMÍNIO: prefixo Yami e Yami_Q_Ray intactos.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: conjunto imutável.
     */
    @Override
    public Set<String> termosProtegidos() {
        return Set.of(
            "Freyja Wion", "Mikumo Guynemer", "Kaname Buccaneer", "Makina Nakajima",
            "Reina Prowler", "Hayate Immelmann", "Mirage Farina Jenius", "Arad Molders",
            "Chuck Mustang", "Messer Ihlefeld", "Bogue Con-Vaart",
            "Maximilian Jenius", "Max Jenius", "Ian Cromwell", "Lady M",
            "Heimdall", "Yami_Q_Ray", "Yami Mikumo", "Yami Freyja", "Yami Kaname",
            "Yami Makina", "Yami Reina", "VF-31AX Kairos-Plus", "Macross Gigant",
            "Walküre", "Delta Flight", "Chaos", "NUNS", "Fold Waves", "Valkyrie",
            "GERWALK", "Battroid", "Absolute Live"
        );
    }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Delta + formas Absolute Live via mesmo mapa.
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
