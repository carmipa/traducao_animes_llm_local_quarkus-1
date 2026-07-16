package org.traducao.projeto.contexto.lore.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGundamSEEDStargazer implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Gundam SEED C.E. 73: Stargazer (OVA).
        - Personagens: Selene McGriff (mulher / pesquisadora da DSSD), Sven Cal Bayang (homem / piloto do Phantom Pain), Sol Ryuune L'ange (homem).
        - Mechas / Termos: GSX-401FW Stargazer Gundam, GAT-X105E Strike Noir Gundam, Phantom Pain, DSSD (Deep Space Survey and Development Organization).
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Gundam SEED C.E. 73: Stargazer", LORE);

    @Override public String getId() { return "gundam_seed_stargazer"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Gundam SEED C.E. 73: Stargazer"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
