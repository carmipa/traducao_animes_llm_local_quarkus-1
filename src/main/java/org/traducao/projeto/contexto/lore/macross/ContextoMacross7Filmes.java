package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacross7Filmes implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross 7 (Filmes e OVAs: Macross 7 The Movie - The Galaxy is Calling Me! / Dynamite 7 / Encore).
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Ray Lovelock (homem), Veffidas Feaze (mulher), Gamlin Kizaki (homem), Pedro (homem), Elma (mulher), Graham (homem).
        - Banda / Musicas: Fire Bomber, canciones de Basara.
        - Mechas / Criaturas: VF-19 Custom Fire Valkyrie, VF-22 Sturmvogel II, Anima Spiritia, Galácticos / Baleias Espaciais (Space Whales / Elma).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross 7 (Filmes & OVAs)", LORE);

    @Override public String getId() { return "macross_7_filmes"; }
    @Override public String getNomeExibicao() { return "Macross 7: Filmes & OVAs (Dynamite 7 / Encore)"; }
    @Override public String obterPromptSistema() { return PROMPT; }

    /**
     * PROPÓSITO DE NEGÓCIO: restaura grafias oficiais Macross (Valkyrie/Zentradi) quando
     * o LLM localiza indevidamente — mapa compartilhado da franquia.
     *
     * <p>INVARIANTES DO DOMÍNIO: só aplica com canônico no original EN.
     *
     * <p>COMPORTAMENTO EM CASO DE FALHA: mapa imutável; sem I/O.
     */
    @Override
    public java.util.Map<String, String> correcoesTerminologia() {
        return CorrecoesTerminologiaMacross.mapa();
    }

}
