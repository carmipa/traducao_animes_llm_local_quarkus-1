package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Frontier.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossFrontier implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Frontier.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Alto Saotome, Sheryl Nome, Ranka Lee, Michael Blanc, Luca Angeloni, Klan Klang, Ozma Lee, Brera Sterne, Vajra, SMS, NUNS, VF-25 Messiah, GERWALK, Battroid, Valkyrie, Fold, Overtechnology.
        - Premissa: a frota emigrante Macross Frontier enfrenta a ameaça alienígena Vajra;
        - Personagens (gênero): Alto Saotome (m), Sheryl Nome (f), Ranka Lee (f),
        - Organizações/termos: SMS (Strategic Military Services), NUNS (New United Nations Spacy),
        - Mecha/naves: VF-25 Messiah, VF-27 Lucifer, Queadluun-Rhea, Macross Quarter, Battle Frontier.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_frontier"; }
    @Override public String getNomeExibicao() { return "Macross Frontier - Revisao de Lore"; }
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
