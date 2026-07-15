package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamHathaway implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam Hathaway.
        - Personagens: Hathaway Noa / Mafty Navue Erin (homem), Gigi Andrade (mulher), Kenneth Sleg (homem), Lane Aim (homem), Gawman Noceria (homem).
        - Mechas / Termos: RX-105 Xi Gundam, RX-104FF Penelope, Organização Terrorista Mafty, Minovsky Flight System.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam Hathaway", LORE);

    @Override public String getId() { return "gundam_hathaway"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam Hathaway"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
