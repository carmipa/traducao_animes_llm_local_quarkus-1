package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Delta: Filme 1 - Passionate Walküre.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDeltaFilme1 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta: Filme 1 - Passionate Walküre.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler, Hayate Immelmann, Mirage Farina Jenius, Messer Ihlefeld, Keith Aero Windermere, Heinz Nerich Windermere, Walküre, Windermere, Var Syndrome, VF-31 Siegfried, GERWALK, Battroid, Overtechnology, Fold.
        - Unidade Tática Musical Walküre (todas femininas): Freyja Wion, Mikumo Guynemer,
        - Esquadrão Delta / Delta Flight: Hayate Immelmann (m), Mirage Farina Jenius (f),
        - Mecha/naves: VF-31 Siegfried, Sv-262 Draken III, Macross Elysion.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_delta_filme1"; }
    @Override public String getNomeExibicao() { return "Macross Delta: Filme 1 - Passionate Walküre - Revisao de Lore"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: mapa determinístico Macross na Opção 7.
     *
     * <p>INVARIANTES DO DOMÍNIO: espelho da Tradução Local.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável.
     */
    @Override
    public Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacrossRevisao.mapa();
    }
}
