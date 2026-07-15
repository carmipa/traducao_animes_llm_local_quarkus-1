package org.traducao.projeto.traducao.contexto.gundam;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGundamVictory implements ProvedorContexto {

    private static final String LORE = """
        - Obra: Mobile Suit Victory Gundam.
        - Personagens: Uso Ewin (homem), Shakti Kareen (mulher), Marbet Fingerhat (mulher), Chronicle Asher (homem), Katejina Loos (mulher), Maria Pure Armonia (mulher), Fonse Kagatie (homem).
        - Mechas / Termos: LM312V04 Victory Gundam, Victory 2 Gundam (V2), Imperio Zanscare, Liga Militar (League Militaire), Angel Halo.
        """;

    private static final String PROMPT = ContextoPrompt.montar("Mobile Suit Victory Gundam", LORE);

    @Override public String getId() { return "gundam_victory"; }
    @Override public String getNomeExibicao() { return "Mobile Suit Victory Gundam"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
