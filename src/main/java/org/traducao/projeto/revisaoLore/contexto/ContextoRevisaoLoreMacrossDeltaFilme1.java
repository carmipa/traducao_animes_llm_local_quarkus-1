package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore excepcional para Passionate Walküre (filme 1).
 *
 * <p>INVARIANTES DO DOMÍNIO: canônicos Delta; sem Heimdall/Yami_Q_Ray (filme 2).
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDeltaFilme1 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta the Movie: Passionate Walküre (Gekijou no Walküre).
        - Regra: releitura da serie — NAO misturar com Absolute Live!!!!!! (filme 2).
        - Walküre ≠ Valkyrie.

        === Roster ===
        - Walküre: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler.
        - Delta Flight: Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang, Messer Ihlefeld.
        - Aerial Knights: Keith Aero Windermere, Heinz Nerich Windermere, Roid Brehm, Bogue Con-Vaart.

        === Termos ===
        - Passionate Walküre; Walküre; Var Syndrome; Fold Waves; Delta Flight; Aerial Knights;
          Chaos; NUNS; Protoculture; VF-31 Siegfried; Sv-262 Draken III; Macross Elysion.
        - GERWALK/Battroid/Fighter Mode oficiais; proibido Veritech.

        === Formas-ruim ===
        - Síndrome Var → Var Syndrome; Ondas Fold → Fold Waves; Esquadrão Delta → Delta Flight;
          Cavaleiros Aéreos → Aerial Knights; Walkure → Walküre; Valquíria → Valkyrie.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_delta_filme1"; }
    @Override public String getNomeExibicao() { return "Macross Delta: Filme 1 - Passionate Walküre - Revisao de Lore"; }
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
