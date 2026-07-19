package org.traducao.projeto.contexto.lore.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoMacrossDynamite7 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross Dynamite 7 (OVA).
        - Personagens: Basara Nekki (homem), Elma (mulher), Graham (homem), Liza Hoyly (mulher).
        - Termos: Planeta Zola, Baleias Espaciais (Space Whales), Galactic Whales.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross Dynamite 7", LORE);

    @Override public String getId() { return "macross_dynamite_7"; }
    @Override public String getNomeExibicao() { return "Macross Dynamite 7"; }
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
