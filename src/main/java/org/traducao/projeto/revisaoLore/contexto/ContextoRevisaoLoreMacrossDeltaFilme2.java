package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Delta: Filme 2 - Absolute Live!!!!!!.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDeltaFilme2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta: Filme 2 - Absolute Live!!!!!!.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Freyja Wion, Mikumo Guynemer, Hayate Immelmann, Mirage Farina Jenius, Maximilian Jenius, Heimdall, Yami_Q_Ray, Yami Mikumo, Yami Freyja, VF-31AX Kairos-Plus, Macross Gigant, Walküre.
        - Continuidade: inédita pós-série TV de Macross Delta (não é só condensação do filme 1).
        - Pilotos/legado: Hayate Immelmann (m), Mirage Farina Jenius (f), Arad Molders (m),
        - Mecha/naves: VF-31AX Kairos-Plus, Sv-262 variants conforme cena, Macross Gigant.
        - Tom: show ao vivo + combate; rivalidade idol Walküre vs Yami_Q_Ray; legado Jenius.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_delta_filme2"; }
    @Override public String getNomeExibicao() { return "Macross Delta: Filme 2 - Absolute Live!!!!!! - Revisao de Lore"; }
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
