package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamF91 implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam F91.
        - Personagens: Seabook Arno (homem), Cecily Fairchild / Berah Ronah (mulher), Carozzo Ronah / Iron Mask (homem), Zabine Chareux (homem), Annamarie Bourget (mulher).
        - Mechas / Termos: F91 Gundam Formula 91, Crossbone Vanguard, VSBR (Variable Speed Beam Rifle), MEPE (Afterimage Effect).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam F91", LORE);

    @Override public String getId() { return "gundam_f91"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam F91"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
