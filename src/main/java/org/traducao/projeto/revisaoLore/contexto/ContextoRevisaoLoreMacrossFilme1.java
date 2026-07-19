package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross: Do You Remember Love?.
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossFilme1 implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross: Do You Remember Love?.
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross, You Remember Love, Oboete Imasu, Zentradi, Meltrandi, Termos, SDF, Protocultura, Valkyrie, Variable Fighter, Minmay Attack, Principais, Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Maximilian Jenius, Max Jenius, Milia Fallyna, Exsedol Folmo, Boddole Zer, Britai Kridanik, Mechas, Strike Valkyrie, Queadluun, Rau, Nousjadeul, Ger, Use, Tom.
        - Termos centrais: SDF-1 Macross, Zentradi, Meltrandi, Protocultura, Valkyrie, Variable Fighter, Minmay Attack.
        - Principais nomes: Hikaru Ichijyo, Lynn Minmay, Misa Hayase, Roy Focker, Maximilian Jenius/Max Jenius, Milia Fallyna, Exsedol Folmo, Boddole Zer, Britai Kridanik.
        - Mechas/naves: VF-1 Valkyrie, VF-1S Strike Valkyrie, Queadluun-Rau, Nousjadeul-Ger, SDF-1.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_filme1"; }
    @Override public String getNomeExibicao() { return "Macross: Do You Remember Love? - Revisao de Lore"; }
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
