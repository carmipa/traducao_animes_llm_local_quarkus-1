package org.traducao.projeto.contexto.lore.evangelion;

import org.springframework.stereotype.Component;
import org.traducao.projeto.contexto.domain.ContextoPrompt;
import org.traducao.projeto.contexto.domain.ProvedorContexto;

@Component
public class ContextoEvangelion222 implements ProvedorContexto {
    private static final String PROMPT = ContextoPrompt.montar("Evangelion: 2.22 You Can (Not) Advance", "- Obra: Evangelion: 2.22 You Can (Not) Advance (Evangelion: 2.0).\n- Personagens: Shinji Ikari, Asuka Shikinami Langley, Mari Illustrious Makinami, Rei Ayanami, Misato Katsuragi.\n- Mecha/Termos: EVA Unit-02, EVA Unit-03, Beast Mode, Zeruel, Near Third Impact.");
    @Override public String getId() { return "evangelion_222"; }
    @Override public String getNomeExibicao() { return "Evangelion: Filme 2 - 2.22 You Can (Not) Advance"; }
    @Override public String obterPromptSistema() { return PROMPT; }
}
