package org.traducao.projeto.traducao.contexto.guiltycrown;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoGuiltyCrown implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Guilty Crown", "- Obra: Guilty Crown.\n- Personagens: Shu Ouma, Inori Yuzuriha, Gai Tsutsugami, Ayase Shinomiya, Tsugumi, Yahiro Samukawa.\n- Organizações: Undertaker (Funeral Parlor), GHQ, Apocalypse Virus, Void Genome.");
    @Override public String getId() { return "guilty_crown"; }
    @Override public String getNomeExibicao() { return "Guilty Crown"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
