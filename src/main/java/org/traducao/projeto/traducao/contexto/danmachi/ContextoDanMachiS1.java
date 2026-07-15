package org.traducao.projeto.traducao.contexto.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoDanMachiS1 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("DanMachi (Season 1)", "- Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon? Season 1 (DanMachi 1).\n- Personagens: Bell Cranel, Hestia, Lilisuka Arde, Welf Crozzo, Yamato Mikoto, Ais Wallenstein.");
    @Override public String getId() { return "danmachi_s1"; }
    @Override public String getNomeExibicao() { return "DanMachi (Season 1)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
