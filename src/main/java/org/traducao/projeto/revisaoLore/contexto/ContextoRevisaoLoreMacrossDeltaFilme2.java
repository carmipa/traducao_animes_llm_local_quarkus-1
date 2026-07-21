package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Absolute Live!!!!!! (filme 2).
 *
 * <p>INVARIANTES DO DOMÍNIO: Heimdall; Yami_Q_Ray + prefixo Yami; Macross Gigant;
 * não fundir com Passionate Walküre.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDeltaFilme2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta the Movie: Absolute Live!!!!!! (Zettai Live!!!!!!).
        - Continuidade INEDITA pos-serie — NAO e condensacao do filme 1 (Passionate Walküre).
        - Walküre ≠ Valkyrie.

        === Ameaca ===
        - Heimdall (Ian Cromwell); Yami_Q_Ray (manter underscore).
        - Yami Mikumo, Yami Freyja, Yami Kaname, Yami Makina, Yami Reina — prefixo Yami obrigatorio.

        === Roster aliado ===
        - Walküre: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Delta Flight: Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang, Messer Ihlefeld.
        - Maximilian Jenius / Max Jenius; Lady M; Bogue Con-Vaart (pode ser Delta 6).

        === Mecha / termos ===
        - VF-31AX Kairos-Plus; Macross Gigant; Absolute Live; Star Singer;
          Fold Waves; Chaos; NUNS; Walküre; Protoculture.
        - GERWALK/Battroid/Fighter Mode; proibido Veritech.

        === Formas-ruim (mapa Delta) ===
        - Síndrome Var → Var Syndrome; Ondas Fold → Fold Waves; Walkure → Walküre;
          Valquíria → Valkyrie; Esquadrão Delta → Delta Flight.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_delta_filme2"; }
    @Override public String getNomeExibicao() { return "Macross Delta: Filme 2 - Absolute Live!!!!!! - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Delta na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossDeltaRevisao.mapa();
    }
}
