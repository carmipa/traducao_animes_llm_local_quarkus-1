package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross Delta (Filmes).
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacrossDeltaFilmes implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross Delta (Filmes).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross Delta, Gekijouban Macross Delta, Gekjou, Walkuere, Zettai Live, Continuidade, Heimdall, Absolute Live, Todas Femininas, Freyja Wion, Mikumo Guynemer, Kaname Buccaneer, Makina Nakajima, Reina Prowler, Unidade Rival, Yami Mikumo, Yami Freyja, Yami Kaname, Yami Makina, Yami Reina, Pilotos, Delta, Hayate Immelmann, Mirage Farina Jenius, Arad Molders, Chuck Mustang, Messer Ihlefeld, Legado, Especiais, Maximilian Jenius, Max Jenius, Macross Gigant, Ian Cromwell, Lady, Termos, Chaos, NUNS, Star Singer, Kairos, Plus.
        - Premissa: Continuidade e releitura cinematográfica dos eventos de Macross Delta, incluindo a batalha contra a unidade cibernética/clonada Yami_Q_Ray e a ameaça de Heimdall em Absolute Live!!!!!!.
        - Unidade Tática Musical Walküre (Todas Femininas): Freyja Wion (mulher), Mikumo Guynemer (mulher), Kaname Buccaneer (mulher), Makina Nakajima (mulher), Reina Prowler (mulher).
        - Unidade Rival Yami_Q_Ray (Absolute Live!!!!!! - Todas Femininas): Yami Mikumo, Yami Freyja, Yami Kaname, Yami Makina, Yami Reina.
        - Pilotos do Esquadrão Delta: Hayate Immelmann (homem), Mirage Farina Jenius (mulher), Arad Molders (homem), Chuck Mustang (homem), Messer Ihlefeld (homem).
        - Personagens de Legado e Especiais: Maximilian Jenius / Max Jenius (homem / capitão lendário da Macross Gigant), Ian Cromwell (homem / líder da Heimdall), Lady M.
        - Organizações e Termos: Chaos, NUNS, Heimdall, Walküre, Yami_Q_Ray, Star Singer, Sv-301t Kairos-Plus, VF-31AX Kairos-Plus, VF-31E Siegfried, Macross Gigant.
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_delta_filmes"; }
    @Override public String getNomeExibicao() { return "Macross Delta (Filmes) - Revisao de Lore"; }
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
