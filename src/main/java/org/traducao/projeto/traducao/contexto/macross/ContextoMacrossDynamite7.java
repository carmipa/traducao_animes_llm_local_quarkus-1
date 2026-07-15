package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

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
}
