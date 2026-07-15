package org.traducao.projeto.traducao.contexto.sidonia;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoSidoniaFilme implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Knights of Sidonia: Love Woven in the Stars (Filme)", "- Obra: Knights of Sidonia: Love Woven in the Stars (Sidonia no Kishi: Ai Tsumugu Hoshi).\n- Personagens: Nagate Tanikaze, Tsumugi Shiraui, Izana Shinoshinari, Yuhata Midorikawa, Kobayashi.");
    @Override public String getId() { return "sidonia_movie"; }
    @Override public String getNomeExibicao() { return "Knights of Sidonia: Love Woven in the Stars (Filme)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
