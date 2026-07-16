package org.traducao.projeto.contexto.lore.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoDanMachiS2 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 2)", "- Obra: DanMachi Season 2.\n- Personagens: Bell Cranel, Hestia, Lili, Welf, Haruhime, Freya, Hermes.");
    @Override public String getId() { return "danmachi_s2"; }
    @Override public String getNomeExibicao() { return "DanMachi (Season 2)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
