package org.traducao.projeto.revisaoLore.contexto;

import org.springframework.stereotype.Component;
import org.traducao.projeto.revisaoLore.application.PromptRevisaoLore;
import org.traducao.projeto.revisaoLore.domain.ports.ProvedorPromptRevisaoLore;

import java.util.Map;

/**
 * PROPÓSITO DE NEGÓCIO: Revisao de Lore lean para Macross 7: Filmes & OVAs (Dynamite 7 / Encore).
 *
 * <p>INVARIANTES DO DOMÍNIO: Valkyrie/Zentradi oficiais; sem Veritech/Valquíria.
 *
 * <p>COMPORTAMENTO EM CASO DE FALHA: sem I/O; prompt e mapa imutáveis.
 */
@Component
public class ContextoRevisaoLoreMacross7Filmes implements ProvedorPromptRevisaoLore {

    private static final String LORE = """
        - Obra: Macross 7: Filmes & OVAs (Dynamite 7 / Encore).
        - Regra: nomes canonicos NAO sao localizados. Corrija so grafia de lore.
        - Nomes/termos: Macross, OVAs, The Movie, The Galaxy, Calling, Dynamite, Encore, Basara Nekki, Mylene Flare Jenius, Ray Lovelock, Veffidas Feaze, Gamlin Kizaki, Pedro, Elma, Graham, Banda, Musicas, Fire Bomber, Basara, Mechas, Criaturas, Custom Fire Valkyrie, Sturmvogel, Anima Spiritia, Baleias Espaciais, Space Whales.
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Ray Lovelock (homem), Veffidas Feaze (mulher), Gamlin Kizaki (homem), Pedro (homem), Elma (mulher), Graham (homem).
        - Banda / Musicas: Fire Bomber, canciones de Basara.
        - Mechas / Criaturas: VF-19 Custom Fire Valkyrie, VF-22 Sturmvogel II, Anima Spiritia, Galácticos / Baleias Espaciais (Space Whales / Elma).
        - Alertas: Valkyrie nao vira Valquiria/Valquíria; Zentradi grafia oficial; proibido Veritech;
          GERWALK/Battroid/Fighter Mode — nao traduzir nomes dos modos.
        """;

    private static final String PROMPT = PromptRevisaoLore.montarPromptSistema(LORE);

    @Override public String getId() { return "macross_7_filmes"; }
    @Override public String getNomeExibicao() { return "Macross 7: Filmes & OVAs (Dynamite 7 / Encore) - Revisao de Lore"; }
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
