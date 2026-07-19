package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Flash Back 2012.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossFilme2 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Flash Back 2012.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross, You Remember Love, Flash Back, Preserve, SDF, Zentradi, Meltrandi, Protocultura, Valkyrie, Variable Fighter, Principais, Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Max Jenius, Milia Fallyna, Mechas, Queadluun, Rau, Regult, Tom.
        - Principais nomes recorrentes: Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Max Jenius, Milia Fallyna.
        - Mechas/naves: VF-1 Valkyrie, SDF-1, Queadluun-Rau, Regult.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_filme2"; }
    @Override public String getNomeExibicao() { return "Macross Flash Back 2012 - Revisao de Lore"; }
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
