package org.traducao.projeto.contexto.lore.guiltycrown;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoGuiltyCrown implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Guilty Crown", "- Obra: Guilty Crown.\n- Personagens: Shu Ouma, Inori Yuzuriha, Gai Tsutsugami, Ayase Shinomiya, Tsugumi, Yahiro Samukawa.\n- Organizações: Undertaker (Funeral Parlor), GHQ, Apocalypse Virus, Void Genome.");
    @Override public String getId() { return "guilty_crown"; }
    @Override public String getNomeExibicao() { return "Guilty Crown"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
