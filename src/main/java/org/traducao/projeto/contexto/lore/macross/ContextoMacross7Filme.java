package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacross7Filme implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross 7: The Galaxy's Calling Me! (Ginga ga Ore wo Yonde Iru!).
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Pedro (homem), Emilia Jenius (mulher / irma de Mylene), Naughty (homem).
        - Banda: Fire Bomber.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross 7: The Galaxy's Calling Me!", LORE);

    @Override public String getId() { return "macross_7_filme"; }
    @Override public String getNomeExibicao() { return "Macross 7: The Galaxy's Calling Me!"; }
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
