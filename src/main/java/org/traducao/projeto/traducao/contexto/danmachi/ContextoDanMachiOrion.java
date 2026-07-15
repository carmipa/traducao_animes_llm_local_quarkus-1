package org.traducao.projeto.traducao.contexto.danmachi;

import org.springframework.stereotype.Component;
import org.traducao.projeto.traducao.contexto.ContextoPrompt;
import org.traducao.projeto.traducao.domain.ports.ProvedorContexto;

@Component
public class ContextoDanMachiOrion implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("DanMachi: Arrow of the Orion", "- Obra: Is It Wrong to Try to Pick Up Girls in a Dungeon?: Arrow of the Orion (Filme).\n- Personagens: Bell Cranel, Hestia, Artemis, Ais Wallenstein, Hermes, Liriruca Arde.");
    @Override public String getId() { return "danmachi_movie"; }
    @Override public String getNomeExibicao() { return "DanMachi: Arrow of the Orion (Filme)"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
