package org.traducao.projeto.traducao.contexto.macross;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoMacross7Encore implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Macross 7 Encore (OVA).
        - Personagens: Basara Nekki (homem), Mylene Flare Jenius (mulher), Ray Lovelock (homem), Veffidas Feaze (mulher), Gamlin Kizaki (homem), Gigil (homem), Sivil (mulher).
        - Banda: Fire Bomber.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Macross 7 Encore", LORE);

    @Override public String getId() { return "macross_7_encore"; }
    @Override public String getNomeExibicao() { return "Macross 7 Encore"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
